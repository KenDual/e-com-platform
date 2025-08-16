import os
import re
import json
import asyncio
from pathlib import Path
from typing import Any, Dict, List, Optional

import faiss
import numpy as np
import httpx
import pandas as pd
from dotenv import load_dotenv

# ---------------------
# Env & paths
# ---------------------
load_dotenv()
APP_DIR = Path(__file__).resolve().parent


def rp(p: str) -> str:
    path = Path(p)
    return str(path if path.is_absolute() else (APP_DIR / path).resolve())


OLLAMA_URL = os.getenv("OLLAMA_URL", "http://127.0.0.1:11434").rstrip("/")
INDEX_PATH = rp(os.getenv("INDEX_PATH", "./laptop_faiss.index"))
META_PATH = rp(os.getenv("META_PATH", "./laptop_meta.jsonl"))


# ---------------------
# Helpers
# ---------------------
def _num_from_text(val) -> Optional[float]:
    if val is None: return None
    s = str(val).strip()
    if not s: return None
    s = s.replace(",", "")
    try:
        return float(s)
    except Exception:
        m = re.search(r"[-+]?\d*\.?\d+(?:[eE][-+]?\d+)?", s)
        return float(m.group(0)) if m else None


def _normalize_battery(v) -> Optional[str]:
    if v is None: return None
    s = str(v).strip()
    if not s: return None
    # ví dụ: "9 ~ 10" -> "9~10h"
    if "~" in s or "-" in s:
        s2 = s.replace(" ", "")
        return f"{s2}h"
    # nếu là số đơn
    try:
        x = float(s.replace(",", "."))
        return f"{int(x)}h" if abs(x - int(x)) < 1e-6 else f"{x}h"
    except Exception:
        return s  # giữ nguyên


def l2_normalize(v: np.ndarray) -> np.ndarray:
    n = np.linalg.norm(v, axis=1, keepdims=True) + 1e-12
    return v / n


def build_specs(row: Dict[str, Any]) -> str:
    bits = []

    def add(lbl, key):
        val = row.get(key)
        if val is not None and str(val).strip():
            bits.append(f"{lbl}: {val}")

    add("Type", "TypeName")
    add("Size", "Inches")
    add("Screen", "ScreenResolution")
    add("CPU", "CPU_Type")
    add("CPU Brand", "CPU_Company")
    add("RAM", "RAM")
    add("Storage", "Memory")
    add("GPU", "GPU")
    add("OS", "OpSys")
    # Append mô tả tự do nếu có
    if row.get("Description"): bits.append(str(row["Description"]))
    return ". ".join(bits)


def build_text(p: Dict[str, Any]) -> str:
    bits = [p["name"]]
    if p.get("brand"):   bits.append(f"Brand: {p['brand']}")
    if p.get("company"): bits.append(f"Company: {p['company']}")
    if p.get("specs"):   bits.append(f"Specs: {p['specs']}")
    if p.get("price") is not None:  bits.append(f"Price: {int(p['price'])} VND")
    if p.get("weight") is not None: bits.append(f"Weight: {p['weight']} kg")
    if p.get("battery"): bits.append(f"Battery: {p['battery']}")
    if p.get("suitable_tasks"):
        bits.append(f"Suitable tasks: {p['suitable_tasks']}")
    if p.get("suitable_jobs"):
        bits.append(f"Suitable jobs: {p['suitable_jobs']}")
    return ". ".join(bits)


async def embed_one(client: httpx.AsyncClient, text: str) -> np.ndarray:
    payload = {"model": "nomic-embed-text", "prompt": text}
    r = await client.post(f"{OLLAMA_URL}/api/embeddings", json=payload)
    r.raise_for_status()
    data = r.json()
    emb = np.array(data.get("embedding", []), dtype=np.float32)
    if emb.size == 0:
        raise RuntimeError("Empty embedding from Ollama.")
    return emb


async def embed_all(texts: List[str], concurrency: int = 1) -> np.ndarray:
    """
    Trả về mảng (N, D) giữ NGUYÊN THỨ TỰ của texts.
    concurrency=1 là an toàn nhất; có thể tăng nếu muốn nhanh hơn.
    """
    results: List[Optional[np.ndarray]] = [None] * len(texts)
    sem = asyncio.Semaphore(concurrency)
    timeout = httpx.Timeout(60.0, connect=10.0)
    async with httpx.AsyncClient(timeout=timeout) as client:
        async def worker(i: int, t: str):
            async with sem:
                results[i] = await embed_one(client, t)

        await asyncio.gather(*(worker(i, t) for i, t in enumerate(texts)))
    return np.vstack(results)  # type: ignore


