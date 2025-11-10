package com.example.sumdays

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageButton
import androidx.viewpager2.widget.ViewPager2
import com.example.sumdays.statistics.EmotionAnalysis
import com.example.sumdays.statistics.Highlight
import com.example.sumdays.statistics.Insights
import com.example.sumdays.statistics.MonthStatistics
import com.example.sumdays.statistics.MonthSummary
import com.example.sumdays.statistics.StatisticsMonthAdapter
import com.example.sumdays.statistics.SummaryDetails
import com.example.sumdays.statistics.WeekSummary
import com.example.sumdays.statistics.WeekSummaryForMonth
import java.time.LocalDate
import android.widget.TextView // TextView 임포트 추가
import android.view.View // View 임포트 추가 (버튼 가시성 설정용)

class StatisticsActivity : AppCompatActivity() {

    private lateinit var monthViewPager: ViewPager2
    private lateinit var statisticsAdapter: StatisticsMonthAdapter

    private lateinit var monthTitleTextView: TextView // 월 제목 TextView 추가
    private lateinit var btnPrevMonth: ImageButton // 이전 달 버튼 추가
    private lateinit var btnNextMonth: ImageButton // 다음 달 버튼 추가

    // ⭐ 더미 데이터 생성 함수
    private fun createDummyData(): List<MonthStatistics> {

        // ==========================================================
        // ⭐⭐ 2025년 10월 데이터 (가장 최근 - 포도 존재) ⭐⭐
        // ==========================================================

        // --- 1주차 주간 요약 (WeekSummary) ---
        val weekInsights10_1 = Insights("감정을 인정하고 관리하는 것이 중요하며, 작은 성공들도 자신감으로 연결시켜보세요.", "초기 긴장 -> 포기하는 순간 -> 해방감과 성취감으로 전환")
        val weekSummaryDetails10_1 = SummaryDetails(listOf("공부", "긴장", "자책", "성취감", "친구"), "이번 주는 시험 준비 동안의 긴장과 집중, 때로는 좌절과 자책이 교차하며 감정의 기복이 컸습니다. 그러나 마지막 날 시험이 끝나면서 큰 해방감과 성취감을 경험했고, 자신감도 조금 회복되었습니다.", "시험 준비와 해방감의 일주일")
        val emotionAnalysis10_1 = EmotionAnalysis(mapOf("positive" to 4, "neutral" to 1, "negative" to 2), "🎉", 0.23f, "decreasing")

        val weekSummary10_1 = WeekSummary(
            startDate = "2025-10-01", endDate = "2025-10-07", diaryCount = 5,
            emotionAnalysis = emotionAnalysis10_1,
            highlights = listOf(
                Highlight("2025-10-05", "새 프로젝트와 일정이 겹쳐 정신없는 한 주였다."),
                Highlight("2025-10-07", "실수도 많았고 해야 할 일은 많은데 집중이 되지 않아 스스로를 자책했다.")
            ),
            insights = weekInsights10_1,
            summary = weekSummaryDetails10_1
        )

        // --- ⭐ 2주차 주간 요약 추가 ⭐ ---
        val weekInsights10_2 = Insights("하루 루틴을 지키려는 노력이 중요하며, 긍정적인 대화를 늘려가세요.", "혼란 -> 안정")
        val weekSummaryDetails10_2 = SummaryDetails(listOf("루틴", "회복", "균형", "대화", "휴식"), "하루하루 계획을 세우고 루틴을 만들며 조금씩 안정감을 되찾았다. 동료와의 대화 속에서 위로를 받았고, 짧은 산책이 도움이 되었다.", "균형을 찾아가는 시도")
        val emotionAnalysis10_2 = EmotionAnalysis(mapOf("positive" to 2, "neutral" to 4, "negative" to 1), "🙂", 0.1f, "stable")

        val weekSummary10_2 = WeekSummary(
            startDate = "2025-10-08", endDate = "2025-10-14", diaryCount = 7,
            emotionAnalysis = emotionAnalysis10_2,
            highlights = listOf(Highlight("2025-10-10", "계획을 지키며 안정감을 되찾았고, 위로를 받았다.")),
            insights = weekInsights10_2,
            summary = weekSummaryDetails10_2
        )

        // --- ⭐ 3주차 주간 요약 추가 ⭐ ---
        val weekInsights10_3 = Insights("자신감을 얻었으니, 남은 과제들에 대한 의욕을 잃지 마세요.", "자신감 상승")
        val weekSummaryDetails10_3 = SummaryDetails(listOf("성취감", "자신감", "성장", "격려", "진전"), "프로젝트의 중간 발표를 성공적으로 마치며 자신감을 얻었다. 주변의 격려 덕분에 성취감이 커졌다. 남은 과제들에 대한 의욕이 생겼다.", "자신감을 되찾다")
        val emotionAnalysis10_3 = EmotionAnalysis(mapOf("positive" to 6, "neutral" to 1, "negative" to 0), "😄", 0.6f, "increasing")

        val weekSummary10_3 = WeekSummary(
            startDate = "2025-10-15", endDate = "2025-10-21", diaryCount = 6,
            emotionAnalysis = emotionAnalysis10_3,
            highlights = listOf(Highlight("2025-10-17", "중간 발표를 성공적으로 마치며 자신감을 얻었다.")),
            insights = weekInsights10_3,
            summary = weekSummaryDetails10_3
        )

        // --- ⭐ 4주차 주간 요약 추가 ⭐ ---
        val weekInsights10_4 = Insights("스스로의 한계를 인정하고 완벽하지 않아도 괜찮다는 생각을 유지하세요.", "안정 -> 평화")
        val weekSummaryDetails10_4 = SummaryDetails(listOf("안정", "여유", "균형", "성숙"), "일과 휴식의 균형이 잘 잡히면서 감정이 안정되었다. 스스로의 한계를 인정하고, 완벽하지 않아도 괜찮다는 생각을 하게 되었다.", "안정된 리듬 속의 성숙")
        val emotionAnalysis10_4 = EmotionAnalysis(mapOf("positive" to 5, "neutral" to 2, "negative" to 0), "😊", 0.4f, "stable")

        val weekSummary10_4 = WeekSummary(
            startDate = "2025-10-22", endDate = "2025-10-28", diaryCount = 4,
            emotionAnalysis = emotionAnalysis10_4,
            highlights = listOf(Highlight("2025-10-25", "일과 휴식의 균형이 잘 잡히면서 감정이 안정되었다.")),
            insights = weekInsights10_4,
            summary = weekSummaryDetails10_4
        )

        // --- 월간 요약 Helper 객체들 (MonthSummary) ---
        val monthInsights10 = Insights("지금의 어려움이 성장의 밑거름임을 기억하며, 작은 성취를 고무 삼아 지속적으로 자신을 격려하세요. 균형 잡힌 생활이 감정을 안정시키는 핵심임을 명심하고, 미래에 대한 희망을 갖고 앞으로 나아가세요.", "불안과 혼란 -> 회복과 안정 -> 자신감과 성취 -> 성숙과 평화 -> 희망과 미래를 향한 다짐")
        val monthSummaryDetails10 = SummaryDetails(listOf("불안", "회복", "성취감", "균형", "자기성장"), "월초에는 불안과 혼란으로 시작했으나, 점차 루틴을 통해 안정감을 찾으며 자신감을 회복하는 모습이 흔적입니다. 중간의 성취와 안정적인 리듬이 더해져, 마지막에는 희망적이고 긍정적인 마무리로 마무리되었습니다. 전체적으로 감정은 시작의 어려움을 딛고 성장과 평온으로 향하는 흐름을 보여줍니다.", "10월의 성장과 회복")
        val monthEmotionAnalysis10 = EmotionAnalysis(mapOf("positive" to 15, "neutral" to 10, "negative" to 5), "🌟", 0.35f, null) // 트렌드는 null

        // --- MonthSummary의 Weeks 배열 ---
        val monthWeeks10 = listOf(
            WeekSummaryForMonth(-0.5f, "😔", listOf("스트레스", "혼란"), "불안한 시작, 혼란스러운 마음", "새로운 프로젝트와 일정이 겹치며 정신없는 한 주였다. 실수도 많았고 자신감이 떨어졌다.", "2025-10-01~2025-10-07"),
            WeekSummaryForMonth(0.1f, "🙂", listOf("루틴", "회복"), "균형을 찾아가는 시도", "하루하루 계획을 세우고 루틴을 만들며 조금씩 안정감을 되찾았다.", "2025-10-08~2025-10-14"),
            WeekSummaryForMonth(0.6f, "😄", listOf("성취감", "자신감"), "자신감을 되찾다", "프로젝트의 중간 발표를 성공적으로 마치며 자신감을 얻었다. 주변의 격려 덕분에 성취감이 커졌다.", "2025-10-15~2025-10-21"),
            WeekSummaryForMonth(0.4f, "😊", listOf("안정", "여유"), "안정된 리듬 속의 성숙", "일과 휴식의 균형이 잘 잡히면서 감정이 안정되었다. 스스로의 한계를 인정하고, 완벽하지 않아도 괜찮다는 생각을 하게 되었다.", "2025-10-22~2025-10-28")
        )

        val monthSummary10 = MonthSummary(
            startDate = "2025-10-01", endDate = "2025-10-31", diaryCount = 28,
            insights = monthInsights10, summary = monthSummaryDetails10,
            emotionAnalysis = monthEmotionAnalysis10,
            weeksForMonth = monthWeeks10
        )


        val monthStatisticsOctober = MonthStatistics(
            year = 2025, month = 10, monthTitle = "2025년 10월",
            // ⭐ 주간 요약 블록 4개를 리스트에 추가
            weekSummaries = listOf(weekSummary10_1, weekSummary10_2, weekSummary10_3, weekSummary10_4),
            monthSummary = monthSummary10
        )

        // ==============================================
        // 1. 2025년 9월 데이터 (Week 2개 이상, MonthSummary 존재)
        // ==============================================

        // --- 주간 요약 Helper 객체들 ---
        val weekInsights9_1 = Insights("작은 성취를 기록하세요.", "단조로운 흐름")
        val weekSummaryDetails9_1 =
            SummaryDetails(listOf("개발", "테스팅"), "테스팅 환경 구축의 일주일", "테스팅 환경 구축 완료")
        val emotionAnalysis9_1 = EmotionAnalysis(
            mapOf("positive" to 3, "neutral" to 2, "negative" to 0),
            "😊",
            0.6f,
            "stable"
        )

        val weekInsights9_2 = Insights("협업 포인트를 명확히 하세요.", "긴장 -> 해소")
        val weekSummaryDetails9_2 = SummaryDetails(listOf("아키텍처", "DB연동"), "백엔드 통합 논의 완료", "아키텍처 확립")
        val emotionAnalysis9_2 = EmotionAnalysis(mapOf("positive" to 5, "neutral" to 1, "negative" to 1), "😄", 0.7f, "increasing")

        // --- WeekSummary 객체들 (나무 기둥 블록) ---
        val weekStatistics9_1 = WeekSummary(
            startDate = "2025-09-01",  // ⭐ 시작일 추가
            endDate = "2025-09-07",    // ⭐ 종료일 추가
            diaryCount = 5,            // ⭐ 일기 카운트 추가 (5/7)
            emotionAnalysis = emotionAnalysis9_1,
            highlights = listOf(Highlight("2025-09-03", "JUnit 4 설정 완료")),
            insights = weekInsights9_1,
            summary = weekSummaryDetails9_1
        )

        val weekStatistics9_2 = WeekSummary(
            startDate = "2025-09-08",
            endDate = "2025-09-14",
            diaryCount = 7,             // ⭐ 일기 카운트 추가 (7/7)
            emotionAnalysis = emotionAnalysis9_2,
            highlights = listOf(Highlight("2025-09-10", "MVVM 구조 확정")),
            insights = weekInsights9_2,
            summary = weekSummaryDetails9_2
        )
        // Month Summary 객체들
        val monthInsights9 = Insights("9월의 안정감을 10월에도 유지하세요.", "긴장 -> 안정 -> 자신감")
        val monthSummaryDetails9 = SummaryDetails(listOf("안정", "계획", "테스팅"), "9월은 개발 기반을 다진 달", "개발 기반 확립의 9월")
        val monthWeeksForMonth9 = listOf(
            WeekSummaryForMonth(
                0.6f,
                "😊",
                listOf("개발", "계획"),
                "주간 요약 1",
                "안정적인 시작",
                "2025-09-01~2025-09-07"
            ),
            WeekSummaryForMonth(
                0.7f,
                "😄",
                listOf("통합", "구조"),
                "주간 요약 2",
                "구조 확정 주",
                "2025-09-08~2025-09-14")
        )

        // ⭐ 월간 API의 Summary 내부에 있던 Emotion Statistics를 분리하여 생성
        val monthEmotionAnalysis9 = EmotionAnalysis(
            distribution = mapOf("positive" to 10, "neutral" to 5, "negative" to 3),
            dominantEmoji = "🌟",
            emotionScore = 0.55f,
            trend = null // trend는 주간 분석에만 사용되므로 null
        )

        val monthSummary9 = MonthSummary(
            startDate = "2025-09-01",
            endDate = "2025-09-30",
            diaryCount = 20,
            insights = monthInsights9,
            summary = monthSummaryDetails9,
            emotionAnalysis = monthEmotionAnalysis9, // ⭐ 분리된 객체 주입
            weeksForMonth = monthWeeksForMonth9
        )


        val monthStatisticsSeptember = MonthStatistics(
            year = 2025,
            month = 9,
            monthTitle = "2025년 9월",
            weekSummaries = listOf(weekStatistics9_1, weekStatistics9_2), // 주간 블록
            monthSummary = monthSummary9 // 월간 요약 (포도)
        )

        // ==============================================
        // 2. 2025년 8월 데이터 (Week 2개 미만, MonthSummary 없음)
        // ==============================================

        val weekInsights8_1 = Insights("계속 아이디어를 구체화하세요.", "혼란")
        val weekSummaryDetails8_1 = SummaryDetails(listOf("아이디어", "회의"), "아이디어 구상의 일주일", "초기 아이디어 구상")
        val emotionAnalysis8_1 = EmotionAnalysis(mapOf("positive" to 1, "neutral" to 3, "negative" to 1), "🤔", 0.1f, "stable")

        val weekStatistics8_1 = WeekSummary(
            startDate = "2025-08-01",
            endDate = "2025-08-07",
            diaryCount = 3,            // ⭐ 일기 카운트 추가 (3/7)
            emotionAnalysis = emotionAnalysis8_1,
            highlights = listOf(Highlight("2025-08-15", "프로젝트 주제 결정")),
            insights = weekInsights8_1,
            summary = weekSummaryDetails8_1
        )
        val monthStatisticsAugust = MonthStatistics(
            year = 2025,
            month = 8,
            monthTitle = "2025년 8월",
            weekSummaries = listOf(weekStatistics8_1), // 주간 블록 1개만
            monthSummary = null // 포도 없음
        )

        return listOf(monthStatisticsOctober, monthStatisticsSeptember, monthStatisticsAugust) // 9월(최근)이 앞에 위치
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 임시로 레이아웃을 설정하거나, 토스트를 띄워 화면 전환 확인
        setContentView(R.layout.activity_statistics)

        // 뷰 컴포넌트 참조
        monthTitleTextView = findViewById(R.id.month_title) // XML에서 이름을 확인하세요.
        btnPrevMonth = findViewById(R.id.btn_prev_month)
        btnNextMonth = findViewById(R.id.btn_next_month)

        // 통계 RecyclerView 초기화 로직
        setupStatisticsRecyclerView()
        // ⭐ 내비게이션 바 설정 함수 호출
        setupNavigationBar()
    }
    private fun setupStatisticsRecyclerView() {
        // 1. ViewPager2 참조
        monthViewPager = findViewById(R.id.month_statistics_recycler_view)

        // 2. 어댑터 연결 및 데이터 설정
        val dummyData = createDummyData()
        // Adapter는 이제 ViewPager2 참조 없이 데이터만 받습니다.
        statisticsAdapter = StatisticsMonthAdapter(dummyData)
        monthViewPager.adapter = statisticsAdapter

        // ⭐ ViewPager2 설정
        // 1) 스와이프(사용자 입력) 비활성화
        monthViewPager.isUserInputEnabled = false

        // 2) 초기 페이지 설정: 가장 최근 달(dummyData의 0번째 인덱스)을 표시
        val initialPosition = 0
        monthViewPager.currentItem = initialPosition

        // 3) 월 제목 초기화 및 버튼 리스너 설정
        updateMonthDisplay(initialPosition)
        setupMonthNavigationButtons(dummyData.size)

        // ⭐ 4) 페이지 변경 콜백 추가 (혹시 모를 외부 변경에 대비하고 제목 갱신)
        monthViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateMonthDisplay(position)
            }
        })
    }

    // ⭐ 월 이동 버튼 클릭 리스너 설정 함수
    private fun setupMonthNavigationButtons(itemCount: Int) {

        // 이전 달 버튼 (Prev): 인덱스 증가 = 과거 달로 이동
        btnPrevMonth.setOnClickListener {
            navigateToMonth(1, itemCount)
        }

        // 다음 달 버튼 (Next): 인덱스 감소 = 미래/최신 달로 이동
        btnNextMonth.setOnClickListener {
            navigateToMonth(-1, itemCount)
        }
    }

    // ⭐ 실제 월 이동을 처리하는 함수
    private fun navigateToMonth(direction: Int, itemCount: Int) {
        val currentPosition = monthViewPager.currentItem
        val newPosition = currentPosition + direction

        // 유효한 범위 내인지 확인
        if (newPosition >= 0 && newPosition < itemCount) {
            // 애니메이션 없이 페이지 이동 (smoothScroll=false)
            monthViewPager.setCurrentItem(newPosition, false)

            // 제목과 버튼 가시성은 OnPageSelected 콜백에서 자동으로 업데이트됩니다.
        }
    }

    // ⭐ 월 제목 텍스트 및 버튼 가시성을 업데이트하는 함수
    private fun updateMonthDisplay(position: Int) {
        val dummyData = statisticsAdapter.monthList // Adapter에서 데이터를 가져옵니다.
        val itemCount = dummyData.size

        // 1. 월 제목 업데이트
        monthTitleTextView.text = dummyData[position].monthTitle

        // 2. 버튼 가시성 업데이트 (데이터가 최신순이므로 인덱스 0이 가장 최신)

        // 이전 달 버튼: 가장 과거 달(마지막 인덱스)에 도달하면 숨김
        btnPrevMonth.visibility = if (position == itemCount - 1) View.INVISIBLE else View.VISIBLE

        // 다음 달 버튼: 가장 최신 달(0번째 인덱스)에 도달하면 숨김
        btnNextMonth.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
    }

    // ⭐ 하단 네비게이션 바의 버튼들 클릭 이벤트 처리 함수 추가
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupNavigationBar() {
        // include_nav_daily 레이아웃에서 뷰들을 찾습니다.
        val btnCalendar = findViewById<ImageButton>(R.id.btnCalendar)
        val btnDaily = findViewById<ImageButton>(R.id.btnDaily)
        val btnInfo = findViewById<ImageButton>(R.id.btnInfo)

        // Calendar로 이동
        btnCalendar.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }

        // DailyWrite (오늘의 일기)로 이동
        btnDaily.setOnClickListener {
            val today = LocalDate.now().toString()
            val intent = Intent(this, DailyWriteActivity::class.java)
            intent.putExtra("date", today)
            startActivity(intent)
            finish()
        }

        // Info 화면 (정보/설정)
        btnInfo.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
}