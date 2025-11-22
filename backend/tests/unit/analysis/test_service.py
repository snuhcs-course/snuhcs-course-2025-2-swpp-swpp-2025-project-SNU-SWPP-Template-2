# backend/tests/unit/analysis/test_service.py
import pytest
from app.features.analysis.service import AnalysisService
from app.features.analysis.comprehensive_analysis.score import NeoPiAnswers

# ------------------------------
# 공통 fixture
# ------------------------------
@pytest.fixture
def mock_answer_repo(mocker):
    repo = mocker.Mock()
    repo.get_by_user.return_value = [mocker.Mock(text="오늘은 행복했어요.")]
    return repo


@pytest.fixture
def mock_analysis_repo(mocker):
    repo = mocker.Mock()
    repo.create_analysis.return_value = {"id": 1, "user_id": 1}
    repo.get_analysis_by_user_id.return_value = mocker.Mock(
        neo_pi_score={
            "CONSCIENTIOUSNESS": 70,
            "NEUROTICISM": 40,
            "EXTRAVERSION": 65,
            "OPENNESS": 60,
            "AGREEABLENESS": 58,
        }
    )
    return repo

@pytest.fixture
def service(mock_answer_repo, mock_analysis_repo):
    return AnalysisService(answer_repository=mock_answer_repo, analysis_repository=mock_analysis_repo)

def test_create_and_get_analysis(service, mock_analysis_repo):
    created = service.create_analysis(1)
    fetched = service.get_analysis_by_user(1)

    mock_analysis_repo.create_analysis.assert_called_once_with(user_id=1)
    mock_analysis_repo.get_analysis_by_user_id.assert_called_once_with(user_id=1)
    assert "id" in created
    assert fetched.neo_pi_score["CONSCIENTIOUSNESS"] == 70

def test_evaluate_user_type_returns_expected(service, mock_analysis_repo):
    user_type = service.evaluate_user_type(user_id=1)
    assert user_type == "목표 지향형"  # C >=65, N <=45

def test_extract_neo_pi_from_answer(service, mocker, mock_answer_repo):
    responses = service.extract_neo_pi_from_answer(user_id=1)
    assert isinstance(responses, list)
    assert len(responses) > 0

def test_evaluate_big_5_score(service, mocker):
    mocker.patch.object(service, "extract_neo_pi_from_answer", return_value=[1] * 121)
    mock_evaluate = mocker.patch("app.features.analysis.service.evaluate", return_value={"O": 60})
    result = service.evaluate_big_5_score(user_id=1, age=23, sex="Male")
    mock_evaluate.assert_called_once()
    assert result == {"O": 60}

def test_get_comment_from_big_5_score(service, mocker):
    pass

def test_extract_personalized_advice(service, mocker, mock_analysis_repo):
    response = service.extract_personalized_advice(user_id=1, age=23, sex="Male")
    assert len(response) > 0
