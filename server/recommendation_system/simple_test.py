"""
간단한 추천 시스템 테스트

이 스크립트는 기본 기능들을 테스트합니다.
"""

import json
import logging
from recommendation_system.user_profile import create_sample_user_profile
from recommendation_system.scoring import create_sample_search_context, ScoringWeights, HybridScorer, MMRReranker

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def test_user_profile_generation():
    """사용자 프로필 생성 테스트"""
    print("=== 사용자 프로필 생성 테스트 ===")
    
    try:
        # 샘플 프로필 생성
        profile = create_sample_user_profile()
        print("생성된 사용자 프로필:")
        print(profile)
        print()
        
        return True
        
    except Exception as e:
        logger.error(f"사용자 프로필 생성 테스트 실패: {e}")
        return False

def test_search_context_creation():
    """검색 컨텍스트 생성 테스트"""
    print("=== 검색 컨텍스트 생성 테스트 ===")
    
    try:
        # 샘플 검색 컨텍스트 생성
        context = create_sample_search_context()
        print("생성된 검색 컨텍스트:")
        print(f"위치: {context.user_location}")
        print(f"예산: {context.budget_range}")
        print(f"최대 거리: {context.max_distance}km")
        print(f"알레르기: {context.allergies}")
        print(f"비선호 재료: {context.dislikes}")
        print(f"선호 카테고리: {context.preferred_categories}")
        print(f"시간대: {context.time_of_day}")
        print(f"요일: {context.day_of_week}")
        print()
        
        return True
        
    except Exception as e:
        logger.error(f"검색 컨텍스트 생성 테스트 실패: {e}")
        return False

def test_scoring_weights():
    """스코어링 가중치 테스트"""
    print("=== 스코어링 가중치 테스트 ===")
    
    try:
        # 기본 가중치 테스트
        weights = ScoringWeights()
        print("기본 스코어링 가중치:")
        print(f"텍스트 유사도: {weights.text_similarity}")
        print(f"인기도: {weights.popularity}")
        print(f"거리: {weights.distance}")
        print(f"가격: {weights.price}")
        print(f"패널티: {weights.penalty}")
        print(f"신선도: {weights.freshness}")
        print()
        
        # 하이브리드 스코어러 테스트
        scorer = HybridScorer(weights)
        
        # 샘플 점수 계산
        popularity_score = scorer.calculate_popularity_score(4.5, 1000)
        print(f"인기도 점수 (평점 4.5, 리뷰 1000건): {popularity_score:.3f}")
        
        distance_score = scorer.calculate_distance_score(
            (126.9619864, 37.477136), (126.9619864, 37.477136)
        )
        print(f"거리 점수 (같은 위치): {distance_score:.3f}")
        
        price_score = scorer.calculate_price_score(7500, (5000, 15000))
        print(f"가격 점수 (7500원, 예산 5000-15000원): {price_score:.3f}")
        
        penalty_score = scorer.calculate_penalty_score(
            ["단팥빵", "크림치즈"], ["땅콩"], ["고수"]
        )
        print(f"패널티 점수 (알레르기/비선호 없음): {penalty_score:.3f}")
        
        freshness_score = scorer.calculate_freshness_score(True, 1000)
        print(f"신선도 점수 (이미지 있음, 리뷰 1000건): {freshness_score:.3f}")
        print()
        
        return True
        
    except Exception as e:
        logger.error(f"스코어링 가중치 테스트 실패: {e}")
        return False

def test_mmr_reranking():
    """MMR 리랭크 테스트"""
    print("=== MMR 리랭크 테스트 ===")
    
    try:
        # 샘플 아이템 생성
        class MockItem:
            def __init__(self, name, keywords):
                self.name = name
                self.keywords = keywords
        
        # 샘플 데이터
        items = [
            (MockItem("단팥빵", ["단팥빵", "빵", "디저트"]), 0.9),
            (MockItem("크림치즈빵", ["크림치즈", "빵", "디저트"]), 0.8),
            (MockItem("맘모스빵", ["맘모스빵", "빵", "디저트"]), 0.85),
            (MockItem("초코범벅", ["초코", "빵", "디저트"]), 0.7),
            (MockItem("생크림빵", ["생크림", "빵", "디저트"]), 0.75),
        ]
        
        # MMR 리랭크 적용
        reranker = MMRReranker(lambda_param=0.7)
        reranked_items = reranker.rerank_with_mmr(items, max_results=3)
        
        print("MMR 리랭크 결과:")
        for i, (item, score) in enumerate(reranked_items, 1):
            print(f"{i}. {item.name} (점수: {score:.3f})")
        print()
        
        return True
        
    except Exception as e:
        logger.error(f"MMR 리랭크 테스트 실패: {e}")
        return False

def test_api_request_format():
    """API 요청 형식 테스트"""
    print("=== API 요청 형식 테스트 ===")
    
    try:
        # 샘플 요청 데이터 생성
        request_data = {
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
        
        print("샘플 API 요청 데이터:")
        print(json.dumps(request_data, ensure_ascii=False, indent=2))
        print()
        
        return True
        
    except Exception as e:
        logger.error(f"API 요청 형식 테스트 실패: {e}")
        return False

def main():
    """메인 테스트 함수"""
    print("추천 시스템 테스트 시작")
    print("=" * 50)
    
    tests = [
        test_user_profile_generation,
        test_search_context_creation,
        test_api_request_format,
        test_scoring_weights,
        test_mmr_reranking,
    ]
    
    passed = 0
    total = len(tests)
    
    for test_func in tests:
        try:
            if test_func():
                passed += 1
                print("✅ 테스트 통과")
            else:
                print("❌ 테스트 실패")
        except Exception as e:
            logger.error(f"테스트 실행 중 오류: {e}")
            print("❌ 테스트 실패")
        print()
    
    print("=" * 50)
    print(f"테스트 결과: {passed}/{total} 통과")
    
    if passed == total:
        print("🎉 모든 테스트가 성공적으로 완료되었습니다!")
    else:
        print("⚠️ 일부 테스트가 실패했습니다.")

if __name__ == "__main__":
    main()
