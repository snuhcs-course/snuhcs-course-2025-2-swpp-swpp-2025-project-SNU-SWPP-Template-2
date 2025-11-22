from __future__ import annotations
from typing import List
from datetime import date
import random
from app.core.config import settings
from dotenv import load_dotenv
load_dotenv()

from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import PydanticOutputParser

from app.features.journal.repository import JournalRepository
from app.features.selfaware.models import Question, Answer, ValueMap
from app.features.selfaware.prompt import category_prompt, personalized_prompt, single_category_prompt, multi_category_prompt, value_score_structured_prompt, value_map_combined_structured_prompt, get_opposite_value_prompt
from app.features.selfaware.prompt import MultiValueScoreStructure, ValueMapAnalysisStructure, OppositeValueStructure, JournalSummary
from app.features.selfaware.repository import QuestionRepository, AnswerRepository, ValueMapRepository, ValueScoreRepository
from app.features.selfaware.prompt import CATEGORIES, CategoryExtractionResponse, QuestionGenerationResponse

class QuestionService:
    def __init__(
        self,
        journal_repository: JournalRepository,
        question_repository: QuestionRepository,
    ) -> None:
        self.journal_repository = journal_repository
        self.question_repository = question_repository


    def generate_selfaware_question(self, user_id: int) -> Question:
        """
        특정 사용자의 일기를 DB에서 불러와 요약 → 감정 분석 및 카테고리 추출 → 자기성찰 질문을 생성 → DB 저장
        """
        llm = ChatOpenAI(model="gpt-5-nano")
        summary_parser  = PydanticOutputParser(pydantic_object=JournalSummary)
        category_parser = PydanticOutputParser(pydantic_object=CategoryExtractionResponse)
        question_parser = PydanticOutputParser(pydantic_object=QuestionGenerationResponse)

        # ✅ 1. 유저의 일기 가져오기
        journals = self.journal_repository.list_journals_by_user(user_id)
        if not journals or len(journals) == 0:
            type = random.randint(0, 1)
            if type == 0:
                return self.generate_single_category_question(user_id)
            if type == 1:
                return self.generate_multi_category_question(user_id)

        # ✅ 2. 최근 일기 3~5개만 선택 (토큰 제한 방지)
        recent_journals = journals[-5:]
        combined_content = "\n".join([f"- {j.content}" for j in recent_journals])

        # ✅ 3. 일기 요약 (너무 길면 LLM 부담됨)
        summary_prompt = ChatPromptTemplate.from_template(
            """다음은 한 사용자의 최근 일기 내용입니다. 
            이 일기들의 전반적인 감정과 주제를 간결하게 요약해 주세요.
            
            최근 일기 내용:
            {journal_text}

            Return JSON:
            - summary
            """
        )

        summary_chain = summary_prompt | llm | summary_parser

        summary_response = summary_chain.invoke({
            "journal_text": combined_content,
        })
        summary = summary_response.summary

        # ✅ 4. 감정 분석 및 관련 카테고리 추출
        category_chain = category_prompt | llm | category_parser
        category_response: CategoryExtractionResponse = category_chain.invoke({"summary": summary})

        analysis = category_response.analysis
        categories = category_response.categories
        categories_text_list = []
        for category_en, category_ko in categories:
            categories_text_list.append("{}({})".format(category_en, category_ko))
        categories_text = ", ".join(categories_text_list)

        # ✅ 5. 분석 결과를 바탕으로 자기성찰 질문 생성
        question_chain = personalized_prompt | llm | question_parser
        response: QuestionGenerationResponse = question_chain.invoke({"summary": summary, "analysis": analysis, "categories": categories_text})

        # DB에 저장
        question = self.question_repository.create_question(
            user_id=user_id,
            question_type="personalized_category",  
            text=response.question,
        )

        return question
    

    def generate_single_category_question(self, user_id: int) -> Question:
        llm = ChatOpenAI(model="gpt-5-nano")
        output_parser = PydanticOutputParser(pydantic_object=QuestionGenerationResponse)
        
        # 랜덤하게 카테고리 선택
        selected_category = random.choice(CATEGORIES)
        category_en = selected_category[0]
        category_ko = selected_category[1]
        category = f"{category_en}({category_ko})"

        single_category_chain = single_category_prompt | llm | output_parser
        response: QuestionGenerationResponse = single_category_chain.invoke({"category": category})

        # DB에 저장
        question = self.question_repository.create_question(
            user_id=user_id,
            question_type="single_category",
            text=response.question,
        )

        return question


    def generate_multi_category_question(self, user_id: int) -> Question:
        llm = ChatOpenAI(model="gpt-5-nano")
        output_parser = PydanticOutputParser(pydantic_object=QuestionGenerationResponse)

        # 랜덤하게 2-3개 카테고리 선택
        num_categories = random.randint(2, 3)
        selected_categories = random.sample(CATEGORIES, num_categories)
        
        categories_text = []
        for category_en, category_ko in selected_categories:
            categories_text.append("{}({})".format(category_en, category_ko))
        
        categories = ", ".join(categories_text)

        multi_category_chain = multi_category_prompt | llm | output_parser
        response: QuestionGenerationResponse = multi_category_chain.invoke({"categories": categories})
    
        # DB에 저장
        question = self.question_repository.create_question(
            user_id=user_id,
            question_type="multi_category",
            text=response.question,
        )

        return question

    def generate_question(self, user_id: int) -> Question:
        type = random.randint(0, 2)
        if type == 0:
            return self.generate_selfaware_question(user_id)
        if type == 1:
            return self.generate_single_category_question(user_id)
        return self.generate_multi_category_question(user_id)


    def get_questions_by_id(self, question_id: int) -> Question | None:
        return self.question_repository.get_question_by_id(question_id)

    def list_questions_by_user(
        self, user_id: int, limit: int = 10, cursor: int | None = None
    ) -> list[Question]:
        return self.question_repository.list_questions_by_user(user_id, limit, cursor)   

    def get_questions_by_date(self, user_id: int, date: date) -> Question | None:
        return self.question_repository.get_question_by_date(user_id, date)


