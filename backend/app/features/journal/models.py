from __future__ import annotations

from datetime import datetime
from typing import TYPE_CHECKING

from sqlalchemy import DateTime, Float, ForeignKey, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.common.utilities import get_korea_time
from app.database.base import Base

# TYPE_CHECKING: 순환 참조를 방지합니다.
if TYPE_CHECKING:
    from app.features.user.models import User


class Journal(Base):
    __tablename__ = "journals"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )

    title: Mapped[str] = mapped_column(String(255), nullable=False)
    content: Mapped[str] = mapped_column(Text, nullable=False)
    # summary: Mapped[str | None] = mapped_column(Text, nullable=True)
    gratitude: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=get_korea_time)

    # (Many to One)
    user: Mapped[User] = relationship(back_populates="journals")

    # (One to Many)
    emotions: Mapped[list[JournalEmotion]] = relationship(
        back_populates="journal", cascade="all, delete-orphan"
    )
    keywords: Mapped[list[JournalKeyword]] = relationship(
        back_populates="journal", cascade="all, delete-orphan"
    )

    # (One to One)
    image: Mapped[JournalImage | None] = relationship(
        back_populates="journal", cascade="all, delete-orphan"
    )


class JournalImage(Base):
    __tablename__ = "journal_images"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    journal_id: Mapped[int] = mapped_column(
        ForeignKey("journals.id", ondelete="CASCADE"), nullable=False
    )

    # image_url: Mapped[str | None] = mapped_column(String(1024), nullable=True)
    # job_id: Mapped[str | None] = mapped_column(
    #     String(255), nullable=True, unique=True, index=True
    # )

    # S3 object key: <journal_id>/<unique_filename>
    s3_key: Mapped[str | None] = mapped_column(String(1024), nullable=True)

    created_at: Mapped[datetime] = mapped_column(DateTime, default=get_korea_time)

    # (One to One)
    journal: Mapped[Journal] = relationship(back_populates="image")


class JournalEmotion(Base):
    __tablename__ = "journal_emotions"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    journal_id: Mapped[int] = mapped_column(
        ForeignKey("journals.id", ondelete="CASCADE"), nullable=False
    )

    emotion: Mapped[str] = mapped_column(String(50), nullable=False)
    intensity: Mapped[int] = mapped_column(Integer, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=get_korea_time)

    # (Many to One)
    journal: Mapped[Journal] = relationship(back_populates="emotions")


class JournalKeyword(Base):
    __tablename__ = "journal_keywords"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    journal_id: Mapped[int] = mapped_column(
        ForeignKey("journals.id", ondelete="CASCADE"), nullable=False
    )

    keyword: Mapped[str] = mapped_column(String(100), index=True, nullable=False)
    emotion: Mapped[str] = mapped_column(String(50), nullable=False)
    summary: Mapped[str] = mapped_column(String(100), nullable=False)
    weight: Mapped[float] = mapped_column(Float, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=get_korea_time)

    # (Many to One)
    journal: Mapped[Journal] = relationship(back_populates="keywords")


# class JournalCausality(Base):
#     __tablename__ = "journal_causalities"

#     id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)

#     emotion_id: Mapped[int] = mapped_column(
#         ForeignKey("journal_emotions.id", ondelete="CASCADE"), nullable=False
#     )
#     keyword_id: Mapped[int] = mapped_column(
#         ForeignKey("journal_keywords.id", ondelete="CASCADE"), nullable=False
#     )
#     journal_id: Mapped[int] = mapped_column(
#         ForeignKey("journals.id", ondelete="CASCADE")
#     )

#     weight: Mapped[float] = mapped_column(Float, nullable=False)
#     created_at: Mapped[datetime] = mapped_column(DateTime, default=get_korea_time)
#     # (Many to One)
#     emotion: Mapped[JournalEmotion] = relationship(back_populates="keyword_causality")
#     keyword: Mapped[JournalKeyword] = relationship(back_populates="emotion_causality")

#     journal: Mapped[Journal] = relationship(back_populates="causalities")
