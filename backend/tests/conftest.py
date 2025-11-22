from collections.abc import Generator
from typing import Any

import pytest
from passlib.context import CryptContext
from sqlalchemy import StaticPool, create_engine
from sqlalchemy.orm import Session, sessionmaker
from starlette.testclient import TestClient

from app.database.base import Base
from app.database.session import get_db_session
from app.features.journal.models import Journal, JournalEmotion, JournalKeyword
from app.features.user.models import User  # noqa: F401 # 사용하는 모든 모델 임포트
from app.main import app

from dotenv import load_dotenv
import os

load_dotenv(".env.local")

# --- 1. 테스트 전용 In memory DB 엔진 설정 ---
SQLALCHEMY_DATABASE_URL = "sqlite:///:memory:"
engine = create_engine(
    SQLALCHEMY_DATABASE_URL,
    connect_args={"check_same_thread": False},
    poolclass=StaticPool,
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


# --- 2. DB 세션 Fixture: Setup과 Teardown을 책임집니다 ---
@pytest.fixture(scope="function")
def db_session() -> Generator[Session, Any, None]:
    """
    각 테스트 함수를 위해 깨끗한 DB와 세션을 생성하고, 끝나면 정리합니다.
    """
    Base.metadata.create_all(bind=engine)
    session = TestingSessionLocal()
    try:
        yield session  # 테스트 함수에 세션을 제공
    finally:
        # [Teardown] 테이블 삭제 및 세션 종료
        session.close()
        Base.metadata.drop_all(bind=engine)


# --- 3. TestClient Fixture: 앱의 의존성을 오버라이드합니다 ---
@pytest.fixture(scope="function")
def client(db_session: Session):
    """
    테스트 DB 세션을 사용하도록 오버라이드된 TestClient를 생성합니다.
    """

    def override_get_db_session():
        yield db_session

    app.dependency_overrides[get_db_session] = override_get_db_session
    with TestClient(app) as c:
        yield c
    # 오버라이드 원상복구
    del app.dependency_overrides[get_db_session]


pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


@pytest.fixture(scope="function")
def test_user(db_session: Session) -> User:
    """
    공용 테스트 사용자를 생성하고 DB에 저장하는 fixture
    """
    # test_me_success에 있던 사용자 생성 로직
    user = User(
        login_id="test_user",
        hashed_password=pwd_context.hash("ValidPass123!"),
        username="Test-User",
    )
    db_session.add(user)
    db_session.commit()
    db_session.refresh(user)
    return user


@pytest.fixture(scope="function")
def auth_headers(client: TestClient, test_user: User) -> dict[str, str]:
    """
    공용 사용자로 로그인하고 인증 헤더(토큰)를 반환하는 fixture
    """
    login_data = {
        "login_id": test_user.login_id,
        "password": "ValidPass123!",  # test_user 생성 시 사용한 비밀번호
    }
    login_response = client.post("/api/v1/auth/login", json=login_data)
    login_response_json = login_response.json()
    access_token = login_response_json["data"]["access"]

    headers = {"Authorization": f"Bearer {access_token}"}
    return headers


@pytest.fixture
def test_journal(db_session: Session, test_user: User) -> Journal:
    """테스트 유저(test_user)가 소유한 일기 fixture"""
    journal = Journal(
        title="Fixture Journal",
        content="This is a journal created by a fixture.",
        gratitude="Thankful for fixtures.",
        user_id=test_user.id,
    )
    db_session.add(journal)
    db_session.flush()

    emotions_list = [
        JournalEmotion(journal_id=journal.id, emotion="happy", intensity=5),
        JournalEmotion(journal_id=journal.id, emotion="anxious", intensity=1),
        JournalEmotion(journal_id=journal.id, emotion="calm", intensity=3),
    ]
    db_session.add_all(emotions_list)

    keywords_list = [
        JournalKeyword(
            journal_id=journal.id,
            keyword="keyword1",
            emotion="happy",
            summary="string1",
            weight=0.9,
        ),
        JournalKeyword(
            journal_id=journal.id,
            keyword="keyword2",
            emotion="anxious",
            summary="string1",
            weight=0.5,
        ),
    ]
    db_session.add_all(keywords_list)

    db_session.commit()
    db_session.refresh(journal)
    return journal
