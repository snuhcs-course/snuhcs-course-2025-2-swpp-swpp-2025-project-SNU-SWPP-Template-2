from datetime import datetime
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.common.schemas import ResponseEnvelope
from app.features.auth.schemas.requests import (
    LoginRequest,
    RefreshTokenRequest,
    SignupRequest,
)
from app.features.auth.schemas.responses import TokenResponse, TokenResponseEnvelope
from app.features.auth.service import AuthService

router = APIRouter(prefix="/auth", tags=["auth"])
security = HTTPBearer()


@router.post(
    "/signup",
    status_code=201,
    summary="Sign up a new user",
    description="Create a new user account and return access and refresh tokens.",
    response_model=TokenResponseEnvelope,
)
def signup(
    request: SignupRequest,
    auth_service: Annotated[AuthService, Depends()],
):
    access, refresh = auth_service.signup(
        request.login_id, request.password, request.username
    )
    return TokenResponseEnvelope(data=TokenResponse(access=access, refresh=refresh))


@router.post(
    "/login",
    status_code=201,
    summary="Login a user",
    description="Log in an existing user and return access and refresh tokens.",
    response_model=TokenResponseEnvelope,
)
def login(
    request: LoginRequest,
    auth_service: Annotated[AuthService, Depends()],
):
    access, refresh = auth_service.login(request.login_id, request.password)
    return TokenResponseEnvelope(data=TokenResponse(access=access, refresh=refresh))


@router.post(
    "/logout",
    status_code=200,
    summary="Log out a user",
    description="Log out the current user and invalidate the refresh token.",
    response_model=ResponseEnvelope,
)
def logout(
    request: RefreshTokenRequest,
    auth_service: Annotated[AuthService, Depends()],
):
    refresh = request.refresh
    if refresh is None:
        raise HTTPException(status_code=401, detail="Invalid refresh token")
    if auth_service.validate_refresh_token(refresh) is None:
        raise HTTPException(status_code=401, detail="Invalid refresh token")

    auth_service.block_refresh_token(refresh, datetime.now())
    return ResponseEnvelope(ok=True, data={"status": "success"})


@router.post(
    "/refresh",
    status_code=200,
    summary="Refresh access token",
    description="Refresh the access token using the refresh token.",
    response_model=TokenResponseEnvelope,
)
def refresh(
    request: RefreshTokenRequest, auth_service: Annotated[AuthService, Depends()]
):
    refresh = request.refresh
    access, refresh = auth_service.refresh(refresh)
    return TokenResponseEnvelope(data=TokenResponse(access=access, refresh=refresh))


@router.post(
    "/verify",
    status_code=200,
    summary="Verify access token",
    description="Verify the access token and return the associated user ID.",
    response_model=ResponseEnvelope,
)
def verify(
    authorization: Annotated[HTTPAuthorizationCredentials, Depends(security)],
    auth_service: Annotated[AuthService, Depends()],
):
    token = authorization.credentials
    payload = auth_service.validate_access_token(token)

    return ResponseEnvelope(data={"login_id": payload.get("sub")})
