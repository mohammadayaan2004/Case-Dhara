# NyayaSetu Backend

A FastAPI backend for Indian legal AI:
- **Mapper** — IPC ↔ BNS section mapping (3-tier: exact → keyword → semantic ML)
- **Summarizer** — Court judgment summarization using BART (local ML, zero API calls)
- **Chatbot** — RAG-based legal Q&A powered by Gemini API

---

## Architecture

| Component | Model | External API? |
|-----------|-------|--------------|
| Mapper | `sentence-transformers/all-mpnet-base-v2` + FAISS | ❌ No |
| Summarizer | `facebook/bart-large-cnn` (fine-tunable) | ❌ No |
| Chatbot | Gemini 2.5 Flash | ✅ Yes (chatbot only) |

The server starts and fully serves mapper + summarizer even without a Gemini API key.
The chatbot returns HTTP 503 gracefully if the key is missing or invalid.

---

## Setup & Run

### 1. Prerequisites

- Python 3.11+
- `tesseract-ocr` (for OCR fallback on scanned PDFs)
  - Ubuntu/Debian: `sudo apt-get install tesseract-ocr`
  - macOS: `brew install tesseract`
  - Windows: install from https://github.com/UB-Mannheim/tesseract/wiki

### 2. Install dependencies

```bash
python -m venv .venv

# Linux/macOS
source .venv/bin/activate

# Windows
.venv\Scripts\activate

pip install -r requirements.txt
```

### 3. Configure environment

Edit `.env`:
```
GEMINI_API_KEY=your_actual_gemini_api_key   # only needed for chatbot
CORS_ORIGINS=http://localhost:8000,http://localhost:3000
```

> If you don't have a Gemini key, leave the placeholder — mapper and summarizer still work.

### 4. Run the server

```bash
# Development (with auto-reload)
python main.py

# Or directly with uvicorn
uvicorn main:app --host 0.0.0.0 --port 8000 --reload

# Production
uvicorn main:app --host 0.0.0.0 --port 8000 --workers 2
```

Server starts at: http://localhost:8000  
API docs: http://localhost:8000/docs  
Health check: http://localhost:8000/api/v1/health

**First startup takes 30–60 seconds** while BART and the sentence-transformer model download from HuggingFace.

### 5. Docker

```bash
docker build -t nyayasetu-backend .
docker run -p 8000:8000 --env-file .env nyayasetu-backend
```

---

## API Endpoints

### Mapper (no API key needed)
```
GET  /api/v1/mapper/search?q=murder&top_k=5
POST /api/v1/mapper/search        { "query": "...", "top_k": 5, "law_filter": "bns" }
POST /api/v1/mapper/batch         { "queries": ["...", "..."], "top_k": 1 }
GET  /api/v1/mapper/section/ipc/302
GET  /api/v1/mapper/section/bns/103
GET  /api/v1/mapper/repealed
```

### Summarizer (no API key needed)
```
POST /api/v1/summarize/pdf        multipart/form-data, field: file (PDF)
POST /api/v1/summarize/text       { "text": "..." }
```

### Chatbot (requires GEMINI_API_KEY)
```
POST /api/v1/chat/message         { "question": "...", "history": [] }
POST /api/v1/chat/stream          same, returns SSE stream
```

### Health
```
GET /api/v1/health
```

---

## Training the ML Models

There are two trainable models. Run these steps in order.

---

### Training Step 1 — Prepare BART training data

This extracts text from your PDF judgments and generates training summaries
using extractive + regex methods (**no Gemini API call**).

```bash
# Place your PDF judgments in:
#   data/training_pdfs/

python scripts/prepare_bart_training_data.py

# Optional flags:
python scripts/prepare_bart_training_data.py \
  --pdf_dir data/training_pdfs \
  --out_dir data/processed/bart_training \
  --val_split 0.1 \
  --max_pdfs 200 \
  --min_summary_len 60
```

Output:
- `data/processed/bart_training/train.json`
- `data/processed/bart_training/val.json`
- `data/processed/bart_training/metadata.json`

---

### Training Step 2 — Fine-tune BART summarizer

Fine-tunes `facebook/bart-large-cnn` on your prepared judgment summaries.

```bash
python scripts/train_bart.py

# With options:
python scripts/train_bart.py \
  --epochs 3 \
  --batch_size 2 \
  --lr 3e-5 \
  --max_input 1024 \
  --max_target 256 \
  --output_dir models/bart_finetuned

# If GPU runs out of memory, reduce batch size:
python scripts/train_bart.py --batch_size 1
```

The fine-tuned model is saved to `models/bart_finetuned/`.  
The summarizer automatically uses it on next startup (falls back to pretrained if absent).

**Hardware guidance:**
| Hardware | Batch size | Time per epoch (100 PDFs) |
|----------|-----------|--------------------------|
| GPU (8GB+) | 2–4 | ~5 min |
| GPU (4–6GB) | 1 | ~10 min |
| CPU only | 1 | ~60–120 min |

---

### Training Step 3 — Fine-tune the retriever (optional)

Fine-tunes `sentence-transformers/all-mpnet-base-v2` on IPC/BNS section pairs
for better semantic search accuracy.

```bash
# First, prepare augmented training data
python scripts/augment_dataset.py

# Then fine-tune
python scripts/finetune_retriever.py
```

Output: `models/finetuned_retriever/`  
The mapper automatically uses it on next startup.

---

### Training Step 4 — Rebuild FAISS index (if data changes)

Run this whenever `data/raw/ipc_bns_data.csv` is updated:

```bash
# Step 1: preprocess CSV → mapping_db.json + lookup files
python scripts/preprocess.py

# Step 2: rebuild FAISS index
python scripts/build_index.py
```

---

## Troubleshooting

**Server crashes at startup with "KMP_DUPLICATE_LIB_OK" error**  
Already fixed — the env var is set in `main.py` before any torch import.

**"FAISS index has N vectors but metadata has M entries"**  
The index and metadata are out of sync. Rebuild:
```bash
python scripts/preprocess.py
python scripts/build_index.py
```

**Chatbot returns 503**  
Check `/api/v1/health` for `chatbot_error`. Most likely your `GEMINI_API_KEY` in `.env` is missing or invalid. Mapper and summarizer still work.

**BART generates very short/empty summaries**  
The model falls back to extractive summarization automatically. For better quality, fine-tune BART using the steps above.

**OCR not working on scanned PDFs**  
Ensure `tesseract-ocr` is installed and on your PATH.
```bash
tesseract --version
```

**Out of GPU memory during training**  
```bash
python scripts/train_bart.py --batch_size 1
```
