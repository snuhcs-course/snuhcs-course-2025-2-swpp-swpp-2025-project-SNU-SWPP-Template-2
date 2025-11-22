# app/features/selfaware/di.py
from fastapi import Depends
from sqlalchemy.orm import Session

from app.database.session import get_db_session
from app.features.journal.repository import JournalRepository
from app.features.selfaware.repository import (
    QuestionRepository, AnswerRepository,
    ValueMapRepository, ValueScoreRepository,
)
from app.features.selfaware.service import (
    QuestionService, AnswerService, ValueMapService, ValueScoreService,
)

def get_question_service(db: Session = Depends(get_db_session)) -> QuestionService:
    return QuestionService(
        journal_repository=JournalRepository(db),
        question_repository=QuestionRepository(db),
    )

def get_answer_service(db: Session = Depends(get_db_session)) -> AnswerService:
    return AnswerService(
        answer_repository=AnswerRepository(db),
    )

def get_value_score_service(db: Session = Depends(get_db_session)) -> ValueScoreService:
    return ValueScoreService(
        question_repository=QuestionRepository(db),
        answer_repository=AnswerRepository(db),
        value_score_repository=ValueScoreRepository(db),
        value_map_repository=ValueMapRepository(db),
    )

def get_value_map_service(db: Session = Depends(get_db_session)) -> ValueMapService:
    return ValueMapService(
        value_map_repository=ValueMapRepository(db),
        value_score_repository=ValueScoreRepository(db),
        answer_repository=AnswerRepository(db),
    )