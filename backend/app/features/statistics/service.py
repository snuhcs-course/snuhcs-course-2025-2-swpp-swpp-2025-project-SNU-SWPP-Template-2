from datetime import date
from typing import Annotated

from fastapi import Depends

from app.features.statistics.repository import StatisticsRepository
from app.features.statistics.schema.responses import (
    EmotionStat,
    StatisticsEmotionResponse,
)


class StatisticsService:
    def __init__(
        self,
        statistics_repository: Annotated[StatisticsRepository, Depends()],
    ) -> None:
        self.statistics_repository = statistics_repository

    def get_emotion_rates(
        self, user_id: int, start_date: date, end_date: date
    ) -> StatisticsEmotionResponse:
        (emotion_counts, total_count) = self.statistics_repository.get_emotion_counts(
            user_id, start_date, end_date
        )

        statistics_list: list[EmotionStat] = []
        if total_count > 0:
            for row in emotion_counts:
                percentage = (row.count / total_count) * 100
                statistics_list.append(
                    {
                        "emotion": row.emotion,
                        "count": row.count,
                        "percentage": round(percentage, 2),
                    }
                )

        return {"total_count": total_count, "statistics": statistics_list}
