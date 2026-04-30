from fastapi import APIRouter, Depends, Query, Request
from middleware.rate_limiter import limiter

from models.mapper import LegalMapper, get_mapper
from schemas.mapper_schema import BatchSearchRequest, MapperResponse, SearchRequest

router = APIRouter(prefix="/api/v1/mapper", tags=["mapper"])


@router.get("/search", response_model=MapperResponse)
@limiter.limit("120/minute")
async def search(
    request: Request,
    q: str = Query(..., max_length=1000, description="Section number, heading, legal phrase, or FIR text"),
    top_k: int = Query(5, ge=1, le=20),
    law: str | None = Query(None, description="Optional: ipc | bns"),
    mapper: LegalMapper = Depends(get_mapper),
):
    return mapper.search(query=q, top_k=top_k, law_filter=law)


@router.post("/search", response_model=MapperResponse)
@limiter.limit("120/minute")
async def search_post(request: Request, req: SearchRequest, mapper: LegalMapper = Depends(get_mapper)):
    return mapper.search(query=req.query, top_k=req.top_k, law_filter=req.law_filter)


@router.post("/batch", response_model=list[MapperResponse])
@limiter.limit("60/minute")
async def batch_search(request: Request, req: BatchSearchRequest, mapper: LegalMapper = Depends(get_mapper)):
    return mapper.batch_search(queries=req.queries, top_k=req.top_k, law_filter=req.law_filter)


@router.get("/repealed", response_model=list[MapperResponse])
async def list_repealed(mapper: LegalMapper = Depends(get_mapper)):
    return mapper.list_repealed(limit=50)


@router.get("/section/ipc/{section}", response_model=MapperResponse)
async def get_ipc_section(section: str, mapper: LegalMapper = Depends(get_mapper)):
    return mapper.search(query=f"IPC {section}", top_k=1)


@router.get("/section/bns/{section}", response_model=MapperResponse)
async def get_bns_section(section: str, mapper: LegalMapper = Depends(get_mapper)):
    return mapper.search(query=f"BNS {section}", top_k=1)
