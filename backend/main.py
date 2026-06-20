"""
Case Dhara FastAPI backend entry point.

Mapper and Summarizer are pure ML models (sentence-transformers + BART).
Chatbot (Gemini) loads lazily — startup never fails if GEMINI_API_KEY is absent.
KMP_DUPLICATE_LIB_OK set before any torch/numpy import to prevent OpenMP conflict.
"""

import os

os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"

import asyncio
from contextlib import asynccontextmanager
from concurrent.futures import ThreadPoolExecutor
import logging

from dotenv import load_dotenv
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from slowapi import _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded

from middleware.rate_limiter import limiter
from models.mapper import get_mapper
from models.summarizer import get_summarizer
from routers import chatbot, health, mapper, summarizer

load_dotenv()

logger = logging.getLogger("case_dhara")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Pre-warm mapper and summarizer (ML models — may take 10-30 seconds on first run).
    Chatbot (Gemini) loads on first request so the server starts without a valid key.
    """
    loop = asyncio.get_running_loop()
    with ThreadPoolExecutor(max_workers=2) as pool:
        await loop.run_in_executor(pool, get_mapper)
        await loop.run_in_executor(pool, get_summarizer)
    logger.info("Mapper and Summarizer loaded. Chatbot will load on first /chat request.")
    yield


app = FastAPI(
    title="Case Dhara API",
    description="IPC to BNS Mapper + Case Summarizer + Legal Chatbot",
    version="2.1.0",
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
