"""
추천 API 엔드포인트

이 모듈은 Django REST Framework를 이용한 추천 API를 구현합니다.
"""

import json
import logging
from typing import Dict, List, Optional, Tuple, Any
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_http_methods
from django.utils.decorators import method_decorator
from django.views import View
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework import status

from .user_profile import UserProfileService, create_sample_user_profile
from .scoring import SearchContext, HybridScorer, MMRReranker, RecommendationReranker
from . import EmbeddingService, VectorIndexBuilder, RecommendationEngine
from users.models import UserPreference

logger = logging.getLogger(__name__)

class RecommendationAPIView(View):
    """추천 API 뷰"""
    
    def __init__(self):
        super().__init__()
        self.embedding_service = None
        self.vector_index_builder = None
        self.recommendation_engine = None
        self.user_profile_service = UserProfileService()
        self.reranker = RecommendationReranker()
        self._initialize_services()
    
    def _initialize_services(self):
        """서비스 초기화"""
        try:
            # 임베딩 서비스 초기화
            self.embedding_service = EmbeddingService()
            
            # 벡터 인덱스 빌더 초기화
            self.vector_index_builder = VectorIndexBuilder(self.embedding_service)
            
            # ChromaDB는 자동으로 인덱스 로드됨 (영구 저장소 사용)
            
            # 추천 엔진 초기화
            self.recommendation_engine = RecommendationEngine(self.vector_index_builder)
            
            logger.info("추천 서비스 초기화 완료")
            
        except Exception as e:
            logger.error(f"추천 서비스 초기화 실패: {e}")
            raise
    
    @method_decorator(csrf_exempt)
    def dispatch(self, *args, **kwargs):
        return super().dispatch(*args, **kwargs)
    
    def post(self, request):
        """추천 요청 처리"""
        try:
            # 요청 데이터 파싱
            data = json.loads(request.body)
            
            # 필수 필드 검증
            required_fields = ['user_id', 'user_location', 'query_type']
            for field in required_fields:
                if field not in data:
                    return JsonResponse({
                        'error': f'필수 필드 누락: {field}'
                    }, status=400)
            
            # 사용자 프로필 생성
            user_profile_text = self._create_user_profile(data)
            
            # 검색 컨텍스트 생성
            search_context = self._create_search_context(data)
            
            # 추천 타입에 따른 검색
            query_type = data.get('query_type', 'menu')
            query_text = data.get('query_text', '')
            max_results = data.get('max_results', 20)
            
            if query_type == 'menu':
                results = self._search_menu_recommendations(
                    user_profile_text, search_context, query_text, max_results
                )
            elif query_type == 'place':
                results = self._search_place_recommendations(
                    user_profile_text, search_context, query_text, max_results
                )
            else:
                return JsonResponse({
                    'error': '잘못된 query_type. menu 또는 place를 사용하세요.'
                }, status=400)
            
            # 응답 생성
            response_data = {
                'success': True,
                'query_type': query_type,
                'total_results': len(results),
                'results': results
            }
            
            return JsonResponse(response_data)
            
        except json.JSONDecodeError:
            return JsonResponse({
                'error': '잘못된 JSON 형식'
            }, status=400)
        except Exception as e:
            logger.error(f"추천 요청 처리 실패: {e}")
            return JsonResponse({
                'error': '서버 내부 오류'
            }, status=500)
    
    def _create_user_profile(self, data: Dict) -> str:
        """사용자 프로필 생성"""
        try:
            user_id = data['user_id']
            
            # 온보딩 데이터
            onboarding_data = data.get('onboarding_data', {})
            
            # 갤러리 분석 데이터
            gallery_analysis = data.get('gallery_analysis')
            
            # 행태 데이터
            behavior_data = data.get('behavior_data')
            
            # 프로필 생성
            profile_text = self.user_profile_service.create_user_profile(
                user_id, onboarding_data, gallery_analysis, behavior_data
            )
            
            return profile_text
            
        except Exception as e:
            logger.error(f"사용자 프로필 생성 실패: {e}")
            # 기본 프로필 사용
            return create_sample_user_profile()
    
    def _create_search_context(self, data: Dict) -> SearchContext:
        """검색 컨텍스트 생성"""
        try:
            user_location = data['user_location']
            onboarding_data = data.get('onboarding_data', {})
            
            return SearchContext(
                user_location=tuple(user_location),
                budget_range=tuple(onboarding_data.get('budget_range', [0, 0])),
                max_distance=onboarding_data.get('distance_preference', 2.0),
                allergies=onboarding_data.get('allergies', []),
                dislikes=onboarding_data.get('dislikes', []),
                preferred_categories=onboarding_data.get('preferred_categories', []),
                time_of_day=data.get('time_of_day', '점심'),
                day_of_week=data.get('day_of_week', '평일')
            )
            
        except Exception as e:
            logger.error(f"검색 컨텍스트 생성 실패: {e}")
            # 기본 컨텍스트 사용
            return SearchContext(
                user_location=(126.9619864, 37.477136),
                budget_range=(5000, 15000),
                max_distance=2.0,
                allergies=[],
                dislikes=[],
                preferred_categories=[],
                time_of_day='점심',
                day_of_week='평일'
            )
    
    def _search_menu_recommendations(self, user_profile_text: str, 
                                   search_context: SearchContext, 
                                   query_text: str, max_results: int) -> List[Dict]:
        """메뉴 추천 검색"""
        try:
            # 사용자 프로필 객체 생성
            from .user_profile import UserProfile
            user_profile = UserProfile(
                user_id="temp",
                taste_preferences={},
                allergies=search_context.allergies,
                dislikes=search_context.dislikes,
                preferred_categories=search_context.preferred_categories,
                gallery_keywords=[],
                behavior_keywords=[],
                budget_range=search_context.budget_range,
                distance_preference=search_context.max_distance,
                profile_text=user_profile_text
            )
            
            # 메뉴 검색
            search_results = self.recommendation_engine.search_menu(
                user_profile, query_text, k=max_results * 2
            )
            
            # 리랭크 적용
            reranked_results = self.reranker.rerank_recommendations(
                search_results, search_context, max_results
            )
            
            # 결과 변환
            results = []
            for item, score, reason in reranked_results:
                result = {
                    'id': item.id,
                    'menu_name': item.menu_name,
                    'place_name': item.place_name,
                    'price': item.price,
                    'category': item.category,
                    'location': item.location,
                    'rating': item.rating,
                    'review_count': item.review_count,
                    'keywords': item.keywords,
                    'voted_keywords': item.voted_keywords,
                    'has_image': item.has_image,
                    'coordinates': item.coordinates,
                    'score': score,
                    'reason': reason
                }
                results.append(result)
            
            return results
            
        except Exception as e:
            logger.error(f"메뉴 추천 검색 실패: {e}")
            return []
    
    def _search_place_recommendations(self, user_profile_text: str, 
                                    search_context: SearchContext, 
                                    query_text: str, max_results: int) -> List[Dict]:
        """가게 추천 검색"""
        try:
            # 사용자 프로필 객체 생성
            from .user_profile import UserProfile
            user_profile = UserProfile(
                user_id="temp",
                taste_preferences={},
                allergies=search_context.allergies,
                dislikes=search_context.dislikes,
                preferred_categories=search_context.preferred_categories,
                gallery_keywords=[],
                behavior_keywords=[],
                budget_range=search_context.budget_range,
                distance_preference=search_context.max_distance,
                profile_text=user_profile_text
            )
            
            # 가게 검색
            search_results = self.recommendation_engine.search_place(
                user_profile, query_text, k=max_results * 2
            )
            
            # 리랭크 적용
            reranked_results = self.reranker.rerank_recommendations(
                search_results, search_context, max_results
            )
            
            # 결과 변환
            results = []
            for item, score, reason in reranked_results:
                result = {
                    'id': item.id,
                    'name': item.name,
                    'category': item.category,
                    'location': item.location,
                    'rating': item.rating,
                    'review_count': item.review_count,
                    'avg_price': item.avg_price,
                    'keywords': item.keywords,
                    'voted_keywords': item.voted_keywords,
                    'features': item.features,
                    'coordinates': item.coordinates,
                    'score': score,
                    'reason': reason
                }
                results.append(result)
            
            return results
            
        except Exception as e:
            logger.error(f"가게 추천 검색 실패: {e}")
            return []

