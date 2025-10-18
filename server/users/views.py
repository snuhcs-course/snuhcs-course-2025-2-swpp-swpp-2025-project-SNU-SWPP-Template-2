from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from django.shortcuts import get_object_or_404
from .models import User, Follow, UserScrap
from .serializers import UserSerializer, ProfileSerializer, FollowSerializer, UserScrapSerializer
from restaurant.models import Restaurant
from . import services


class MeViewSet(viewsets.ViewSet):
    permission_classes = [IsAuthenticated]

    def list(self, request):
        return Response(UserSerializer(request.user).data)

    @action(detail=False, methods=["patch"])
    def preferences(self, request):
        profile = services.update_profile_preferences(user=request.user, patch=request.data)
        return Response(ProfileSerializer(profile).data)


class UserViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = User.objects.select_related("profile").all()
    serializer_class = UserSerializer
    permission_classes = [IsAuthenticated]

    @action(detail=True, methods=["get"])
    def followers(self, request, pk=None):
        users = services.list_followers(user_id=pk)
        return Response(UserSerializer(users, many=True).data)

    @action(detail=True, methods=["get"])
    def followings(self, request, pk=None):
        users = services.list_followings(user_id=pk)
        return Response(UserSerializer(users, many=True).data)


class FollowViewSet(viewsets.GenericViewSet):
    permission_classes = [IsAuthenticated]
    serializer_class = FollowSerializer

    @action(detail=False, methods=["post"])
    def request(self, request):
        f = services.request_follow(follower=request.user, following_id=request.data["following_id"])
        return Response(FollowSerializer(f).data, status=status.HTTP_201_CREATED)

    @action(detail=False, methods=["post"])
    def accept(self, request):
        f = services.accept_follow(follower_id=request.data["follower_id"], following=request.user)
        return Response(FollowSerializer(f).data)

    @action(detail=False, methods=["post"])
    def unfollow(self, request):
        services.unfollow(follower=request.user, following_id=request.data["following_id"])
        return Response(status=status.HTTP_204_NO_CONTENT)


class SuggestionViewSet(viewsets.ViewSet):
    permission_classes = [IsAuthenticated]

    def list(self, request):
        users = services.list_follow_suggestions(user=request.user, limit=int(request.query_params.get("limit", 10)))
        return Response(UserSerializer(users, many=True).data)


class ScrapViewSet(viewsets.GenericViewSet):
    """음식점 스크랩 관리 API"""
    permission_classes = [IsAuthenticated]
    serializer_class = UserScrapSerializer
    
    def get_queryset(self):
        """현재 유저의 스크랩만 조회"""
        return UserScrap.objects.filter(user=self.request.user).select_related('restaurant')
    
    def list(self, request):
        """내 스크랩 목록 조회"""
        scraps = self.get_queryset().order_by('-created_at')
        serializer = self.get_serializer(scraps, many=True)
        return Response(serializer.data)
    
    def create(self, request):
        """스크랩 추가"""
        restaurant_id = request.data.get('restaurant_id')
        
        if not restaurant_id:
            return Response(
                {"error": "restaurant_id is required"},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        # 음식점 존재 확인
        restaurant = get_object_or_404(Restaurant, id=restaurant_id)
        
        # 이미 스크랩했는지 확인
        if UserScrap.objects.filter(user=request.user, restaurant=restaurant).exists():
            return Response(
                {"error": "Already scrapped"},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        # 스크랩 생성
        scrap = UserScrap.objects.create(user=request.user, restaurant=restaurant)
        serializer = self.get_serializer(scrap)
        return Response(serializer.data, status=status.HTTP_201_CREATED)
    
    def destroy(self, request, pk=None):
        """스크랩 삭제"""
        scrap = get_object_or_404(self.get_queryset(), pk=pk)
        scrap.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)
    
    @action(detail=False, methods=['post'], url_path='toggle')
    def toggle(self, request):
        """스크랩 토글 (있으면 삭제, 없으면 추가)"""
        restaurant_id = request.data.get('restaurant_id')
        
        if not restaurant_id:
            return Response(
                {"error": "restaurant_id is required"},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        restaurant = get_object_or_404(Restaurant, id=restaurant_id)
        
        # 이미 스크랩했는지 확인
        scrap = UserScrap.objects.filter(user=request.user, restaurant=restaurant).first()
        
        if scrap:
            # 스크랩 해제
            scrap.delete()
            return Response(
                {"scrapped": False, "message": "Scrap removed"},
                status=status.HTTP_200_OK
            )
        else:
            # 스크랩 추가
            scrap = UserScrap.objects.create(user=request.user, restaurant=restaurant)
            serializer = self.get_serializer(scrap)
            return Response(
                {"scrapped": True, "data": serializer.data},
                status=status.HTTP_201_CREATED
            )
