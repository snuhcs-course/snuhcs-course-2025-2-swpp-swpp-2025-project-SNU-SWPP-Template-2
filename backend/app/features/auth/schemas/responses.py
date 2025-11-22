from pydantic import BaseModel

from app.common.schemas import ResponseEnvelope


class TokenResponse(BaseModel):
    access: str
    refresh: str


class TokenResponseEnvelope(ResponseEnvelope):
    data: TokenResponse
