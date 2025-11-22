from pydantic import BaseModel, Field
from datetime import datetime
from typing import Optional, List, Dict

from app.features.selfaware.models import Question, Answer, ValueMap
from app.features.selfaware.prompt import CAT_EN, CAT_KO

class QuestionResponse(BaseModel):
    id: int
    question_type: str
    text: str
    created_at: datetime
    
    @staticmethod
    def from_question(question: Question) -> "QuestionResponse":
        return QuestionResponse(
            id=question.id,
            question_type=question.question_type,
            text=question.text,
            created_at=question.created_at
        )
    
class AnswerResponse(BaseModel):
    id: int
    question_id: int
    type: Optional[str] = None
    text: str
    created_at: datetime
    updated_at: datetime
    
    @staticmethod
    def from_answer(answer: Answer) -> "AnswerResponse":
        return AnswerResponse(
            id=answer.id,
            question_id=answer.question_id,
            type=answer.type,
            text=answer.text,
            created_at=answer.created_at,
            updated_at=answer.updated_at
        )
        
class QAResponse(BaseModel):
    question: QuestionResponse
    answer: Optional[AnswerResponse] = None


class QACursorResponse(BaseModel):
    items: List[QAResponse]
    next_cursor: int | None = Field(
        None, description="다음 페이지의 QA 데이터를 요청할 때 사용할 마지막 아이템의 ID"
    )
    
    @staticmethod
    def from_QAs(questions: List[Question], answers: List[Answer]) -> "QACursorResponse":
        items = [
            QAResponse(
                question=QuestionResponse.from_question(q),
                answer=AnswerResponse.from_answer(a) if a else None
            ) for q, a in zip(questions, answers)
        ]
        next_cursor = items[-1].question.id if items else None

        return QACursorResponse(
            items=items,
            next_cursor=next_cursor
        )

    
class TopValueScoresResponse(BaseModel):
    value_scores: List[Dict]
    
class ValueMapResponse(BaseModel):
    category_scores: List[Dict]
    updated_at: datetime

    @staticmethod
    def from_value_map(value_map: ValueMap) -> "ValueMapResponse":
        category_scores = []
        for idx, cat_en in enumerate(CAT_EN):
            category_dict = {}
            category_dict["category_en"] = cat_en
            category_dict["category_ko"] = CAT_KO[cat_en]           
            category_dict["score"] = getattr(value_map, f"score_{idx}")
            category_scores.append(category_dict)
        
        return ValueMapResponse(
            category_scores=category_scores,
            updated_at=value_map.updated_at
        )
        
class PersonalityInsightResponse(BaseModel):
    comment: str
    personality_insight: str
    updated_at: datetime

    @staticmethod
    def from_value_map(value_map: ValueMap) -> "PersonalityInsightResponse":
        return PersonalityInsightResponse(
            comment=value_map.comment,
            personality_insight=value_map.personality_insight,
            updated_at=value_map.updated_at
        )
