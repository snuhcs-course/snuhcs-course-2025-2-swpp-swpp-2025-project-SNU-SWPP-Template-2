"""
사용자 개인화 벡터 생성 시스템

이 모듈은 사용자의 온보딩 정보, 갤러리 분석 결과, 앱 내 행태를 바탕으로
개인화된 사용자 벡터를 생성합니다.
"""

import json
import logging
from typing import Dict, List, Optional, Tuple, Any
from dataclasses import dataclass
from datetime import datetime, timedelta

logger = logging.getLogger(__name__)

@dataclass
class TastePreference:
    """맛 선호도 데이터 클래스"""
    spicy: float  # 매운맛 (0-5)
    sweet: float  # 단맛 (0-5)
    salty: float  # 짠맛 (0-5)
    sour: float   # 신맛 (0-5)
    bitter: float # 쓴맛 (0-5)

@dataclass
class UserOnboardingData:
    """사용자 온보딩 데이터 클래스"""
    user_id: str
    taste_preferences: TastePreference
    allergies: List[str]
    dislikes: List[str]
    preferred_categories: List[str]
    budget_range: Tuple[int, int]
    distance_preference: float  # km

@dataclass
class GalleryAnalysisResult:
    """갤러리 분석 결과 데이터 클래스"""
    user_id: str
    frequent_keywords: List[Tuple[str, int]]  # (키워드, 빈도)
    time_patterns: Dict[str, Any]  # 시간대별 선호 키워드 또는 선호도
    day_patterns: Dict[str, Any]   # 요일별 선호 키워드 또는 선호도
    recent_keywords: List[str]  # 최근 90일 키워드

@dataclass
class UserBehaviorData:
    """사용자 행태 데이터 클래스"""
    user_id: str
    liked_menus: List[str]
    liked_places: List[str]
    saved_menus: List[str]
    saved_places: List[str]
    reviewed_menus: List[str]
    reviewed_places: List[str]
    clicked_keywords: List[str]
    search_history: List[str]

