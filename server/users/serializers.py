from rest_framework import serializers
from .models import User, Profile, Follow, UserScrap
from restaurant.serializers import RestaurantSerializer


class ProfileSerializer(serializers.ModelSerializer):
    class Meta:
        model = Profile
        fields = ("bio", "preferences", "updated_at")


class UserSerializer(serializers.ModelSerializer):
    profile = ProfileSerializer(read_only=True)
    
    class Meta:
        model = User
        fields = ("id", "username", "email", "profile")


class FollowSerializer(serializers.ModelSerializer):
    class Meta:
        model = Follow
        fields = ("id", "follower", "following", "status", "created_at")
        read_only_fields = ("status", "created_at")


class UserScrapSerializer(serializers.ModelSerializer):
    """유저 스크랩 직렬화"""
    restaurant = RestaurantSerializer(read_only=True)
    restaurant_id = serializers.IntegerField(write_only=True)
    
    class Meta:
        model = UserScrap
        fields = ("id", "user", "restaurant", "restaurant_id", "created_at")
        read_only_fields = ("id", "user", "created_at")
    
    def create(self, validated_data):
        # user는 view에서 자동으로 설정됨
        return super().create(validated_data)
