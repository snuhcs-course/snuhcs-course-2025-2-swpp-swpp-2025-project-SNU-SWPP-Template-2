#!/usr/bin/env python3
"""
추천 API 테스트 스크립트

이 스크립트는 추천 시스템의 API 엔드포인트를 테스트합니다.

사용법:
    python test_recommendation_api.py                    # 메뉴 추천 테스트 (기본)
    python test_recommendation_api.py --test-type menu   # 메뉴 추천 테스트
    python test_recommendation_api.py --test-type place  # 가게 추천 테스트
    python test_recommendation_api.py --test-type error  # 오류 케이스 테스트
    python test_recommendation_api.py --test-type all    # 모든 테스트 실행

테스트 케이스:
    - 메뉴 추천: 6가지 다양한 사용자 프로필 (디저트, 한식, 건강식, 야식, 카페, 가족식사)
    - 가게 추천: 기본 테스트 케이스
    - 오류 케이스: 10가지 오류 상황 테스트 (필수 필드 누락, 잘못된 형식 등)
"""

import os
import sys
import django
import json
import requests
import time
from typing import Dict, List, Any

# Django 설정
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')
django.setup()

# API 기본 설정
BASE_URL = "http://localhost:8000"
API_BASE = f"{BASE_URL}/api/v1/recommendation"

