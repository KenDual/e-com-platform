import os
import json
from pathlib import Path
from typing import Dict, Any, List, Optional

import faiss
import numpy as np
import httpx
from dotenv import load_dotenv
from fastapi import FastAPI, Query, HTTPException
from pydantic import BaseModel, Field

# -----------------------------------------------------------------------------
# Config & Globals
# -----------------------------------------------------------------------------
load_dotenv()  # load .env in current working dir

APP_DIR = Path(__file__).resolve().parent

def resolve_path(p: str) -> str:
    # Allow relative paths inside .env
    if not p:
        return ""
    path = Path(p)
    return str(path if path.is_absolute() else (APP_DIR / path).resolve())

OLLAMA_URL = os.getenv("OLLAMA_URL", "http://127.0.0.1:11434").rstrip("/")
INDEX_PATH = resolve_path(os.getenv("INDEX_PATH", "./laptop_faiss.index"))
META_PATH = resolve_path(os.getenv("META_PATH", "./laptop_meta.jsonl"))
PORT = int(os.getenv("PORT", "8001"))

# FAISS index & metadata store
faiss_index: Optional[faiss.Index] = None
meta_by_vid: Dict[int, Dict[str, Any]] = {}
index_dim: Optional[int] = None

# HTTP client (reuse connection)
http_client: Optional[httpx.AsyncClient] = None

# -----------------------------------------------------------------------------
# Models
# -----------------------------------------------------------------------------
class Product(BaseModel):
    id: int
    name: str
    brand: Optional[str] = None
    company: Optional[str] = None
    specs: Optional[str] = None
    image: Optional[str] = None
    price: Optional[float] = Field(default=None, description="VND, numeric if available")
    weight: Optional[float] = None
    battery: Optional[str] = None

class SearchResponse(BaseModel):
    products: List[Product]

# -----------------------------------------------------------------------------
# App
# -----------------------------------------------------------------------------
app = FastAPI(title="AI Retriever Mini-Service", version="1.0.0")

# -----------------------------------------------------------------------------
# Helpers
# -----------------------------------------------------------------------------
def _normalize(vec: np.ndarray) -> np.ndarray:
    # For cosine via inner-product: normalize query to unit length
    norm = np.linalg.norm(vec) + 1e-12
    return vec / norm

async def get_query_embedding(text: str) -> np.ndarray:
    """
    Call Ollama /api/embeddings. Ollama expects "prompt" for embeddings.
    Response: {"embedding": [float, ...]}
    """
    global http_client
    if http_client is None:
        http_client = httpx.AsyncClient(timeout=httpx.Timeout(20.0, connect=5.0))

    url = f"{OLLAMA_URL}/api/embeddings"
    payload = {"model": "nomic-embed-text", "prompt": text}

    try:
        r = await http_client.post(url, json=payload)
        r.raise_for_status()
        data = r.json()
        emb = np.array(data.get("embedding", []), dtype=np.float32)
        if emb.size == 0:
            raise ValueError("Empty embedding returned from Ollama.")
        return emb
    except httpx.HTTPError as e:
        raise HTTPException(status_code=502, detail=f"Ollama error: {str(e)}")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Embedding error: {str(e)}")

