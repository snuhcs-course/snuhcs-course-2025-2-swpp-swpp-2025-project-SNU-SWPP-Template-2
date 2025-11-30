from __future__ import annotations
from typing import Literal, List
from pydantic import BaseModel, Field
from langchain_core.prompts import ChatPromptTemplate

CATEGORIES = [
    ("Neuroticism", "불안정성"),
    ("Extraversion", "외향성"),
    ("Openness to Experience", "개방성"),
    ("Agreeableness", "수용성"),
    ("Conscientiousness", "성실성"),
]
CAT_EN = [en for en, _ in CATEGORIES]
CAT_KO = {en: ko for en, ko in CATEGORIES}

Language = Literal["ko", "en"]
QuestionType = Literal[
    "single_category",      # 하나의 가치 카테고리에 대한 질문
    "multi_category",       # 여러 가치 카테고리에 대한 복합 질문
    "personalized_category" # 사용자의 일기 데이터를 바탕으로 한 질문
]


class CategoryExtractionResponse(BaseModel):
    analysis: str = Field(description="일기의 주요 감정과 그 이유에 대한 분석")
    categories: list[tuple[str, str]] = Field(description="관련 가치 카테고리 리스트 [(영어 표준명, 한국어 표준명)] 형태")

class QuestionGenerationResponse(BaseModel):
    question: str = Field(description="생성된 자기성찰 질문")
    rationale: str = Field(description="질문 생성 이유")

# 감정 분석 프롬프트
emotion_prompt = ChatPromptTemplate.from_template(
    """
    다음 일기의 주요 감정과 그 이유를 분석해줘.
    감정은 하나 이상일 수 있고, 가능한 한 구체적으로 설명해줘.

    일기:
    {journal}
    
    Return Json
    """
)

category_prompt = ChatPromptTemplate.from_template(
    """다음 일기 요약을 분석하여:
    1. 주요 감정과 그 이유를 분석해주세요
    2. 다음 가치 카테고리 중 일기 내용과 가장 관련이 깊은 1-2개를 선택해주세요:
       - 불안정성 (Neuroticism)
       - 외향성 (Extraversion)
       - 개방성 (Openness to Experience)
       - 수용성 (Agreeableness)
       - 성실성 (Conscientiousness)

    일기 요약:
    {summary}
    
    Return JSON:
    - analysis: 주요 감정과 그 이유
    - categories: 관련 가치 카테고리 리스트 [(영어 표준명, 한국어 표준명)] 형태
    """
)

# 자기 성찰 질문 생성 프롬프트
personalized_prompt = ChatPromptTemplate.from_template(
    """
    다음 일기 내용과 감정 분석 결과를 기반으로
    해당 카테고리와 관련된 사용자가 자기 성찰을 할 수 있도록 돕는 질문 1개를 만들어줘.
    질문은 구체적이고, 감정의 원인을 탐색할 수 있도록 구성해줘.

    일기 요약: {summary}  
    감정 분석 결과: {analysis}
    Target categories: {categories}
    
    Guidelines:
    - Invite a concrete episode, feelings, and why it mattered.
    - Avoid yes/no; ask one sentence only.
    - Tone/Style: "사려 깊고 비판단적"
    - 질문은 한국어 한 문장으로 작성하세요.

    Return JSON:
    - question
    - rationale
    """
)

single_category_prompt = ChatPromptTemplate.from_template(
    """You are a coaching-style question designer for a self-reflection diary app.

    Goal: Create exactly ONE open-ended question about a single value category, natural and everyday-language (no category names).

    Target category: {category}

    Guidelines:
    - Invite a concrete episode, feelings, and why it mattered.
    - Avoid yes/no; ask one sentence only.
    - Tone/Style: "사려 깊고 비판단적"
    - 질문은 한국어 한 문장으로 작성하세요.

    Return JSON:
    - question
    - rationale
    """
)

multi_category_prompt = ChatPromptTemplate.from_template(
    """You are a coaching-style question designer for a self-reflection diary app.

    Goal: Create exactly ONE open-ended question that explores tensions/trade-offs or priorities ACROSS multiple value categories, without naming categories.

    Target categories: {categories}

    Guidelines:
    - Encourage reflection on how the user balances these values in real-life decisions.
    - Ask for a specific situation or episode.
    - Avoid yes/no; ask one sentence only.
    - Tone/Style: "사려 깊고 비판단적"
    - 질문은 한국어 한 문장으로 작성하세요.

    Return JSON:
    - question
    - rationale
    """
)

class MultiValueScoreStructure(BaseModel):
    detected_values: List[ValueScoreStructure]

