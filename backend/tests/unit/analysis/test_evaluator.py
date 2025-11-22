import pytest
from app.features.analysis.comprehensive_analysis.evaluator import evaluate

# -------------------------------
# 기본 Fixture
# -------------------------------
@pytest.fixture
def sample_responses():
    # 120문항 모두 3점으로 가정 (중간 응답)
    return [3] * 121

# -------------------------------
# 1️⃣ 정상 작동 테스트
# -------------------------------
def test_evaluate_valid_male(sample_responses):
    result = evaluate(sample_responses, Sex="Male", Age=25)
    # 반환값은 dict 형태여야 함
    assert isinstance(result, dict)
    # Big Five 주요 키가 포함되어야 함
    for key in ["EXTRAVERSION", "AGREEABLENESS", "CONSCIENTIOUSNESS", "NEUROTICISM", "OPENNESS"]:
        assert key in result

def test_evaluate_valid_female(sample_responses):
    result = evaluate(sample_responses, Sex="Female", Age=30)
    assert isinstance(result, dict)
    assert "EXTRAVERSION" in result
    assert result["EXTRAVERSION"] > 0

# -------------------------------
# 2️⃣ 예외 처리 테스트 (성별 / 나이)
# -------------------------------
@pytest.mark.parametrize("sex", ["Other", "Unknown", ""])
def test_evaluate_invalid_sex(sample_responses, sex):
    with pytest.raises(Exception) as exc:
        evaluate(sample_responses, Sex=sex, Age=25)
    assert "sex" in str(exc.value).lower()

@pytest.mark.parametrize("age", [0, 5, 9])
def test_evaluate_invalid_age(sample_responses, age):
    with pytest.raises(Exception) as exc:
        evaluate(sample_responses, Sex="Male", Age=age)
    assert "age" in str(exc.value).lower()

# -------------------------------
# 3️⃣ flag=True 옵션 테스트 (튜플 반환)
# -------------------------------
def test_evaluate_flag_true(sample_responses):
    result = evaluate(sample_responses, Sex="Male", Age=25, flag=True)
    # flag=True일 때는 tuple 반환
    assert isinstance(result, tuple)
    assert len(result) == 10  # (SEP, SEFP, SAP, SAFP, SCP, SCFP, SOP, SOFP, SNP, SNFP)
