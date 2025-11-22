import os
from datetime import date
from unittest.mock import AsyncMock  # AsyncMock 사용

import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session

from app.features.journal.models import Journal, JournalEmotion, JournalKeyword
from app.features.journal.schemas.responses import (
    JournalImageResponse,
    PresignedUrlResponse,
)
from app.features.user.models import User

# --- 1. 일지 생성 (POST /) ---


def test_create_journal_success(
    client: TestClient,
    auth_headers: dict[str, str],
    db_session: Session,
    test_user: User,
):
    """
    일지 생성 (POST /api/v1/journal) 성공 테스트
    """
    journal_data = {
        "title": "오늘의 일지",
        "content": "오늘은 테스트 코드를 작성했다.",
        "emotions": {"happy": 1, "sad": 3, "anxious": 1, "calm": 3, "lethargic": 4},
        "gratitude": "오늘 살아있음에 감사하다.",
    }

    # 1. API 요청
    response = client.post("/api/v1/journal", headers=auth_headers, json=journal_data)
    db_session.flush()

    # 2. 상태 코드 검증 (201)
    assert response.status_code == 201

    # 3. 응답 데이터 검증
    response_data = response.json()
    assert response_data["title"] == journal_data["title"]
    assert response_data["content"] == journal_data["content"]
    assert response_data["gratitude"] == journal_data["gratitude"]
    assert "id" in response_data
    assert len(response_data["emotions"]) == 5
    assert {"emotion": "happy", "intensity": 1} in response_data["emotions"]

    # 4. DB 상태 검증
    db_journal = (
        db_session.query(Journal).filter(Journal.id == response_data["id"]).first()
    )
    assert db_journal is not None
    assert db_journal.title == "오늘의 일지"
    assert db_journal.user_id == test_user.id
    assert len(db_journal.emotions) == 5


# --- 2. 일지 목록 조회 (GET /me) ---


def test_get_journal_entries_by_user_success(
    client: TestClient,
    auth_headers: dict[str, str],
    db_session: Session,
    test_user: User,
    test_journal: Journal,  # 픽스처로 J1 생성됨
):
    """
    일지 목록 조회 (GET /api/v1/journal/me) 성공 테스트
    """
    # 두 번째 일지 J2 생성
    j2 = Journal(title="두 번째 일지", content="...", user_id=test_user.id)
    db_session.add(j2)
    db_session.commit()
    # test_journal (J1), j2 (J2) -> 총 2개. J2가 최신

    # 1. API 요청 (URL 수정: /journal -> /journal/me)
    response = client.get("/api/v1/journal/me?limit=10", headers=auth_headers)

    # 2. 상태 코드 검증
    assert response.status_code == 200

    # 3. 응답 데이터 검증 (커서 기반, total 없음)
    response_data = response.json()
    assert len(response_data["items"]) == 2
    assert response_data["next_cursor"] is None  # 10개 limit인데 2개뿐이므로
    assert response_data["items"][0]["title"] == j2.title
    assert response_data["items"][1]["title"] == test_journal.title


def test_get_journal_entries_pagination(
    client: TestClient,
    auth_headers: dict[str, str],
    db_session: Session,
    test_user: User,
):
    """
    일지 목록 조회 (GET /api/v1/journal/me) 커서 페이지네이션 테스트
    """
    j1 = Journal(title="J1", content="...", user_id=test_user.id)
    j2 = Journal(title="J2", content="...", user_id=test_user.id)
    j3 = Journal(title="J3", content="...", user_id=test_user.id)
    db_session.add_all([j1, j2, j3])
    db_session.commit()
    # 생성 순서: J1 -> J2 -> J3 (J3가 최신. ID가 가장 큼)

    # 1. API 요청 (limit=2)
    response1 = client.get("/api/v1/journal/me?limit=2", headers=auth_headers)
    assert response1.status_code == 200
    data1 = response1.json()

    assert len(data1["items"]) == 2
    assert data1["items"][0]["title"] == "J3"  # 최신
    assert data1["items"][1]["title"] == "J2"
    assert data1["next_cursor"] == j2.id  # 다음 커서는 J2의 ID

    cursor = data1["next_cursor"]

    # 2. API 요청 (cursor=J2.id, limit=2)
    response2 = client.get(
        f"/api/v1/journal/me?limit=2&cursor={cursor}", headers=auth_headers
    )
    assert response2.status_code == 200
    data2 = response2.json()

    assert len(data2["items"]) == 1
    assert data2["items"][0]["title"] == "J1"
    assert data2["next_cursor"] is None  # 마지막 페이지


