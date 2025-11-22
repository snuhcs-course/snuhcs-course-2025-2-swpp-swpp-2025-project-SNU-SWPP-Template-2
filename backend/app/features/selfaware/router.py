from datetime import date
from typing import Annotated
import asyncio
from sqlalchemy.exc import IntegrityError
from fastapi import APIRouter, Depends, Query, HTTPException, status, BackgroundTasks
from fastapi.security import HTTPBearer

from app.common.authorization import get_current_user
from app.features.user.models import User
from app.features.selfaware.models import Question
from app.features.selfaware.service import (
    QuestionService,
    AnswerService,
    ValueMapService,
    ValueScoreService,
)
from app.features.selfaware.di import (
    get_question_service,
    get_answer_service,
    get_value_map_service,
    get_value_score_service,
)
from app.features.selfaware.schemas.responses import (
    QAResponse,
    QuestionResponse,
    AnswerResponse,
    QACursorResponse,
    ValueMapResponse,
    TopValueScoresResponse,
    PersonalityInsightResponse
)
from app.features.selfaware.schemas.requests import (
    AnswerRequest
)
from app.features.analysis.service import (
    AnalysisService
)
from app.features.analysis.di import get_analysis_service

security = HTTPBearer()

# ✅ 하나의 self-aware 라우터
router = APIRouter(prefix="/self-aware", tags=["selfaware"])

# -----------------------------
# 🔧 백그라운드 작업 함수
# -----------------------------
def process_value_score_extraction(
    user_id: int,
    question_id: int, 
    answer_id: int,
    value_score_service: ValueScoreService,
    value_map_service: ValueMapService
):
    """백그라운드에서 value score 추출 및 value map 업데이트"""
    try:
        print(f"Starting value score extraction for user {user_id}, question {question_id}, answer {answer_id}")
        
        # 1. value score 추출 및 value map 업데이트
        detected_values = value_score_service.extract_value_score_from_answer(user_id, question_id, answer_id)
        print(f"Extracted {len(detected_values)} value scores for user {user_id}")
        
        # 2. value map comment 생성 (선택적)
        try:
            value_map_service.generate_comment(user_id)
            print(f"Generated comment for user {user_id}")
        except Exception as comment_error:
            print(f"Warning: Could not generate comment for user {user_id}: {comment_error}")
            # comment 생성 실패는 전체 프로세스를 중단하지 않음
            
    except Exception as e:
        print(f"Error processing value score for user {user_id}, question {question_id}, answer {answer_id}: {e}")
        # 로깅을 위해 에러를 출력하지만 예외를 다시 발생시키지 않음

def update_analysis_table(
    user_id: int,
    analysis_service: AnalysisService,
):
    try:
        if analysis_service.get_analysis_by_user(user_id) == None:
            analysis_service.create_analysis(user_id)
            print(f"Analysis table for user {user_id} created")
        print("Start updating neo_pi_score")
        analysis_service.update_neo_pi_score(user_id)
        print("Start updating user_id")
        analysis_service.update_user_type(user_id)
        print("Start updating comprehensive_analysis")
        analysis_service.update_comprehensive_analysis(user_id)
        print("Start updating personalized_advice")
        analysis_service.update_personalized_advice(user_id)
    except Exception as e:
        print(f"Error processing updating analysis table for user {user_id}: {e}")
        # 로깅을 위해 에러를 출력하지만 예외를 다시 발생시키지 않음

# -----------------------------
# 🧠 Question 관련 엔드포인트
# -----------------------------

@router.get(
    "/question/{question_id}",
    status_code=200,
    summary="Get a specific question by ID",
    response_model=QuestionResponse,
)
def get_question(
    question_id: int,
    question_service: Annotated[QuestionService, Depends(get_question_service)],
    user: User = Depends(get_current_user)
) -> QuestionResponse:
    """특정 질문을 조회합니다."""
    question = question_service.get_questions_by_id(question_id)
    if not question:
        raise HTTPException(status_code=404, detail="Question not found.")
    if question.user_id != user.id:
        raise HTTPException(status_code=403, detail="No Authorization.")
    
    return QuestionResponse.from_question(question)


@router.get(
    "/question",
    response_model=QAResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create a new question if not exists for today. If exists, return the existing one.",
)
def create_or_get_today_question(
    question_service: Annotated[QuestionService, Depends(get_question_service)],
    answer_service: Annotated[AnswerService, Depends(get_answer_service)],
    user: User = Depends(get_current_user),
    date: date = Query(description="조회할 날짜 (YYYY-MM-DD)"),
) -> QAResponse:
    """
    오늘 날짜의 질문이 이미 존재하면 해당 질문을 반환하고 (답변이 있으면 함께 반환),
    존재하지 않으면 새로운 질문을 생성하여 저장한 후 반환합니다.
    """
    question = question_service.get_questions_by_date(user.id, date)
    if not question:
        try: 
            question = question_service.generate_question(user.id)
        except IntegrityError:
            question = question_service.get_questions_by_date(user.id, date)
        assert type(question) == Question
        return QAResponse(question=QuestionResponse.from_question(question))
    else:
        answer = answer_service.get_answer_by_question(question.id)
        if not answer:
            return QAResponse(question=QuestionResponse.from_question(question))
        else:
            return QAResponse(
                question=QuestionResponse.from_question(question),
                answer=AnswerResponse.from_answer(answer)
            )
            
