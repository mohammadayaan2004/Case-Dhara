from fastapi import APIRouter

from models.chatbot import _chatbot_error, chatbot_singleton_loaded
from models.mapper import mapper_singleton_loaded
from models.summarizer import summarizer_singleton_loaded

router = APIRouter(prefix="/api/v1/health", tags=["health"])


@router.get("")
async def health_check():
    chatbot_ok = chatbot_singleton_loaded()
    return {
        "status": "ok",
        "mapper_loaded": mapper_singleton_loaded(),
        "summarizer_loaded": summarizer_singleton_loaded(),
        "chatbot_loaded": chatbot_ok,
        "chatbot_error": _chatbot_error if not chatbot_ok else None,
    }