class UserProfileGenerator:
    """사용자 프로필 생성기"""
    
    def __init__(self):
        self.logger = logging.getLogger(__name__)
    
    def generate_user_profile(self, onboarding_data: UserOnboardingData,
                            gallery_analysis: Optional[GalleryAnalysisResult] = None,
                            behavior_data: Optional[UserBehaviorData] = None) -> str:
        """사용자 프로필 텍스트 생성"""
        try:
            profile_parts = []
            
            # 온보딩 정보 기반 프로필
            profile_parts.append(self._generate_taste_profile(onboarding_data.taste_preferences))
            profile_parts.append(self._generate_allergy_profile(onboarding_data.allergies))
            profile_parts.append(self._generate_dislike_profile(onboarding_data.dislikes))
            profile_parts.append(self._generate_category_profile(onboarding_data.preferred_categories))
            profile_parts.append(self._generate_budget_profile(onboarding_data.budget_range))
            profile_parts.append(self._generate_distance_profile(onboarding_data.distance_preference))
            
            # 갤러리 분석 결과 기반 프로필
            if gallery_analysis:
                profile_parts.append(self._generate_gallery_profile(gallery_analysis))
            
            # 행태 데이터 기반 프로필
            if behavior_data:
                profile_parts.append(self._generate_behavior_profile(behavior_data))
            
            return " ".join(profile_parts)
            
        except Exception as e:
            self.logger.error(f"사용자 프로필 생성 실패: {e}")
            raise
    
    def _generate_taste_profile(self, taste_prefs: TastePreference) -> str:
        """맛 선호도 프로필 생성"""
        taste_descriptions = []
        
        if taste_prefs.spicy >= 4:
            taste_descriptions.append("매운맛 매우 선호")
        elif taste_prefs.spicy >= 3:
            taste_descriptions.append("매운맛 선호")
        elif taste_prefs.spicy <= 1:
            taste_descriptions.append("매운맛 선호 낮음")
        
        if taste_prefs.sweet >= 4:
            taste_descriptions.append("단맛 매우 선호")
        elif taste_prefs.sweet >= 3:
            taste_descriptions.append("단맛 선호")
        elif taste_prefs.sweet <= 1:
            taste_descriptions.append("단맛 선호 낮음")
        
        if taste_prefs.salty >= 4:
            taste_descriptions.append("짠맛 매우 선호")
        elif taste_prefs.salty >= 3:
            taste_descriptions.append("짠맛 선호")
        elif taste_prefs.salty <= 1:
            taste_descriptions.append("짠맛 선호 낮음")
        
        if taste_prefs.sour >= 4:
            taste_descriptions.append("신맛 매우 선호")
        elif taste_prefs.sour >= 3:
            taste_descriptions.append("신맛 선호")
        elif taste_prefs.sour <= 1:
            taste_descriptions.append("신맛 선호 낮음")
        
        if taste_prefs.bitter >= 4:
            taste_descriptions.append("쓴맛 매우 선호")
        elif taste_prefs.bitter >= 3:
            taste_descriptions.append("쓴맛 선호")
        elif taste_prefs.bitter <= 1:
            taste_descriptions.append("쓴맛 선호 낮음")
        
        return f"맛 선호도: {', '.join(taste_descriptions)}"
    
    def _generate_allergy_profile(self, allergies: List[str]) -> str:
        """알레르기 프로필 생성"""
        if not allergies:
            return "알레르기 없음"
        return f"알레르기: {', '.join(allergies)}"
    
    def _generate_dislike_profile(self, dislikes: List[str]) -> str:
        """비선호 재료 프로필 생성"""
        if not dislikes:
            return "비선호 재료 없음"
        return f"비선호 재료: {', '.join(dislikes)}"
    
    def _generate_category_profile(self, categories: List[str]) -> str:
        """선호 카테고리 프로필 생성"""
        if not categories:
            return "선호 카테고리 없음"
        return f"선호 카테고리: {', '.join(categories)}"
    
    def _generate_budget_profile(self, budget_range: Tuple[int, int]) -> str:
        """예산 프로필 생성"""
        min_budget, max_budget = budget_range
        if min_budget == 0 and max_budget == 0:
            return "예산 제한 없음"
        return f"예산 범위: {min_budget:,}원 ~ {max_budget:,}원"
    
    def _generate_distance_profile(self, distance_pref: float) -> str:
        """거리 선호도 프로필 생성"""
        if distance_pref <= 0.5:
            return "가까운 거리 선호"
        elif distance_pref <= 1.0:
            return "보통 거리 선호"
        elif distance_pref <= 2.0:
            return "먼 거리도 가능"
        else:
            return "거리 제한 없음"
    
    def _generate_gallery_profile(self, gallery_analysis: GalleryAnalysisResult) -> str:
        """갤러리 분석 프로필 생성"""
        profile_parts = []
        
        # 빈도 높은 키워드
        if gallery_analysis.frequent_keywords:
            top_keywords = [kw for kw, freq in gallery_analysis.frequent_keywords[:10]]
            profile_parts.append(f"자주 먹는 메뉴: {', '.join(top_keywords)}")
        
        # 최근 키워드
        if gallery_analysis.recent_keywords:
            recent_keywords = gallery_analysis.recent_keywords[:5]
            profile_parts.append(f"최근 선호: {', '.join(recent_keywords)}")
        
        # 시간대 패턴
        if gallery_analysis.time_patterns:
            time_patterns = []
            for time_slot, value in gallery_analysis.time_patterns.items():
                if isinstance(value, list) and value:
                    # 리스트인 경우 키워드로 처리
                    time_patterns.append(f"{time_slot}: {', '.join(value[:3])}")
                elif isinstance(value, (int, float)) and value > 0:
                    # 숫자인 경우 선호도로 처리
                    time_patterns.append(f"{time_slot}: {value:.1f}")
            if time_patterns:
                profile_parts.append(f"시간대 패턴: {'; '.join(time_patterns)}")
        
        return " ".join(profile_parts)
    
    def _generate_behavior_profile(self, behavior_data: UserBehaviorData) -> str:
        """행태 데이터 프로필 생성"""
        profile_parts = []
        
        # 좋아요한 메뉴/가게 키워드
        if behavior_data.liked_menus:
            profile_parts.append(f"좋아요한 메뉴: {', '.join(behavior_data.liked_menus[:5])}")
        
        if behavior_data.liked_places:
            profile_parts.append(f"좋아요한 가게: {', '.join(behavior_data.liked_places[:5])}")
        
        # 저장한 메뉴/가게 키워드
        if behavior_data.saved_menus:
            profile_parts.append(f"저장한 메뉴: {', '.join(behavior_data.saved_menus[:5])}")
        
        if behavior_data.saved_places:
            profile_parts.append(f"저장한 가게: {', '.join(behavior_data.saved_places[:5])}")
        
        # 클릭한 키워드
        if behavior_data.clicked_keywords:
            profile_parts.append(f"관심 키워드: {', '.join(behavior_data.clicked_keywords[:5])}")
        
        # 검색 히스토리
        if behavior_data.search_history:
            profile_parts.append(f"검색 히스토리: {', '.join(behavior_data.search_history[:5])}")
        
        return " ".join(profile_parts)

