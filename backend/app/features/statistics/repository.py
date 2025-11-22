from datetime import date, datetime, timedelta
from typing import Annotated

from fastapi import Depends
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.database.session import get_db_session
from app.features.journal.models import Journal, JournalEmotion


class StatisticsRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def get_emotion_counts(
        self, user_id: int, start_date: date, end_date: date
    ) -> tuple[list, int]:
        start_datetime = datetime.combine(start_date, datetime.min.time())
        end_datetime = datetime.combine(
            end_date + timedelta(days=1), datetime.min.time()
        )

        query = (
            self.session.query(
                JournalEmotion.emotion,
                func.count(JournalEmotion.intensity).label("count"),
            )
            .filter(
                Journal.user_id == user_id,
                Journal.created_at >= start_datetime,
                Journal.created_at < end_datetime,
            )
            .group_by(JournalEmotion.emotion)
        )

        results = query.all()

        total_count = sum(row.count for row in results)
        return results, total_count
