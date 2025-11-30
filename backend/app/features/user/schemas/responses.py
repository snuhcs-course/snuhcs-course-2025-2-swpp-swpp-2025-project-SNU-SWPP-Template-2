from pydantic import BaseModel

from app.common.schemas import ResponseEnvelope


class ProfileResponse(BaseModel):
    id: int
    login_id: str
    username: str


class ProfileResponseEnvelope(ResponseEnvelope):
    data: ProfileResponse
