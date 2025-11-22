from datetime import UTC, datetime
from typing import Annotated

from fastapi import Depends, HTTPException

from app.features.auth.repository import BlockedTokenRepository
from app.features.auth.security import (
    create_token,
    hash_password,
    hash_token,
    verify_password,
    verify_token,
)
from app.features.user.repository import UserRepository


class AuthService:
    def __init__(
        self,
        user_repository: Annotated[UserRepository, Depends()],
        blocked_token_repository: Annotated[BlockedTokenRepository, Depends()],
    ) -> None:
        self.user_repository = user_repository
        self.blocked_token_repository = blocked_token_repository

    def signup(
        self, login_id: str, password: str, username: str | None
    ) -> tuple[str, str]:
        if username is None:
            username = login_id

        if self.user_repository.get_user_by_login_id(login_id) is not None:
            raise HTTPException(status_code=400, detail="Same login ID already exists")

        hashed_password = hash_password(password)
        user = self.user_repository.add_user(login_id, hashed_password, username)

        access_token = create_token(user.login_id, token_type="access")
        refresh_token = create_token(user.login_id, token_type="refresh")
        return access_token, refresh_token

    def login(self, login_id: str, password: str) -> tuple[str, str]:
        user = self.user_repository.get_user_by_login_id(login_id)
        if user is None or not verify_password(password, user.hashed_password):
            raise HTTPException(status_code=401, detail="Invalid login ID or password")

        access_token = create_token(user.login_id, token_type="access")
        refresh_token = create_token(user.login_id, token_type="refresh")
        return access_token, refresh_token

    def validate_access_token(self, token: str) -> dict:
        payload = verify_token(token, expected_type="access")
        return payload

    def validate_refresh_token(self, token: str) -> dict:
        payload = verify_token(token, expected_type="refresh")
        # Check if the token is blocked
        token_id = hash_token(token)
        if self.blocked_token_repository.is_token_blocked(token_id):
            raise HTTPException(status_code=401, detail="Token has been revoked")

        return payload

    def refresh(self, refresh: str) -> tuple[str, str]:
        payload = self.validate_refresh_token(refresh)
        login_id = payload.get("sub")

        new_access = create_token(login_id, token_type="access")
        new_refresh = create_token(login_id, token_type="refresh")

        # Block the old refresh token
        exp_timestamp = payload.get("exp")
        if exp_timestamp is None:
            raise HTTPException(status_code=401, detail="Invalid token: no exp")
        expired_at = datetime.fromtimestamp(exp_timestamp, tz=UTC)
        token_id = hash_token(refresh)
        self.blocked_token_repository.add_blocked_token(token_id, expired_at)

        return new_access, new_refresh

    def block_refresh_token(self, token: str, expired_at: datetime) -> None:
        token_id = hash_token(token)
        self.blocked_token_repository.add_blocked_token(token_id, expired_at)
