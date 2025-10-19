# 음식점 & 메뉴 추천 시스템

이 시스템은 음식점과 메뉴 데이터를 메뉴 단위와 가게 단위로 분리해 임베딩·검색하는 구조를 구현합니다.

## 주요 기능

- **메뉴 단위 추천**: 개별 메뉴를 기반으로 한 정확한 추천
- **가게 단위 추천**: 음식점 전체를 기반으로 한 장소 추천
- **개인화 벡터**: 사용자의 온보딩 정보, 갤러리 분석, 행태 데이터를 통한 개인화
- **하이브리드 스코어링**: 텍스트 유사도, 인기도, 거리, 가격 등을 종합 고려
- **MMR 리랭크**: 다양성을 확보한 추천 결과 제공

## 시스템 구조

### 1. 데이터 처리
- JSON 데이터를 메뉴/가게 문서로 변환
- 한국어 텍스트 임베딩을 위한 문서 템플릿 생성
- ChromaDB 벡터 인덱스 구축

### 2. 사용자 프로필
- 온보딩 정보 (맛 선호도, 알레르기, 비선호 재료 등)
- 갤러리 분석 결과 (자주 먹은 메뉴, 시간대 패턴 등)
- 앱 내 행태 (좋아요, 저장, 리뷰 등)

### 3. 추천 엔진
- 텍스트 유사도 기반 1차 검색
- 하이브리드 스코어링으로 2차 리랭크
- MMR 알고리즘으로 다양성 확보

## 설치 및 설정

### 1. 의존성 설치
```bash
pip install -r requirements.txt
```

### 2. Django 설정
`config/settings.py`에 `recommendation_system` 앱이 추가되어 있는지 확인하세요.

### 3. 데이터 처리 및 인덱스 구축
```bash
python manage.py build_index --json-file restaurant/management/commands/장블랑제리_상세.json
```

## API 사용법

### 1. 메뉴 추천 API
```bash
POST /api/v1/recommendation/recommend/menu/
```

**요청 예시:**
```json
{
  "user_id": "sample_user",
  "user_location": [126.9619864, 37.477136],
  "query_text": "단팥빵 크림치즈",
  "max_results": 10,
  "onboarding_data": {
    "spicy": 2.0,
    "sweet": 4.5,
    "salty": 3.0,
    "allergies": ["땅콩", "견과류"],
    "dislikes": ["고수", "양고기"],
    "preferred_categories": ["베이커리", "디저트"],
    "budget_range": [5000, 15000],
    "distance_preference": 1.0
  },
  "gallery_analysis": {
    "frequent_keywords": [["단팥빵", 15], ["크림치즈", 12]],
    "recent_keywords": ["단팥빵", "크림치즈", "맘모스빵"]
  },
  "behavior_data": {
    "liked_menus": ["단팥빵", "크림치즈", "맘모스빵"],
    "liked_places": ["쟝블랑제리", "파리바게뜨"]
  }
}
```

**응답 예시:**
```json
{
  "success": true,
  "query_type": "menu",
  "total_results": 1,
  "results": [
    {
      "id": "12800337_4",
      "menu_name": "맘모스빵",
      "place_name": "쟝블랑제리 낙성대본점",
      "price": 7500,
      "category": "베이커리",
      "location": "서울/관악구/봉천동",
      "rating": 4.44,
      "review_count": 25804,
      "keywords": ["단팥빵", "크림치즈", "맘모스빵"],
      "voted_keywords": ["빵이 맛있어요", "가성비가 좋아요"],
      "has_image": true,
      "coordinates": [126.9619864, 37.477136],
      "score": 0.95,
      "reason": "'베이커리' 선호 카테고리, '맘모스빵' 키워드 매칭, 높은 평점 4.4 (리뷰 25804건)"
    }
  ]
}
```

### 2. 가게 추천 API
```bash
POST /api/v1/recommendation/recommend/place/
```

### 3. 헬스 체크 API
```bash
GET /api/v1/recommendation/health/
```

## 테스트

시스템 테스트를 실행하려면:
```bash
python recommendation_system/test_system.py
```

## 주요 클래스 및 모듈

### 1. DocumentTemplateGenerator
- 메뉴/가게 문서 템플릿 생성
- JSON 데이터를 추천에 유의미한 한국어 문장으로 변환

### 2. EmbeddingService
- 한국어 문장 임베딩 모델 (`jhgan/ko-sbert-sts`) 사용
- 텍스트를 벡터로 변환

### 3. VectorIndexBuilder
- ChromaDB 벡터 인덱스 구축
- 메뉴/가게 인덱스 분리 관리

### 4. UserProfileGenerator
- 사용자 개인화 벡터 생성
- 온보딩, 갤러리, 행태 데이터 통합

### 5. HybridScorer
- 하이브리드 스코어링 구현
- 텍스트 유사도, 인기도, 거리, 가격, 패널티 종합 고려

### 6. MMRReranker
- MMR 알고리즘으로 다양성 확보
- 관련성과 다양성의 균형 조절

## 성능 최적화

### 1. 인덱스 설정
- HNSW 파라미터: `efConstruction=80`, `efSearch=64~128`
- 벡터 차원: 768 (ko-sbert-sts)

### 2. 배치 처리
- 대량 데이터 처리 시 배치 단위로 임베딩 생성
- 메모리 효율성 고려

### 3. 캐싱
- 사용자 프로필 캐싱
- 인덱스 메모리 로딩

## 확장 가능성

### 1. 이미지 임베딩
- CLIP 계열 모델로 이미지 임베딩 추가
- Late fusion으로 텍스트+이미지 결합

### 2. 실시간 업데이트
- 사용자 행태 실시간 반영
- 증분 인덱스 업데이트

### 3. A/B 테스트
- 가중치 튜닝을 위한 A/B 테스트 프레임워크
- 사용자 피드백 기반 모델 개선

## 문제 해결

### 1. 메모리 부족
- 배치 크기 조정
- 인덱스 분할 로딩

### 2. 느린 응답 시간
- 인덱스 파라미터 조정
- 캐싱 전략 개선

### 3. 정확도 개선
- 가중치 튜닝
- 추가 피처 엔지니어링

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.
