from passlib.context import CryptContext
from sqlalchemy.orm import Session
from starlette.testclient import TestClient

from app.features.user.models import User

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def test_signup_success(client: TestClient, db_session: Session):
    """Test successful user signup with database integration."""
    # Arrange: API에 전송할 데이터를 정의합니다.
    data = {
        "login_id": "new_user",
        "password": "qwerQWER123!",
        "username": "new-user",
    }

    # Act: API 엔드포인트를 호출합니다.
    # 이 client는 conftest.py에서 DB 연결이 오버라이드된 상태입니다.
    response = client.post("/api/v1/auth/signup", json=data)
    db_session.flush()

    # Assert 1: API 응답을 검증합니다.
    assert response.status_code == 201
    response_json = response.json()
    assert response_json["ok"] is True
    assert "access" in response_json["data"]
    assert "refresh" in response_json["data"]
    assert response_json["error"] is None

    # Assert 2: 데이터베이스 상태를 직접 검증합니다.
    user_in_db = (
        db_session.query(User).filter(User.login_id == data["login_id"]).first()
    )

    assert user_in_db is not None
    assert user_in_db.login_id == data["login_id"]
    assert user_in_db.username == data["username"]
    assert user_in_db.hashed_password != data["password"]


def test_signup_duplicate_login_id(client: TestClient, db_session: Session):
    """Test user signup with a duplicate login_id."""
    # Arrange: 이미 존재하는 사용자를 DB에 추가합니다.
    existing_user = User(
        login_id="existing_user",
        hashed_password="hashedpassword",
        username="Existing-User",
    )
    db_session.add(existing_user)
    db_session.commit()

    # Act: 동일한 login_id로 회원가입을 시도합니다.
    data = {
        "login_id": "existing_user",  # 중복된 login_id
        "password": "AnotherPass123!",
        "username": "Another-User",
    }
    response = client.post("/api/v1/auth/signup", json=data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 400
    response_json = response.json()
    assert response_json["detail"] == "Same login ID already exists"


def test_signup_invalid_password(client: TestClient, db_session: Session):
    """Test user signup with an invalid password."""
    # Arrange: API에 전송할 데이터를 정의합니다.
    data = {
        "login_id": "new_user2",
        "password": "short",  # 너무 짧은 비밀번호
        "username": "new-user2",
    }
    response = client.post("/api/v1/auth/signup", json=data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 400
    response_json = response.json()
    assert response_json["detail"] == "Invalid password format"


def test_signup_invalid_username(client: TestClient, db_session: Session):
    """Test user signup with an invalid username."""
    # Arrange: API에 전송할 데이터를 정의합니다.
    data = {
        "login_id": "new_user3",
        "password": "ValidPass123!",
        "username": "Invalid Username!",  # 공백과 특수문자가 포함된 username
    }
    response = client.post("/api/v1/auth/signup", json=data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 400
    response_json = response.json()
    assert response_json["detail"] == "Invalid username format"


def test_signup_missing_fields(client: TestClient, db_session: Session):
    """Test user signup with missing required fields."""
    # Arrange: API에 전송할 데이터를 정의합니다.
    data = {
        "login_id": "new_user4",
        "password": "ValidPass123!",
    }
    response = client.post("/api/v1/auth/signup", json=data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 422
    response_json = response.json()
    error_detail = response_json["detail"][0]
    assert error_detail["loc"] == ["body", "username"]  # 에러 위치: body의 'username'
    assert error_detail["msg"] == "Field required"  # 에러 메시지
    assert error_detail["type"] == "missing"  # 에러 타입


def test_login_success(client: TestClient, db_session: Session):
    """Test successful user login with database integration."""
    # Arrange: 이미 존재하는 사용자를 DB에 추가합니다.
    existing_user = User(
        login_id="login_user",
        hashed_password=pwd_context.hash("ValidPass123!"),
        username="Login-User",
    )
    db_session.add(existing_user)
    db_session.commit()

    # Act: 올바른 자격 증명으로 로그인 시도
    data = {
        "login_id": "login_user",
        "password": "ValidPass123!",
    }
    response = client.post("/api/v1/auth/login", json=data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 201
    response_json = response.json()
    assert response_json["ok"] is True
    assert "access" in response_json["data"]
    assert "refresh" in response_json["data"]
    assert response_json["error"] is None


def test_login_invalid_password(client: TestClient, db_session: Session):
    """Test user login with an invalid password."""
    # Arrange: 이미 존재하는 사용자를 DB에 추가합니다.
    existing_user = User(
        login_id="login_user2",
        hashed_password=pwd_context.hash("ValidPass123!"),
        username="Login-User2",
    )
    db_session.add(existing_user)
    db_session.commit()

    # Act: 잘못된 비밀번호로 로그인 시도
    data = {
        "login_id": "login_user2",
        "password": "WrongPass123!",
    }
    response = client.post("/api/v1/auth/login", json=data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 401
    response_json = response.json()
    assert response_json["detail"] == "Invalid login ID or password"


def test_login_nonexistent_user(client: TestClient, db_session: Session):
    """Test user login with a non-existent user."""
    # Act: 존재하지 않는 사용자로 로그인 시도
    data = {
        "login_id": "nonexistent_user",
        "password": "SomePass123!",
    }
    response = client.post("/api/v1/auth/login", json=data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 401
    response_json = response.json()
    assert response_json["detail"] == "Invalid login ID or password"


def test_login_missing_fields(client: TestClient, db_session: Session):
    """Test user login with missing required fields."""
    # Arrange: API에 전송할 데이터를 정의합니다.
    data = {
        "login_id": "some_user",
        # "password" 필드가 누락되었습니다.
    }
    response = client.post("/api/v1/auth/login", json=data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 422
    response_json = response.json()
    error_detail = response_json["detail"][0]
    assert error_detail["loc"] == ["body", "password"]  # 에러 위치: body의 'password'
    assert error_detail["msg"] == "Field required"  # 에러 메시지
    assert error_detail["type"] == "missing"  # 에러 타입


def test_logout_success(client: TestClient, db_session: Session):
    """Test successful user logout with database integration."""
    # Arrange: 이미 존재하는 사용자를 DB에 추가합니다.
    existing_user = User(
        login_id="logout_user",
        hashed_password=pwd_context.hash("ValidPass123!"),
        username="Logout-User",
    )
    db_session.add(existing_user)
    db_session.commit()

    # 먼저 로그인하여 토큰을 얻습니다.
    login_data = {
        "login_id": "logout_user",
        "password": "ValidPass123!",
    }
    login_response = client.post("/api/v1/auth/login", json=login_data)
    db_session.flush()
    login_response_json = login_response.json()
    refresh_token = login_response_json["data"]["refresh"]

    # Act: 올바른 리프레시 토큰으로 로그아웃 시도
    logout_data = {
        "refresh": refresh_token,
    }
    response = client.post("/api/v1/auth/logout", json=logout_data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 200
    response_json = response.json()
    assert response_json["ok"] is True
    assert response_json["data"]["status"] == "success"
    assert response_json["error"] is None


def test_logout_invalid_token(client: TestClient, db_session: Session):
    """Test user logout with an invalid refresh token."""
    # Act: 잘못된 리프레시 토큰으로 로그아웃 시도
    logout_data = {
        "refresh": "invalid_token",
    }
    response = client.post("/api/v1/auth/logout", json=logout_data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 401
    response_json = response.json()
    assert response_json["detail"] == "Invalid token"


def test_refresh_success(client: TestClient, db_session: Session):
    """Test successful token refresh with database integration."""
    # Arrange: 이미 존재하는 사용자를 DB에 추가합니다.
    existing_user = User(
        login_id="refresh_user",
        hashed_password=pwd_context.hash("ValidPass123!"),
        username="Refresh-User",
    )
    db_session.add(existing_user)
    db_session.commit()

    # 먼저 로그인하여 토큰을 얻습니다.
    login_data = {
        "login_id": "refresh_user",
        "password": "ValidPass123!",
    }
    login_response = client.post("/api/v1/auth/login", json=login_data)
    db_session.flush()
    login_response_json = login_response.json()
    refresh_token = login_response_json["data"]["refresh"]

    # Act: 올바른 리프레시 토큰으로 토큰 갱신 시도
    refresh_data = {
        "refresh": refresh_token,
    }
    response = client.post("/api/v1/auth/refresh", json=refresh_data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 200
    response_json = response.json()
    assert response_json["ok"] is True
    assert "access" in response_json["data"]
    assert "refresh" in response_json["data"]
    assert response_json["error"] is None


def test_refresh_invalid_token(client: TestClient, db_session: Session):
    """Test token refresh with an invalid refresh token."""
    # Act: 잘못된 리프레시 토큰으로 토큰 갱신 시도
    refresh_data = {
        "refresh": "invalid_token",
    }
    response = client.post("/api/v1/auth/refresh", json=refresh_data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 401
    response_json = response.json()
    assert response_json["detail"] == "Invalid token"


def test_verify_success(client: TestClient, db_session: Session):
    """Test successful access token verification with database integration."""
    # Arrange: 이미 존재하는 사용자를 DB에 추가합니다.
    existing_user = User(
        login_id="verify_user",
        hashed_password=pwd_context.hash("ValidPass123!"),
        username="Verify-User",
    )
    db_session.add(existing_user)
    db_session.commit()

    # 먼저 로그인하여 토큰을 얻습니다.
    login_data = {
        "login_id": "verify_user",
        "password": "ValidPass123!",
    }
    login_response = client.post("/api/v1/auth/login", json=login_data)
    db_session.flush()
    login_response_json = login_response.json()
    access_token = login_response_json["data"]["access"]

    # Pass the token in the Authorization header if required by your API
    headers = {"Authorization": f"Bearer {access_token}"}
    response = client.post(
        "/api/v1/auth/verify", json={"access": access_token}, headers=headers
    )
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 200
    response_json = response.json()
    assert response_json["ok"] is True
    assert response_json["data"]["login_id"] == "verify_user"
    assert response_json["error"] is None


def test_verify_invalid_token(client: TestClient, db_session: Session):
    """Test access token verification with an invalid token."""
    # Act: 잘못된 액세스 토큰으로 검증 시도
    verify_data = {
        "access": "invalid_token",
    }
    response = client.post("/api/v1/auth/verify", json=verify_data)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 403
    response_json = response.json()
    assert response_json["detail"] == "Not authenticated"


def test_me_success(client: TestClient, db_session: Session):
    """Test successful retrieval of user profile with database integration."""
    # Arrange: 이미 존재하는 사용자를 DB에 추가합니다.
    existing_user = User(
        login_id="profile_user",
        hashed_password=pwd_context.hash("ValidPass123!"),
        username="Profile-User",
    )
    db_session.add(existing_user)
    db_session.commit()

    # 먼저 로그인하여 토큰을 얻습니다.
    login_data = {
        "login_id": "profile_user",
        "password": "ValidPass123!",
    }
    login_response = client.post("/api/v1/auth/login", json=login_data)
    db_session.flush()
    login_response_json = login_response.json()
    access_token = login_response_json["data"]["access"]

    # Pass the token in the Authorization header
    headers = {"Authorization": f"Bearer {access_token}"}
    response = client.get("/api/v1/user/me", headers=headers)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 200
    response_json = response.json()
    assert response_json["ok"] is True
    assert response_json["data"]["login_id"] == "profile_user"
    assert response_json["data"]["username"] == "Profile-User"
    assert response_json["error"] is None


def test_me_unauthenticated(client: TestClient, db_session: Session):
    """Test retrieval of user profile without authentication."""
    # Act: 인증 없이 사용자 프로필 조회 시도
    response = client.get("/api/v1/user/me")
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 403
    response_json = response.json()
    assert response_json["detail"] == "Not authenticated"


def test_me_invalid_token(client: TestClient, db_session: Session):
    """Test retrieval of user profile with an invalid token."""
    # Act: 잘못된 액세스 토큰으로 사용자 프로필 조회 시도
    headers = {"Authorization": "Bearer invalid_token"}
    response = client.get("/api/v1/user/me", headers=headers)
    db_session.flush()

    # Assert: API 응답을 검증합니다.
    assert response.status_code == 401
    response_json = response.json()
    assert response_json["detail"] == "Invalid token"
