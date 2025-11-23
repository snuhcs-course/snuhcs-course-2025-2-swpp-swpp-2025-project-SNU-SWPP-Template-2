"""
AI recommendation services for barter matching and book exploration.
"""
import sys
from pathlib import Path
from typing import List, Dict, Any

from django.conf import settings
from books.models import BookCopy, BookWishlist
from accounts.models import UserTaste

# Add AI model path to Python path
AI_MODEL_PATH = Path(settings.BASE_DIR).parent / "ai-model" / "src"
if str(AI_MODEL_PATH) not in sys.path:
    sys.path.insert(0, str(AI_MODEL_PATH))


class AIRecommendationService:
    """Service for AI-powered book recommendations."""
    
    @staticmethod
    def get_barter_context_data(requester, recipient, requested_book) -> Dict[str, Any]:
        """
        교환 추천을 위한 컨텍스트 데이터 수집.
        
        Args:
            requester: 교환 요청자 (A)
            recipient: 교환 수신자 (B)
            requested_book: B가 요청받은 책
            
        Returns:
            AI 모델에 전달할 컨텍스트 데이터
        """
        # A의 교환 가능한 책들
        requester_available_books = BookCopy.objects.filter(
            owner=requester,
            is_for_barter=True,
            trade_status="available"
        ).select_related('publication').prefetch_related(
            'publication__authors',
            'publication__genres'
        )
        
        # B의 위시리스트
        recipient_wishlist = BookWishlist.objects.filter(
            user=recipient
        ).select_related('book__publication').prefetch_related(
            'book__publication__authors',
            'book__publication__genres'
        )
        
        # B의 취향 정보
        try:
            recipient_taste = UserTaste.objects.get(user=recipient)
        except UserTaste.DoesNotExist:
            recipient_taste = None
                
        # B의 서재 (이미 가지고 있는 책 - 추천에서 제외하기 위해)
        recipient_library = BookCopy.objects.filter(
            owner=recipient
        ).select_related('publication')
        
        return {
            'requester': {
                'id': str(requester.id),
                'username': requester.username,
                'available_books': [
                    {
                        'id': str(book.id),
                        'title': book.publication.title,
                        'authors': [a.name for a in book.publication.authors.all()],
                        'genres': [g.name for g in book.publication.genres.all()],
                        'description': book.publication.description,
                        'condition': book.condition,
                        'pages': book.publication.pages,
                        'language': book.publication.language,
                    }
                    for book in requester_available_books
                ]
            },
            'recipient': {
                'id': str(recipient.id),
                'username': recipient.username,
                'requested_book': {
                    'id': str(requested_book.id),
                    'title': requested_book.publication.title,
                    'authors': [a.name for a in requested_book.publication.authors.all()],
                    'genres': [g.name for g in requested_book.publication.genres.all()],
                },
                'wishlist': [
                    {
                        'id': str(item.book.id),
                        'title': item.book.publication.title,
                        'authors': [a.name for a in item.book.publication.authors.all()],
                        'genres': [g.name for g in item.book.publication.genres.all()],
                        'priority': item.priority,
                        'notes': item.notes,
                    }
                    for item in recipient_wishlist
                ],
                'taste': {
                    'favorite_genres': recipient_taste.favorite_genres if recipient_taste else [],
                    'favorite_authors': recipient_taste.favorite_authors if recipient_taste else [],
                    'favorite_books': recipient_taste.favorite_books if recipient_taste else [],
                    'preferred_length': recipient_taste.preferred_length if recipient_taste else None,
                    'preferred_moods': recipient_taste.preferred_moods if recipient_taste else [],
                    'reading_purposes': recipient_taste.reading_purposes if recipient_taste else [],
                } if recipient_taste else {},
                'owned_book_ids': [str(book.publication_id) for book in recipient_library],
                'owned_book_titles': [book.publication.title for book in recipient_library]
            }
        }
    
    @staticmethod
    def get_exploration_context_data(user) -> Dict[str, Any]:
        """
        탐색 탭 추천을 위한 컨텍스트 데이터 수집.
        
        Args:
            user: 추천을 받을 사용자
            
        Returns:
            AI 모델에 전달할 컨텍스트 데이터
        """
        # 사용자의 위시리스트
        user_wishlist = BookWishlist.objects.filter(
            user=user
        ).select_related('book__publication').prefetch_related(
            'book__publication__authors',
            'book__publication__genres'
        )
        
        # 사용자의 취향 정보
        try:
            user_taste = UserTaste.objects.get(user=user)
        except UserTaste.DoesNotExist:
            user_taste = None
        
        # 사용자의 현재 서재 (이미 가지고 있는 책 - 추천에서 제외하기 위해)
        user_library = BookCopy.objects.filter(
            owner=user
        ).select_related('publication')
        
        return {
            'user': {
                'id': str(user.id),
                'username': user.username,
                'wishlist': [
                    {
                        'id': str(item.book.id),
                        'title': item.book.publication.title,
                        'authors': [a.name for a in item.book.publication.authors.all()],
                        'genres': [g.name for g in item.book.publication.genres.all()],
                        'priority': item.priority,
                        'notes': item.notes,
                    }
                    for item in user_wishlist
                ],
                'taste': {
                    'favorite_genres': user_taste.favorite_genres if user_taste else [],
                    'favorite_authors': user_taste.favorite_authors if user_taste else [],
                    'favorite_books': user_taste.favorite_books if user_taste else [],
                    'preferred_length': user_taste.preferred_length if user_taste else None,
                    'preferred_moods': user_taste.preferred_moods if user_taste else [],
                    'reading_purposes': user_taste.reading_purposes if user_taste else [],
                } if user_taste else {},
                'owned_book_ids': [str(book.id) for book in user_library]
            }
        }
    
    @staticmethod
    def recommend_books_for_barter(requester, recipient, requested_book, limit: int = 3) -> List[str]:
        """
        AI를 사용하여 교환에 적합한 책들을 추천.
        실제 모델 호출 예시 포함.
        """
        context_data = AIRecommendationService.get_barter_context_data(
            requester, recipient, requested_book
        )
        # 실제 AI 모델 호출 예시
        try:
            from pipeline.recommender import BarterRecommender
            from data.entities import BarterContext, Item, UserProfile, TradeRequest
            # 컨텍스트 변환 (간단 예시)
            items = {}
            for book in context_data['requester']['available_books']:
                items[book['id']] = Item(
                    item_id=book['id'],
                    owner_id=context_data['requester']['id'],
                    title=book['title'],
                    valuation=1.0,
                    facets={
                        'genre': ','.join(book['genres']),
                        'author': ','.join(book['authors'])
                    },
                    metadata={}
                )
            profiles = {
                context_data['requester']['id']: UserProfile(
                    user_id=context_data['requester']['id'],
                    display_name=context_data['requester']['username'],
                    trust_score=0.8,
                    reliability=0.9,
                    preferences=context_data['recipient']['taste'],
                ),
                context_data['recipient']['id']: UserProfile(
                    user_id=context_data['recipient']['id'],
                    display_name=context_data['recipient']['username'],
                    trust_score=0.8,
                    reliability=0.9,
                    preferences=context_data['recipient']['taste'],
                )
            }
            requests = [
                TradeRequest(
                    user_id=context_data['recipient']['id'],
                    desired_item_ids=[context_data['recipient']['requested_book']['id']],
                    desired_facets={}
                )
            ]
            context = BarterContext(
                items=items,
                profiles=profiles,
                requests=requests
            )
            recommender = BarterRecommender()
            recommendations = recommender.recommend(context, limit=limit)
            return [rec.candidate.item_id for rec in recommendations]
        except Exception as e:
            # 임시: 랜덤으로 선택 (AI 모델 통합 실패 시)
            available_books = BookCopy.objects.filter(
                owner=requester,
                is_for_barter=True,
                trade_status="available"
            ).order_by('?')[:limit]
            return [str(book.id) for book in available_books]
    
    @staticmethod
    def recommend_books_for_exploration(user, limit: int = 10) -> List[Dict[str, Any]]:
        """
        AI를 사용하여 탐색 탭에서 보여줄 책들을 추천.
        실제 모델 호출 예시 포함.
        """
        context_data = AIRecommendationService.get_exploration_context_data(user)
        try:
            from pipeline.recommender import BarterRecommender
            from data.entities import BarterContext, Item, UserProfile, TradeRequest
            # 컨텍스트 변환 (간단 예시)
            items = {}
            # ... (탐색용 변환 로직 필요)
            context = BarterContext(
                items=items,
                profiles={},
                requests=[]
            )
            recommender = BarterRecommender()
            recommendations = recommender.recommend(context, limit=limit)
            # 예시: 추천 결과를 표준화된 dict로 변환
            return [
                {
                    'id': rec.candidate.item_id,
                    'title': rec.candidate.title,
                    'authors': [],
                    'genres': [],
                    'owner': {},
                    'condition': '',
                    'cover_image': None,
                }
                for rec in recommendations
            ]
        except Exception as e:
            # 임시: 사용자가 갖고 있지 않은 교환 가능한 책들 반환
            owned_ids = BookCopy.objects.filter(owner=user).values_list('publication_id', flat=True)
            recommended_books = BookCopy.objects.filter(
                is_for_barter=True,
                trade_status="available"
            ).exclude(
                publication_id__in=owned_ids
            ).exclude(
                owner=user
            ).select_related('publication', 'owner').prefetch_related(
                'publication__authors',
                'publication__genres'
            ).order_by('?')[:limit]
            return [
                {
                    'id': str(book.id),
                    'title': book.publication.title,
                    'authors': [a.name for a in book.publication.authors.all()],
                    'genres': [g.name for g in book.publication.genres.all()],
                    'owner': {
                        'id': str(book.owner.id),
                        'username': book.owner.username,
                    },
                    'condition': book.condition,
                    'cover_image': book.publication.cover_image.url if book.publication.cover_image else None,
                }
                for book in recommended_books
            ]
