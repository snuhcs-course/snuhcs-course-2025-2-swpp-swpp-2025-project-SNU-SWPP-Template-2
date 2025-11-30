from __future__ import annotations
from typing import Annotated, List, Optional, Literal, Dict, Any, Tuple
from fastapi import Depends
from datetime import datetime, timezone, date
import random
import json
import re

from fastapi import APIRouter, Depends
from pydantic import BaseModel, Field

from app.core.config import settings
from dotenv import load_dotenv
load_dotenv()

from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser, PydanticOutputParser
from langchain.schema.runnable import RunnableMap

from app.features.journal.models import Journal
from app.features.journal.repository import JournalRepository
from app.features.selfaware.models import Question, Answer, ValueMap, ValueScore
from app.features.selfaware.prompt import category_prompt, personalized_prompt, single_category_prompt, multi_category_prompt, value_score_structured_prompt, value_map_combined_structured_prompt, get_opposite_value_prompt
from app.features.selfaware.prompt import MultiValueScoreStructure, ValueMapAnalysisStructure, OppositeValueStructure, JournalSummary
from app.features.selfaware.repository import QuestionRepository, AnswerRepository, ValueMapRepository, ValueScoreRepository
from app.features.selfaware.value_map import analyze_personality
from app.features.selfaware.prompt import CATEGORIES, CategoryExtractionResponse, QuestionGenerationResponse

from app.features.selfaware.personality_insight.score import questions, choices, prompt, NeoPiAnswers

from app.features.selfaware.personality_insight.evaluator import evaluate

from app.features.selfaware.personality_insight.data.en.prompts import big_5_prompt, total_comment_prompt, extraversion_explanations, openness_explanations, agreeableness_explanations, conscientiousness_explanations, neuroticism_explanations

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
            
            {format_instructions}
            """
        )

        summary_chain = summary_prompt | llm | summary_parser

        summary_response = summary_chain.invoke({
            "journal_text": combined_content,
            "format_instructions": summary_parser.get_format_instructions()
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
        category = "{}({})".format(category_en, category_ko)

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
        """
        해당 question이 실제 존재하는지 확인한 뒤,
        answer를 DB에 생성한다.
        """
        answer = self.answer_repository.create_answer(
            user_id=user_id,
            question_id=question_id,
            text=text
        )
        return answer


    def get_answer_by_question(self, question_id: int) -> Answer | None:
        return self.answer_repository.get_by_question(question_id)
    
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
        
        print("detected_values:", detected_values)

        # 혹은 value_map을 user가 등록되었을 때, craete해도 좋을 듯 합니다
        value_map = self.value_map_repository.get_by_user(user_id)
        if not value_map:
            print("value_map created")
            self.value_map_repository.create_value_map(user_id=user_id)
        
        print(value_map)

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
                    "value": response['opposite_value'],
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
    
    def extract_neo_pi_from_answer(self, user_id:int):
        llm = ChatOpenAI(model="gpt-5-nano").with_structured_output(NeoPiAnswers)

        answers = self.answer_repository.get_by_user(user_id)

        if not answers:
            raise Exception("answer has not written")
        
        answers_text = [answer.text for answer in answers]

        neo_pi_questions = questions
        neo_pi_choices   = choices
        neo_pi_prompt    = prompt

        neo_pi_chain = neo_pi_prompt | llm | StrOutputParser()
        
        responses = [neo_pi_chain.invoke({
            "choices": neo_pi_choices,
            "questions": neo_pi_questions[20*i:20*(i+1)],
            "conversation": answers_text,
        }) for i in range(6)]

        for i in range(6):        
            if responses[i] != 20:
                raise
        total_response = responses[0] + responses[1] + responses[2] + responses[3] + responses[4] + responses[5]

        return total_response
        
    def evaluate_big_5_score(self, user_id, age, sex):
        neo_pi = self.extract_neo_pi_from_answer(user_id)
        return evaluate(neo_pi, sex, age)
    
    def get_comment_from_big_5_score(self, user_id, age, sex):
        score_json = self.evaluate_big_5_score(user_id, age, sex)
        parser = StrOutputParser()
        llm = ChatOpenAI(model="gpt-5-nano")
        big_5_chain = big_5_prompt | llm | parser
        total_comment_chain = total_comment_prompt | llm | parser
        a_response = big_5_chain.invoke({"big_5_explanations": agreeableness_explanations, "big_5_score": score_json})
        c_response = big_5_chain.invoke({"big_5_explanations": conscientiousness_explanations, "big_5_score": score_json})
        e_response = big_5_chain.invoke({"big_5_explanations": extraversion_explanations, "big_5_score": score_json})
        n_response = big_5_chain.invoke({"big_5_explanations": neuroticism_explanations, "big_5_score": score_json})
        o_response = big_5_chain.invoke({"big_5_explanations": openness_explanations, "big_5_score": score_json})
        total_response = total_comment_chain.invoke({"a_response": a_response,"c_response": c_response,"e_response": e_response,"n_response": n_response,"o_response": o_response})
        # example output
        # 당신은 타인에 대한 신뢰와 공감을 바탕으로 협력과 조화를 중시하는 성향이 강하고, 갈등을 피하며 상황에 따라 타협하는 균형 감각이 돋보입니다. 필요할 때는 진실되고 분명하게 자기주장을 표현하는 편이라, 다른 사람의 필요를 돕는 동시에 자신의 목소리도 잃지 않는 모습을 보입니다. 계획성과 체계성을 바탕으로 목표를 명확히 설정하고 꾸준히 달성하려는 자기효능감과 의무감이 강하며, 질서정돈과 성취추구, 자기절제의 특성이 뚜렷합니다. 즉흥적 선택보다는 신중한 판단을 우선하고, 어려운 과제도 끝까지 밀고 나가려는 지속성을 가지며, 상황에 맞춘 주의 집중력을 발휘합니다. 외향적이고 에너지 넘치는 사회적 성향으로 사람들과의 교류에서 활력을 얻되, 자신의 페이스를 지키는 여유도 함께 갖추고 있습니다. 미학과 아이디어에 대한 예민한 감수성과 창 의적 사고를 통해 추상적이고 상징적인 표현을 즐기고, 새로운 경험과 규범에 대한 도전 의식이 강합니다. 그러나 타인 평가에 예민하고 스트레스 상황에서 일시적으로 흔들릴 수 있어, 스트레 스 관리와 감정 조절이 도움이 될 수 있습니다. 전반적으로 구조화된 환경에서도 창의성과 대인 관계를 균형 있게 발휘하는 능력이 돋보이며, 과도한 완벽주의나 몰입으로 인한 부작용을 주의하면 더욱 생산성과 창의성을 폭넓게 확장할 수 있습니다.
        return total_response

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

        # personality_insight = self.get_comment_from_big_5_score(user_id, 23, "Male")

        return self.value_map_repository.generate_comment(user_id = user_id, personality_insight = response.personality_insight, comment = response.comment)