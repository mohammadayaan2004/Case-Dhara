from typing import Annotated, List, Literal, Optional

from pydantic import BaseModel, Field


StatusType = Literal["mapped", "repealed", "new_in_bns", "merged", "split"]


class MappingRecord(BaseModel):
    id: int
    ipc_section: str
    ipc_heading: str
    ipc_description: str
    bns_section: str
    bns_heading: str
    bns_description: str
    status: StatusType
    confidence: float = Field(ge=0.0, le=1.0)
    retrieval_tier: int = Field(ge=1, le=3)


class MapperResponse(BaseModel):
    query: str
    results: List[MappingRecord]
    total: int
    retrieval_tier: int = Field(ge=1, le=3)
    message: str


class SearchRequest(BaseModel):
    query: str
    top_k: int = Field(default=5, ge=1, le=20)
    law_filter: Optional[str] = None


class BatchSearchRequest(BaseModel):
    queries: Annotated[List[str], Field(min_length=1, max_length=20)]
    top_k: int = Field(default=1, ge=1, le=5)
    law_filter: Optional[str] = None
