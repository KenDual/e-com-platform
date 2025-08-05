import os, faiss, pandas as pd, numpy as np, sqlalchemy as sa, dotenv
dotenv.load_dotenv()

eng = sa.create_engine(os.getenv("DB_DSN"))
df  = pd.read_sql("SELECT embedding FROM products", eng)
dim = len(df.embedding.iloc[0])
index = faiss.IndexFlatIP(dim)
index.add(np.vstack(df.embedding.values.astype(np.float32)))
faiss.write_index(index, "laptop_faiss.index")
print("Đã ghi laptop_faiss.index")
