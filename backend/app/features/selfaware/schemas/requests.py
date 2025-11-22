from pydantic import BaseModel


class AnswerRequest(BaseModel):
    question_id: int
    text: str