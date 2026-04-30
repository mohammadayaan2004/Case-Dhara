from fastapi import APIRouter, Depends, Request
from fastapi.responses import StreamingResponse

from middleware.rate_limiter import limiter
from models.chatbot import LegalChatbot, get_chatbot
from schemas.chatbot_schema import ChatRequest, ChatResponse

router = APIRouter(prefix="/api/v1/chat", tags=["chatbot"])


@router.post("/message", response_model=ChatResponse)
@limiter.limit("60/minute")
async def chat(
    request: Request,
    body: ChatRequest,
    chatbot: LegalChatbot = Depends(get_chatbot),
):
    return chatbot.chat(body)


@router.post("/stream")
@limiter.limit("60/minute")
async def chat_stream(
    request: Request,
    body: ChatRequest,
    chatbot: LegalChatbot = Depends(get_chatbot),
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
