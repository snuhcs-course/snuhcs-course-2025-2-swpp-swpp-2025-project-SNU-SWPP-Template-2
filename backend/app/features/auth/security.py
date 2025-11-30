import hashlib
from datetime import UTC, datetime, timedelta

import jwt
from fastapi import HTTPException
from jwt.exceptions import ExpiredSignatureError, InvalidTokenError
from passlib.context import CryptContext

from app.core.config import settings

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


# functions for password hashing and verification
def hash_password(password: str) -> str:
    return pwd_context.hash(password)


def verify_password(plain_password: str, hashed_password: str) -> bool:
    return pwd_context.verify(plain_password, hashed_password)


def hash_token(token: str) -> str:
    return hashlib.sha256(token.encode()).hexdigest()


def create_token(login_id: str, token_type="access") -> str:
    now = datetime.now(UTC)
    if token_type == "access":
        expires = now + timedelta(minutes=settings.JWT_ACCESS_TOKEN_EXPIRE_MINUTES)
    elif token_type == "refresh":
        expires = now + timedelta(days=settings.JWT_REFRESH_TOKEN_EXPIRE_DAYS)
    else:
        raise HTTPException(status_code=400, detail="Invalid token type")

    payload = {"sub": login_id, "exp": expires, "type": token_type}
    encoded_token = jwt.encode(payload, settings.JWT_SECRET, algorithm=settings.JWT_ALG)
    return encoded_token


def verify_token(token: str, expected_type: str) -> dict:
    try:
        payload = jwt.decode(token, settings.JWT_SECRET, algorithms=[settings.JWT_ALG])
        sub: int = payload.get("sub")
        token_type: str = payload.get("type")

        if sub is None:
            raise HTTPException(status_code=401, detail="Invalid token: no sub")
        if token_type != expected_type:
            raise HTTPException(
                status_code=401,
                detail=f"Invalid token type: expected {expected_type}, got {token_type}",
            )

        return payload

    except ExpiredSignatureError as err:
        raise HTTPException(status_code=401, detail="Token has expired") from err
    except InvalidTokenError as err:
        raise HTTPException(status_code=401, detail="Invalid token") from err
