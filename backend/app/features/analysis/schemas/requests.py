from pydantic import BaseModel

class UserTypeRequest(BaseModel):
    user_id: int

class ComprehensiveAnalysisRequest(BaseModel):
    user_id: int

class PersonalizedAdviceRequest(BaseModel):
    user_id: int