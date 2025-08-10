import os
import sys
import json
import argparse
from pathlib import Path
from typing import Dict, Any, Tuple, List, Optional

import faiss
import numpy as np
import httpx
from dotenv import load_dotenv


# -----------------------------
# Helpers
# -----------------------------
def resolve_path(base_dir: Path, p: str) -> str:
    if not p:
        return ""
    path = Path(p)
    return str(path if path.is_absolute() else (base_dir / path).resolve())


def safe_float(v) -> Optional[float]:
    if v is None:
        return None
    s = str(v).strip()
    if s == "":
        return None
    try:
        return float(s.replace(",", ""))  # chấp nhận "24,990,000"
    except Exception:
        return None


def load_index(index_path: str) -> faiss.Index:
    if not Path(index_path).exists():
        raise FileNotFoundError(f"FAISS index not found: {index_path}")
    idx = faiss.read_index(index_path)
    return idx


def load_meta_jsonl(meta_path: str) -> Tuple[Dict[int, Dict[str, Any]], Dict[str, Any]]:
    """
    Trả về:
      - mapping: vector_id -> metadata chuẩn hoá
      - stats: thống kê số dòng, số duplicate vector_id, số parse lỗi,...
    """
    stats = {
        "lines": 0,
        "json_errors": 0,
        "dup_vector_ids": 0,
        "missing_vector_id": 0
    }
    mapping: Dict[int, Dict[str, Any]] = {}
    seen_vid = set()

    with open(meta_path, "r", encoding="utf-8") as f:
        for lineno, line in enumerate(f):
            raw = line.strip()
            if not raw:
                continue
            stats["lines"] += 1
            try:
                obj = json.loads(raw)
            except json.JSONDecodeError:
                stats["json_errors"] += 1
                continue

            vid = obj.get("vector_id")
            if vid is None:
                # fallback: dùng số dòng làm vector_id
                vid = lineno
                stats["missing_vector_id"] += 1

            try:
                vid = int(vid)
            except Exception:
                # vector_id không hợp lệ -> bỏ qua
                stats["json_errors"] += 1
                continue

            if vid in seen_vid:
                stats["dup_vector_ids"] += 1
                # giữ bản ghi đầu tiên, bỏ qua trùng còn lại
                continue
            seen_vid.add(vid)

            pid = obj.get("id", vid)
            try:
                pid = int(pid)
            except Exception:
                # nếu không ép được int thì gắn theo vid
                pid = int(vid)

            name = str(obj.get("name", f"Item {pid}"))
            brand = obj.get("brand") or obj.get("company") or None
            company = obj.get("company") or brand
            specs = obj.get("specs")
            image = obj.get("image")
            price = safe_float(obj.get("price"))
            weight = safe_float(obj.get("weight"))
            battery = obj.get("battery")

            mapping[vid] = {
                "id": pid,
                "name": name,
                "brand": brand,
                "company": company,
                "specs": specs,
                "image": image,
                "price": price,
                "weight": weight,
                "battery": battery,
            }

    return mapping, stats


async def fetch_embed_dim(ollama_url: str, timeout: float = 20.0) -> Optional[int]:
    """
    Gọi /api/embeddings để lấy 1 embedding và suy ra dim. Nếu lỗi -> None.
    """
    url = f"{ollama_url.rstrip('/')}/api/embeddings"
    payload = {"model": "nomic-embed-text", "prompt": "ping"}
    try:
        async with httpx.AsyncClient(timeout=httpx.Timeout(timeout, connect=5.0)) as client:
            r = await client.post(url, json=payload)
            r.raise_for_status()
            data = r.json()
            emb = data.get("embedding", [])
            if isinstance(emb, list):
                return len(emb)
            return None
    except Exception:
        return None


def summarize_list(xs: List[Any], max_items: int = 10) -> str:
    xs = list(xs)
    if not xs:
        return "[]"
    if len(xs) <= max_items:
        return json.dumps(xs, ensure_ascii=False)
    return json.dumps(xs[:max_items], ensure_ascii=False)[:-1] + f', ...] (total={len(xs)})'