def load_metadata_jsonl(path: str) -> Dict[int, Dict[str, Any]]:
    """
    JSONL mapping:
      - Preferred: each line has "vector_id": int
      - Fallback: line number (0-based) is the vector id
    Required field in output: id, name
    """
    out: Dict[int, Dict[str, Any]] = {}
    with open(path, "r", encoding="utf-8") as f:
        for lineno, line in enumerate(f):
            line = line.strip()
            if not line:
                continue
            try:
                obj = json.loads(line)
            except json.JSONDecodeError:
                continue

            vid = obj.get("vector_id")
            if vid is None:
                vid = lineno  # fallback to line index

            # Normalize fields & provide safe defaults
            pid = int(obj.get("id", vid))
            name = str(obj.get("name", f"Item {pid}"))
            brand = obj.get("brand") or obj.get("company") or None
            company = obj.get("company") or brand
            specs = obj.get("specs")
            image = obj.get("image")
            price = obj.get("price")
            try:
                # Accept both int/float/str for price
                price = float(price) if price is not None and str(price).strip() != "" else None
            except Exception:
                price = None
            weight = obj.get("weight")
            try:
                weight = float(weight) if weight is not None and str(weight).strip() != "" else None
            except Exception:
                weight = None
            battery = obj.get("battery")

            out[int(vid)] = {
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
    return out

def ensure_ready():
    if faiss_index is None or index_dim is None:
        raise HTTPException(status_code=503, detail="Index not loaded yet.")
    if not meta_by_vid:
        raise HTTPException(status_code=503, detail="Metadata not loaded yet.")

# -----------------------------------------------------------------------------
# Startup / Shutdown
# -----------------------------------------------------------------------------
@app.on_event("startup")
async def on_startup():
    global faiss_index, index_dim, meta_by_vid, http_client

    # Load FAISS index
    if not Path(INDEX_PATH).exists():
        raise RuntimeError(f"FAISS index not found at {INDEX_PATH}")
    faiss_index = faiss.read_index(INDEX_PATH)
    index_dim = faiss_index.d

    # Load metadata
    if not Path(META_PATH).exists():
        raise RuntimeError(f"Metadata file not found at {META_PATH}")
    meta_by_vid = load_metadata_jsonl(META_PATH)

    # Warm HTTP client
    http_client = httpx.AsyncClient(timeout=httpx.Timeout(20.0, connect=5.0))

    # Optional: quick ping to confirm embedding dim (not strictly required)
    try:
        emb = await get_query_embedding("ping")
        # Log mismatch but do not crash; you can enforce if you want.
        if emb.shape[0] != index_dim:
            print(f"[WARN] Embedding dim ({emb.shape[0]}) ≠ index dim ({index_dim}). "
                  f"Check that both use nomic-embed-text (768).")
    except Exception as e:
        # Do not block startup; only warn. Real request will raise clear error.
        print(f"[WARN] Failed warm-embed: {e}")

    print(f"[READY] Index loaded: {INDEX_PATH} (dim={index_dim}, ntotal={faiss_index.ntotal})")
    print(f"[READY] Metadata loaded: {len(meta_by_vid)} records from {META_PATH}")
    print(f"[READY] Ollama at: {OLLAMA_URL}")

@app.on_event("shutdown")
async def on_shutdown():
    global http_client
    if http_client is not None:
        await http_client.aclose()
        http_client = None

# -----------------------------------------------------------------------------
# Routes
# -----------------------------------------------------------------------------
@app.get("/healthz")
def healthz():
    info = {
        "status": "ok" if (faiss_index is not None and meta_by_vid) else "not_ready",
        "index_path": INDEX_PATH,
        "meta_path": META_PATH,
        "index_dim": index_dim,
        "ntotal": int(faiss_index.ntotal) if faiss_index is not None else 0,
        "meta_count": len(meta_by_vid),
        "ollama_url": OLLAMA_URL,
    }
    return info

@app.get("/search", response_model=SearchResponse)
async def search(
        q: str = Query(..., min_length=1, description="User query in Vietnamese or English"),
        top_k: int = Query(5, ge=1, le=20),
):
    ensure_ready()

    # 1) Embed query
    emb = await get_query_embedding(q)
    if emb.shape[0] != index_dim:
        # Auto-fix by padding/trunc (rare), or just reject. We reject for safety.
        raise HTTPException(
            status_code=400,
            detail=f"Embedding dim {emb.shape[0]} != index dim {index_dim}. "
                   f"Ensure nomic-embed-text (768) was used for both."
        )

    # 2) Normalize for cosine with IndexFlatIP / IP-based indexes
    qv = _normalize(emb.astype(np.float32)).reshape(1, -1)

    # 3) FAISS search
    k = min(top_k, faiss_index.ntotal if faiss_index is not None else top_k)
    if k <= 0:
        return SearchResponse(products=[])

    try:
        scores, ids = faiss_index.search(qv, k)  # scores shape: (1, k), ids: (1, k)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"FAISS search error: {str(e)}")

    # 4) Map ids -> metadata
    seen = set()
    products: List[Product] = []
    for vid in ids[0].tolist():
        if vid == -1:
            continue
        if vid in seen:
            continue
        seen.add(vid)

        meta = meta_by_vid.get(int(vid))
        if not meta:
            # Skip missing meta (as per roadmap "ẩn sản phẩm thiếu")
            continue

        # Ensure fields
        products.append(Product(
            id=int(meta.get("id")),
            name=str(meta.get("name")),
            brand=meta.get("brand"),
            company=meta.get("company") or meta.get("brand"),
            specs=meta.get("specs"),
            image=meta.get("image"),
            price=meta.get("price"),
            weight=meta.get("weight"),
            battery=meta.get("battery"),
        ))

    return SearchResponse(products=products)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="127.0.0.1", port=PORT, reload=True)