class RecommendationAPITester:
    """추천 API 테스터"""
    
    def __init__(self):
        self.base_url = API_BASE
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        })
    
    def test_server_connection(self) -> bool:
        """서버 연결 테스트"""
        try:
            response = requests.get(f"{BASE_URL}/admin/", timeout=5)
            print(f"✅ 서버 연결 성공: {response.status_code}")
            return True
        except requests.exceptions.RequestException as e:
            print(f"❌ 서버 연결 실패: {e}")
            return False
    
    def test_menu_recommendation(self) -> Dict[str, Any]:
        """메뉴 추천 API 테스트 - 다양한 테스트 케이스"""
        print("\n🍽️ 메뉴 추천 API 테스트")
        print("=" * 50)
        
        # 다양한 테스트 케이스 정의
        test_cases = [
            {
                "name": "단팥빵 애호가 (디저트 선호)",
                "data": {
                    "user_id": "test_user_001",
                    "user_location": [126.9619864, 37.477136],  # 낙성대역
                    "query_text": "단팥빵 맛있는 곳",
                    "max_results": 10,
                    "onboarding_data": {
                        "taste_preferences": {"spicy": 2, "sweet": 5, "salty": 3},
                        "allergies": ["땅콩"],
                        "dislikes": ["고수", "양고기"],
                        "preferred_categories": ["베이커리", "디저트"],
                        "budget_range": [0, 15000],
                        "distance_preference": 2.0
                    },
                    "gallery_analysis": {
                        "frequent_keywords": ["단팥빵", "크림치즈", "맘모스빵"],
                        "time_patterns": {"morning": 0.3, "lunch": 0.5, "dinner": 0.2},
                        "day_patterns": {"weekday": 0.7, "weekend": 0.3}
                    },
                    "behavior_data": {
                        "liked_items": ["단팥빵", "크림치즈빵"],
                        "saved_restaurants": ["쟝블랑제리"],
                        "recent_searches": ["베이커리", "디저트"]
                    },
                    "time_of_day": "점심",
                    "day_of_week": "평일"
                }
            },
            {
                "name": "매운 음식 애호가 (한식 선호)",
                "data": {
                    "user_id": "test_user_002",
                    "user_location": [126.9780, 37.5665],  # 명동
                    "query_text": "매운 김치찌개 맛집",
                    "max_results": 8,
                    "onboarding_data": {
                        "taste_preferences": {"spicy": 5, "sweet": 2, "salty": 4},
                        "allergies": [],
                        "dislikes": ["생선", "해산물"],
                        "preferred_categories": ["한식", "찌개"],
                        "budget_range": [5000, 25000],
                        "distance_preference": 1.5
                    },
                    "gallery_analysis": {
                        "frequent_keywords": ["김치찌개", "된장찌개", "불고기"],
                        "time_patterns": {"morning": 0.1, "lunch": 0.6, "dinner": 0.3},
                        "day_patterns": {"weekday": 0.8, "weekend": 0.2}
                    },
                    "behavior_data": {
                        "liked_items": ["김치찌개", "불고기"],
                        "saved_restaurants": ["김치찌개집"],
                        "recent_searches": ["한식", "찌개"]
                    },
                    "time_of_day": "점심",
                    "day_of_week": "평일"
                }
            },
            {
                "name": "건강식 애호가 (샐러드/비건 선호)",
                "data": {
                    "user_id": "test_user_003",
                    "user_location": [126.9910, 37.5563],  # 강남
                    "query_text": "건강한 샐러드 맛집",
                    "max_results": 6,
                    "onboarding_data": {
                        "taste_preferences": {"spicy": 1, "sweet": 3, "salty": 2},
                        "allergies": ["견과류"],
                        "dislikes": ["튀김", "기름진 음식"],
                        "preferred_categories": ["샐러드", "비건", "헬스푸드"],
                        "budget_range": [8000, 30000],
                        "distance_preference": 3.0
                    },
                    "gallery_analysis": {
                        "frequent_keywords": ["샐러드", "아보카도", "퀴노아"],
                        "time_patterns": {"morning": 0.4, "lunch": 0.5, "dinner": 0.1},
                        "day_patterns": {"weekday": 0.6, "weekend": 0.4}
                    },
                    "behavior_data": {
                        "liked_items": ["아보카도샐러드", "퀴노아볼"],
                        "saved_restaurants": ["그린샐러드"],
                        "recent_searches": ["샐러드", "비건"]
                    },
                    "time_of_day": "아침",
                    "day_of_week": "주말"
                }
            },
            {
                "name": "야식 애호가 (치킨/피자 선호)",
                "data": {
                    "user_id": "test_user_004",
                    "user_location": [126.9342, 37.3595],  # 홍대
                    "query_text": "야식으로 먹을 치킨",
                    "max_results": 5,
                    "onboarding_data": {
                        "taste_preferences": {"spicy": 4, "sweet": 3, "salty": 4},
                        "allergies": [],
                        "dislikes": ["해산물"],
                        "preferred_categories": ["치킨", "피자", "야식"],
                        "budget_range": [15000, 40000],
                        "distance_preference": 2.5
                    },
                    "gallery_analysis": {
                        "frequent_keywords": ["치킨", "피자", "떡볶이"],
                        "time_patterns": {"morning": 0.0, "lunch": 0.2, "dinner": 0.8},
                        "day_patterns": {"weekday": 0.3, "weekend": 0.7}
                    },
                    "behavior_data": {
                        "liked_items": ["양념치킨", "피자"],
                        "saved_restaurants": ["치킨집"],
                        "recent_searches": ["치킨", "야식"]
                    },
                    "time_of_day": "저녁",
                    "day_of_week": "주말"
                }
            },
            {
                "name": "커피 애호가 (카페 선호)",
                "data": {
                    "user_id": "test_user_005",
                    "user_location": [126.9780, 37.5665],  # 이태원
                    "query_text": "맛있는 아메리카노 카페",
                    "max_results": 7,
                    "onboarding_data": {
                        "taste_preferences": {"spicy": 1, "sweet": 4, "salty": 1},
                        "allergies": [],
                        "dislikes": ["매운 음식"],
                        "preferred_categories": ["카페", "커피", "디저트"],
                        "budget_range": [3000, 12000],
                        "distance_preference": 1.0
                    },
                    "gallery_analysis": {
                        "frequent_keywords": ["아메리카노", "라떼", "케이크"],
                        "time_patterns": {"morning": 0.5, "lunch": 0.3, "dinner": 0.2},
                        "day_patterns": {"weekday": 0.4, "weekend": 0.6}
                    },
                    "behavior_data": {
                        "liked_items": ["아메리카노", "라떼"],
                        "saved_restaurants": ["스타벅스", "투썸플레이스"],
                        "recent_searches": ["카페", "커피"]
                    },
                    "time_of_day": "아침",
                    "day_of_week": "주말"
                }
            },
            {
                "name": "가족 식사 (한식/중식 선호)",
                "data": {
                    "user_id": "test_user_006",
                    "user_location": [127.0276, 37.4979],  # 잠실
                    "query_text": "가족과 함께 갈 중국집",
                    "max_results": 4,
                    "onboarding_data": {
                        "taste_preferences": {"spicy": 3, "sweet": 3, "salty": 4},
                        "allergies": ["견과류"],
                        "dislikes": ["매운 음식"],
                        "preferred_categories": ["중식", "한식", "가족식당"],
                        "budget_range": [20000, 50000],
                        "distance_preference": 4.0
                    },
                    "gallery_analysis": {
                        "frequent_keywords": ["짜장면", "탕수육", "볶음밥"],
                        "time_patterns": {"morning": 0.1, "lunch": 0.4, "dinner": 0.5},
                        "day_patterns": {"weekday": 0.2, "weekend": 0.8}
                    },
                    "behavior_data": {
                        "liked_items": ["짜장면", "탕수육"],
                        "saved_restaurants": ["중국집"],
                        "recent_searches": ["중식", "가족식당"]
                    },
                    "time_of_day": "저녁",
                    "day_of_week": "주말"
                }
            }
        ]
        
        results = []
        
        for i, test_case in enumerate(test_cases, 1):
            print(f"\n🧪 테스트 케이스 {i}: {test_case['name']}")
            print("-" * 40)
            
            try:
                print("📤 요청 데이터:")
                print(json.dumps(test_case['data'], indent=2, ensure_ascii=False))
                
                # API 호출
                start_time = time.time()
                response = self.session.post(
                    f"{self.base_url}/recommend/menu/",
                    json=test_case['data'],
                    timeout=30
                )
                end_time = time.time()
                
                print(f"\n⏱️ 응답 시간: {end_time - start_time:.2f}초")
                print(f"📊 응답 상태: {response.status_code}")
                
                if response.status_code == 200:
                    result = response.json()
                    print(f"✅ 메뉴 추천 성공!")
                    print(f"📋 추천 결과: {len(result.get('results', []))}개")
                    
                    # 추천 결과 출력 (상위 3개만)
                    recommendations = result.get('results', [])
                    for j, rec in enumerate(recommendations[:3], 1):
                        print(f"\n{j}. {rec.get('menu_name', 'N/A')} - {rec.get('place_name', 'N/A')}")
                        print(f"   💰 가격: {rec.get('price', 0):,}원")
                        print(f"   ⭐ 평점: {rec.get('rating', 0):.1f} ({rec.get('review_count', 0):,}건)")
                        print(f"   📍 위치: {rec.get('location', 'N/A')}")
                        print(f"   💡 추천 이유: {rec.get('reason', 'N/A')}")
                    
                    results.append({
                        "test_case": test_case['name'],
                        "status": "success",
                        "result": result,
                        "response_time": end_time - start_time
                    })
                else:
                    print(f"❌ 메뉴 추천 실패: {response.status_code}")
                    print(f"오류 내용: {response.text}")
                    results.append({
                        "test_case": test_case['name'],
                        "status": "error",
                        "error": f"HTTP {response.status_code}",
                        "response_time": end_time - start_time
                    })
                    
            except requests.exceptions.RequestException as e:
                print(f"❌ 요청 오류: {e}")
                results.append({
                    "test_case": test_case['name'],
                    "status": "error",
                    "error": str(e),
                    "response_time": 0
                })
            
            # 테스트 케이스 간 잠시 대기
            if i < len(test_cases):
                time.sleep(1)
        
        # 전체 결과 요약
        print(f"\n📊 테스트 결과 요약")
        print("=" * 50)
        success_count = sum(1 for r in results if r['status'] == 'success')
        total_count = len(results)
        avg_response_time = sum(r['response_time'] for r in results) / total_count if total_count > 0 else 0
        
        print(f"총 테스트 케이스: {total_count}개")
        print(f"성공: {success_count}개")
        print(f"실패: {total_count - success_count}개")
        print(f"평균 응답 시간: {avg_response_time:.2f}초")
        
        for result in results:
            status_icon = "✅" if result['status'] == 'success' else "❌"
            print(f"{status_icon} {result['test_case']}: {result['status']} ({result['response_time']:.2f}초)")
        
        return {
            "summary": {
                "total_tests": total_count,
                "success_count": success_count,
                "failure_count": total_count - success_count,
                "avg_response_time": avg_response_time
            },
            "detailed_results": results
        }
    
    def test_place_recommendation(self) -> Dict[str, Any]:
        """가게 추천 API 테스트"""
        print("\n🏪 가게 추천 API 테스트")
        print("=" * 50)
        
        # 테스트 데이터
        test_data = {
            "user_id": "test_user_001",
            "user_location": [126.9619864, 37.477136],
            "query_text": "베이커리 맛집",
            "max_results": 5,
            "onboarding_data": {
                "taste_preferences": {
                    "spicy": 2,
                    "sweet": 5,
                    "salty": 3
                },
                "allergies": ["땅콩"],
                "dislikes": ["고수"],
                "preferred_categories": ["베이커리"],
                "budget_range": [0, 20000],
                "distance_preference": 3.0
            },
            "gallery_analysis": {
                "frequent_keywords": ["단팥빵", "크림치즈"],
                "time_patterns": {
                    "morning": 0.4,
                    "lunch": 0.4,
                    "dinner": 0.2
                }
            },
            "behavior_data": {
                "liked_items": ["단팥빵"],
                "saved_restaurants": ["쟝블랑제리"]
            },
            "time_of_day": "아침",
            "day_of_week": "주말"
        }
        
        try:
            print("📤 요청 데이터:")
            print(json.dumps(test_data, indent=2, ensure_ascii=False))
            
            # API 호출
            start_time = time.time()
            response = self.session.post(
                f"{self.base_url}/recommend/place/",
                json=test_data,
                timeout=30
            )
            end_time = time.time()
            
            print(f"\n⏱️ 응답 시간: {end_time - start_time:.2f}초")
            print(f"📊 응답 상태: {response.status_code}")
            
            if response.status_code == 200:
                result = response.json()
                print(f"✅ 가게 추천 성공!")
                print(f"📋 추천 결과: {len(result.get('results', []))}개")
                
                # 추천 결과 출력
                recommendations = result.get('results', [])
                for i, rec in enumerate(recommendations[:3], 1):
                    print(f"\n{i}. {rec.get('name', 'N/A')}")
                    print(f"   🏷️ 카테고리: {rec.get('category', 'N/A')}")
                    print(f"   ⭐ 평점: {rec.get('rating', 0):.1f} ({rec.get('review_count', 0):,}건)")
                    print(f"   💰 평균 가격: {rec.get('avg_price', 0):,}원")
                    print(f"   📍 위치: {rec.get('location', 'N/A')}")
                    print(f"   💡 추천 이유: {rec.get('reason', 'N/A')}")
                
                return result
            else:
                print(f"❌ 가게 추천 실패: {response.status_code}")
                print(f"오류 내용: {response.text}")
                return {"error": f"HTTP {response.status_code}"}
                
        except requests.exceptions.RequestException as e:
            print(f"❌ 요청 오류: {e}")
            return {"error": str(e)}
    
    def test_error_cases(self):
        """오류 케이스 및 엣지 케이스 테스트"""
        print("\n🚨 오류 케이스 및 엣지 케이스 테스트")
        print("=" * 50)
        
        error_test_cases = [
            {
                "name": "필수 필드 누락 (user_location)",
                "data": {
                    "user_id": "test_user_error_001",
                    # user_location 누락
                    "query_text": "테스트"
                },
                "expected_status": 400
            },
            {
                "name": "필수 필드 누락 (query_text)",
                "data": {
                    "user_id": "test_user_error_002",
                    "user_location": [126.9619864, 37.477136]
                    # query_text 누락
                },
                "expected_status": 400
            },
            {
                "name": "잘못된 좌표 형식",
                "data": {
                    "user_id": "test_user_error_003",
                    "user_location": "잘못된 좌표",  # 문자열로 잘못된 형식
                    "query_text": "테스트"
                },
                "expected_status": 400
            },
            {
                "name": "범위를 벗어난 좌표",
                "data": {
                    "user_id": "test_user_error_004",
                    "user_location": [999.999, 999.999],  # 범위를 벗어난 좌표
                    "query_text": "테스트"
                },
                "expected_status": 400
            },
            {
                "name": "음수 예산 범위",
                "data": {
                    "user_id": "test_user_error_005",
                    "user_location": [126.9619864, 37.477136],
                    "query_text": "테스트",
                    "onboarding_data": {
                        "budget_range": [-1000, 5000]  # 음수 예산
                    }
                },
                "expected_status": 400
            },
            {
                "name": "잘못된 맛 선호도 범위",
                "data": {
                    "user_id": "test_user_error_006",
                    "user_location": [126.9619864, 37.477136],
                    "query_text": "테스트",
                    "onboarding_data": {
                        "taste_preferences": {
                            "spicy": 10,  # 범위를 벗어난 값 (1-5 범위)
                            "sweet": 5,
                            "salty": 3
                        }
                    }
                },
                "expected_status": 400
            },
            {
                "name": "빈 쿼리 텍스트",
                "data": {
                    "user_id": "test_user_error_007",
                    "user_location": [126.9619864, 37.477136],
                    "query_text": ""  # 빈 문자열
                },
                "expected_status": 400
            },
            {
                "name": "매우 긴 쿼리 텍스트",
                "data": {
                    "user_id": "test_user_error_008",
                    "user_location": [126.9619864, 37.477136],
                    "query_text": "매우" * 1000  # 매우 긴 쿼리
                },
                "expected_status": 400
            },
            {
                "name": "잘못된 시간대 값",
                "data": {
                    "user_id": "test_user_error_009",
                    "user_location": [126.9619864, 37.477136],
                    "query_text": "테스트",
                    "time_of_day": "잘못된시간대"  # 잘못된 시간대
                },
                "expected_status": 400
            },
            {
                "name": "잘못된 요일 값",
                "data": {
                    "user_id": "test_user_error_010",
                    "user_location": [126.9619864, 37.477136],
                    "query_text": "테스트",
                    "day_of_week": "잘못된요일"  # 잘못된 요일
                },
                "expected_status": 400
            }
        ]
        
        results = []
        
        for i, test_case in enumerate(error_test_cases, 1):
            print(f"\n🧪 오류 테스트 케이스 {i}: {test_case['name']}")
            print("-" * 40)
            
            try:
                print("📤 요청 데이터:")
                print(json.dumps(test_case['data'], indent=2, ensure_ascii=False))
                
                # API 호출
                start_time = time.time()
                response = self.session.post(
                    f"{self.base_url}/recommend/menu/",
                    json=test_case['data'],
                    timeout=10
                )
                end_time = time.time()
                
                print(f"\n⏱️ 응답 시간: {end_time - start_time:.2f}초")
                print(f"📊 응답 상태: {response.status_code}")
                print(f"📝 응답 내용: {response.text[:200]}...")
                
                if response.status_code == test_case['expected_status']:
                    print(f"✅ 예상된 오류 처리 성공 ({test_case['expected_status']})")
                    results.append({
                        "test_case": test_case['name'],
                        "status": "success",
                        "expected_status": test_case['expected_status'],
                        "actual_status": response.status_code,
                        "response_time": end_time - start_time
                    })
                else:
                    print(f"❌ 예상과 다른 응답: {test_case['expected_status']} != {response.status_code}")
                    results.append({
                        "test_case": test_case['name'],
                        "status": "unexpected",
                        "expected_status": test_case['expected_status'],
                        "actual_status": response.status_code,
                        "response_time": end_time - start_time
                    })
                    
            except requests.exceptions.RequestException as e:
                print(f"❌ 요청 오류: {e}")
                results.append({
                    "test_case": test_case['name'],
                    "status": "error",
                    "error": str(e),
                    "response_time": 0
                })
            
            # 테스트 케이스 간 잠시 대기
            if i < len(error_test_cases):
                time.sleep(0.5)
        
        # 오류 테스트 결과 요약
        print(f"\n📊 오류 테스트 결과 요약")
        print("=" * 50)
        success_count = sum(1 for r in results if r['status'] == 'success')
        unexpected_count = sum(1 for r in results if r['status'] == 'unexpected')
        error_count = sum(1 for r in results if r['status'] == 'error')
        total_count = len(results)
        
        print(f"총 오류 테스트 케이스: {total_count}개")
        print(f"예상대로 처리됨: {success_count}개")
        print(f"예상과 다름: {unexpected_count}개")
        print(f"요청 오류: {error_count}개")
        
        for result in results:
            if result['status'] == 'success':
                print(f"✅ {result['test_case']}: 예상대로 처리됨 ({result['actual_status']})")
            elif result['status'] == 'unexpected':
                print(f"⚠️ {result['test_case']}: 예상과 다름 ({result['expected_status']} != {result['actual_status']})")
            else:
                print(f"❌ {result['test_case']}: 요청 오류")
        
        return {
            "summary": {
                "total_tests": total_count,
                "success_count": success_count,
                "unexpected_count": unexpected_count,
                "error_count": error_count
            },
            "detailed_results": results
        }
    
    def run_all_tests(self):
        """모든 테스트 실행"""
        print("🚀 추천 API 테스트 시작")
        print("=" * 60)
        
        # 서버 연결 테스트
        if not self.test_server_connection():
            print("❌ 서버 연결 실패로 테스트 중단")
            return
        
        # 각 테스트 실행
        menu_result = self.test_menu_recommendation()
        place_result = self.test_place_recommendation()
        error_result = self.test_error_cases()
        
        # 결과 요약
        print("\n📊 전체 테스트 결과 요약")
        print("=" * 60)
        
        # 메뉴 추천 테스트 결과
        if 'summary' in menu_result:
            menu_summary = menu_result['summary']
            print(f"🍽️ 메뉴 추천 테스트:")
            print(f"   총 테스트: {menu_summary['total_tests']}개")
            print(f"   성공: {menu_summary['success_count']}개")
            print(f"   실패: {menu_summary['failure_count']}개")
            print(f"   평균 응답시간: {menu_summary['avg_response_time']:.2f}초")
        else:
            print(f"🍽️ 메뉴 추천: {'✅ 성공' if 'error' not in menu_result else '❌ 실패'}")
        
        # 가게 추천 테스트 결과
        print(f"🏪 가게 추천: {'✅ 성공' if 'error' not in place_result else '❌ 실패'}")
        
        # 오류 케이스 테스트 결과
        if 'summary' in error_result:
            error_summary = error_result['summary']
            print(f"🚨 오류 케이스 테스트:")
            print(f"   총 테스트: {error_summary['total_tests']}개")
            print(f"   예상대로 처리: {error_summary['success_count']}개")
            print(f"   예상과 다름: {error_summary['unexpected_count']}개")
            print(f"   요청 오류: {error_summary['error_count']}개")
        
        # 전체 성공 여부 판단
        menu_success = 'summary' in menu_result and menu_result['summary']['failure_count'] == 0
        place_success = 'error' not in place_result
        error_success = 'summary' in error_result and error_result['summary']['unexpected_count'] == 0
        
        if menu_success and place_success and error_success:
            print("\n🎉 모든 테스트 통과!")
        else:
            print("\n⚠️ 일부 테스트 실패 - 로그를 확인하세요")
        
        return {
            "menu_test": menu_result,
            "place_test": place_result,
            "error_test": error_result
        }

def main():
    """메인 실행 함수"""
    import argparse
    
    parser = argparse.ArgumentParser(description='추천 API 테스트 스크립트')
    parser.add_argument('--test-type', choices=['menu', 'place', 'error', 'all'], 
                       default='menu', help='실행할 테스트 타입')
    parser.add_argument('--verbose', '-v', action='store_true', 
                       help='상세한 출력')
    
    args = parser.parse_args()
    
    tester = RecommendationAPITester()
    
    print(f"🧪 테스트 타입: {args.test_type}")
    print("=" * 50)
    
    if args.test_type == 'menu':
        tester.test_menu_recommendation()
    elif args.test_type == 'place':
        tester.test_place_recommendation()
    elif args.test_type == 'error':
        tester.test_error_cases()
    elif args.test_type == 'all':
        tester.run_all_tests()

if __name__ == "__main__":
    main()
