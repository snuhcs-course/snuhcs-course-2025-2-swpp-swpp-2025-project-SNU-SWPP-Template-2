import os
from datetime import date
from unittest.mock import AsyncMock, MagicMock, Mock

import pytest

# 에러 클래스 import 경로 확인 (app/features/journal/errors.py)
from app.features.journal.errors import (
    ImageUploadError,
    JournalBadRequestError,
    JournalNotFoundError,
    JournalUpdateError,
)
from app.features.journal.models import Journal, JournalEmotion, JournalImage
from app.features.journal.repository import JournalRepository, S3Repository
from app.features.journal.schemas.requests import (
    ImageCompletionRequest,
    ImageGenerateRequest,
    ImageUploadRequest,
)
from app.features.journal.service import JournalOpenAIService, JournalService

# --- 픽스처: Mock 객체 준비 ---


@pytest.fixture
def mock_journal_repo() -> Mock:
    """JournalRepository의 가짜 Mock 객체를 생성합니다."""
    return Mock(spec=JournalRepository)


@pytest.fixture
def mock_s3_repo() -> Mock:
    """S3Repository의 가짜 Mock 객체를 생성합니다."""
    # S3 관련 메서드는 async이므로 AsyncMock을 사용하는 것이 좋습니다.
    repo = Mock(spec=S3Repository)
    repo.generate_upload_url = AsyncMock()
    repo.check_file_exists = AsyncMock()
    repo.delete_object = AsyncMock()
    return repo


@pytest.fixture
def journal_service(mock_journal_repo: Mock, mock_s3_repo: Mock) -> JournalService:
    """가짜 Repository를 주입받은 JournalService 인스턴스를 생성합니다."""
    return JournalService(
        journal_repository=mock_journal_repo, s3_repository=mock_s3_repo
    )


# --- JournalService 테스트 케이스 ---


def test_create_journal_success(
    journal_service: JournalService, mock_journal_repo: Mock
):
    """
    [Service] create_journal 성공 테스트
    """
    # Given
    user_id = 1
    title = "테스트 일기"
    content = "내용"
    emotions = {"happy": 5}
    gratitude = "감사"

    mock_journal = Journal(id=100, user_id=user_id, title=title)
    mock_journal_repo.add_journal.return_value = mock_journal

    # When
    result = journal_service.create_journal(
        user_id=user_id,
        title=title,
        content=content,
        emotions=emotions,
        gratitude=gratitude,
    )

    # Then
    mock_journal_repo.add_journal.assert_called_once_with(
        user_id=user_id,
        title=title,
        content=content,
        emotions=emotions,
        gratitude=gratitude,
    )
    assert result == mock_journal


def test_get_journal_success(journal_service: JournalService, mock_journal_repo: Mock):
    """
    [Service] get_journal 성공 테스트
    """
    # Given
    journal_id = 1
    mock_journal = Journal(id=journal_id, title="Found")
    mock_journal_repo.get_journal_by_id.return_value = mock_journal

    # When
    result = journal_service.get_journal(journal_id)

    # Then
    mock_journal_repo.get_journal_by_id.assert_called_once_with(journal_id)
    assert result == mock_journal


def test_get_journal_not_found(
    journal_service: JournalService, mock_journal_repo: Mock
):
    """
    [Service] get_journal 실패 테스트 (404)
    """
    # Given
    mock_journal_repo.get_journal_by_id.return_value = None

    # When & Then
    with pytest.raises(JournalNotFoundError):
        journal_service.get_journal(999)


def test_delete_journal_success(
    journal_service: JournalService, mock_journal_repo: Mock
):
    """
    [Service] delete_journal 성공 테스트
    """
    # Given
    journal_id = 1
    mock_journal = Journal(id=journal_id)
    mock_journal_repo.get_journal_by_id.return_value = mock_journal

    # When
    journal_service.delete_journal(journal_id)

    # Then
    mock_journal_repo.delete_journal.assert_called_once_with(mock_journal)


def test_list_journals_by_user(
    journal_service: JournalService, mock_journal_repo: Mock
):
    """
    [Service] list_journals_by_user 테스트
    """
    # Given
    user_id = 1
    limit = 10
    cursor = 5
    mock_list = [Journal(id=1), Journal(id=2)]
    mock_journal_repo.list_journals_by_user.return_value = mock_list

    # When
    result = journal_service.list_journals_by_user(user_id, limit, cursor)

    # Then
    mock_journal_repo.list_journals_by_user.assert_called_once_with(
        user_id, limit, cursor
    )
    assert result == mock_list


