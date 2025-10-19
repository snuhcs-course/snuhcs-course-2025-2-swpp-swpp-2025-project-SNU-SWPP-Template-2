from django.db import models
from django.contrib.auth.models import User

class EquipmentRecommendation(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE)
    user_level = models.CharField(max_length=20)
    sport_type = models.CharField(max_length=50)
    response = models.JSONField()
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"{self.user.username} - {self.sport_type} ({self.user_level})"

#------------------------------------------------------

# from django.db import models
# from django.contrib.auth.models import User

# class UserProfile(models.Model):
#     user = models.OneToOneField(User, on_delete=models.CASCADE)
#     level = models.CharField(max_length=20)  # 초급/중급/고급
    # preferred_sports = models.JSONField(default=list)  # 선호 운동 종목

# class RecommendationHistory(models.Model):
#     user = models.ForeignKey(User, on_delete=models.CASCADE)
#     request_data = models.JSONField()  # 앱에서 보낸 JSON
    # recommendation_result = models.JSONField()  # OpenAI 추천 결과
    # created_at = models.DateTimeField(auto_now_add=True)

#------------------------------------------------------