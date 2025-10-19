from django.urls import path
from .views import get_recommendation

urlpatterns = [
    path('', get_recommendation),
]

#------------------------------------------------------

# from django.urls import path
# from .views import EquipmentRecommendationView

# urlpatterns = [
#     path('', EquipmentRecommendationView.as_view(), name='equipment-recommendation'),
# ]
