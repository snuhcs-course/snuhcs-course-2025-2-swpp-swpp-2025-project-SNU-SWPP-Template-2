from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.features.auth.service import AuthService
from app.features.user.schemas.responses import ProfileResponse, ProfileResponseEnvelope
from app.features.user.service import UserService

router = APIRouter(prefix="/user", tags=["user"])
security = HTTPBearer()


@router.get(
    "/me",
    status_code=200,
    summary="Get profile of user",
    description="Retrieve information about the currently authenticated user.",
    response_model=ProfileResponseEnvelope,
)
def me(
    authorization: Annotated[HTTPAuthorizationCredentials, Depends(security)],
    auth_service: Annotated[AuthService, Depends()],
    user_service: Annotated[UserService, Depends()],
):
    token = authorization.credentials
    payload = auth_service.validate_access_token(token)
    login_id: str = payload.get("sub")

    user = user_service.get_user_by_login_id(login_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    return ProfileResponseEnvelope(
        data=ProfileResponse(id=user.id, login_id=user.login_id, username=user.username)
    )