class AnswerService:
    def __init__(
        self,
        answer_repository: AnswerRepository,
    ) -> None:
        self.answer_repository = answer_repository


    def create_answer(self, user_id: int, question_id: int, text: str) -> Answer:
        answer = self.answer_repository.create_answer(
            user_id=user_id,
            question_id=question_id,
            text=text
        )
        return answer

    def get_answer_by_question(self, question_id: int) -> Answer | None:
        return self.answer_repository.get_by_question(question_id)
    
    def get_answer_by_user(self, user_id: int) -> list[Answer]:
        return self.answer_repository.get_by_user(user_id)

    def list_answers_by_user(self, user_id: int, question_ids: List[int]) -> list[Answer]:
        return self.answer_repository.list_answers_by_user(user_id, question_ids)

    def extract_keyword(self, id: int):
        pass


class ValueScoreService:
    def __init__(
        self,
        question_repository: QuestionRepository,
        answer_repository: AnswerRepository,
        value_score_repository: ValueScoreRepository,
        value_map_repository: ValueMapRepository,
    ) -> None:
        self.question_repository = question_repository
        self.answer_repository = answer_repository
        self.value_score_repository = value_score_repository
        self.value_map_repository = value_map_repository


    def extract_value_score_from_answer(self, user_id:int, question_id:int, answer_id: int):
        llm = ChatOpenAI(model="gpt-5-nano").with_structured_output(MultiValueScoreStructure)

        question = self.question_repository.get_question_by_id(question_id)
        answer = self.answer_repository.get_answer_by_id(answer_id)

        if not question or not answer:
            raise Exception("question or answer not written")

        value_score_structured_chain = value_score_structured_prompt | llm

        response = value_score_structured_chain.invoke({
            "question": question.text, 
            "answer": answer.text
        })

        assert type(response) == MultiValueScoreStructure
        detected_values = response.detected_values
        
        # print("detected_values:", detected_values)

        # 혹은 value_map을 user가 등록되었을 때, craete해도 좋을 듯 합니다
        value_map = self.value_map_repository.get_by_user(user_id)
        if not value_map:
            print("value_map created")
            self.value_map_repository.create_value_map(user_id=user_id)

        for v in detected_values:
            value_score = self.value_score_repository.create_value_score(
                user_id = user_id,
                question_id = question_id,
                answer_id = answer_id,
                category = v.category_key,
                value = v.value,
                confidence= v.confidence,
                intensity= v.intensity,
                polarity= v.polarity,
                evidence_quotes= [v.evidence],
            )
            self.value_map_repository.update_by_value_score(value_score)
        
        return detected_values

    
    def get_top_value_scores(self, user_id: int):
        top_value_scores = self.value_score_repository.get_top_5_value_scores(user_id)
        value_scores = []

        print(top_value_scores)

        if top_value_scores:
            for top_value_score in top_value_scores:
                if top_value_score.polarity == 0:
                    continue  # polarity가 0이면 append하지 않음
                
                if top_value_score.polarity == -1:
                    llm = ChatOpenAI(model="gpt-5-nano").with_structured_output(OppositeValueStructure)
                    get_opposite_value_chain = get_opposite_value_prompt | llm
                    response = get_opposite_value_chain.invoke({"value":top_value_score.value, "category":top_value_score.category})
                    value_scores.append({
                    "value": response.opposite_value, # type: ignore
                    "intensity": top_value_score.intensity
                    })
                else:
                    value_scores.append({
                    "value": top_value_score.value,
                    "intensity": top_value_score.intensity
                    })

        return value_scores

class ValueMapService:
    def __init__(
        self,
        value_map_repository: ValueMapRepository,
        value_score_repository: ValueScoreRepository,
        answer_repository: AnswerRepository,
    ) -> None:
        self.value_score_repository = value_score_repository
        self.value_map_repository = value_map_repository
        self.answer_repository = answer_repository

    def create_value_map(self, user_id: int):
        return self.value_map_repository.create_value_map(user_id=user_id)

    def get_value_map_by_user(self, user_id) -> ValueMap | None:
        return self.value_map_repository.get_by_user(user_id)
    
    def generate_comment(self, user_id: int):
        value_map = self.value_map_repository.get_by_user(user_id)
        if not value_map:
            raise Exception("value_map does't exist")

        llm = ChatOpenAI(model="gpt-5-nano").with_structured_output(ValueMapAnalysisStructure)
        value_map_combined_structured_chain = value_map_combined_structured_prompt | llm

        response = value_map_combined_structured_chain.invoke(
            {"score_0": value_map.score_0,
             "score_1": value_map.score_1,
             "score_2": value_map.score_2,
             "score_3": value_map.score_3,
             "score_4": value_map.score_4,})
        assert type(response) == ValueMapAnalysisStructure

        return self.value_map_repository.generate_comment(user_id = user_id, personality_insight = response.personality_insight, comment = response.comment)