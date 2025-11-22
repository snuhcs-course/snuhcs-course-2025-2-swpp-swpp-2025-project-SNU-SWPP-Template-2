from datetime import date
from typing import Annotated

from fastapi import APIRouter, Depends, Query, status

from app.common.authorization import get_current_user
from app.features.statistics.schema.responses import StatisticsEmotionResponse
from app.features.statistics.service import StatisticsService
from app.features.user.models import User

router = APIRouter(prefix="/statistics", tags=["statistics"])


@router.get(
    "/emotion-rate",
    response_model=StatisticsEmotionResponse,
    status_code=status.HTTP_200_OK,
    summary="감정비율 계산",
    description="""
    특정 기간에 대한 일기의 감정 기록을 검색하여 비율을 계산합니다.
    """,
)
def get_emotion_rates(
    statistics_service: Annotated[StatisticsService, Depends()],
    start_date: date = Query(..., description="조회 시작 날짜 (YYYY-MM-DD 형식)"),
    end_date: date = Query(..., description="조회 종료 날짜 (YYYY-MM-DD 형식)"),
    user: User = Depends(get_current_user),
) -> StatisticsEmotionResponse:
    return statistics_service.get_emotion_rates(user.id, start_date, end_date)
