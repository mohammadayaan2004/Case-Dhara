"""
NyayaSetu FastAPI backend entry point.

Fixes applied:
- KMP_DUPLICATE_LIB_OK set before any imports to prevent OpenMP conflict
- Uvicorn configured with --reload-dir and --reload-exclude to avoid .venv loops
- __main__ guard for Windows multiprocessing spawn safety
"""

import os

# ── OpenMP conflict fix (must be BEFORE any torch/numpy/sklearn import) ──
os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"

import asyncio
from contextlib import asynccontextmanager
from concurrent.futures import ThreadPoolExecutor

from dotenv import load_dotenv
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from slowapi import _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded

from middleware.rate_limiter import limiter
from models.chatbot import get_chatbot
from models.mapper import get_mapper
from models.summarizer import get_summarizer
from routers import chatbot, health, mapper, summarizer

load_dotenv()


@asynccontextmanager
async def lifespan(app: FastAPI):
    loop = asyncio.get_running_loop()
    with ThreadPoolExecutor(max_workers=2) as pool:
        await loop.run_in_executor(pool, get_mapper)
        await loop.run_in_executor(pool, get_summarizer)
    get_chatbot()
    yield


app = FastAPI(
    title="NyayaSetu API",
    description="IPC to BNS Mapper + Case Summarizer + Legal Chatbot",
    version="2.0.0",
    lifespan=lifespan,
)

app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

_cors = os.getenv("CORS_ORIGINS", "http://10.0.2.2:8000,http://localhost:8000")
_origins = [o.strip() for o in _cors.split(",") if o.strip()]

app.add_middleware(
    CORSMiddleware,
    allow_origins=_origins,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health.router)
app.include_router(mapper.router)
app.include_router(summarizer.router)
app.include_router(chatbot.router)


# ── Windows-safe entry point with proper reload config ──
if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
        reload_dirs=[".", "models", "routers", "middleware", "schemas", "services"],
        reload_excludes=[".venv", "__pycache__", "*.pyc", "data"],
    )