def test_update_journal_success(
    journal_service: JournalService, mock_journal_repo: Mock
):
    """
    [Service] update_journal 성공 테스트
    """
    # Given
    journal_id = 1
    mock_journal = Journal(id=journal_id, title="Old")
    mock_journal_repo.get_journal_by_id.return_value = mock_journal

    # When
    journal_service.update_journal(journal_id, title="New Title")

    # Then
    mock_journal_repo.update_journal.assert_called_once_with(
        journal=mock_journal, title="New Title", content=None, gratitude=None
    )


def test_update_journal_not_found(
    journal_service: JournalService, mock_journal_repo: Mock
):
    """
    [Service] update_journal 실패 테스트 (일기 없음)
    """
    # Given
    mock_journal_repo.get_journal_by_id.return_value = None

    # When & Then
    with pytest.raises(JournalNotFoundError):
        journal_service.update_journal(journal_id=999, title="수정 시도")


def test_update_journal_no_data(
    journal_service: JournalService, mock_journal_repo: Mock
):
    """
    [Service] update_journal 실패 테스트 (데이터 없음)
    """
    # When & Then
    with pytest.raises(JournalUpdateError):
        journal_service.update_journal(journal_id=1)  # 모든 인자 None


def test_search_journals_success(
    journal_service: JournalService, mock_journal_repo: Mock
):
    """
    [Service] search_journals 성공 테스트
    """
    # Given
    user_id = 1
    title = "search"
    mock_result = [Journal(id=1, title="search result")]
    mock_journal_repo.search_journals.return_value = mock_result

    # When
    result = journal_service.search_journals(user_id, title=title)

    # Then
    mock_journal_repo.search_journals.assert_called_once()
    assert result == mock_result


def test_search_journals_invalid_date(journal_service: JournalService):
    """
    [Service] search_journals 실패 테스트 (날짜 범위 오류)
    """
    # Given
    start = date(2023, 1, 10)
    end = date(2023, 1, 5)  # start > end

    # When & Then
    with pytest.raises(JournalBadRequestError):
        journal_service.search_journals(user_id=1, start_date=start, end_date=end)


# --- S3 이미지 관련 테스트 (Async) ---


@pytest.mark.asyncio
async def test_create_image_presigned_url_success(
    journal_service: JournalService, mock_journal_repo: Mock, mock_s3_repo: Mock
):
    """
    [Service] create_image_presigned_url 성공 테스트
    """
    # Given
    journal_id = 1
    payload = ImageUploadRequest(filename="test.jpg", content_type="image/jpeg")

    mock_journal = Journal(id=journal_id)
    mock_journal_repo.get_journal_by_id.return_value = mock_journal

    mock_s3_repo.generate_upload_url.return_value = {
        "presigned_url": "http://pre-signed",
        "file_url": "http://file-url",
    }

    # When
    result = await journal_service.create_image_presigned_url(journal_id, payload)

    # Then
    mock_journal_repo.get_journal_by_id.assert_called_with(journal_id=journal_id)
    mock_s3_repo.generate_upload_url.assert_awaited_once()
    assert result.presigned_url == "http://pre-signed"
    assert "images/journals/1/" in result.s3_key  # 키 생성 로직 확인


@pytest.mark.asyncio
async def test_create_image_presigned_url_journal_not_found(
    journal_service: JournalService, mock_journal_repo: Mock
):
    """
    [Service] create_image_presigned_url 실패 테스트 (저널 없음)
    """
    # Given
    mock_journal_repo.get_journal_by_id.return_value = None
    payload = ImageUploadRequest(filename="t.jpg", content_type="image/jpeg")

    # When & Then
    with pytest.raises(JournalNotFoundError):
        await journal_service.create_image_presigned_url(999, payload)