@router.get(
    "/QA-history",
    response_model=QACursorResponse,
    status_code=status.HTTP_200_OK,
    summary="Get all question & answer pairs by user ID with pagination",
)
def get_user_QAs(
    question_service: Annotated[QuestionService, Depends(get_question_service)],
    answer_service: Annotated[AnswerService, Depends(get_answer_service)],
    limit: int = Query(default=10, le=50, description="Number of items to retrieve"),
    cursor: int | None = Query(None, description="ID of the last item from the previous page for pagination"),
    user: User = Depends(get_current_user)
) -> QACursorResponse:
    questions = question_service.list_questions_by_user(user.id, limit, cursor)
    questions_ids = [q.id for q in questions]
    answers = answer_service.list_answers_by_user(user.id, questions_ids)
    return QACursorResponse.from_QAs(questions, answers)

# -----------------------------
# 💬 Answer 관련 엔드포인트
# -----------------------------

@router.post(
    "/answer",
    response_model=AnswerResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Submit answer for a question",
)
def submit_answer(
    request: AnswerRequest,
    background_tasks: BackgroundTasks,
    question_service: Annotated[QuestionService, Depends(get_question_service)],
    answer_service: Annotated[AnswerService, Depends(get_answer_service)],
    value_score_service: Annotated[ValueScoreService, Depends(get_value_score_service)],
    value_map_service: Annotated[ValueMapService, Depends(get_value_map_service)],
    analysis_service: Annotated[AnalysisService, Depends(get_analysis_service)],
    user: User = Depends(get_current_user)
) -> AnswerResponse:
    """답변을 제출하고 백그라운드에서 value score를 추출하여 value map을 업데이트합니다."""
    question_id = request.question_id
    question = question_service.get_questions_by_id(question_id)
    if not question:
        raise HTTPException(status_code=404, detail="Question not found.")
    if question.user_id != user.id:
        raise HTTPException(status_code=403, detail="No Authorization.")

    # 1. 답변 생성 및 즉시 응답 반환
    answer = answer_service.create_answer(user_id=user.id, question_id=question_id, text=request.text)
    
    # 2. 백그라운드에서 value score 추출 및 value map 업데이트
    background_tasks.add_task(
        process_value_score_extraction,
        user.id,
        question_id,
        answer.id,
        value_score_service,
        value_map_service
    )

    # 3. 충분한 answers가 모였을 경우, analysis 업데이트
    answers = answer_service.get_answer_by_user(user_id = user.id)
    if len(answers) > 0 and len(answers) % 10 == 0:
        background_tasks.add_task(
            update_analysis_table,
            user.id,
            analysis_service
        )

    return AnswerResponse.from_answer(answer)
    

# -----------------------------
# 🗺️ Value Map/Score 관련 엔드포인트
# -----------------------------

@router.get(
    "/value-map",
    response_model=ValueMapResponse,
    status_code=status.HTTP_200_OK,
    summary="Get value map by user ID",
)
def get_value_map_by_user(
    value_map_service: Annotated[ValueMapService, Depends(get_value_map_service)],
    user: User = Depends(get_current_user)
) -> ValueMapResponse:
    value_map = value_map_service.get_value_map_by_user(user.id)
    if not value_map:
        raise HTTPException(status_code=404, detail="Value map not found.")
    return ValueMapResponse.from_value_map(value_map)

    
@router.get(
    "/top-value-scores", 
    response_model=TopValueScoresResponse,
    status_code=status.HTTP_200_OK,
    summary="Get top value scores by user ID",
)
def get_top_value_scores(
    value_score_service: Annotated[ValueScoreService, Depends(get_value_score_service)],
    user: User = Depends(get_current_user)
) -> TopValueScoresResponse:
    value_scores = value_score_service.get_top_value_scores(user.id)
    return TopValueScoresResponse(value_scores=value_scores)

@router.get(
    "/personality-insight", 
    response_model=PersonalityInsightResponse,
    status_code=status.HTTP_200_OK,
    summary="Get personality insight by user ID",
)
def get_personality_insight(
    value_map_service: Annotated[ValueMapService, Depends(get_value_map_service)],
    user: User = Depends(get_current_user)
) -> PersonalityInsightResponse:
    value_map = value_map_service.get_value_map_by_user(user.id)
    if not value_map:
        raise HTTPException(status_code=404, detail="Value map not found.")
    return PersonalityInsightResponse.from_value_map(value_map)