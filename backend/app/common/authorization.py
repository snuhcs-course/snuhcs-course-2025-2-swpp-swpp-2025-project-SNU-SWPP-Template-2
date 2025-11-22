from typing import Annotated

from fastapi import Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.features.auth.service import AuthService
from app.features.user.models import User
from app.features.user.service import UserService

security = HTTPBearer()


def get_current_user(
    user_service: Annotated[UserService, Depends()],
    auth_service: Annotated[AuthService, Depends()],
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(security)],
) -> User:
    token = credentials.credentials
    payload = auth_service.validate_access_token(token)
    login_id: str = payload.get("sub")

    user = user_service.get_user_by_login_id(login_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return user