def test_get_journal_entries_empty(
    client: TestClient,
    auth_headers: dict[str, str],
):
    """
    일지 목록 조회 (GET /api/v1/journal/me) 결과 없음 테스트
    """
    # 1. API 요청 (URL 수정)
    response = client.get("/api/v1/journal/me", headers=auth_headers)

    # 2. 상태 코드 검증
    assert response.status_code == 200

    # 3. 응답 데이터 검증
    response_data = response.json()
    assert len(response_data["items"]) == 0
    assert response_data["next_cursor"] is None


# --- 3. 일지 검색 (GET /search) ---


def test_search_journals_by_title_success(
    client: TestClient,
    auth_headers: dict[str, str],
    db_session: Session,
    test_user: User,
):
    """
    일지 검색 (GET /api/v1/journal/search) (제목) 성공 테스트
    """
    j1 = Journal(title="apple and banana", content="...", user_id=test_user.id)
    j2 = Journal(title="banana and grape", content="...", user_id=test_user.id)
    j3 = Journal(title="grape and apple", content="...", user_id=test_user.id)
    db_session.add_all([j1, j2, j3])
    db_session.commit()

    # 1. API 요청
    response = client.get("/api/v1/journal/search?title=banana", headers=auth_headers)

    # 2. 상태 코드 검증
    assert response.status_code == 200

    # 3. 응답 데이터 검증 (j3, j1이 검색되어야 함. 최신순)
    response_data = response.json()
    assert len(response_data["items"]) == 2
    assert response_data["items"][0]["title"] == "banana and grape"
    assert response_data["items"][1]["title"] == "apple and banana"


def test_search_journals_by_date_success(
    client: TestClient,
    auth_headers: dict[str, str],
    test_journal: Journal,  # conftest에서 생성됨
):
    """
    일지 검색 (GET /api/v1/journal/search) (기간) 성공 테스트
    """
    today = date.today().isoformat()

    # 1. API 요청 (오늘 날짜로 검색)
    response = client.get(
        f"/api/v1/journal/search/?start_date={today}&end_date={today}",
        headers=auth_headers,
    )

    # 2. 상태 코드 검증
    assert response.status_code == 200

    # 3. 응답 데이터 검증
    response_data = response.json()
    assert len(response_data["items"]) == 1
    assert response_data["items"][0]["id"] == test_journal.id


def test_search_journals_no_results(
    client: TestClient,
    auth_headers: dict[str, str],
):
    """
    일지 검색 (GET /api/v1/journal/search) 결과 없음 테스트
    """
    # 1. API 요청 (파라미터 'title' 수정)
    response = client.get(
        "/api/v1/journal/search/?title=zzxxyy99", headers=auth_headers
    )

    # 2. 상태 코드 검증
    assert response.status_code == 200

    # 3. 응답 데이터 검증
    response_data = response.json()
    assert len(response_data["items"]) == 0


def test_search_journals_bad_request(
    client: TestClient,
    auth_headers: dict[str, str],
):
    """
    일지 검색 (GET /api/v1/journal/search) 실패 테스트 (파라미터 없음 400)
    """
    # 1. API 요청 (아무 파라미터 없이)
    response = client.get("/api/v1/journal/search", headers=auth_headers)

    # 2. 상태 코드 검증 (400 Bad Request)
    assert response.status_code == 400


# --- 4. 키워드 기반 일지 검색 (GET /search-keyword) ---


def test_search_journals_by_keyword_success(
    client: TestClient,
    auth_headers: dict[str, str],
    test_journal: Journal,  # 'fixture', 'database' 키워드 가짐
):
    """
    키워드 기반 일지 검색 (GET /api/v1/journal/search-keyword) 성공 테스트
    """
    # 1. API 요청
    response = client.get(
        "/api/v1/journal/search-keyword?keyword=keyword1", headers=auth_headers
    )

    # 2. 상태 코드 검증
    assert response.status_code == 200

    # 3. 응답 데이터 검증
    response_data = response.json()
    assert len(response_data["items"]) == 1
    assert response_data["items"][0]["id"] == test_journal.id


# --- 5. 일지 단일 조회 (GET /{journal_id}) ---