@pytest.mark.asyncio
async def test_complete_image_upload_success(
    journal_service: JournalService, mock_journal_repo: Mock, mock_s3_repo: Mock
):
    """
    [Service] complete_image_upload 성공 테스트
    """
    # Given
    journal_id = 1
    payload = ImageCompletionRequest(s3_key="some/key.jpg")

    mock_journal = Journal(id=journal_id)
    mock_journal_repo.get_journal_by_id.return_value = mock_journal

    # S3 파일 존재 확인
    mock_s3_repo.check_file_exists.return_value = True

    # 기존 이미지 없음
    mock_journal_repo.get_image_by_journal_id.return_value = None

    # 저장될 이미지
    mock_saved_image = JournalImage(id=10, journal_id=journal_id, s3_key="some/key.jpg")
    mock_journal_repo.replace_journal_image.return_value = mock_saved_image

    # When
    result = await journal_service.complete_image_upload(journal_id, payload)

    # Then
    mock_s3_repo.check_file_exists.assert_awaited_once_with(payload.s3_key)
    mock_journal_repo.replace_journal_image.assert_called_once()
    assert result == mock_saved_image


@pytest.mark.asyncio
async def test_complete_image_upload_file_missing(
    journal_service: JournalService, mock_s3_repo: Mock
):
    """
    [Service] complete_image_upload 실패 테스트 (S3에 파일 없음)
    """
    # Given
    mock_s3_repo.check_file_exists.return_value = False
    payload = ImageCompletionRequest(s3_key="missing.jpg")

    # When & Then
    with pytest.raises(ImageUploadError):
        await journal_service.complete_image_upload(1, payload)


# --- JournalOpenAIService 테스트 ---


@pytest.mark.asyncio  # async 함수 테스트
async def test_extract_keywords_logic(mocker):
    """
    [Service] extract_keywords_with_emotion_associations 테스트
    - 목표: LLM(LangChain)을 올바른 인자로 호출하고, 그 결과를 repository에 잘 저장하는가?
    """
    # 1. 준비 (Given)
    mock_repo = Mock(spec=JournalRepository)

    # JournalOpenAIService의 __init__이 복잡하므로 의존성 주입을 모킹
    # __init__ 내부에서 AsyncOpenAI()를 호출하므로 이를 모킹해야 함
    mocker.patch("app.features.journal.service.AsyncOpenAI")

    # _load_prompt 모킹
    mocker.patch(
        "app.features.journal.service._load_prompt", return_value="fake prompt"
    )

    # ChatOpenAI 모킹 (LangChain)
    mock_chain = AsyncMock()
    mock_structured_llm = MagicMock()
    mock_structured_llm.with_structured_output.return_value = mock_chain
    mocker.patch(
        "app.features.journal.service.ChatOpenAI", return_value=mock_structured_llm
    )

    # 서비스 인스턴스 생성 (이제 __init__에서 에러가 안 남)
    service = JournalOpenAIService(journal_repository=mock_repo)

    # (테스트용 데이터 준비)
    journal_id = 1
    mock_journal = Journal(
        id=journal_id,
        content="오늘 너무 행복했다.",
        emotions=[JournalEmotion(emotion="happy", intensity=5)],
    )
    mock_repo.get_journal_by_id.return_value = mock_journal

    # (LangChain이 반환할 가짜 결과)
    mock_llm_result = MagicMock()  # JournalKeywordsListResponse 모형
    mock_llm_result.data = [
        {
            "keyword": "강아지",
            "emotion": "happy",
            "summary": "오늘 강아지를 봐서 행복했다.",
            "weight": 0.9,
        }
    ]
    mock_chain.return_value = mock_llm_result
    mock_chain.ainvoke.return_value = mock_llm_result

    # (Repo가 반환할 가짜 결과)
    mock_saved_keywords = [MagicMock()]  # list[JournalKeyword] 모형
    mock_repo.add_keywords_emotion_associations.return_value = mock_saved_keywords

    # 2. 실행 (When)
    result = await service.extract_keywords_with_emotion_associations(journal_id)

    # 3. 검증 (Then)
    # 3-1. 올바른 Journal을 조회했는가?
    mock_repo.get_journal_by_id.assert_called_once_with(journal_id=journal_id)

    # 3-2. LangChain 체인을 올바른 입력으로 호출했는가?
    assert mock_chain.called

    # 3-3. LLM의 결과를 Repository에 잘 전달했는가?
    mock_repo.add_keywords_emotion_associations.assert_called_once_with(
        journal_id=journal_id, keyword_emotion_associations=mock_llm_result.data
    )

    # 3-4. 최종 결과를 잘 반환했는가?
    assert result == mock_saved_keywords


