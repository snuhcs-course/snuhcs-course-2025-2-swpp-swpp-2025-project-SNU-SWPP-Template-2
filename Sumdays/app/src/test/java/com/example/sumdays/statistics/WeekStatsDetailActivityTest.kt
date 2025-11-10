package com.example.sumdays.statistics

import android.content.Intent
import android.os.Build
import android.widget.ImageButton
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sumdays.*
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication
import com.github.mikephil.charting.data.BarDataSet
import java.time.LocalDate
import kotlinx.parcelize.Parcelize

// --- DUMMY DATA CLASS DEFINITIONS (실제 앱 코드와 완벽히 일치하도록 수정) ---

@Parcelize
data class WeekSummary(
    val startDate: String,
    val endDate: String,
    val diaryCount: Int,
    val emotionAnalysis: EmotionAnalysis,
    val highlights: List<Highlight>,
    val insights: Insights,
    val summary: SummaryDetails
) : android.os.Parcelable // 수동 구현 코드를 제거했습니다.
// --- END OF DUMMY DATA CLASS DEFINITIONS ---


@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [Build.VERSION_CODES.TIRAMISU],
    packageName = "com.example.sumdays"
)
class WeekStatsDetailActivityTest {

    private lateinit var activity: WeekStatsDetailActivity
    private lateinit var shadowApplication: ShadowApplication
    private lateinit var mockWeekSummary: WeekSummary

    @Before
    fun setUp() {
        // Mock WeekSummary 데이터 생성
        mockWeekSummary = WeekSummary(
            startDate = "2025-10-20",
            endDate = "2025-10-26",
            diaryCount = 5,
            // 1. SummaryDetails 생성자 사용
            summary = SummaryDetails(
                title = "10월 넷째 주",
                overview = "전반적으로 긍정적인 감정이 우세했으며, 주말에 활동성이 증가했습니다.",
                emergingTopics = listOf("취미", "친구")
            ),
            emotionAnalysis = EmotionAnalysis(
                distribution = mapOf("positive" to 50, "neutral" to 30, "negative" to 20),
                dominantEmoji = "😄",
                // 2. Float 타입 값 사용
                emotionScore = 0.65f,
                trend = "increasing"
            ),
            highlights = listOf(
                Highlight("2025-10-22", "새로운 프로젝트를 시작하며 느낀 설렘"),
                Highlight("2025-10-25", "오랜만에 친구들과의 즐거운 저녁 식사")
            ),
            insights = Insights(
                advice = "긍정적인 감정을 유지하기 위해 주간 목표를 설정해 보세요.",
                emotionCycle = "화요일에 감정 점수가 가장 높았으며, 일요일에 소폭 하락했습니다."
            )
        )

        // Intent에 mock data를 담아 Activity 빌드
        val intent = Intent().apply {
            putExtra("week_summary", mockWeekSummary)
        }

        // ActivityController를 사용하여 Activity 생성 및 생명 주기 메소드 호출
        activity = Robolectric.buildActivity(WeekStatsDetailActivity::class.java, intent).setup().get()
        shadowApplication = Shadows.shadowOf(activity.application)
    }

    // --- 1. 초기화 및 데이터 바인딩 테스트 ---

    @Test
    fun `testActivityInitialization_success`() {
        // 액티비티가 null이 아니며 성공적으로 생성되었는지 확인
        assertNotNull(activity)
    }

    @Test
    fun `testDataBinding_weekInfoAndDiaryCount`() {
        // 주간 제목 및 주제
        assertEquals(mockWeekSummary.summary.title, activity.findViewById<TextView>(R.id.week_title_text_view).text.toString())

        // 날짜 범위 및 주제 바인딩
        val expectedRangeText = "2025-10-20 ~ 2025-10-26 | 취미, 친구"
        assertEquals(expectedRangeText, activity.findViewById<TextView>(R.id.week_range_text_view).text.toString())

        // 일기 작성 횟수
        assertEquals("5/7", activity.findViewById<TextView>(R.id.diary_count_ratio).text.toString())
    }

    @Test
    fun `testDataBinding_overviewAndInsights`() {
        // 요약 개요
        assertEquals(mockWeekSummary.summary.overview, activity.findViewById<TextView>(R.id.overview_text_view).text.toString())

        // 통찰/조언
        assertEquals(mockWeekSummary.insights.advice, activity.findViewById<TextView>(R.id.advice_text_view).text.toString())
        assertEquals(mockWeekSummary.insights.emotionCycle, activity.findViewById<TextView>(R.id.emotion_cycle_text_view).text.toString())
    }

