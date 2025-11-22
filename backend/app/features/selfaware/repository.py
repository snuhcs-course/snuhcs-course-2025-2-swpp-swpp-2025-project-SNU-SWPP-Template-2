from typing import Annotated, Optional, List, Sequence
from datetime import date, datetime, time, timedelta, timezone
from fastapi import Depends
from sqlalchemy import select, desc
from sqlalchemy.orm import Session
from app.database.session import get_db_session
from app.features.selfaware.models import Question, Answer, ValueMap, ValueScore
from app.common.utilities import get_korea_time

# -------------------------------
# Question Repository
# -------------------------------
class QuestionRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def create_question(
        self, 
        user_id: int,
        question_type: str,
        text: str,        
    ) -> Question:
        question = Question(
            user_id=user_id,
            question_type=question_type,
            text=text,
        )
        
        self.session.add(question)
        self.session.flush()
        return question

    def get_question_by_id(self, question_id: int) -> Question | None:
        return self.session.get(Question, question_id)

    def list_questions_by_user(
        self, user_id: int, limit: int = 10, cursor: int | None = None
    ) -> list[Question]:
        # cursor가 None이면 최신 글부터, cursor가 주어지면 해당 ID보다 작은 글부터
        query = (
            self.session.query(Question)
            .filter(Question.user_id == user_id)
            .order_by(Question.id.desc())
        )
        # cursor가 주어지면 해당 ID보다 작은 글부터
        if cursor is not None:
            query = query.filter(Question.id < cursor)
        # limit만큼 가져오기
        return query.limit(limit).all()

    def get_question_by_date(self, user_id: int, target_date: date) -> Question | None:
        KST = timezone(timedelta(hours=9))

        # 한국 시간으로 날짜 기준 구간 계산
        start_kst = datetime.combine(target_date, time.min, tzinfo=KST)
        end_kst = start_kst + timedelta(days=1)

        return self.session.scalar(
            select(Question)
            .where(
                Question.user_id == user_id,
                Question.created_at >= start_kst,
                Question.created_at < end_kst,
            )
            .order_by(Question.created_at.desc())
            .limit(1)
        )



# -------------------------------
# Answer Repository
# -------------------------------
class AnswerRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def create_answer(
        self, 
        user_id: int,   
        question_id: int,
        text: str
    ) -> Answer:
        answer = Answer(
            user_id=user_id,
            question_id=question_id,
            text=text,
        )
        self.session.add(answer)
        self.session.flush()
        return answer

    def get_answer_by_id(self, answer_id: int) -> Optional[Answer]:
        return self.session.get(Answer, answer_id)

    def get_by_question(self, question_id: int) -> Optional[Answer]:
        return  (
            self.session.query(Answer)
            .filter(
                Answer.question_id == question_id
            )
            .first()
        )

    # user의 모든 answer를 반환... 아직 사용된 적 없는 듯 함. 가장 최근 하나를 사용하는 용도라면 all->first, Sequence->Optional
    def get_by_user(self, user_id: int) -> list[Answer]:
        return (
            self.session.query(Answer)
            .filter(Answer.user_id == user_id)
            .all()
        )
        
    def list_answers_by_user(self, user_id: int, question_ids: List[int]) -> list[Answer]:
        return (
            self.session.query(Answer)
            .filter(
                Answer.user_id == user_id,
                Answer.question_id.in_(question_ids)
            )
            .order_by(Answer.id.desc())
            .all()
        )

# -------------------------------
# ValueScore Repository
# -------------------------------
class ValueScoreRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def create_value_score(
        self, 
        user_id: int,
        question_id: int,
        answer_id: int,
        category: str,
        value: str,
        confidence: float,
        intensity: float,
        polarity: int,
        evidence_quotes: Optional[List[str]] = None,
    ) -> ValueScore:
        value_score = ValueScore(
            user_id=user_id,
            question_id=question_id,
            answer_id=answer_id,
            category=category,
            value=value,   
            confidence=confidence,
            intensity=intensity,
            polarity=polarity,
        )
        
        if evidence_quotes:
            value_score.evidence_quotes = evidence_quotes
        
        self.session.add(value_score)
        self.session.flush()
        self.session.commit()
        return value_score
    
    def get_top_5_value_scores(self, user_id: int):
        query = (
            select(ValueScore)
            .where(ValueScore.user_id == user_id)
            .order_by(desc(ValueScore.intensity * ValueScore.confidence * (ValueScore.polarity * ValueScore.polarity)))  # intensity * confidence 기준 정렬, polarity 0이면 후순위
            .limit(5)
        )
        return self.session.scalars(query).all()

# -------------------------------
# ValueMap Repository
# -------------------------------
class ValueMapRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def create_value_map(
        self, 
        user_id: int
    ) -> ValueMap:
        value_map = ValueMap(
            user_id=user_id,
        )
        self.session.add(value_map)
        self.session.flush()
        self.session.commit()
        return value_map
    
    def update_by_value_score(
        self, 
        value_score: ValueScore
    ):
        value_map = (
            self.session.query(ValueMap)
            .filter(
                ValueMap.user_id == value_score.user_id
            )
            .first()
        )
        
        if not value_map:
            raise 

        if value_score.category == "Neuroticism":
            value = ((value_score.intensity * value_score.polarity * value_score.confidence + 1) * 50 + value_map.count_0 * value_map.score_0) // (value_map.count_0+1)
            count =  value_map.count_0 + 1
            category = 0
        elif value_score.category == "Extraversion":
            value = ((value_score.intensity * value_score.polarity * value_score.confidence + 1) * 50 + value_map.count_1 * value_map.score_1) // (value_map.count_1+1)
            count =  value_map.count_1 + 1
            category = 1
        elif value_score.category == "Openness to Experience":
            value = ((value_score.intensity * value_score.polarity * value_score.confidence + 1) * 50 + value_map.count_2 * value_map.score_2) // (value_map.count_2+1)
            count =  value_map.count_2 + 1
            category = 2
        elif value_score.category == "Agreeableness":
            value = ((value_score.intensity * value_score.polarity * value_score.confidence + 1) * 50 + value_map.count_3 * value_map.score_3) // (value_map.count_3+1)
            count =  value_map.count_3 + 1
            category = 3
        elif value_score.category == "Conscientiousness":
            value = ((value_score.intensity * value_score.polarity * value_score.confidence + 1) * 50 + value_map.count_4 * value_map.score_4) // (value_map.count_4+1)
            count =  value_map.count_4 + 1
            category = 4
        else:
            raise

        # 실제 객체에 반영
        setattr(value_map, f"score_{category}", value)
        setattr(value_map, f"count_{category}", count)

        # 갱신 시간 업데이트 (선택적)
        value_map.updated_at = get_korea_time()

        self.session.flush()
        self.session.commit()
        return value_map

    def get_by_user(self, user_id: int) -> Optional[ValueMap]:
        return (
            self.session.query(ValueMap)
            .filter(
                ValueMap.user_id == user_id
            )
            .first()
        )
    
    def generate_comment(
        self, 
        user_id: int, 
        personality_insight: str, 
        comment: str
    ):
        value_map = self.get_by_user(user_id)
        if not value_map:
            raise

        setattr(value_map, "personality_insight", personality_insight)
        setattr(value_map, "comment", comment)

        self.session.flush()
        self.session.commit()
        return value_map