def load_csv(csv_path: str, eur_to_vnd: float = 27000.0) -> List[Dict[str, Any]]:
    """
    Map đúng dataset của bạn:
      - name  <- Product
      - brand <- Company
      - company <- Company
      - specs <- ghép TypeName, Inches, ScreenResolution, CPU_Company, CPU_Type, RAM, Memory, GPU, OpSys, Description
      - price (VND) <- Price_EURO * eur_to_vnd (nếu eur_to_vnd > 0), else giữ None
      - weight <- 'Weight (kg)' (float)
      - battery <- 'Battery_life_hours' (chuẩn hóa thành 'xh' / 'x~yh')
    """
    df = pd.read_csv(csv_path, encoding="utf-8-sig")
    # Đảm bảo các cột tồn tại, nếu thiếu thì tạo cột rỗng
    for col in ["Product", "Company", "Description", "TypeName", "Inches", "ScreenResolution",
            "CPU_Company", "CPU_Type", "RAM", "Memory", "GPU", "OpSys",
            "Weight (kg)", "Battery_life_hours", "Price_EURO",
            "suitable_tasks", "suitable_jobs"]:
        if col not in df.columns:
            df[col] = None

    items: List[Dict[str, Any]] = []
    for idx, r in df.iterrows():
        name = (str(r["Product"]).strip() if pd.notna(r["Product"]) else None)
        if not name:
            continue

        brand = (str(r["Company"]).strip() if pd.notna(r["Company"]) else None)
        company = brand
        weight = _num_from_text(r["Weight (kg)"]) if pd.notna(r["Weight (kg)"]) else None
        battery = _normalize_battery(r["Battery_life_hours"]) if pd.notna(r["Battery_life_hours"]) else None

        price = None
        if pd.notna(r["Price_EURO"]):
            euro = _num_from_text(r["Price_EURO"])
            if euro is not None and eur_to_vnd and eur_to_vnd > 0:
                price = float(euro) * float(eur_to_vnd)
    
        row_full = {c: r[c] for c in df.columns}
        specs = build_specs(row_full)

        suitable_tasks = (str(r["suitable_tasks"]).strip() if pd.notna(r["suitable_tasks"]) else None)
        suitable_jobs  = (str(r["suitable_jobs"]).strip()  if pd.notna(r["suitable_jobs"])  else None)

        items.append({
            "id": None,
            "name": name,
            "brand": brand,
            "company": company,
            "specs": specs if specs else (str(r["Description"]).strip() if pd.notna(r["Description"]) else None),
            "image": None,
            "price": price,
            "weight": weight,
            "battery": battery,
            "suitable_tasks": suitable_tasks,
            "suitable_jobs": suitable_jobs,
        })
    return items


def write_jsonl(meta_items: List[Dict[str, Any]], path: str):
    with open(path, "w", encoding="utf-8") as f:
        for it in meta_items:
            f.write(json.dumps(it, ensure_ascii=False) + "\n")


# ---------------------
# Main
# ---------------------
def main():
    import argparse
    parser = argparse.ArgumentParser(description="Build FAISS index + JSONL metadata from CSV (Ollama embeddings).")
    parser.add_argument("--csv", required=True, help="Path to products CSV")
    parser.add_argument("--index-out", default=INDEX_PATH, help="Output FAISS index path")
    parser.add_argument("--meta-out", default=META_PATH, help="Output JSONL metadata path")
    parser.add_argument("--limit", type=int, default=0, help="Limit number of rows (0=all)")
    parser.add_argument("--eur-to-vnd", type=float, default=27000.0,
                        help="FX rate to convert EURO to VND (0 to disable)")
    parser.add_argument("--concurrency", type=int, default=1, help="Embedding concurrency (order-safe)")
    args = parser.parse_args()

    csv_path = rp(args.csv)
    products = load_csv(csv_path, eur_to_vnd=args.eur_to_vnd)
    if args.limit and args.limit > 0:
        products = products[:args.limit]

    # Lọc tối thiểu có name và gán id fallback
    cleaned: List[Dict[str, Any]] = []
    next_fallback_id = 1_000_000
    for p in products:
        if not p.get("name"):
            continue
        if p.get("id") is None:
            p["id"] = next_fallback_id
            next_fallback_id += 1
        cleaned.append(p)

    if not cleaned:
        raise SystemExit("CSV không có bản ghi hợp lệ (thiếu name).")

    texts = [build_text(p) for p in cleaned]

    # Embed (giữ thứ tự)
    vecs = asyncio.run(embed_all(texts, concurrency=max(1, args.concurrency)))

    dim = vecs.shape[1]
    if dim != 768:
        print(f"[WARN] Embedding dim = {dim}, kỳ vọng 768 (nomic-embed-text).")

    # Normalize & build index (IP ~ cosine)
    vecs = l2_normalize(vecs.astype(np.float32))
    index = faiss.IndexFlatIP(dim)
    index.add(vecs)

    # Ghi index
    faiss.write_index(index, args.index_out)

    # Ghi metadata JSONL theo vector_id 0..n-1
    meta_items: List[Dict[str, Any]] = []
    for vid, p in enumerate(cleaned):
        meta_items.append({
            "vector_id": vid,
            "id": int(p["id"]),
            "name": p.get("name") or f"Item {vid}",
            "brand": p.get("brand"),
            "company": p.get("company"),
            "specs": p.get("specs"),
            "image": p.get("image"),
            "price": p.get("price"),
            "weight": p.get("weight"),
            "battery": p.get("battery"),
            "suitable_tasks": p.get("suitable_tasks"),
        "suitable_jobs": p.get("suitable_jobs"),
        })
    write_jsonl(meta_items, args.meta_out)

    print("-----")
    print(f"[DONE] index: {args.index_out}")
    print(f"[DONE] meta : {args.meta_out}")
    print(f"ntotal={index.ntotal}, dim={index.d}")


if __name__ == "__main__":
    main()
