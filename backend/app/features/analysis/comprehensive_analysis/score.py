from dotenv import load_dotenv
from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel, Field
from typing import List
import re

load_dotenv()
# -------------------------------
# 1️⃣ 파일 로드
# -------------------------------

def load_questions_with_keyed(path: str) -> list[str]:
    """
    questions.ts 파일에서 각 문항의 text와 keyed를 추출.
    text가 큰따옴표, 작은따옴표, 줄바꿈 포함해도 동작함.
    """
    with open(path, "r", encoding="utf-8") as f:
        text = f.read()

    # text: "..." or text: '...' (줄바꿈 포함 허용)
    texts = re.findall(r'text\s*:\s*[\'"]([^\'"]+)[\'"]', text)
    # keyed: 'plus' or "minus"
    keyeds = re.findall(r'keyed\s*:\s*[\'"](\w+)[\'"]', text)

    if len(texts) != len(keyeds):
        print(f"text({len(texts)})와 keyed({len(keyeds)}) 개수가 다릅니다!")

    results = [f"{t} (keyed: {k})" for t, k in zip(texts, keyeds)]
    return results


questions = load_questions_with_keyed("app/features/analysis/comprehensive_analysis/data/ko/questions.ts")

choices = """
각 문항에 대해 1부터 5까지의 5점 척도로 응답합니다.

각 문항의 방향(keyed)에 따라 선택 의미가 다릅니다.

keyed: plus
    1. 매우 그렇지 않다
    2. 그렇지 않다
    3. 보통이다
    4. 그렇다
    5. 매우 그렇다

keyed: minus
    (반대로 채점됩니다)
    1. 매우 그렇다
    2. 그렇다
    3. 보통이다
    4. 그렇지 않다
    5. 매우 그렇지 않다

즉, 'plus' 문항은 점수가 높을수록 해당 특성이 강하다는 뜻이며,
'minus' 문항은 점수가 높을수록 해당 특성이 약하다는 뜻입니다.
"""

print(f"Loaded {len(questions)} questions, choices")

# -------------------------------
# 2️⃣ LLM & Prompt 구성
# -------------------------------

template = """
당신은 심리분석 전문가입니다.
사용자의 일기 내용을 바탕으로 NEO-PI-R 형태의 20개 문항에 대해 답변을 예측하세요.

입력된 일기:
{conversation}

다음은 문항 응답에 대한 설명입니다:
{choices}

다음은 문항 목록입니다:
{questions}


각 문항에 대해 사용자가 답할 것으로 예상되는 가장 가능성 높은 응답(1~5)을 하나씩 예측하세요.
출력은 반드시 다음 구조를 따르세요:

각 문항에 대한 1~5의 정수값 리스트 (길이는 문항 수와 동일)
"""

prompt = ChatPromptTemplate.from_template(template)

class NeoPiAnswers(BaseModel):
    answers: List[int] = Field(
        description="각 문항(1~5 척도)에 대한 예측 응답 리스트. 길이는 질문 수와 동일해야 함."
    )