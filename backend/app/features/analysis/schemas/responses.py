from pydantic import BaseModel, Field
from datetime import datetime
from typing import Optional, List, Dict, Any

from app.features.selfaware.models import Question, Answer, ValueScore, ValueMap
from app.features.selfaware.prompt import CAT_EN, CAT_KO
from app.features.analysis.models import Analysis

UserTypeDescription = {
    "목표 지향형":"목표를 세우면 끝까지 밀고 나가는 실천형입니다. 장기적인 계획을 세우고 성취감을 통해 동기부여를 받습니다.",
    "탐험가형":"새로운 가능성을 찾아 나서는 모험가입니다. 변화를 즐기며 새로운 경험을 통해 배우는 것을 좋아합니다.",
    "사교가형":"주변 사람들에게 긍정적인 에너지를 전하며, 관계를 통해 성장하는 타입입니다.",
    "배려형":"신뢰를 바탕으로 함께의 가치를 중시하며, 조직과 관계에서 안정감을 제공합니다.",
    "사색가형":"깊이 있는 통찰로 문제를 해결하는 사색가입니다. 혼자 몰입하는 시간에서 영감을 얻습니다.",
    "도전형":"자신의 신념에 따라 행동하며 변화를 이끄는 리더형입니다. 높은 목표를 향해 돌진합니다.",
    "안정추구형":"감정 기복이 적고 스트레스 상황에서도 안정적으로 대처합니다.",
    "감성형":"감정과 감각을 섬세하게 느끼는 예술가형입니다. 타인의 감정에도 민감하게 반응합니다.",
    "분석형":"규칙과 절차를 중시하며, 안정적이고 효율적인 실행력을 보입니다.",
    "변화추구형":"자유로운 발상으로 새로운 방식을 시도하는 혁신가입니다. 틀에 얽매이지 않고 변화를 주도합니다.",
    "균형형":"다양한 상황에서 균형 잡힌 판단을 내리는 타입입니다. 특정 성향에 치우치지 않아 유연하게 대응할 수 있습니다.",
}

class UserTypeResponse(BaseModel):
    user_type: Optional[str] = None # 분류 유형 ex) 목표 성취형
    description: Optional[str] = None
    updated_at: datetime
    
    @staticmethod
    def from_analysis(analysis: Analysis) -> "UserTypeResponse":
        return UserTypeResponse(
            user_type = analysis.user_type,
            description = None if analysis.user_type == None else UserTypeDescription[analysis.user_type],
            updated_at = analysis.updated_at
        )

class ComprehensiveAnalysisResponse(BaseModel):
    conscientiousness: Optional[str] = None
    neuroticism: Optional[str] = None
    extraversion: Optional[str] = None
    openness: Optional[str] = None
    agreeableness: Optional[str] = None
    updated_at: datetime
    
    @staticmethod
    def from_analysis(analysis: Analysis) -> "ComprehensiveAnalysisResponse":
        return ComprehensiveAnalysisResponse(
            conscientiousness = analysis.conscientiousness,
            neuroticism = analysis.neuroticism,
            extraversion = analysis.extraversion,
            openness = analysis.openness,
            agreeableness = analysis.agreeableness,
            updated_at=analysis.updated_at
        )

class PersonalizedAdviceResponse(BaseModel):
    advice_type: Optional[str] = None # 조언 이론 유형
    personalized_advice: Optional[str] = None 
    updated_at: datetime
    
    @staticmethod
    def from_analysis(analysis: Analysis) -> "PersonalizedAdviceResponse":
        return PersonalizedAdviceResponse(
            advice_type = analysis.advice_type,
            personalized_advice=analysis.personalized_advice,
            updated_at=analysis.updated_at
        )