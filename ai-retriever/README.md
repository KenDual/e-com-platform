python -m venv .venv

.\.venv\Scripts\activate

ollama pull nomic-embed-text
ollama pull llama3.1:8b

pip install --upgrade pip

pip install fastapi uvicorn httpx faiss-cpu numpy pandas pyarrow pydantic python-dotenv

python -m pip install --upgrade pip setuptools wheel

pip install fastapi uvicorn[standard] httpx numpy orjson python-dotenv

#build again FAISS.index file
python .\build_index_from_csv.py --csv ".\laptop_with_desc.csv" --index-out ".\laptop_faiss.index" --meta-out ".\laptop_meta.jsonl"

uvicorn main:app --host 127.0.0.1 --port 8001 --reload

#Test AI response (Powershell)
@'
{
"model": "llama3.1:8b",
"prompt": "hello",
"stream": true
}
'@ | curl.exe -N "http://localhost:11434/api/generate" -H "Content-Type: application/json" --data-binary '@-'