# -----------------------------
# Main validate
# -----------------------------
def validate(index_path: str, meta_path: str, ollama_url: str, expect_dim: int, max_show: int = 10) -> int:
    base_dir = Path(__file__).resolve().parent

    index_path = resolve_path(base_dir, index_path)
    meta_path = resolve_path(base_dir, meta_path)

    print(f"== Validate assets ==")
    print(f"- INDEX_PATH : {index_path}")
    print(f"- META_PATH  : {meta_path}")
    print(f"- OLLAMA_URL : {ollama_url}")
    print(f"- EXPECT_DIM : {expect_dim}")
    print("---------------")

    # 1) Load index
    idx = load_index(index_path)
    index_dim = idx.d
    index_ntotal = int(idx.ntotal)
    print(f"[Index] dim={index_dim}, ntotal={index_ntotal}")

    # 2) Load metadata
    meta_map, meta_stats = load_meta_jsonl(meta_path)
    meta_count = len(meta_map)
    print(f"[Meta ] count={meta_count} (lines={meta_stats['lines']}, json_errors={meta_stats['json_errors']}, "
          f"dup_vector_ids={meta_stats['dup_vector_ids']}, auto_vector_id={meta_stats['missing_vector_id']})")

    # 3) Coverage check: 0..ntotal-1 phải có trong meta_map
    expected_vids = set(range(index_ntotal))
    have_vids = set(meta_map.keys())
    missing_vids = sorted(expected_vids - have_vids)
    extra_vids = sorted([v for v in have_vids if v < 0 or v >= index_ntotal])

    print(f"[Cover] missing_vids={len(missing_vids)}, extra_vids={len(extra_vids)}")
    if missing_vids:
        print(f"        sample missing: {summarize_list(missing_vids, max_show)}")
    if extra_vids:
        print(f"        sample extra:   {summarize_list(extra_vids, max_show)}")

    # 4) Basic field checks
    missing_name = [vid for vid, m in meta_map.items() if not str(m.get('name', '')).strip()]
    missing_id = [vid for vid, m in meta_map.items() if m.get('id') is None]
    invalid_price = [vid for vid, m in meta_map.items() if m.get('price') is None and ('price' in m)]
    # weight đã safe_float, None là chấp nhận (không bắt buộc)
    print(f"[Field] missing name={len(missing_name)}, missing id={len(missing_id)}, invalid price={len(invalid_price)}")
    if missing_name:
        print(f"        sample: {summarize_list(missing_name, max_show)}")
    if missing_id:
        print(f"        sample: {summarize_list(missing_id, max_show)}")
    if invalid_price:
        print(f"        sample: {summarize_list(invalid_price, max_show)}")

    # 5) Embedding dim từ Ollama
    import asyncio
    emb_dim = asyncio.run(fetch_embed_dim(ollama_url))
    if emb_dim is None:
        print("[Embed] WARN: Không gọi được Ollama hoặc không nhận được embedding (bỏ qua kiểm tra dim runtime).")
    else:
        print(f"[Embed] embedding_dim={emb_dim}")

    # 6) Đánh giá pass/fail
    dim_ok = (index_dim == expect_dim)
    if emb_dim is not None:
        dim_ok = dim_ok and (emb_dim == expect_dim)

    coverage_ok = (len(missing_vids) == 0)
    basic_ok = (len(missing_name) == 0 and len(missing_id) == 0)

    status = "OK" if (dim_ok and coverage_ok and basic_ok) else "FAIL"
    print("---------------")
    print(f"STATUS: {status}")
    if status == "FAIL":
        # Gợi ý xử lý ngắn gọn
        if not dim_ok:
            print("- Dim mismatch: Hãy chắc chắn **nomic-embed-text (768)** dùng cho cả index & truy vấn.")
        if not coverage_ok:
            print("- Coverage: Hãy rebuild metadata hoặc đảm bảo mỗi dòng có `vector_id` phủ đủ 0..ntotal-1.")
        if not basic_ok:
            print("- Field: Bổ sung `id` và `name` cho các bản ghi thiếu.")
        return 1
    return 0


# -----------------------------
# CLI
# -----------------------------
def main():
    load_dotenv()  # đọc .env nếu có
    default_index = os.getenv("INDEX_PATH", "./laptop_faiss.index")
    default_meta = os.getenv("META_PATH", "./laptop_meta.jsonl")
    default_ollama = os.getenv("OLLAMA_URL", "http://127.0.0.1:11434")

    parser = argparse.ArgumentParser(description="Validate FAISS index & metadata for ai-retriever.")
    parser.add_argument("--index", default=default_index, help="Path to FAISS index file")
    parser.add_argument("--meta", default=default_meta, help="Path to metadata JSONL")
    parser.add_argument("--ollama-url", default=default_ollama, help="Ollama base URL")
    parser.add_argument("--expect-dim", type=int, default=768, help="Expected embedding dimension")
    parser.add_argument("--max-show", type=int, default=10, help="Max samples to show in lists")
    args = parser.parse_args()

    code = validate(args.index, args.meta, args.ollama_url, args.expect_dim, args.max_show)
    sys.exit(code)


if __name__ == "__main__":
    main()
