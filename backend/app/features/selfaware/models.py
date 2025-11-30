from __future__ import annotations

from datetime import datetime, timezone, date
from typing import TYPE_CHECKING, List, Optional

from sqlalchemy import String, Integer, Text, ForeignKey, DateTime, JSON, Float, Date, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database.base import Base
if TYPE_CHECKING:
    from app.features.user.models import User

def utcnow() -> datetime:
    return datetime.now(timezone.utc)

class Question(Base):
    __tablename__ = "questions"
    __table_args__ = (
        UniqueConstraint("user_id", "date", name="uq_user_date"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False)

    question_type: Mapped[str | None] = mapped_column(String(50), nullable=True) # single_category | multi_category | personalized_category
    text: Mapped[str] = mapped_column(Text, nullable=False)
    
    date: Mapped[date] = mapped_column(Date, default=lambda: utcnow().date(), nullable=False)

    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)

    user: Mapped[User] = relationship(back_populates="questions")
    answers: Mapped[Answer] = relationship(back_populates="question", uselist=False, cascade="all, delete-orphan")

class Answer(Base):
    __tablename__ = "answers"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)

    user_id: Mapped[int] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )
    question_id: Mapped[int] = mapped_column(
        ForeignKey("questions.id", ondelete="CASCADE"), nullable=False, unique=True, index=True
    )
    type: Mapped[str | None] = mapped_column(String(50), nullable=True) # single_category | multi_category | personalized_category
    text: Mapped[str] = mapped_column(Text, nullable=False)

    keywords: Mapped[Optional[List[str]]] = mapped_column(JSON, nullable=True) # 확인 필요

    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow, nullable=False)

    user: Mapped[User] = relationship(back_populates="answers")
    question: Mapped[Question] = relationship(back_populates="answers")
    
    value_scores: Mapped[List[ValueScore]] = relationship(
        back_populates="answer",
        cascade="all, delete-orphan",
        passive_deletes=True
    )

class ValueScore(Base):
    __tablename__ = "value_scores"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)

    answer_id: Mapped[int] = mapped_column(
        ForeignKey("answers.id", ondelete="CASCADE"), nullable=False, index=True
    )

    user_id: Mapped[int] = mapped_column(Integer, nullable=False, index=True)

    question_id: Mapped[int] = mapped_column(Integer, nullable=False, index=True) # Doc에 명시가 안되어 있는데, service에서 사용할 가능성 있어 남겨둠, 확정시 문서에 추가
    
    category: Mapped[str] = mapped_column(String(64), nullable=False)  # 대분류
    value: Mapped[str] = mapped_column(String(80), nullable=False)  # 소분류

    confidence: Mapped[float] = mapped_column(Float, nullable=False)               # 0..1
    intensity: Mapped[float]  = mapped_column(Float, nullable=False)               # 0..1
    polarity:  Mapped[int]    = mapped_column(Integer, nullable=False, default=1)  # -1, 0, +1       # 서버 계산치(즉시/증분)

    # 증거(문장/구) — 최대 2개 저장 추천
    evidence_quotes: Mapped[Optional[List[str]]] = mapped_column(JSON)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    
    answer: Mapped[Answer] = relationship(back_populates="value_scores")

# 추가 수정 필요
class ValueMap(Base):
    __tablename__ = "value_maps"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )

    score_0: Mapped[int] = mapped_column(Integer, nullable=False, default = 0)
    score_1: Mapped[int] = mapped_column(Integer, nullable=False, default = 0)
    score_2: Mapped[int] = mapped_column(Integer, nullable=False, default = 0)
    score_3: Mapped[int] = mapped_column(Integer, nullable=False, default = 0)
    score_4: Mapped[int] = mapped_column(Integer, nullable=False, default = 0)

    count_0: Mapped[int] = mapped_column(Integer, nullable=False, default = 0)
    count_1: Mapped[int] = mapped_column(Integer, nullable=False, default = 0)
    count_2: Mapped[int] = mapped_column(Integer, nullable=False, default = 0)
    count_3: Mapped[int] = mapped_column(Integer, nullable=False, default = 0)
    count_4: Mapped[int] = mapped_column(Integer, nullable=False, default = 0)


    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow, nullable=False)

    comment: Mapped[str] = mapped_column(Text, nullable=True)
    personality_insight: Mapped[str] = mapped_column(Text, nullable=True)

    user: Mapped[User] = relationship(back_populates="value_maps")



