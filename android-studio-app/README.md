# AI 스포츠 PT 안드로이드 앱 🏃‍♂️⚡

Galaxy Note 20에 최적화된 AI 기반 스포츠 개인 훈련 관리 앱

---

## 📂 프로젝트 구조 & Feature 매핑

```
android-studio-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/aisportspt/app/
│   │   │   ├── MainActivity.kt              # 앱 메인 (BottomNavigation)
│   │   │   ├── LoginActivity.kt             # 로그인 컨테이너
│   │   │   │
│   │   │   ├── model/                       # 데이터 모델
│   │   │   │   ├── Sport.kt                 # 스포츠 데이터
│   │   │   │   └── ShoppingItem.kt          # 쇼핑 아이템 데이터
│   │   │   │
│   │   │   └── ui/
│   │   │       ├── fragments/               # 주요 화면 (Fragment 중심)
│   │   │       │   ├── SportsFragment.kt        # 🏆 스포츠 관리
│   │   │       │   ├── AiCoachFragment.kt       # 🤖 AI 코치
│   │   │       │   ├── TrainingFragment.kt      # 🏋️ 훈련 계획
│   │   │       │   ├── ShoppingFragment.kt      # 🛍 쇼핑
│   │   │       │   ├── UserFragment.kt          # 👤 사용자 프로필
│   │   │       │   │
│   │   │       │   └── login/                   # 🔑 로그인/회원가입 플로우
│   │   │       │       ├── LoginEmailFragment.kt     # Step1: 이메일 입력
│   │   │       │       ├── LoginPasswordFragment.kt  # Step2: 비밀번호 입력
│   │   │       │       ├── SignupStep1Fragment.kt    # Step3-1: 기본 정보
│   │   │       │       ├── SignupStep2Fragment.kt    # Step3-2: 관심 운동
│   │   │       │       └── SignupStep3Fragment.kt    # Step3-3: 운동 수준 → MainActivity
│   │   │       │
│   │   │       ├── dialogs/                 # 다이얼로그 모음
│   │   │       │   ├── AddSessionDialogFragment.kt       # 세션 추가
│   │   │       │   ├── AddSportDialogFragment.kt         # 스포츠 추가
│   │   │       │   ├── CreateTrainingPlanDialogFragment.kt # 훈련 계획 생성
│   │   │       │   ├── ModifyDateDialogFragment.kt       # 날짜 수정
│   │   │       │   └── ModifyPlanDialogFragment.kt       # 계획 수정
│   │   │       │
│   │   │       ├── adapters/                # RecyclerView 어댑터
│   │   │       │   ├── AchievementAdapter.kt
│   │   │       │   ├── FeedbackAdapter.kt
│   │   │       │   ├── ShoppingAdapter.kt
│   │   │       │   ├── SportsAdapter.kt
│   │   │       │   └── TrainingPlanAdapter.kt
│   │   │       │
│   │   │       └── viewmodels/
│   │   │           └── MainViewModel.kt     # MVVM 데이터 관리
│   │   │
│   │   └── res/                             # UI 리소스
│   │       ├── layout/                      # 화면 레이아웃
│   │       │   ├── activity_login.xml
│   │       │   ├── activity_main.xml
│   │       │   ├── fragment_sports.xml
│   │       │   ├── fragment_ai_coach.xml
│   │       │   ├── fragment_training.xml
│   │       │   ├── fragment_shopping.xml
│   │       │   ├── fragment_user.xml
│   │       │   ├── fragment_login_email.xml
│   │       │   ├── fragment_login_password.xml
│   │       │   ├── fragment_signup_step1.xml
│   │       │   ├── fragment_signup_step2.xml
│   │       │   ├── fragment_signup_step3.xml
│   │       │   │
│   │       │   ├── dialog_add_session.xml
│   │       │   ├── dialog_add_sport.xml
│   │       │   ├── dialog_create_plan.xml
│   │       │   ├── dialog_create_training_plan.xml
│   │       │   ├── dialog_modify_date.xml
│   │       │   ├── dialog_modify_plan.xml
│   │       │   │
│   │       │   ├── item_achievement.xml
│   │       │   ├── item_feedback.xml
│   │       │   ├── item_shopping_card.xml
│   │       │   ├── item_sport_card.xml
│   │       │   └── item_training_plan.xml
│   │       │
│   │       ├── menu/bottom_navigation.xml   # 하단 네비게이션
│   │       ├── drawable/…                   # 아이콘 & 배경
│   │       └── values/ (colors, strings, themes)
```

---

## 📌 Feature 연결 요약

* **SportsFragment** → 스포츠 카드, 목표 진행도, 스포츠 추가 (AddSportDialog)
* **AiCoachFragment** → 카메라 프리뷰, AI 자세 비교, 피드백 리스트 (FeedbackAdapter)
* **TrainingFragment** → 훈련 계획 카드, 생성/수정 다이얼로그 (CreateTrainingPlanDialog / ModifyPlanDialog / ModifyDateDialog)
* **ShoppingFragment** → 카테고리 버튼 + 2열 상품 카드 (ShoppingAdapter, item_shopping_card.xml)
* **UserFragment** → 사용자 계정/설정
* **Login 플로우** (LoginEmail → LoginPassword → SignupStep1~3) → `admin@test.com / 1234` 더미 계정 지원

---

## 🔑 테스트 계정 (더미)

* 이메일: `admin@test.com`
* 비밀번호: `1234`

---
