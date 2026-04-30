"""
In-memory LRU cache for mapper queries and summarizer PDF results.
"""

import hashlib
import threading
from typing import Any

from cachetools import LRUCache

_mapper_cache = LRUCache(maxsize=500)
_summary_cache = LRUCache(maxsize=50)
_mapper_lock = threading.Lock()
_summary_lock = threading.Lock()


def mapper_cache_key(query: str, top_k: int, law_filter: str | None) -> str:
    payload = f"{query}:{top_k}:{law_filter or ''}"
    return hashlib.md5(payload.encode()).hexdigest()


def get_cached_mapper(key: str) -> Any:
    with _mapper_lock:
        return _mapper_cache.get(key)


def set_cached_mapper(key: str, value: Any) -> None:
    with _mapper_lock:
        _mapper_cache[key] = value


def summary_cache_key(pdf_bytes: bytes) -> str:
    return hashlib.sha256(pdf_bytes).hexdigest()


def get_cached_summary(key: str) -> Any:
    with _summary_lock:
        return _summary_cache.get(key)


def set_cached_summary(key: str, value: Any) -> None:
    with _summary_lock:
        _summary_cache[key] = value
