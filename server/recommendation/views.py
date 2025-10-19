from django.http import JsonResponse

def get_recommendation(request):
    # 여기서는 DB 없이 바로 dummy JSON 반환

    dummy_data = {
        "recommendations": [
            {"name": "입문용 골프채", "price": 150000, "level": 3, "image_url": "ic_logo"},
            {"name": "고급 골프공 세트", "price": 45000, "level": 4, "image_url": "ic_logo"},
            {"name": "골프화 (남성용)", "price": 120000, "level": 2, "image_url": "ic_logo"},
            {"name": "골프 장갑", "price": 25000, "level": 5, "image_url": "ic_logo"},
            {"name": "골프복", "price": 125000, "level": 5, "image_url": "ic_logo"}
        ]
    }
    return JsonResponse(dummy_data)

#------------------------------------------------------

# from rest_framework.views import APIView
# from rest_framework.response import Response
# from rest_framework import status
# from django.conf import settings
# import openai, json
# from .models import EquipmentRecommendation
# from .serializers import EquipmentRecommendationSerializer

# openai.api_key = settings.OPENAI_API_KEY

# class EquipmentRecommendationView(APIView):
#     def post(self, request):
#         user = request.user
#         user_level = request.data.get('user_level', 'beginner')
#         sport_type = request.data.get('sport_type', 'fitness')

#         prompt = f"Recommend 3 workout equipments suitable for a {user_level} user interested in {sport_type}. Return a JSON array of objects with fields: name, description, and amazon_search_keyword."

#         completion = openai.ChatCompletion.create(
#             model='gpt-4-turbo',
#             messages=[{'role': 'user', 'content': prompt}],
#             temperature=0.7
#         )

#         response_text = completion.choices[0].message.content.strip()
#         try:
#             response_json = json.loads(response_text)
#         except:
#             response_json = {'error': 'Invalid JSON', 'raw': response_text}

#         rec = EquipmentRecommendation.objects.create(
#             user=user,
#             user_level=user_level,
#             sport_type=sport_type,
#             response=response_json
#         )

#         serializer = EquipmentRecommendationSerializer(rec)
#         return Response(serializer.data, status=status.HTTP_200_OK)

#------------------------------------------------------

# from django.shortcuts import get_object_or_404
# from rest_framework.views import APIView
# from rest_framework.response import Response
# import openai
# from .models import UserProfile, RecommendationHistory
# from django.conf import settings

# openai.api_key = settings.OPENAI_API_KEY

# class EquipmentRecommendationView(APIView):
#     def post(self, request):
#         user_id = request.data.get("user_id")
#         user_profile = get_object_or_404(UserProfile, user__id=user_id)

        # DB 정보와 앱에서 온 요청 합치기
        # prompt_data = {
        #     "level": user_profile.level,
        #     "preferred_sports": user_profile.preferred_sports,
        #     "requested_info": request.data
        # }

        # OpenAI API 호출
        # response = openai.ChatCompletion.create(
        #     model="gpt-4",
        #     messages=[{"role": "user", "content": str(prompt_data)}]
        # )

        # result = response["choices"][0]["message"]["content"]

        # 추천 히스토리 저장
        # RecommendationHistory.objects.create(
        #     user_id=user_id,
        #     request_data=request.data,
        #     recommendation_result={"recommendation": result}
        # )

        # return Response({"recommendation": result})
