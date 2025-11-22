from datetime import date
from unittest.mock import MagicMock, Mock

import pytest
from botocore.exceptions import ClientError
from sqlalchemy.orm import Session
from sqlalchemy.sql.selectable import Select

from app.features.journal.models import Journal, JournalImage, JournalKeyword
from app.features.journal.repository import JournalRepository, S3Repository
from app.features.journal.schemas.responses import KeywordEmotionAssociationItem


@pytest.fixture
def mock_session() -> Mock:
    """SQLAlchemy Session의 가짜 Mock 객체를 생성합니다."""
    # 쿼리 체이닝(query.filter.order_by...)을 테스트하기 위해 MagicMock 사용
    return MagicMock(spec=Session)


@pytest.fixture
def journal_repo(mock_session: Mock) -> JournalRepository:
    """가짜 Session을 주입받은 JournalRepository 인스턴스를 생성합니다."""
    return JournalRepository(session=mock_session)


# --- 테스트 케이스 ---


def test_add_journal_db(journal_repo: JournalRepository, mock_session: Mock):
    """
    [Repository] add_journal 테스트
    - 목표: session.add와 session.flush가 올바르게 호출되는가?
    """
    # 1. 준비 (Given)
    user_id = 1
    title = "DB 저장 테스트"
    content = "내용"
    emotions = {"happy": 5, "sad": 1}
    gratitude = "감사"

    # 2. 실행 (When)
    journal = journal_repo.add_journal(
        user_id=user_id,
        title=title,
        content=content,
        emotions=emotions,
        gratitude=gratitude,
    )

    # 3. 검증 (Then)
    assert journal.user_id == user_id
    assert journal.title == title
    assert journal.gratitude == gratitude
    assert len(journal.emotions) == 2
    assert journal.emotions[0].emotion == "happy"
    assert journal.emotions[0].intensity == 5
    mock_session.add.assert_called_once_with(journal)
    mock_session.flush.assert_called_once()


def test_get_journal_by_id(journal_repo: JournalRepository, mock_session: Mock):
    """
    [Repository] get_journal_by_id 테스트
    - 목표: session.get이 올바른 인자로 호출되는가?
    """
    # 1. 준비 (Given)
    journal_id = 42
    mock_journal_obj = Journal(id=journal_id)
    mock_session.get.return_value = mock_journal_obj

    # 2. 실행 (When)
    result = journal_repo.get_journal_by_id(journal_id)

    # 3. 검증 (Then)
    mock_session.get.assert_called_once_with(Journal, journal_id)
    assert result == mock_journal_obj


def test_delete_journal(journal_repo: JournalRepository, mock_session: Mock):
    """
    [Repository] delete_journal 테스트
    - 목표: session.delete가 올바른 인자로 호출되는가?
    """
    # 1. 준비 (Given)
    mock_journal = Journal(id=1)

    # 2. 실행 (When)
    journal_repo.delete_journal(mock_journal)

    # 3. 검증 (Then)
    mock_session.delete.assert_called_once_with(mock_journal)


def test_update_journal(journal_repo: JournalRepository, mock_session: Mock):
    """
    [Repository] update_journal 테스트
    - 목표: 전달된 Journal 객체의 속성을 변경하고 flush를 호출하는가?
    """
    # 1. 준비 (Given)
    mock_journal = MagicMock(spec=Journal)
    mock_journal.title = "Old Title"
    mock_journal.content = "Old Content"
    mock_journal.gratitude = None

    # 2. 실행 (When)
    journal_repo.update_journal(
        journal=mock_journal,
        title="New Title",
        content="New Content",
        gratitude="New Gratitude",
    )

    # 3. 검증 (Then)
    assert mock_journal.title == "New Title"
    assert mock_journal.content == "New Content"
    assert mock_journal.gratitude == "New Gratitude"
    mock_session.flush.assert_called_once()


def test_update_journal_partial(journal_repo: JournalRepository, mock_session: Mock):
    """
    [Repository] update_journal 부분 수정 테스트
    - 목표: None이 아닌 값만 업데이트하는가?
    """
    # 1. 준비 (Given)
    mock_journal = MagicMock(spec=Journal)
    mock_journal.title = "Old Title"
    mock_journal.content = "Old Content"

    # 2. 실행 (When)
    journal_repo.update_journal(journal=mock_journal, title="New Title", content=None)

    # 3. 검증 (Then)
    assert mock_journal.title == "New Title"
    assert mock_journal.content == "Old Content"  # None이므로 변경되지 않음
    mock_session.flush.assert_called_once()


def test_list_journals_by_user_no_cursor(
    journal_repo: JournalRepository, mock_session: Mock
):
    """
    [Repository] list_journals_by_user (커서 없음) 테스트
    - 목표: 올바른 쿼리 체이닝을 생성하는가?
    """
    # 1. 준비 (Given)
    user_id = 1
    limit = 5

    # 2. 실행 (When)
    journal_repo.list_journals_by_user(user_id=user_id, limit=limit, cursor=None)

    # 3. 검증 (Then)
    mock_session.query.assert_called_once_with(Journal)
    chain = mock_session.query.return_value

    # filter(user_id)
    chain.filter.assert_called_once()
    # order_by(id.desc)
    chain.filter.return_value.order_by.assert_called_once()
    # limit(5)
    chain.filter.return_value.order_by.return_value.limit.assert_called_once_with(limit)
    # all()
    chain.filter.return_value.order_by.return_value.limit.return_value.all.assert_called_once()


