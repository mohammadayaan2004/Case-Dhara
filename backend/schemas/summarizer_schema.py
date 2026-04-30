from typing import Dict, List, Optional

from pydantic import BaseModel


class SectionReference(BaseModel):
    raw_ref: str
    ipc_section: str
    ipc_heading: str
    bns_section: str
    bns_heading: str
    status: str


class CaseSummary(BaseModel):
    case_title: str
    citation: Optional[str] = None
    court: str
    bench: Optional[str] = None
    date: Optional[str] = None
    parties: Dict[str, str]
    sections_invoked: List[SectionReference]
    facts: str
    issues: List[str]
    arguments: Dict[str, str]
    evidence: str
    held: str
    ratio_decidendi: str
    order: str
    summary_short: str
    outcome: Optional[str] = None


class SummarizeTextRequest(BaseModel):
    text: str
