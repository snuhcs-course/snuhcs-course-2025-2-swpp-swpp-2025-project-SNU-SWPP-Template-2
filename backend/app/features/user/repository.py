from typing import Annotated

from fastapi import Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.database.session import get_db_session

from .models import User


class UserRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def add_user(self, login_id: str, hashed_password: str, username: str) -> User:
        user = User(
            login_id=login_id, hashed_password=hashed_password, username=username
        )
        self.session.add(user)
        return user

    def get_user_by_login_id(self, login_id: str) -> User | None:
        return self.session.scalar(select(User).filter(User.login_id == login_id))
