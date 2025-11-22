from __future__ import annotations
import random, json
from app.core.config import settings
from dotenv import load_dotenv

from langchain_openai import ChatOpenAI
from langchain_core.output_parsers import StrOutputParser

from app.features.selfaware.repository import (
    AnswerRepository, 
)
from app.features.analysis.comprehensive_analysis.score import (
    questions, choices, prompt, 
    NeoPiAnswers
)
from app.features.analysis.comprehensive_analysis.evaluator import evaluate
from app.features.analysis.comprehensive_analysis.data.en.prompts import (
    big_5_prompt, 
    extraversion_explanations, 
    openness_explanations, 
    agreeableness_explanations, 
    conscientiousness_explanations, 
    neuroticism_explanations
)
from app.features.analysis.repository import AnalysisRepository
from app.features.analysis.prompt import personalized_advice_prompt

load_dotenv()

class AnalysisService:
    def __init__(
        self,
        answer_repository: AnswerRepository,
        analysis_repository: AnalysisRepository,
    ) -> None:
        self.answer_repository = answer_repository
        self.analysis_repository = analysis_repository

    def create_analysis(self, user_id: int):
        return self.analysis_repository.create_analysis(user_id=user_id)
    
    def get_analysis_by_user(self, user_id: int):
        return self.analysis_repository.get_analysis_by_user_id(user_id=user_id)
    
    def extract_neo_pi_from_answer(self, user_id:int):
        llm = ChatOpenAI(model="gpt-5-nano").with_structured_output(NeoPiAnswers)

        answers = self.answer_repository.get_by_user(user_id)

        if not answers:
            raise Exception("answer has not written")
        
        answers_text = [answer.text for answer in answers]

        neo_pi_questions = questions
        neo_pi_choices   = choices
        neo_pi_prompt    = prompt

        neo_pi_chain = neo_pi_prompt | llm
        
        responses = [neo_pi_chain.invoke({
            "choices": neo_pi_choices,
            "questions": neo_pi_questions[20*i:20*(i+1)],
            "conversation": answers_text,
        }) for i in range(6)]

        assert type(responses[0]) == NeoPiAnswers
        assert type(responses[1]) == NeoPiAnswers
        assert type(responses[2]) == NeoPiAnswers
        assert type(responses[3]) == NeoPiAnswers
        assert type(responses[4]) == NeoPiAnswers
        assert type(responses[5]) == NeoPiAnswers

        if len(responses[0].answers) != 20:
            raise
        if len(responses[1].answers) != 20:
            raise   
        if len(responses[2].answers) != 20:
            raise   
        if len(responses[3].answers) != 20:
            raise   
        if len(responses[4].answers) != 20:
            raise   
        if len(responses[5].answers) != 20:
            raise       
            
        total_response = [0] + responses[0].answers + responses[1].answers + responses[2].answers + responses[3].answers + responses[4].answers + responses[5].answers
        print("valid big 5 score generated")
        return total_response
    
    def evaluate_big_5_score(self, user_id, age, sex, flag = False):
        neo_pi = self.extract_neo_pi_from_answer(user_id)
        return evaluate(neo_pi, sex, age, flag)
    
    def update_neo_pi_score(self, user_id: int):
        neo_pi_score = self.evaluate_big_5_score(user_id, 23, "Male", flag = False)
        self.analysis_repository.update_analysis(user_id=user_id, neo_pi_score=neo_pi_score)
    
    def evaluate_user_type(self, user_id):
        analysis = self.analysis_repository.get_analysis_by_user_id(user_id)
        if analysis == None:
            raise
        score = analysis.neo_pi_score
        if score == None:
            raise
        C, N, E, O, A = score['CONSCIENTIOUSNESS'], score['NEUROTICISM'], score['EXTRAVERSION'], score['OPENNESS'], score['AGREEABLENESS']
        if C >= 65 and N <= 45:
            return "목표 지향형"
        if O >= 60 and E >= 60:
            return "탐험가형"
        if E >= 60 and A >= 60:
            return "사교가형"
        if C >= 60 and A >= 60:
            return "배려형"
        if O >= 60 and E <= 45:
            return "사색가형"
        if E >= 60 and A <= 45:
            return "도전형"
        if N <= 40 and C >= 50:
            return "안정추구형"
        if N >= 60 and O >= 60:
            return "감성형"
        if C >= 65 and O <= 45:
            return "분석형"
        if O >= 65 and C <= 45:
            return "변화추구형"
        return "균형형"
    
    def update_user_type(self, user_id: int):
        user_type = self.evaluate_user_type(user_id)
        self.analysis_repository.update_analysis(user_id=user_id, user_type=user_type)

    def get_comment_from_big_5_score(self, user_id, age, sex):
        analysis = self.get_analysis_by_user(user_id)
        if analysis == None or analysis.neo_pi_score == None:
            raise
        score_json = json.dumps(analysis.neo_pi_score, indent=4)
        parser = StrOutputParser()
        llm = ChatOpenAI(model="gpt-5-nano")
        big_5_chain = big_5_prompt | llm | parser
        a_response = big_5_chain.invoke({"big_5_explanations": agreeableness_explanations, "big_5_score": score_json})
        c_response = big_5_chain.invoke({"big_5_explanations": conscientiousness_explanations, "big_5_score": score_json})
        e_response = big_5_chain.invoke({"big_5_explanations": extraversion_explanations, "big_5_score": score_json})
        n_response = big_5_chain.invoke({"big_5_explanations": neuroticism_explanations, "big_5_score": score_json})
        o_response = big_5_chain.invoke({"big_5_explanations": openness_explanations, "big_5_score": score_json})
        return a_response, c_response, e_response, n_response, o_response
    
    def update_comprehensive_analysis(self, user_id: int):
        a_response, c_response, e_response, n_response, o_response = self.get_comment_from_big_5_score(user_id, 23, "Male")
        self.analysis_repository.update_analysis(user_id=user_id, conscientiousness=c_response, neuroticism=n_response, extraversion=e_response, openness=o_response, agreeableness=a_response)

    def extract_personalized_advice(self, user_id: int, age, sex):
        analysis = self.analysis_repository.get_analysis_by_user_id(user_id)
        if analysis == None:
            raise
        score = analysis.neo_pi_score
        if score == None:
            raise

        llm = ChatOpenAI(model="gpt-5-nano")
        personalized_advice_chain = personalized_advice_prompt | llm | StrOutputParser()

        theory = random.choice(["CBT", "ACT", "EQ"])

        response = personalized_advice_chain.invoke({"theory": theory, "neo_pi_summary": score})

        return response
    
    def update_personalized_advice(self, user_id: int):
        personalized_advice = self.extract_personalized_advice(user_id, 23, "Male")
        self.analysis_repository.update_analysis(user_id=user_id, personalized_advice=personalized_advice)