def test_get_journal_success(
    client: TestClient,
    auth_headers: dict[str, str],
    test_journal: Journal,
):
    """
    일지 단일 조회 (GET /api/v1/journal/{journal_id}) 성공 테스트
    """
    # 1. API 요청
    response = client.get(f"/api/v1/journal/{test_journal.id}", headers=auth_headers)

    # 2. 상태 코드 검증
    assert response.status_code == 200

    # 3. 응답 데이터 검증 (emotions, keywords 확인)
    response_data = response.json()
    assert response_data["id"] == test_journal.id
    assert response_data["title"] == test_journal.title
    assert len(response_data["emotions"]) == 3
    assert {"emotion": "happy", "intensity": 5} in response_data["emotions"]
    assert len(response_data["keywords"]) == 2
    assert response_data["keywords"][0]["keyword"] == "keyword1"


def test_get_journal_not_found(
    client: TestClient,
    auth_headers: dict[str, str],
):
    """
    일지 단일 조회 (GET /api/v1/journal/{journal_id}) 실패 테스트 (404 Not Found)
    """
    # 1. API 요청 (존재하지 않는 ID)
    response = client.get("/api/v1/journal/99999", headers=auth_headers)

    # 2. 상태 코드 검증
    assert response.status_code == 401


# --- 6. 일지 수정 (PATCH /{journal_id}) ---


def test_update_journal_entry_success(
    client: TestClient,
    auth_headers: dict[str, str],
    db_session: Session,
    test_journal: Journal,
):
    """
    일지 수정 (PATCH /api/v1/journal/{journal_id}) 성공 테스트
    """
    update_data = {
        "title": "수정된 제목",
        "content": "수정된 내용입니다.",
        "gratitude": "수정된 감사일기.",
    }

    # 1. API 요청
    response = client.patch(
        f"/api/v1/journal/{test_journal.id}",
        headers=auth_headers,
        json=update_data,
    )
    db_session.flush()

    # 2. 상태 코드 검증 (200)
    assert response.status_code == 200

    # 3. 응답 데이터 검증
    assert response.text == '"Update Success"'

    # 4. DB 검증
    db_journal = db_session.get(Journal, test_journal.id)
    assert db_journal.title == update_data["title"]
    assert db_journal.content == update_data["content"]


# --- 7. 일지 삭제 (DELETE /{journal_id}) ---


def test_delete_journal_entry_success(
    client: TestClient,
    auth_headers: dict[str, str],
    db_session: Session,
    test_journal: Journal,
):
    """
    일지 삭제 (DELETE /api/v1/journal/{journal_id}) 성공 테스트
    """
    journal_id = test_journal.id

    # 1. API 요청
    response = client.delete(
        f"/api/v1/journal/{journal_id}",
        headers=auth_headers,
    )
    db_session.flush()

    # 2. 상태 코드 검증 (200)
    assert response.status_code == 200

    # 3. 응답 데이터 검증 (단순 문자열 "Deletion Success")
    assert response.text == '"Deletion Success"'

    # 4. DB 검증 (Cascade로 Emotion, Keyword도 삭제되어야 함)
    db_journal = db_session.get(Journal, journal_id)
    assert db_journal is None

    db_emotion = (
        db_session.query(JournalEmotion)
        .filter(JournalEmotion.journal_id == journal_id)
        .first()
    )
    assert db_emotion is None

    db_keyword = (
        db_session.query(JournalKeyword)
        .filter(JournalKeyword.journal_id == journal_id)
        .first()
    )
    assert db_keyword is None


# --- 8. 키워드/감정 연관관계 분석 (POST /{journal_id}/analyze) ---


@pytest.mark.asyncio
async def test_analyze_journal_success(
    client: TestClient,
    auth_headers: dict[str, str],
    test_journal: Journal,
    mocker,  # pytest-mock 픽스처
):
    """
    일지 분석/키워드 추출 (POST /{journal_id}/analyze) 성공 테스트
    """
    mocker.patch.dict(os.environ, {"OPENAI_API_KEY": "fake_key_for_testing"})

    # 모킹할 반환값 (Service는 list[JournalKeyword]를 반환)
    mocked_keywords_list = [
        JournalKeyword(
            journal_id=test_journal.id,
            keyword="키워드1",
            emotion="happy",
            summary="요약1",
            weight=0.8,
        ),
        JournalKeyword(
            journal_id=test_journal.id,
            keyword="키워드2",
            emotion="anxious",
            summary="요약2",
            weight=0.5,
        ),
    ]

    # 모킹 대상 수정 (JournalOpenAIService의 async 메서드)
    mocker.patch(
        "app.features.journal.service.JournalOpenAIService.extract_keywords_with_emotion_associations",
        new_callable=AsyncMock,  # async 함수이므로 AsyncMock 사용
        return_value=mocked_keywords_list,
    )

    # 1. API 요청 (엔드포인트, HTTP 메서드 수정)
    response = client.post(
        f"/api/v1/journal/{test_journal.id}/analyze",
        headers=auth_headers,
    )

    # 2. 상태 코드 검증 (201)
    assert response.status_code == 201

    # 3. 응답 데이터 검증 (JournalKeywordsListResponse 스키마)
    response_data = response.json()
    assert len(response_data["data"]) == 2
    assert response_data["data"][0]["keyword"] == "키워드1"
    assert response_data["data"][0]["emotion"] == "happy"
    assert response_data["data"][1]["keyword"] == "키워드2"