    @Test
    fun `testDataBinding_emotionAnalysis`() {
        // 대표 감정 및 점수
        assertEquals("대표 감정: 😄", activity.findViewById<TextView>(R.id.dominant_emoji_text_view).text.toString())

        // 감정 점수 포맷팅 확인 (%.2f)
        assertEquals("감정 점수: 0.65", activity.findViewById<TextView>(R.id.emotion_score_text_view).text.toString())

        // 감정 추세
        assertEquals("감정 추세: 상승세 📈", activity.findViewById<TextView>(R.id.emotion_trend_text_view).text.toString())
    }

    @Test
    fun `testDataBinding_highlights`() {
        // 하이라이트 목록 조인 검증
        val expectedHighlights =
            "2025-10-22: 새로운 프로젝트를 시작하며 느낀 설렘\n\n2025-10-25: 오랜만에 친구들과의 즐거운 저녁 식사"
        assertEquals(expectedHighlights, activity.findViewById<TextView>(R.id.highlights_text_view).text.toString())
    }

    @Test
    fun `testBarChartSetup_dataAndLabels`() {
        val barChart = activity.findViewById<BarChart>(R.id.emotion_analysis_bar_chart)
        assertNotNull(barChart.data)

        val dataSet = barChart.data.getDataSetByIndex(0) as BarDataSet
        assertEquals(3, dataSet.entryCount)

        // --- ⭐️ 데이터 값 검증 순서 수정: Negative -> Neutral -> Positive ---
        // 0번째: negative (20)
        assertEquals(20f, dataSet.getEntryForIndex(0).y, 0.01f)
        // 1번째: neutral (30)
        assertEquals(30f, dataSet.getEntryForIndex(1).y, 0.01f)
        // 2번째: positive (50)
        assertEquals(50f, dataSet.getEntryForIndex(2).y, 0.01f)

        // --- ⭐️ X축 라벨 순서 수정: "부정" -> "중립" -> "긍정" ---
        val xAxis = barChart.xAxis
        val labels = listOf("부정", "중립", "긍정") // 순서 변경
        val formatter = xAxis.valueFormatter as IndexAxisValueFormatter

        for (i in labels.indices) {
            assertEquals(labels[i], formatter.getFormattedValue(i.toFloat()))
        }
    }


    // --- 2. 리스너 및 내비게이션 테스트 ---

    @Test
    fun `testBackButton_finishesActivity`() {
        // GIVEN
        val backButton = activity.findViewById<ImageButton>(R.id.back_button)

        // WHEN
        backButton.performClick()

        // THEN
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `testBtnCalendarClick_startsCalendarActivityAndFinishes`() {
        // GIVEN
        val btnCalendar: ImageButton = activity.findViewById(R.id.btnCalendar)

        // WHEN
        btnCalendar.performClick()

        // THEN: Intent 검증
        val actual = shadowApplication.nextStartedActivity

        assertNotNull(actual)
        assertEquals(CalendarActivity::class.java.name, actual.component?.className)

        // 현재 액티비티가 종료되었는지 확인
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `testBtnDailyClick_startsDailyWriteActivityWithTodayAndFinishes`() {
        // GIVEN
        val btnDaily: ImageButton = activity.findViewById(R.id.btnDaily)
        val expectedDate = LocalDate.now().toString()

        // WHEN
        btnDaily.performClick()

        // THEN: Intent 검증
        val actual = shadowApplication.nextStartedActivity

        assertNotNull(actual)
        assertEquals(DailyWriteActivity::class.java.name, actual.component?.className)

        // date Extra에 오늘 날짜가 올바르게 담겨 있는지 확인
        assertEquals(expectedDate, actual.getStringExtra("date"))

        // 현재 액티비티가 종료되었는지 확인
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `testBtnInfoClick_startsSettingsActivity`() {
        // GIVEN
        val btnInfo: ImageButton = activity.findViewById(R.id.btnInfo)

        // WHEN
        btnInfo.performClick()

        // THEN: Intent 검증
        val actual = shadowApplication.nextStartedActivity

        assertNotNull(actual)
        assertEquals(SettingsActivity::class.java.name, actual.component?.className)

        // Info 버튼 클릭 시에는 finish()가 호출되지 않도록 코딩되어 있으므로, 종료되지 않았는지 확인
        assertFalse(activity.isFinishing)
    }
}