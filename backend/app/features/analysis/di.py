from app.database.session import get_db_session
from app.features.analysis.repository import AnalysisRepository
from app.features.analysis.service import AnalysisService
from app.features.selfaware.repository import AnswerRepository
from fastapi import Depends
from sqlalchemy.orm import Session

def get_analysis_service(db: Session = Depends(get_db_session)) -> AnalysisService:
    return AnalysisService(
        answer_repository=AnswerRepository(db),
        analysis_repository=AnalysisRepository(db),
    )