def test_list_journals_by_user_with_cursor(
    journal_repo: JournalRepository, mock_session: Mock
):
    """
    [Repository] list_journals_by_user (커서 있음) 테스트
    - 목표: 커서가 있을 때 filter가 추가로 호출되는가?
    """
    # 1. 준비 (Given)
    user_id = 1
    limit = 10
    cursor = 100

    # 2. 실행 (When)
    journal_repo.list_journals_by_user(user_id=user_id, limit=limit, cursor=cursor)

    # 3. 검증 (Then)
    mock_session.query.assert_called_once_with(Journal)
    chain = mock_session.query.return_value

    # 3-1. filter(user_id) 호출
    chain.filter.assert_called_once()

    # 3-2. order_by(id.desc) 호출
    chain.filter.return_value.order_by.assert_called_once()

    # 3-3. filter(id < cursor) 호출
    chain.filter.return_value.order_by.return_value.filter.assert_called_once()

    # 3-4. limit(10) 호출
    chain.filter.return_value.order_by.return_value.filter.return_value.limit.assert_called_once_with(
        limit
    )

    # 3-5. all() 호출
    chain.filter.return_value.order_by.return_value.filter.return_value.limit.return_value.all.assert_called_once()


def test_search_journals_all_params(
    journal_repo: JournalRepository, mock_session: Mock
):
    """
    [Repository] search_journals (모든 파라미터) 테스트
    - 목표: 모든 인자가 있을 때 올바른 쿼리 체이닝을 생성하는가?
    """
    # 1. 준비 (Given)
    user_id = 1
    title = "파이썬"
    start_date = date(2023, 1, 1)
    end_date = date(2023, 1, 31)
    limit = 20
    cursor = 100

    # 2. 실행 (When)
    journal_repo.search_journals(
        user_id=user_id,
        title=title,
        start_date=start_date,
        end_date=end_date,
        limit=limit,
        cursor=cursor,
    )

    # 3. 검증 (Then)
    mock_session.query.assert_called_once_with(Journal)

    chain = mock_session.query.return_value

    chain.filter.assert_called_once()  # .filter(user_id)
    chain.filter.return_value.filter.assert_called_once()  # .filter(title)
    chain.filter.return_value.filter.return_value.filter.assert_called_once()  # .filter(start_date)
    chain.filter.return_value.filter.return_value.filter.return_value.filter.assert_called_once()  # .filter(end_date)
    chain.filter.return_value.filter.return_value.filter.return_value.filter.return_value.filter.assert_called_once()  # .filter(cursor)

    # 5번의 filter가 끝난 후의 최종 체인
    final_chain = chain.filter.return_value.filter.return_value.filter.return_value.filter.return_value.filter.return_value

    final_chain.order_by.assert_called_once()
    final_chain.order_by.return_value.limit.assert_called_once_with(limit)
    final_chain.order_by.return_value.limit.return_value.all.assert_called_once()


def test_add_keywords_emotion_associations(
    journal_repo: JournalRepository, mock_session: Mock
):
    """
    [Repository] add_keywords_emotion_associations 테스트
    - 목표: 기존 키워드를 삭제하고 새 키워드를 add, flush 하는가?
    """
    # 1. 준비 (Given)
    journal_id = 1
    mock_journal = MagicMock(spec=Journal)
    mock_session.get.return_value = mock_journal

    associations = [
        KeywordEmotionAssociationItem(
            keyword="k1", emotion="happy", summary="s1", weight=0.5
        ),
        KeywordEmotionAssociationItem(
            keyword="k2", emotion="sad", summary="s2", weight=0.8
        ),
    ]

    # 2. 실행 (When)
    result = journal_repo.add_keywords_emotion_associations(journal_id, associations)

    # 3. 검증 (Then)
    assert mock_session.get.call_count == 2
    mock_session.get.assert_any_call(Journal, journal_id)

    # 3-2. 기존 키워드를 삭제했는가? (drop_journal_keywords 로직)
    assert mock_journal.keywords == []

    # 3-3. 새 키워드를 2번 add 했는가?
    assert mock_session.add.call_count == 2
    assert isinstance(mock_session.add.call_args_list[0].args[0], JournalKeyword)
    assert mock_session.add.call_args_list[0].args[0].keyword == "k1"
    assert isinstance(mock_session.add.call_args_list[1].args[0], JournalKeyword)
    assert mock_session.add.call_args_list[1].args[0].keyword == "k2"

    # 3-4. flush를 호출했는가?
    mock_session.flush.assert_called_once()

    # 3-5. 결과를 반환했는가?
    assert len(result) == 2


def test_get_journals_by_keyword(journal_repo: JournalRepository, mock_session: Mock):
    """
    [Repository] get_journals_by_keyword 테스트
    - 목표: session.execute가 select 구문으로 호출되는가?
    """
    # 1. 준비 (Given)

    # 2. 실행 (When)
    journal_repo.get_journals_by_keyword(user_id=1, keyword="test", limit=5)

    # 3. 검증 (Then)
    mock_session.execute.assert_called_once()

    executed_arg = mock_session.execute.call_args[0][0]
    assert isinstance(executed_arg, Select)

    mock_session.execute.return_value.scalars.assert_called_once()
    mock_session.execute.return_value.scalars.return_value.all.assert_called_once()


def test_replace_journal_image_new(journal_repo: JournalRepository, mock_session: Mock):
    """
    [Repository] replace_journal_image (새 이미지) 테스트
    - 목표: 기존 이미지가 없을 때 delete 없이 add, flush가 1번씩 호출되는가?
    """
    # 1. 준비 (Given)
    journal_id = 1
    s3_key = "new_key"

    # 2. 실행 (When)
    result = journal_repo.replace_journal_image(
        journal_id=journal_id, existing_image=None, s3_key=s3_key
    )

    # 3. 검증 (Then)
    mock_session.delete.assert_not_called()
    mock_session.add.assert_called_once()
    mock_session.flush.assert_called_once()

    assert isinstance(result, JournalImage)
    assert result.journal_id == journal_id
    assert result.s3_key == s3_key


def test_replace_journal_image_existing(
    journal_repo: JournalRepository, mock_session: Mock
):
    """
    [Repository] replace_journal_image (기존 이미지 교체) 테스트
    - 목표: 기존 이미지가 있을 때 delete, add, flush*2가 호출되는가?
    """
    # 1. 준비 (Given)
    journal_id = 1
    s3_key = "new_key"
    mock_existing = JournalImage(id=99, journal_id=journal_id, s3_key="old_key")

    # 2. 실행 (When)
    result = journal_repo.replace_journal_image(
        journal_id=journal_id, existing_image=mock_existing, s3_key=s3_key
    )

    # 3. 검증 (Then)
    mock_session.delete.assert_called_once_with(mock_existing)
    mock_session.add.assert_called_once()

    # delete 후 1번, add 후 1번
    assert mock_session.flush.call_count == 2

    assert isinstance(result, JournalImage)
    assert result.s3_key == s3_key


def test_delete_journal_image(journal_repo: JournalRepository, mock_session: Mock):
    """
    [Repository] delete_journal_image 테스트
    """
    # Given
    mock_image = JournalImage(id=1, s3_key="key")

    # When
    journal_repo.delete_journal_image(mock_image)

    # Then
    mock_session.delete.assert_called_once_with(mock_image)
    mock_session.flush.assert_called_once()


# --- S3Repository 테스트 (boto3 모킹) ---


@pytest.fixture
def s3_repo(mocker):
    # boto3.client를 모킹하여 실제 AWS 연결 방지
    mock_boto_client = MagicMock()
    mocker.patch(
        "app.features.journal.repository.boto3.client", return_value=mock_boto_client
    )
    return S3Repository()


@pytest.mark.asyncio
async def test_s3_generate_upload_url_success(s3_repo):
    """[S3Repository] Presigned URL 생성 성공"""
    # Given
    s3_key = "test.jpg"
    content_type = "image/jpeg"
    s3_repo.s3_client.generate_presigned_url.return_value = "http://presigned-url"

    # When
    result = await s3_repo.generate_upload_url(s3_key, content_type)

    # Then
    assert result["presigned_url"] == "http://presigned-url"
    assert s3_key in result["file_url"]


@pytest.mark.asyncio
async def test_s3_generate_upload_url_failure(s3_repo):
    """[S3Repository] Presigned URL 생성 실패 (ClientError)"""
    # Given
    s3_repo.s3_client.generate_presigned_url.side_effect = ClientError({}, "operation")

    # When
    result = await s3_repo.generate_upload_url("k", "t")

    # Then
    assert result is None


@pytest.mark.asyncio
async def test_s3_check_file_exists_true(s3_repo):
    """[S3Repository] 파일 존재 확인 (있음)"""
    # Given (head_object가 에러를 안 내면 성공)
    s3_repo.s3_client.head_object.return_value = {}

    # When
    exists = await s3_repo.check_file_exists("key")

    # Then
    assert exists is True


@pytest.mark.asyncio
async def test_s3_check_file_exists_false(s3_repo):
    """[S3Repository] 파일 존재 확인 (없음 - 404)"""
    # Given
    error_response = {"Error": {"Code": "404"}}
    s3_repo.s3_client.head_object.side_effect = ClientError(
        error_response, "head_object"
    )

    # When
    exists = await s3_repo.check_file_exists("key")

    # Then
    assert exists is False


@pytest.mark.asyncio
async def test_s3_delete_object(s3_repo):
    """[S3Repository] 파일 삭제"""
    # When
    result = await s3_repo.delete_object("key")

    # Then
    s3_repo.s3_client.delete_object.assert_called_once()
    assert result is True
