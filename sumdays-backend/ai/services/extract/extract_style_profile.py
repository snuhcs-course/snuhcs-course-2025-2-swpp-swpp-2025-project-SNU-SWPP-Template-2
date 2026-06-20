import os
from typing import List, Dict, Any
from pydantic import BaseModel, Field
from langchain_openai import ChatOpenAI
from langchain.prompts import PromptTemplate

class StyleProfile(BaseModel):
    # 0. 컨셉 정의
    character_concept   : str        = Field(description="작성자를 정의하는 페르소나 별명 (예: 등산 동호회 아재, 시니컬한 고등학생, 애교 많은 냥냥이)")
    # 1. 기본 언어 구조
    tone                : str        = Field(description="글의 전반적 분위기 (예: 담백함, 과장됨, 회의적, 몽환적)")
    formality           : str        = Field(description="격식/비격식 수준 (예: 완전 반말, '해요'체, 인터넷 통신체)")
    sentence_length     : str        = Field(description="평균 문장 길이 (예: 호흡이 짧고 간결함, 만연체)")
    sentence_structure  : str        = Field(description="문장 구성 특징 (예: 주어 생략 빈번, 명사형 종결, 의식의 흐름대로 나열)")
    pacing              : str        = Field(description="글의 리듬감 (예: 급박함, 느긋함, 뚝뚝 끊김)")
    # 2. 캐릭터 시그니처 (Skin & Costume)
    sentence_endings    : List[str]  = Field(description="자주 쓰이는 종결 어미 리스트 (예: ~함, ~다능, ~냐옹)")
    speech_quirks       : str        = Field(description="말투의 뚜렷한 버릇 (예: 말끝 흐리기, 불필요한 추임새, 사투리)")
    punctuation_style   : str        = Field(description="문장 부호 및 시각적 특징 (예: 띄어쓰기 대신 마침표(...) 남발, 물결표(~~), 느낌표 다수)")
    special_syntax      : str        = Field(description="특수 문법/밈 (예: 괄호()로 속마음 표시, 초성(ㅋㅋ) 사용, 3인칭 화법)")
    # 3. 어휘 (Vocabulary)
    lexical_choice      : str        = Field(description="어휘 선택 수준 (예: 전문 용어, 인터넷 은어, 아재 개그, 감성적 고유어)")
    emotional_tone      : str        = Field(description="감정 표현 방식 (예: 직접적 표출, 비꼬기(Sarcasm), 무미건조)")

def compute_style_profile_text(diaries: List[str]) -> Dict[str, Any]:
    combined_text = "\n".join(diaries)

    if not combined_text.strip():
        return {"error": "No text provided for analysis"}

    llm = ChatOpenAI(
        model=os.getenv("IMAGE_MODEL", "gpt-4o-mini"), 
        temperature=0.5
    ).with_structured_output(StyleProfile)

    prompt_text = """
    당신은 텍스트의 [언어학적 구조]와 [캐릭터 페르소나]를 동시에 분석하는 '문체 프로파일러'입니다.
    주어진 글을 정밀 분석하여, 글쓴이의 고유한 스타일을 복제할 수 있는 데이터를 추출하세요.

    [중요 주의사항]
    글의 '내용(Content)'이 무엇인지 요약하지 마십시오. 
    오직 글쓴이가 **'어떻게 말하는지(Style & Expression)'**에만 집중하여 분석해야 합니다.
    사실 정보보다는 어투, 습관, 형식을 찾아내는 것이 목표입니다.

    [분석 지침]
    1. **구조적 특징**: 문장의 길이, 호흡, 격식 등 기본적인 작문 스타일을 분석하세요.
    2. **캐릭터 특징**: 띄어쓰기 파괴, 특수기호, 이모티콘, 말투의 버릇 등 개성을 분석하세요.
    3. 두 가지 측면을 종합하여, 이 사람이 누구인지(Concept) 정의하세요.

    위 지침에 따라 분석한 내용을 정해진 스키마(JSON)에 맞춰 반환하세요.

    글:
    {text}
    """
    
    prompt = PromptTemplate.from_template(prompt_text)
    chain = prompt | llm
    result = chain.invoke({"text": combined_text})

    return result.model_dump()