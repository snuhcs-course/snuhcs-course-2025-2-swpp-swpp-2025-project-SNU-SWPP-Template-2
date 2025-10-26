"""
추천 시스템 URL 설정
"""

from django.urls import path
from . import api

urlpatterns = [
    path('recommend/menu/', api.recommend_menu, name='recommend_menu'),
    path('recommend/place/', api.recommend_place, name='recommend_place'),
    path('health/', api.health_check, name='health_check'),
]
