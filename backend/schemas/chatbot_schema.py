from typing import Annotated, List, Literal

from pydantic import BaseModel, Field


class ChatMessage(BaseModel):
    role: Literal["user", "assistant"]
    content: str


class ChatRequest(BaseModel):
    question: str = Field(..., min_length=2, max_length=2000)
    history: Annotated[List[ChatMessage], Field(max_length=20)] = []


class ChatResponse(BaseModel):
    answer: str
    retrieved_sections: List[str]
    retrieval_tier: int
