from pydantic import BaseModel, Field
from dotenv import load_dotenv
load_dotenv()
from langchain_openai import ChatOpenAI
from langchain_core.prompts import PromptTemplate

# 1. 분석 지표 정의
class SelfawareValue(BaseModel):
    self_reflection: float = Field(description="자기이해")
    growth_mindset: float = Field(description="성장 지향성")
    empathy: float = Field(description="공감 능력")
    autonomy: float = Field(description="자율성")
    emotional_regulation: float = Field(description="감정 조절력")
    purpose: float = Field(description="삶의 목적성")

prompt = PromptTemplate.from_template(
    "다음은 한 사용자의 질문과 대답입니다.\n"
    "이 내용을 기반으로 사용자의 성향을 다음 6가지 기준에 따라 0~100 사이 intensity 값으로 정량적으로 분석하세요.\n"
    "각 값은 반드시 숫자(float)로만 출력되어야 합니다.\n"
    "기준:\n"
    "- 자기이해 (self_reflection)\n"
    "- 성장 지향성 (growth_mindset)\n"
    "- 공감 능력 (empathy)\n"
    "- 자율성 (autonomy)\n"
    "- 감정 조절력 (emotional_regulation)\n"
    "- 삶의 목적성 (purpose)\n\n"
    "질문과 답변:\n{formatted_qa}\n\n"
)

# 3. LLM 인스턴스
llm = ChatOpenAI(name="gpt-5-nano").with_structured_output(SelfawareValue)

def analyze_personality(qa_pairs: list[dict]):
    """
    qa_pairs: [{"question": "...", "answer": "..."}, ...]
    """
    formatted_qa = "\n".join(
        [f"Q: {qa['question']}\nA: {qa['answer']}\n" for qa in qa_pairs]
    )

    chain = prompt | llm

    try:
        result = chain.invoke({"formatted_qa": formatted_qa})
        return result
    except Exception as e:
        print(f"[analyze_personality] Error: {e}")
        # fallback 값 (중립)
        return {
            "self_reflection": 50.0,
            "growth_mindset": 50.0,
            "empathy": 50.0,
            "autonomy": 50.0,
            "emotional_regulation": 50.0,
            "purpose": 50.0
        }