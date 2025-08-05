# api_server.py – FastAPI dùng index 768-dim
from fastapi import FastAPI
from pydantic import BaseModel
import os, dotenv, sqlalchemy as sa, pandas as pd, numpy as np, faiss, requests

dotenv.load_dotenv()
ENG     = sa.create_engine(os.getenv("DB_DSN"))
OLLAMA  = os.getenv("OLLAMA_BASE", "http://localhost:11434")
MODEL_EMBED = "nomic-embed-text"
MODEL_GEN   = "llama3.1:8b"

class ChatReq(BaseModel):
    query: str
class ProductDTO(BaseModel):
    id:int; name:str; company:str; price:int; specs:str|None=None; image:str|None=None
class ChatRes(BaseModel):
    answer:str; products:list[ProductDTO]

app = FastAPI(title="Laptop Advisor API")

# ── Nạp dữ liệu & index
df = pd.read_sql("""
    SELECT p.ProductId   AS id,
           p.ProductName AS name,
           p.Company     AS company,   -- <-- alias chữ thường
           p.Description AS specs,
           p.Price_VND   AS price,
           p.ImageCover  AS image,
           e.Vector,
           e.Dim
    FROM   dbo.Products p
    JOIN   dbo.ProductEmbeddings e ON e.ProductId = p.ProductId
    WHERE  p.IsActive = 1
""", ENG)

vecs = np.vstack(df.Vector.apply(lambda b: np.frombuffer(b, np.float32)))
index = faiss.read_index("laptop_faiss.index")    # dim = 768

def embed(text:str)->np.ndarray:
    r = requests.post(f"{OLLAMA}/api/embeddings",
                      json={"model": MODEL_EMBED, "prompt": text}).json()
    if "embedding" not in r:
        raise RuntimeError(r)
    return np.array(r["embedding"], dtype=np.float32)

def llama(prompt:str)->str:
    r = requests.post(f"{OLLAMA}/api/generate",
                      json={"model": MODEL_GEN, "prompt": prompt, "stream": False}).json()
    return r["response"]

@app.get("/")
def root():
    return {"msg":"Laptop Advisor API OK – dùng /docs để thử"}

PRODUCT_KEYS = ["id", "name", "company", "price", "specs", "image"]

@app.post("/chat", response_model=ChatRes)
def chat(req: ChatReq):
    qvec = embed(req.query).reshape(1, -1)
    _, idx = index.search(qvec, 5)
    raw_top = df.iloc[idx[0]].to_dict("records")

    # giữ đúng schema ProductDTO
    top5 = [{k: rec[k] for k in PRODUCT_KEYS} for rec in raw_top]

    context = "\n".join(
        [f"- {p['name']} ({p['price']:,} VND)" for p in top5]
    )
    prompt = (
        "You are a professional laptop sales advisor.\n\nContext:\n"
        + context +
        f"\n\nUser: {req.query}\nAnswer:"
    )
    answer = llama(prompt)
    return {"answer": answer.strip(), "products": top5}
