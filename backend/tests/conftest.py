from collections.abc import Generator
from typing import Any

import pytest
from sqlalchemy import StaticPool, create_engine
from sqlalchemy.orm import Session, sessionmaker
from starlette.testclient import TestClient

from app.database.base import Base
from app.database.session import get_db_session
from app.features.user.models import User  # noqa: F401 # 사용하는 모든 모델 임포트
from app.main import app

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