class UserProfileService:
    """사용자 프로필 서비스"""
    
    def __init__(self):
        self.profile_generator = UserProfileGenerator()
        self.logger = logging.getLogger(__name__)
    
    def create_user_profile(self, user_id: str, onboarding_data: Dict,
                          gallery_analysis: Optional[Dict] = None,
                          behavior_data: Optional[Dict] = None) -> str:
        """사용자 프로필 생성"""
        try:
            # 온보딩 데이터 변환
            taste_preferences = onboarding_data.get("taste_preferences", {})
            taste_prefs = TastePreference(
                spicy=taste_preferences.get("spicy", onboarding_data.get("spicy", 3.0)),
                sweet=taste_preferences.get("sweet", onboarding_data.get("sweet", 3.0)),
                salty=taste_preferences.get("salty", onboarding_data.get("salty", 3.0)),
                sour=taste_preferences.get("sour", onboarding_data.get("sour", 3.0)),
                bitter=taste_preferences.get("bitter", onboarding_data.get("bitter", 3.0))
            )
            
            onboarding = UserOnboardingData(
                user_id=user_id,
                taste_preferences=taste_prefs,
                allergies=onboarding_data.get("allergies", []),
                dislikes=onboarding_data.get("dislikes", []),
                preferred_categories=onboarding_data.get("preferred_categories", []),
                budget_range=tuple(onboarding_data.get("budget_range", [0, 0])),
                distance_preference=onboarding_data.get("distance_preference", 1.0)
            )
            
            # 갤러리 분석 데이터 변환
            gallery = None
            if gallery_analysis:
                # frequent_keywords 데이터 구조 처리
                frequent_keywords = gallery_analysis.get("frequent_keywords", [])
                if frequent_keywords and isinstance(frequent_keywords[0], str):
                    # 문자열 리스트인 경우 빈도 1로 설정
                    frequent_keywords = [(kw, 1) for kw in frequent_keywords]
                elif frequent_keywords and isinstance(frequent_keywords[0], list):
                    # 리스트의 리스트인 경우 튜플로 변환
                    frequent_keywords = [tuple(kw) if isinstance(kw, list) else (kw, 1) for kw in frequent_keywords]
                
                gallery = GalleryAnalysisResult(
                    user_id=user_id,
                    frequent_keywords=frequent_keywords,
                    time_patterns=gallery_analysis.get("time_patterns", {}),
                    day_patterns=gallery_analysis.get("day_patterns", {}),
                    recent_keywords=gallery_analysis.get("recent_keywords", [])
                )
            
            # 행태 데이터 변환
            behavior = None
            if behavior_data:
                behavior = UserBehaviorData(
                    user_id=user_id,
                    liked_menus=behavior_data.get("liked_items", behavior_data.get("liked_menus", [])),
                    liked_places=behavior_data.get("saved_restaurants", behavior_data.get("liked_places", [])),
                    saved_menus=behavior_data.get("saved_menus", []),
                    saved_places=behavior_data.get("saved_places", []),
                    reviewed_menus=behavior_data.get("reviewed_menus", []),
                    reviewed_places=behavior_data.get("reviewed_places", []),
                    clicked_keywords=behavior_data.get("recent_searches", behavior_data.get("clicked_keywords", [])),
                    search_history=behavior_data.get("search_history", [])
                )
            
            # 프로필 생성
            profile_text = self.profile_generator.generate_user_profile(
                onboarding, gallery, behavior
            )
            
            return profile_text
            
        except Exception as e:
            self.logger.error(f"사용자 프로필 생성 실패: {e}")
            raise
    
    def update_user_profile(self, user_id: str, new_data: Dict) -> str:
        """사용자 프로필 업데이트"""
        try:
            # 기존 프로필 로드 (실제 구현에서는 DB에서 로드)
            # 여기서는 간단히 새 데이터로 프로필 생성
            return self.create_user_profile(user_id, new_data)
            
        except Exception as e:
            self.logger.error(f"사용자 프로필 업데이트 실패: {e}")
            raise