def test_get_journals_by_keyword(
    journal_service: JournalService, mock_journal_repo: Mock
):
    """
    [Service] get_journals_by_keyword 테스트
    """
    # Given
    user_id = 1
    keyword = "happy"
    mock_result = [Journal(id=1)]
    mock_journal_repo.get_journals_by_keyword.return_value = mock_result

    # When
    result = journal_service.get_journals_by_keyword(user_id, keyword)

    # Then
    mock_journal_repo.get_journals_by_keyword.assert_called_once_with(
        user_id=user_id, keyword=keyword, limit=10, cursor=None
    )
    assert result == mock_result


def test_get_journal_owner(journal_service: JournalService, mock_journal_repo: Mock):
    """
    [Service] get_journal_owner 테스트
    """
    # Given
    journal_id = 1
    mock_journal = Journal(id=journal_id, user_id=99)
    mock_journal_repo.get_journal_by_id.return_value = mock_journal

    # When
    owner_id = journal_service.get_journal_owner(journal_id)

    # Then
    assert owner_id == 99


def test_get_journal_owner_none(
    journal_service: JournalService, mock_journal_repo: Mock
):
    """
    [Service] get_journal_owner 실패 테스트 (존재하지 않는 일지)
    """
    # Given
    mock_journal_repo.get_journal_by_id.return_value = None

    # When
    owner_id = journal_service.get_journal_owner(999)

    # Then
    assert owner_id is None


@pytest.mark.asyncio
async def test_create_image_presigned_url_s3_fail(
    journal_service: JournalService, mock_journal_repo: Mock, mock_s3_repo: Mock
):
    """
    [Service] create_image_presigned_url 실패 테스트 (S3 에러)
    """
    # Given
    mock_journal_repo.get_journal_by_id.return_value = Journal(id=1)
    mock_s3_repo.generate_upload_url.return_value = None  # S3 실패 시뮬레이션

    payload = ImageUploadRequest(filename="t.jpg", content_type="image/jpeg")

    # When & Then
    with pytest.raises(ImageUploadError):
        await journal_service.create_image_presigned_url(1, payload)


# --- JournalOpenAIService 이미지 생성 테스트 ---


@pytest.mark.asyncio
async def test_request_image_generation_success(mocker):
    """
    [Service] request_image_generation (AI 이미지 생성) 테스트
    """
    # Mock Setup
    mocker.patch.dict(os.environ, {"OPENAI_API_KEY": "fake_key_for_testing"})
    mocker.patch(
        "app.features.journal.service._load_prompt", return_value="fake_prompt"
    )

    # OpenAI Client Mocking
    mock_chain = AsyncMock()
    mock_structured_llm = MagicMock()
    mock_structured_llm.with_structured_output.return_value = mock_chain
    mocker.patch(
        "app.features.journal.service.ChatOpenAI", return_value=mock_structured_llm
    )

    # Service 인스턴스 생성 (init bypass 후 수동 설정)
    mock_client = AsyncMock()
    mocker.patch("app.features.journal.service.AsyncOpenAI", return_value=mock_client)
    # 프롬프트 로드 Mocking
    service = JournalOpenAIService(journal_repository=MagicMock())

    # 메서드 내부에서 사용할 헬퍼 함수들의 반환값 모킹

    # _generate_scene_prompt_from_diary (GPT 호출) 모킹
    # service.py의 내부 private 메서드를 직접 모킹
    mocker.patch.object(
        service,
        "_generate_scene_prompt_from_diary",
        new_callable=AsyncMock,
        return_value="A beautiful scene",
    )

    # _generate_image_from_prompt (DALL-E 호출) 모킹
    mocker.patch.object(
        service,
        "_generate_image_from_prompt",
        new_callable=AsyncMock,
        return_value="base64_image_data",
    )

    # Execution
    request = ImageGenerateRequest(content="Today was good", style="natural")
    result = await service.request_image_generation(request)

    # Verification
    assert result == "base64_image_data"

    # 서비스 내부의 헬퍼 함수들이 올바르게 호출되었는지 확인
    service._generate_scene_prompt_from_diary.assert_awaited_once_with(
        request.content, service.prompt_natural
    )
    service._generate_image_from_prompt.assert_awaited_once_with("A beautiful scene")
