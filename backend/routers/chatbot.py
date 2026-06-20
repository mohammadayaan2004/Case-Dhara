from fastapi import APIRouter, Depends, HTTPException, Request
from fastapi.responses import StreamingResponse

from middleware.rate_limiter import limiter
from models.chatbot import LegalChatbot, get_chatbot
from schemas.chatbot_schema import ChatRequest, ChatResponse

router = APIRouter(prefix="/api/v1/chat", tags=["chatbot"])


def _get_chatbot_or_503() -> LegalChatbot:
    """Dependency: raises HTTP 503 with a helpful message if Gemini is unavailable."""
    try:
        return get_chatbot()
    except RuntimeError as e:
        raise HTTPException(
            status_code=503,
            detail=(
                f"Chatbot unavailable: {e}. "
                "The mapper and summarizer are still fully operational."
            ),
        )


@router.post("/message", response_model=ChatResponse)
@limiter.limit("60/minute")
async def chat(
    request: Request,
    body: ChatRequest,
    chatbot: LegalChatbot = Depends(_get_chatbot_or_503),
):
    try:
        return chatbot.chat(body)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Chat failed: {exc}") from exc


@router.post("/stream")
@limiter.limit("60/minute")
async def chat_stream(
    request: Request,
    body: ChatRequest,
    chatbot: LegalChatbot = Depends(_get_chatbot_or_503),
):
    def generate():
        try:
            for chunk in chatbot.stream(body):
                yield f"data: {chunk}\n\n"
            yield "data: [DONE]\n\n"
        except Exception as exc:
            yield f"data: [ERROR] {exc}\n\n"

    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )
