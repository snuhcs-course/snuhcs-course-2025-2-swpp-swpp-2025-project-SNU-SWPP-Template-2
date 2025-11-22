import pytest
from datetime import datetime
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from app.database.base import Base
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy import Integer, String

# -----------------------
# 실제 모델 Import
# -----------------------
from app.features.user.models import User
from app.features.selfaware.models import (
    Question,
    Answer,
    ValueScore,
    ValueMap,
)
from app.features.selfaware.repository import (
    QuestionRepository,
    AnswerRepository,
    ValueScoreRepository,
    ValueMapRepository,
)

# -----------------------
# DB Session Fixture
# -----------------------
@pytest.fixture(scope="function")
def db_session():
    engine = create_engine("sqlite:///:memory:", echo=False)
    TestingSessionLocal = sessionmaker(bind=engine)

    Base.metadata.create_all(engine)
    session = TestingSessionLocal()

    # 기본 User 1명 생성
    user = User(username="tester", login_id="tester",hashed_password="password")
    session.add(user)
    session.commit()

    yield session
    session.close()


# -----------------------
# Repo Fixtures
# -----------------------
@pytest.fixture
def question_repo(db_session):
    return QuestionRepository(session=db_session)

@pytest.fixture
def answer_repo(db_session):
    return AnswerRepository(session=db_session)

@pytest.fixture
def value_score_repo(db_session):
    return ValueScoreRepository(session=db_session)

@pytest.fixture
def value_map_repo(db_session):
    return ValueMapRepository(session=db_session)



# ============================================================
#                     Question Repository
# ============================================================

def test_create_question(question_repo, db_session):
    q = question_repo.create_question(
        user_id=1,
        question_type="single_category",
        text="오늘 어떤 하루였나요?",
    )
    db_session.commit()

    assert q.id is not None
    assert q.text == "오늘 어떤 하루였나요?"
    assert q.user_id == 1


def test_get_question_by_id(question_repo, db_session):
    q = question_repo.create_question(1, "single_category", "테스트 질문")
    db_session.commit()

    fetched = question_repo.get_question_by_id(q.id)
    assert fetched.id == q.id
    assert fetched.text == "테스트 질문"


def test_list_questions_by_user(question_repo, db_session):
    for i in range(1):
        question_repo.create_question(1, "single_category", f"Q{i}")
    db_session.commit()

    questions = question_repo.list_questions_by_user(1)
    assert len(questions) == 1


# ============================================================
#                     Answer Repository
# ============================================================

def test_create_answer(answer_repo, question_repo, db_session):
    q = question_repo.create_question(1, "single_category", "Q1")
    db_session.commit()

    a = answer_repo.create_answer(
        user_id=1,
        question_id=q.id,
        text="오늘은 즐거웠어요."
    )
    db_session.commit()

    assert a.id is not None
    assert a.question_id == q.id


def test_get_by_question(answer_repo, question_repo, db_session):
    q = question_repo.create_question(1, "single", "질문")
    db_session.commit()

    answer_repo.create_answer(1, q.id, "답변입니다")
    db_session.commit()

    fetched = answer_repo.get_by_question(q.id)
    assert fetched.text == "답변입니다"


def test_list_answers_by_user(answer_repo, question_repo, db_session):
    q1 = question_repo.create_question(1, "single", "Q1")
    db_session.commit()

    answer_repo.create_answer(1, q1.id, "A1")
    db_session.commit()

    answers = answer_repo.list_answers_by_user(1, [q1.id])
    assert len(answers) == 1



# ============================================================
#                ValueScore Repository
# ============================================================

def test_create_value_score(value_score_repo, question_repo, answer_repo, db_session):
    q = question_repo.create_question(1, "single", "Q")
    db_session.commit()
    a = answer_repo.create_answer(1, q.id, "A")
    db_session.commit()

    score = value_score_repo.create_value_score(
        user_id=1,
        question_id=q.id,
        answer_id=a.id,
        category="Extraversion",
        value="Sociability",
        confidence=0.8,
        intensity=0.6,
        polarity=1
    )

    assert score.id is not None
    assert score.category == "Extraversion"
    assert score.confidence == 0.8


def test_get_top_5_value_scores(value_score_repo, question_repo, answer_repo, db_session):
    q = question_repo.create_question(1, "single", "Q")
    db_session.commit()
    a = answer_repo.create_answer(1, q.id, "A")
    db_session.commit()

    # 여러 Score 생성
    for i in range(10):
        value_score_repo.create_value_score(
            user_id=1,
            question_id=q.id,
            answer_id=a.id,
            category="Extraversion",
            value=f"v{i}",
            confidence=0.5 + i * 0.01,
            intensity=0.7,
            polarity=1
        )

    top_scores = value_score_repo.get_top_5_value_scores(1)
    assert len(top_scores) == 5



# ============================================================
#                     ValueMap Repository
# ============================================================

def test_create_value_map(value_map_repo, db_session):
    vm = value_map_repo.create_value_map(1)
    assert vm.id is not None
    assert vm.user_id == 1


def test_update_by_value_score(value_map_repo, value_score_repo, question_repo, answer_repo, db_session):
    q = question_repo.create_question(1, "single", "Q")
    db_session.commit()
    a = answer_repo.create_answer(1, q.id, "A")
    db_session.commit()

    # Pre-create ValueMap
    value_map_repo.create_value_map(1)

    score = value_score_repo.create_value_score(
        user_id=1,
        question_id=q.id,
        answer_id=a.id,
        category="Agreeableness",
        value="Warmth",
        confidence=0.9,
        intensity=0.9,
        polarity=1,
    )

    updated_map = value_map_repo.update_by_value_score(score)
    assert updated_map.count_3 == 1
    assert updated_map.score_3 > 0


def test_generate_comment(value_map_repo, db_session):
    vm = value_map_repo.create_value_map(1)

    value_map_repo.generate_comment(
        user_id=1,
        personality_insight="당신은 친화적인 성향입니다.",
        comment="대인 관계에서 강점을 보이네요."
    )

    fetched = value_map_repo.get_by_user(1)
    assert fetched.personality_insight is not None
    assert fetched.comment is not None