class ValueScoreStructure(BaseModel):
    value: str = Field(
        description=(
            "Summarize the specific personal value, belief, or priority reflected in the user's response. "
            "Available options: "
            "[Friendliness, Gregariousness, Assertiveness, Activity Level, Excitement-Seeking, Cheerfulness,"
            "Trust, Morality, Altruism, Cooperation, Modesty, Sympathy,"
            "Self-Efficacy, Orderliness, Dutifulness, Achievement-Striving, Self-Discipline, Cautiousness,"
            "Anxiety, Anger, Depression, Self-Consciousness, Immoderation, Vulnerability,"
            "Imagination, Artistic Interests, Emotionality, Adventurousness, Intellect, Liberalism]."
        )
    )
    category_key: str = Field(
        description=(
            "Choose **one** category that best represents the value expressed "
            "in the user's response. Available options: "
            "[Neuroticism, Extraversion, Openness to Experience, Agreeableness, Conscientiousness]."
        )
    )
    confidence: float = Field(
        description=(
            "Model's confidence level (0.0-1.0) in the selected category and value assignment."
        )
    )
    intensity: float = Field(
        description=(
            "Degree of emotional or motivational intensity (0.0-1.0) expressed by the user toward this value."
        )
    )
    polarity: int = Field(
        description=(
            "Overall sentiment polarity of the response: use -1 for negative, 0 for neutral, and +1 for positive tone."
        )
    )
    evidence: str = Field(
        description=(
            "Directly quote or paraphrase the key part of the user's response that supports the selected value/category."
        )
    )

# get value score from prompt

value_score_structured_prompt = ChatPromptTemplate.from_template("""
You are an assistant that analyzes diary entries to identify the user's underlying personal value and emotional tone.

Given the following question and answer, extract up to six personal values expressed in the user's response.

Guidelines:
- Write all output in **Korean**, except the value and category which should be in **English canonical form** (e.g., Neuroticism, Extraversion, Openness to Experience, Agreeableness, Conscientiousness).
- Select the **single category** that best matches the main theme or motivation.
- Assess your confidence and emotional intensity on a 0.0-1.0 scale.
- Assign polarity as -1 for negative, 0 for neutral, +1 for positive sentiment.
- Use a short quote (1-2 sentences) from the answer as evidence.

Question:
{question}

Answer:
\"\"\"{answer}\"\"\"

Return the structured result following the ValueScoreStructure.
""")

class ValueMapAnalysisStructure(BaseModel):
    comment: str = Field(
        description=(
            "A short, natural Korean sentence (1 sentence) summarizing the person's most prominent values "
            "and tendencies based on the given scores. "
            "It should sound like a concise personality comment that could appear in an app UI."
        )
    )
    personality_insight: str = Field(
        description=(
            "A longer, natural Korean summary (about 2-3 sentences) that provides a deeper psychological insight "
            "into the person's core motivations, priorities, and worldview. "
            "It should interpret the overall pattern of scores, describing what kind of person they might be "
            "or what they value most in life."
        )
    )

value_map_combined_structured_prompt = ChatPromptTemplate.from_template("""
당신은 사람의 가치관과 성향을 분석하여 자연스러운 한국어 문장으로 설명하는 심리 분석 전문가입니다.

다음은 한 사람의 5가지 가치관 분야별 점수(intensity)입니다:
- 불안정성 (Neuroticism): {score_0}
- 외향성 (Extraversion): {score_1}
- 개방성 (Openness to Experience): {score_2}
- 수용성 (Agreeableness): {score_3}
- 성실성 (Conscientiousness): {score_4}

각 분야가 의미하는 바는 다음과 같습니다.
- Neuroticism refers to the tendency to experience negative feelings.
- Extraversion is marked by pronounced engagement with the external world.
- Openness to Experience describes a dimension of cognitive style that distinguishes imaginative, creative people from down-to-earth, conventional people.
- Agreeableness reflects individual differences in concern with cooperation and social harmony. Agreeable individuals value getting along with others.                                                               
- Conscientiousness concerns the way in which we control, regulate, and direct our impulses.
                                                                        
이 정보를 바탕으로:
1. `comment`: 위 사람의 가치관과 성향을 자연스럽게 요약한 **한 문장짜리 코멘트**를 작성하세요.
2. `personality_insight`: 위 점수의 전반적 패턴을 해석하여, **2~3문장 분량의 심리적 통찰**을 작성하세요.
3. 모든 출력은 한국어로 하세요.

결과는 ValueMapAnalysisStructure에 맞게 구조화하세요.
""")

class OppositeValueStructure(BaseModel):
    opposite_value: str = Field(
        description="주어진 value와 category를 기준으로, 동일 카테고리 내에서 반대되는 가치를 영어 canonical 형태로 반환합니다."
    )


get_opposite_value_prompt = ChatPromptTemplate.from_template("""
당신은 사람의 가치관을 분석하고, 주어진 카테고리 내에서 반대되는 가치를 추출하는 전문가입니다.

입력:
- value: {value}
- category: {category}

지시사항:
1. 주어진 value와 같은 category 내부에서 가장 적절한 반대되는 value를 선택하세요.
2. 반드시 영어 canonical 형태로 반환하세요.
3. 출력은 OppositeValueStructure 구조로 작성하세요.
""")

# Journal Summary
class JournalSummary(BaseModel):
    summary: str = Field(description="최근 일기의 전반적인 요약")