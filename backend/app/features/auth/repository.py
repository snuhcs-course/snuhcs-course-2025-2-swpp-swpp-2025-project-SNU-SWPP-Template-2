from datetime import datetime
from typing import Annotated

from fastapi import Depends
from sqlalchemy import exists, select
from sqlalchemy.orm import Session

from app.database.session import get_db_session

from .models import BlockedToken


class BlockedTokenRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def add_blocked_token(self, token_id: str, expired_at: datetime) -> BlockedToken:
        blocked_token = BlockedToken(token_id=token_id, expired_at=expired_at)
        self.session.add(blocked_token)
        return blocked_token

    def is_token_blocked(self, token_id: str) -> bool:
        statement = select(exists().where(BlockedToken.token_id == token_id))
        return self.session.scalar(statement)