@api_view(['POST'])
@permission_classes([IsAuthenticated])
def recommend_menu(request):
    """메뉴 추천 API"""
    try:
        # 요청 데이터 파싱
        data = request.data
        
        # 필수 필드 검증 (user_id 제거)
        required_fields = ['user_location']
        for field in required_fields:
            if field not in data:
                return Response({
                    'error': f'필수 필드 누락: {field}'
                }, status=status.HTTP_400_BAD_REQUEST)
        
        # 사용자 프로필 생성
        user_profile_service = UserProfileService()
        user_id = request.user.username  # 인증된 사용자의 username 사용
        
        # DB에서 유저의 온보딩 정보 조회
        try:
            # 커스텀 User 모델 사용
            from users.models import User
            user = request.user  # 이미 인증된 사용자
            user_preference = UserPreference.objects.get(user=user)
            
            # DB에서 가져온 정보로 onboarding_data 구성
            onboarding_data = {
                'taste_preferences': {
                    'spicy': user_preference.spicy_level,
                    'sweet': user_preference.sweet_level,
                    'salty': user_preference.salty_level,
                    'sour': 3.0,  # 기본값 (DB에 없는 필드)
                    'bitter': 3.0  # 기본값 (DB에 없는 필드)
                },
                'allergies': user_preference.allergies,
                'dislikes': user_preference.disliked_ingredients,
                'preferred_categories': user_preference.favorite_cuisines,
                'budget_range': data.get('budget_range', [0, 0]),  # API 파라미터에서 가져오거나 기본값
                'distance_preference': data.get('distance_preference', 2.0)  # API 파라미터에서 가져오거나 기본값
            }
        except UserPreference.DoesNotExist:
            # 온보딩 정보가 없는 경우 기본값 사용
            onboarding_data = {
                'taste_preferences': {
                    'spicy': 3.0,
                    'sweet': 3.0,
                    'salty': 3.0,
                    'sour': 3.0,
                    'bitter': 3.0
                },
                'allergies': [],
                'dislikes': [],
                'preferred_categories': [],
                'budget_range': data.get('budget_range', [0, 0]),
                'distance_preference': data.get('distance_preference', 2.0)
            }
        
        gallery_analysis = data.get('gallery_analysis')
        behavior_data = data.get('behavior_data')
        
        user_profile_text = user_profile_service.create_user_profile(
            user_id, onboarding_data, gallery_analysis, behavior_data
        )
        
        # 검색 컨텍스트 생성
        search_context = SearchContext(
            user_location=tuple(data['user_location']),
            budget_range=tuple(onboarding_data.get('budget_range', [0, 0])),
            max_distance=onboarding_data.get('distance_preference', 2.0),
            allergies=onboarding_data.get('allergies', []),
            dislikes=onboarding_data.get('dislikes', []),
            preferred_categories=onboarding_data.get('preferred_categories', []),
            time_of_day=data.get('time_of_day', '점심'),
            day_of_week=data.get('day_of_week', '평일')
        )
        
        # 추천 실행
        query_text = data.get('query_text', '')
        max_results = data.get('max_results', 20)
        
        # 실제 추천 실행
        embedding_service = EmbeddingService()
        vector_builder = VectorIndexBuilder(embedding_service, './chroma_db')
        recommendation_engine = RecommendationEngine(vector_builder)
        
        # 메뉴 검색 실행
        menu_results = recommendation_engine.search_menu(
            user_profile_text, 
            query_text, 
            k=max_results * 2  # MMR을 위해 더 많이 검색
        )
        
        # 하이브리드 스코어링 적용
        scorer = HybridScorer()
        scored_results = []
        for menu_doc, similarity_score in menu_results:
            # 메뉴 데이터 준비
            item_data = {
                'rating': menu_doc['rating'],
                'review_count': menu_doc['review_count'],
                'price': menu_doc['price'],
                'coordinates': menu_doc['coordinates'],
                'keywords': menu_doc['keywords'],
                'has_image': menu_doc['has_image']
            }
            score = scorer.calculate_hybrid_score(similarity_score, item_data, search_context)
            scored_results.append((menu_doc, score))
        
        # MMR 리랭킹 적용
        reranker = MMRReranker()
        final_results = reranker.rerank_with_mmr(scored_results, max_results)
        
        # 결과 포맷팅
        formatted_results = []
        for menu_doc, score in final_results:
            formatted_results.append({
                'id': menu_doc['id'],
                'menu_name': menu_doc['menu_name'],
                'place_name': menu_doc['place_name'],
                'price': menu_doc['price'],
                'category': menu_doc['category'],
                'location': menu_doc['location'],
                'rating': menu_doc['rating'],
                'review_count': menu_doc['review_count'],
                'keywords': menu_doc['keywords'],
                'voted_keywords': menu_doc['voted_keywords'],
                'has_image': menu_doc['has_image'],
                'image_urls': menu_doc['image_urls'],  # 이미지 URL 추가
                'coordinates': menu_doc['coordinates'],
                'score': score,
                'reason': f"'{menu_doc['category']}' 카테고리, 평점 {menu_doc['rating']:.1f} (리뷰 {menu_doc['review_count']:,}건)"
            })
        
        return Response({
            'success': True,
            'query_type': 'menu',
            'total_results': len(formatted_results),
            'results': formatted_results
        })
        
    except Exception as e:
        logger.error(f"메뉴 추천 API 오류: {e}")
        return Response({
            'error': '서버 내부 오류'
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@permission_classes([AllowAny])
def recommend_place(request):
    """가게 추천 API"""
    try:
        # 요청 데이터 파싱
        data = request.data
        
        # 필수 필드 검증
        required_fields = ['user_id', 'user_location']
        for field in required_fields:
            if field not in data:
                return Response({
                    'error': f'필수 필드 누락: {field}'
                }, status=status.HTTP_400_BAD_REQUEST)
        
        # 사용자 프로필 생성
        user_profile_service = UserProfileService()
        user_id = data['user_id']
        onboarding_data = data.get('onboarding_data', {})
        gallery_analysis = data.get('gallery_analysis')
        behavior_data = data.get('behavior_data')
        
        user_profile_text = user_profile_service.create_user_profile(
            user_id, onboarding_data, gallery_analysis, behavior_data
        )
        
        # 검색 컨텍스트 생성
        search_context = SearchContext(
            user_location=tuple(data['user_location']),
            budget_range=tuple(onboarding_data.get('budget_range', [0, 0])),
            max_distance=onboarding_data.get('distance_preference', 2.0),
            allergies=onboarding_data.get('allergies', []),
            dislikes=onboarding_data.get('dislikes', []),
            preferred_categories=onboarding_data.get('preferred_categories', []),
            time_of_day=data.get('time_of_day', '점심'),
            day_of_week=data.get('day_of_week', '평일')
        )
        
        # 추천 실행
        query_text = data.get('query_text', '')
        max_results = data.get('max_results', 20)
        
        # 실제 추천 실행
        embedding_service = EmbeddingService()
        vector_builder = VectorIndexBuilder(embedding_service, './chroma_db')
        recommendation_engine = RecommendationEngine(vector_builder)
        
        # 가게 검색 실행
        place_results = recommendation_engine.search_place(
            user_profile_text,
            query_text,
            k=max_results * 2  # MMR을 위해 더 많이 검색
        )
        
        # 하이브리드 스코어링 적용
        scorer = HybridScorer()
        scored_results = []
        for place_doc, similarity_score in place_results:
            # 가게 데이터 준비
            item_data = {
                'rating': place_doc['rating'],
                'review_count': place_doc['review_count'],
                'price': place_doc['avg_price'],
                'coordinates': place_doc['coordinates'],
                'keywords': place_doc['keywords'],
                'has_image': True  # 가게는 기본적으로 이미지가 있다고 가정
            }
            score = scorer.calculate_hybrid_score(similarity_score, item_data, search_context)
            scored_results.append((place_doc, score))
        
        # MMR 리랭킹 적용
        reranker = MMRReranker()
        final_results = reranker.rerank_with_mmr(scored_results, max_results)
        
        # 결과 포맷팅
        formatted_results = []
        for place_doc, score in final_results:
            formatted_results.append({
                'id': place_doc['id'],
                'name': place_doc['name'],
                'category': place_doc['category'],
                'location': place_doc['location'],
                'rating': place_doc['rating'],
                'review_count': place_doc['review_count'],
                'avg_price': place_doc['avg_price'],
                'keywords': place_doc['keywords'],
                'voted_keywords': place_doc['voted_keywords'],
                'features': place_doc['features'],
                'coordinates': place_doc['coordinates'],
                'score': score,
                'reason': f"'{place_doc['category']}' 카테고리, 평점 {place_doc['rating']:.1f} (리뷰 {place_doc['review_count']:,}건)"
            })
        
        return Response({
            'success': True,
            'query_type': 'place',
            'total_results': len(formatted_results),
            'results': formatted_results
        })
        
    except Exception as e:
        logger.error(f"가게 추천 API 오류: {e}")
        return Response({
            'error': '서버 내부 오류'
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['GET'])
@permission_classes([AllowAny])
def health_check(request):
    """헬스 체크 API"""
    return Response({
        'status': 'healthy',
        'service': 'recommendation_system'
    })

def create_sample_request():
    """샘플 요청 데이터 생성"""
    return {
        "user_id": "sample_user",
        "user_location": [126.9619864, 37.477136],
        "query_type": "menu",
        "query_text": "단팥빵 크림치즈",
        "max_results": 10,
        "time_of_day": "점심",
        "day_of_week": "평일",
        "onboarding_data": {
            "spicy": 2.0,
            "sweet": 4.5,
            "salty": 3.0,
            "sour": 2.5,
            "bitter": 1.5,
            "allergies": ["땅콩", "견과류"],
            "dislikes": ["고수", "양고기"],
            "preferred_categories": ["베이커리", "디저트"],
            "budget_range": [5000, 15000],
            "distance_preference": 1.0
        },
        "gallery_analysis": {
            "frequent_keywords": [
                ["단팥빵", 15],
                ["크림치즈", 12],
                ["맘모스빵", 10]
            ],
            "recent_keywords": ["단팥빵", "크림치즈", "맘모스빵"]
        },
        "behavior_data": {
            "liked_menus": ["단팥빵", "크림치즈", "맘모스빵"],
            "liked_places": ["쟝블랑제리", "파리바게뜨"],
            "clicked_keywords": ["단팥빵", "크림치즈", "맘모스빵"]
        }
    }

if __name__ == "__main__":
    # 샘플 요청 데이터 출력
    sample_request = create_sample_request()
    print("샘플 요청 데이터:")
    print(json.dumps(sample_request, ensure_ascii=False, indent=2))
