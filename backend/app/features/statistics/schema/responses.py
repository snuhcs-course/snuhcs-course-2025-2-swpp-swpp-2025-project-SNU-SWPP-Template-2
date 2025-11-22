from pydantic import BaseModel

# class StatisticsEmotionResponse(BaseModel):
#     happy: float
#     sad: float
#     anxious: float
#     calm: float
#     annoyed: float
#     satisfied: float
#     bored: float
#     interested: float
#     lethargic: float
#     energetic: float

#     @staticmethod
#     def from_dictionary(
#         emotion_rate: dict[str, float],
#     ) -> "StatisticsEmotionResponse":
#         return StatisticsEmotionResponse(
#             happy=emotion_rate["happy"],
#             sad=emotion_rate["sad"],
#             anxious=emotion_rate["anxious"],
#             calm=emotion_rate["calm"],
#             annoyed=emotion_rate["annoyed"],
#             satisfied=emotion_rate["satisfied"],
#             bored=emotion_rate["bored"],
#             interested=emotion_rate["interested"],
#             lethargic=emotion_rate["lethargic"],
#             energetic=emotion_rate["energetic"],
#         )


class EmotionStat(BaseModel):
    emotion: str
    count: int
    percentage: float


class StatisticsEmotionResponse(BaseModel):
    total_count: int
    statistics: list[EmotionStat]