# --- 9. S3 업로드 URL 생성 (POST /{journal_id}/image) ---


@pytest.mark.asyncio
async def test_generate_image_upload_url_success(
    client: TestClient,
    auth_headers: dict[str, str],
    test_journal: Journal,
    mocker,
):
    """
    S3 Presigned URL 생성 (POST /{journal_id}/image) 성공 테스트
    """
    mock_presigned_response = PresignedUrlResponse(
        presigned_url="https://s3.example.com/upload-url",
        file_url="https://s3.example.com/file-url",
        s3_key="images/journals/...",
    )

    # JournalService의 async 메서드 모킹
    mocker.patch(
        "app.features.journal.service.JournalService.create_image_presigned_url",
        new_callable=AsyncMock,
        return_value=mock_presigned_response,
    )

    request_data = {"filename": "test.jpg", "content_type": "image/jpeg"}

    # 1. API 요청
    response = client.post(
        f"/api/v1/journal/{test_journal.id}/image",
        headers=auth_headers,
        json=request_data,
    )

    # 2. 상태 코드 검증 (201)
    assert response.status_code == 201

    # 3. 응답 데이터 검증
    response_data = response.json()
    assert response_data["presigned_url"] == mock_presigned_response.presigned_url
    assert response_data["s3_key"] == mock_presigned_response.s3_key


# --- 10. S3 업로드 완료 (POST /{journal_id}/image/complete) ---


@pytest.mark.asyncio
async def test_complete_image_upload_success(
    client: TestClient,
    auth_headers: dict[str, str],
    test_journal: Journal,
    mocker,
):
    """
    S3 업로드 완료 (POST /{journal_id}/image/complete) 성공 테스트
    """
    s3_key = "images/journals/1/test.jpg"

    # DB에 저장된 JournalImage 모형
    mock_journal_image = JournalImageResponse(
        id=1,
        journal_id=test_journal.id,
        s3_key=s3_key,
        created_at=date.today().isoformat(),
    )

    mocker.patch(
        "app.features.journal.service.JournalService.complete_image_upload",
        new_callable=AsyncMock,
        return_value=mock_journal_image,
    )

    request_data = {"s3_key": s3_key}

    # 1. API 요청
    response = client.post(
        f"/api/v1/journal/{test_journal.id}/image/complete",
        headers=auth_headers,
        json=request_data,
    )

    # 2. 상태 코드 검증 (201)
    assert response.status_code == 201

    # 3. 응답 데이터 검증
    response_data = response.json()
    assert response_data["s3_key"] == s3_key
    assert response_data["journal_id"] == test_journal.id


# --- 11. AI 이미지 생성 (POST /image/generate) ---


@pytest.mark.asyncio
async def test_request_journal_image_generation_success(
    client: TestClient,
    mocker,
):
    """
    AI 이미지 생성 (POST /image/generate) 성공 테스트
    """
    mock_response = "base64-encoded-image-string"
    mocker.patch.dict(os.environ, {"OPENAI_API_KEY": "fake_key_for_testing"})

    mocker.patch(
        "app.features.journal.service.JournalOpenAIService.request_image_generation",
        new_callable=AsyncMock,
        return_value=mock_response,
    )

    request_data = {
        "content": "A beautiful sunset over the mountains.",
        "style": "natural",
    }

    # 1. API 요청
    response = client.post(
        "/api/v1/journal/image/generate",
        json=request_data,
    )

    # 2. 상태 코드 검증 (201)
    assert response.status_code == 201

    # 3. 응답 데이터 검증
    response_data = response.json()
    assert response_data["image_base64"] == mock_response
