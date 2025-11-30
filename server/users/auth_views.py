from django.views.decorators.csrf import ensure_csrf_cookie, get_token, csrf_exempt
from django.contrib.auth import authenticate, login, logout
from django.utils.decorators import method_decorator
from django.http import JsonResponse
from django.views.decorators.http import require_POST
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework import status
from .serializers import UserSerializer
from . import services


@api_view(["GET"])
@permission_classes([AllowAny])
@ensure_csrf_cookie
def csrf(request):
    # This view exists so the client can fetch a CSRF cookie before login
    # Return the token in JSON so clients (mobile) can read and set the header
    token = get_token(request)
    return JsonResponse({"detail": "CSRF cookie set", "csrfToken": token})


@api_view(["POST"])
@permission_classes([AllowAny])
@csrf_exempt
def login_view(request):
    username = request.data.get("username")
    password = request.data.get("password")
    user = authenticate(request, username=username, password=password)
    if user is None:
        return JsonResponse({"detail": "아이디 또는 비밀번호가 올바르지 않습니다"}, status=400)
    login(request, user)
    # Session cookie will be set in response
    return JsonResponse(UserSerializer(user).data)


@api_view(["POST"])
@csrf_exempt
def logout_view(request):
    logout(request)
    return JsonResponse({"detail": "로그아웃되었습니다"}, status=204)


@api_view(["POST"])
@permission_classes([AllowAny])
@csrf_exempt
def register_view(request):
    """회원가입 API"""
    username = request.data.get("username")
    email = request.data.get("email")
    password = request.data.get("password")
    
    # 필수 필드 검증
    if not all([username, email, password]):
        return JsonResponse(
            {"detail": "아이디, 이메일, 비밀번호를 모두 입력해주세요"}, 
            status=status.HTTP_400_BAD_REQUEST
        )
    
    # 비밀번호 길이 검증
    if len(password) < 8:
        return JsonResponse(
            {"detail": "비밀번호는 8자 이상이어야 합니다"}, 
            status=status.HTTP_400_BAD_REQUEST
        )
    
    try:
        # 사용자 생성
        user = services.create_user_with_profile(
            username=username,
            email=email,
            password=password,
            bio="",
            preferences={}
        )
        
        # 자동 로그인
        login(request, user)
        
        return JsonResponse(
            UserSerializer(user).data, 
            status=status.HTTP_201_CREATED
        )
        
    except Exception as e:
        # 중복 사용자명이나 이메일 등의 에러 처리
        import logging
        logger = logging.getLogger(__name__)
        logger.error(f"Registration error: {e}")
        
        error_message = str(e)
        if "username" in error_message.lower():
            return JsonResponse(
                {"detail": "이미 사용 중인 아이디입니다"}, 
                status=status.HTTP_400_BAD_REQUEST
            )
        elif "email" in error_message.lower():
            return JsonResponse(
                {"detail": "이미 사용 중인 이메일입니다"}, 
                status=status.HTTP_400_BAD_REQUEST
            )
        else:
            return JsonResponse(
                {"detail": "회원가입에 실패했습니다"}, 
                status=status.HTTP_400_BAD_REQUEST
            )


@api_view(["POST"])
@permission_classes([IsAuthenticated])
@csrf_exempt
def delete_account_view(request):
    """계정 삭제 API"""
    password = request.data.get("password")
    
    # 비밀번호 필수 확인
    if not password:
        return JsonResponse(
            {"detail": "비밀번호를 입력해주세요"}, 
            status=status.HTTP_400_BAD_REQUEST
        )
    
    # 현재 로그인한 사용자의 비밀번호 확인
    user = request.user
    if not user.check_password(password):
        return JsonResponse(
            {"detail": "비밀번호가 올바르지 않습니다"}, 
            status=status.HTTP_400_BAD_REQUEST
        )
    
    try:
        # 사용자 계정 삭제 (연관된 데이터도 함께 삭제됨)
        user_id = user.id
        username = user.username
        
        # 로그아웃 먼저 수행
        logout(request)
        
        # 사용자 삭제
        user.delete()
        
        return JsonResponse(
            {"detail": f"계정 {username}이 성공적으로 삭제되었습니다"}, 
            status=status.HTTP_200_OK
        )
        
    except Exception as e:
        return JsonResponse(
            {"detail": "계정 삭제에 실패했습니다"}, 
            status=status.HTTP_500_INTERNAL_SERVER_ERROR
        )