def create_sample_user_profile() -> str:
    """샘플 사용자 프로필 생성"""
    profile_service = UserProfileService()
    
    # 샘플 온보딩 데이터
    onboarding_data = {
        "spicy": 2.0,  # 매운맛 선호 낮음
        "sweet": 4.5,  # 단맛 매우 선호
        "salty": 3.0,  # 짠맛 보통
        "sour": 2.5,   # 신맛 선호 낮음
        "bitter": 1.5, # 쓴맛 선호 낮음
        "allergies": ["땅콩", "견과류"],
        "dislikes": ["고수", "양고기"],
        "preferred_categories": ["베이커리", "디저트", "일식"],
        "budget_range": [5000, 15000],
        "distance_preference": 1.0
    }
    
    # 샘플 갤러리 분석 데이터
    gallery_analysis = {
        "frequent_keywords": [
            ("단팥빵", 15),
            ("크림치즈", 12),
            ("맘모스빵", 10),
            ("초코범벅", 8),
            ("생크림빵", 7)
        ],
        "time_patterns": {
            "아침": ["단팥빵", "크림치즈"],
            "점심": ["맘모스빵", "초코범벅"],
            "저녁": ["생크림빵", "디저트"]
        },
        "day_patterns": {
            "평일": ["단팥빵", "크림치즈"],
            "주말": ["맘모스빵", "초코범벅", "생크림빵"]
        },
        "recent_keywords": ["단팥빵", "크림치즈", "맘모스빵", "초코범벅", "생크림빵"]
    }
    
    # 샘플 행태 데이터
    behavior_data = {
        "liked_menus": ["단팥빵", "크림치즈", "맘모스빵"],
        "liked_places": ["쟝블랑제리", "파리바게뜨", "뚜레쥬르"],
        "saved_menus": ["초코범벅", "생크림빵"],
        "saved_places": ["쟝블랑제리", "파리바게뜨"],
        "clicked_keywords": ["단팥빵", "크림치즈", "맘모스빵"],
        "search_history": ["단팥빵", "크림치즈", "맘모스빵", "베이커리", "디저트"]
    }
    
    return profile_service.create_user_profile(
        "sample_user", onboarding_data, gallery_analysis, behavior_data
    )

if __name__ == "__main__":
    # 샘플 프로필 생성 및 출력
    sample_profile = create_sample_user_profile()
    print("샘플 사용자 프로필:")
    print(sample_profile)
