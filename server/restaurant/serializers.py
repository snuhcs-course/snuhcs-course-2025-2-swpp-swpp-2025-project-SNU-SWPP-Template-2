from rest_framework import serializers
from .models import Restaurant, RestaurantMenu


class RestaurantSerializer(serializers.ModelSerializer):
    """음식점 정보 직렬화"""
    
    class Meta:
        model = Restaurant
        fields = (
            "id",
            "name",
            "address",
            "latitude",
            "longitude",
            "phone",
            "image_url",
            "source",
            "created_at",
        )
        read_only_fields = ("id", "created_at")


class RestaurantMenuSerializer(serializers.ModelSerializer):
    """음식점-메뉴 매핑 직렬화"""
    
    class Meta:
        model = RestaurantMenu
        fields = ("id", "restaurant", "menu")

