"""
Root level views for the config project.
"""
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework import status


@api_view(['GET'])
@permission_classes([AllowAny])
def health_check(request):
    """
    헬스 체크 API
    서버가 정상적으로 동작 중인지 확인합니다.
    """
    return Response({
        'status': 'healthy',
        'message': 'Server is running',
        'version': '1.0.0'
    }, status=status.HTTP_200_OK)

