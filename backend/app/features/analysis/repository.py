from app.common.utilities import get_korea_time
from app.database.session import get_db_session
from app.features.analysis.models import Analysis
from fastapi import Depends
from typing import Annotated, Optional
from sqlalchemy.orm import Session

# -------------------------------
# Analysis Repository
# -------------------------------
class AnalysisRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def create_analysis(
        self, 
        user_id: int,   
    ) -> Analysis:
        analysis = Analysis(
            user_id=user_id,
        )
        self.session.add(analysis)
        self.session.flush()
        return analysis

    def get_analysis_by_id(self, id: int) -> Optional[Analysis]:
        return self.session.get(Analysis, id)

    def get_analysis_by_user_id(self, user_id: int) -> Optional[Analysis]:
        return  (
            self.session.query(Analysis)
            .filter(
                Analysis.user_id == user_id
            )
            .first()
        )

    def update_analysis(
        self,
        user_id: int,
        user_type: Optional[str] = None,
        neo_pi_score: Optional[dict] = None,
        conscientiousness: Optional[str] = None,
        neuroticism: Optional[str] = None,
        extraversion: Optional[str] = None,
        openness: Optional[str] = None,
        agreeableness: Optional[str] = None,
        advice_type: Optional[str] = None,
        personalized_advice: Optional[str] = None
    ):
        analysis = self.get_analysis_by_user_id(user_id)
        if analysis == None:
            raise
        
        if user_type != None:
            analysis.user_type = user_type
        if neo_pi_score != None:
            analysis.neo_pi_score = neo_pi_score
        if conscientiousness != None:
            analysis.conscientiousness = conscientiousness
        if neuroticism != None:
            analysis.neuroticism = neuroticism
        if extraversion != None:
            analysis.extraversion = extraversion
        if openness != None:
            analysis.openness = openness
        if agreeableness != None:
            analysis.agreeableness = agreeableness
        if advice_type != None:
            analysis.advice_type = advice_type
        if personalized_advice != None:
            analysis.personalized_advice = personalized_advice

        analysis.updated_at = get_korea_time()

        self.session.flush()
        self.session.commit() # background에서 진행 예정