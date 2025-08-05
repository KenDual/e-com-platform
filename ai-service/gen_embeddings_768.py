import os, numpy as np, pandas as pd, requests, dotenv, faiss, tqdm
import sqlalchemy as sa
from sqlalchemy import text

# ────────────────────────────────────────────────────────────
# 1. Kết nối & cấu hình
# ────────────────────────────────────────────────────────────
dotenv.load_dotenv()
ENG    = sa.create_engine(os.getenv("DB_DSN"))
OLLAMA = os.getenv("OLLAMA_BASE", "http://localhost:11434")
MODEL  = "nomic-embed-text"        # vector 768-dim

# ────────────────────────────────────────────────────────────
# 2. Lấy danh sách sản phẩm
# ────────────────────────────────────────────────────────────
rows = pd.read_sql("""
    SELECT ProductId, Description
    FROM   dbo.Products
    WHERE  IsActive = 1
""", ENG)

print(f"▶ Đang sinh embedding cho {len(rows)} sản phẩm…")

# câu lệnh MERGE (dùng bind-param)
sql_merge = text("""
MERGE dbo.ProductEmbeddings AS tgt
USING (VALUES (:pid, :vec, :dim)) AS src(ProductId, Vector, Dim)
ON tgt.ProductId = src.ProductId
WHEN MATCHED THEN UPDATE SET Vector = src.Vector, Dim = src.Dim
WHEN NOT MATCHED THEN INSERT (ProductId, Vector, Dim)
                    VALUES (src.ProductId, src.Vector, src.Dim);
""")

vec_list = []
# ────────────────────────────────────────────────────────────
# 3. Vòng lặp embed + ghi DB
# ────────────────────────────────────────────────────────────
for pid, desc in tqdm.tqdm(rows.itertuples(index=False), total=len(rows)):
    resp = requests.post(
        f"{OLLAMA}/api/embeddings",
        json={"model": MODEL, "prompt": desc}
    ).json()

    if "embedding" not in resp:
        raise RuntimeError(f"Ollama /embeddings error: {resp}")

    vec = np.array(resp["embedding"], dtype=np.float32)
    vec_list.append(vec)

    # dùng transaction auto-commit
    with ENG.begin() as conn:
        conn.execute(sql_merge, {"pid": pid,
                                 "vec": vec.tobytes(),
                                 "dim": len(vec)})

# ────────────────────────────────────────────────────────────
# 4. Xây FAISS index & lưu file
# ────────────────────────────────────────────────────────────
mat  = np.vstack(vec_list)
index = faiss.IndexFlatIP(mat.shape[1])
index.add(mat)
faiss.write_index(index, "laptop_faiss.index")

print(f"✓ Hoàn tất: đã ghi {len(vec_list)} vector (dim = {mat.shape[1]}) và file laptop_faiss.index")