from typing import Any

from pydantic import BaseModel


class ErrorResponse(BaseModel):
    code: str
    message: str


class ResponseEnvelope(BaseModel):
    ok: bool = True
    data: Any | None = None
    error: ErrorResponse | None = None
