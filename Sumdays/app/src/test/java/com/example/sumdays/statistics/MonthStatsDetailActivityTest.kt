package com.example.sumdays.statistics

import android.content.Intent
import android.os.Build
import android.widget.ImageButton
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sumdays.* // CalendarActivity, DailyWriteActivity, SettingsActivity 등을 포함한다고 가정
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication
import java.time.LocalDate
import kotlinx.parcelize.Parcelize

// --- DUMMY DATA CLASS DEFINITIONS (실제 앱 코드와 완벽히 일치하도록 정의) ---
// WeekSummaryForMonth, MonthSummary, Insights, SummaryDetails, EmotionAnalysis 정의는
// MonthSummary.kt 및 WeekStatsDetailActivity.kt에서 사용된 것과 동일해야 합니다.


@Parcelize
data class MonthSummary(
    val startDate: String,
    val endDate: String,
    val diaryCount: Int,
    val insights: Insights,
    val summary: SummaryDetails,
    val weeksForMonth: List<WeekSummaryForMonth>,
    val emotionAnalysis: EmotionAnalysis
) : android.os.Parcelable
// --- END OF DUMMY DATA CLASS DEFINITIONS ---


@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [Build.VERSION_CODES.TIRAMISU],
    packageName = "com.example.sumdays"
)
class MonthStatsDetailActivityTest {

    private lateinit var activity: MonthStatsDetailActivity
    private lateinit var shadowApplication: ShadowApplication
    private lateinit var mockMonthSummary: MonthSummary

    @Before
    fun setUp() {
        // Mock WeekSummaryForMonth 데이터 (4주)
        val mockWeeks = listOf(
            WeekSummaryForMonth(0.7f, "😊", listOf("운동", "공부"), "긍정적인 1주차", "기초 다지기에 집중했습니다.", "09/01~09/07"),
            WeekSummaryForMonth(0.4f, "😐", listOf("업무", "회의"), "평이했던 2주차", "업무량이 증가하여 바빴습니다.", "09/08~09/14"),
            WeekSummaryForMonth(0.8f, "😄", listOf("여행", "취미"), "최고의 3주차", "휴가를 다녀와서 행복했습니다.", "09/15~09/21"),
            WeekSummaryForMonth(0.5f, "🙂", listOf("정리", "계획"), "마무리 4주차", "차분하게 월말을 정리했습니다.", "09/22~09/30")
        )

        // Mock MonthSummary 데이터 생성
        mockMonthSummary = MonthSummary(
            startDate = "2025-09-01",
            endDate = "2025-09-30",
            diaryCount = 28, // 30일 중 28일 작성
            summary = SummaryDetails(
                title = "9월은 발전의 달",
                overview = "전반적으로 감정 점수가 우수했으며, 특히 3주차에 정점을 찍었습니다.",
                emergingTopics = listOf("업무", "여행", "계획")
            ),
            emotionAnalysis = EmotionAnalysis(
                distribution = mapOf("positive" to 60, "neutral" to 25, "negative" to 15),
                dominantEmoji = "😃",
                emotionScore = 0.7f,
                trend = "stable"
            ),
            weeksForMonth = mockWeeks,
            insights = Insights(
                advice = "3주차의 긍정적인 요소를 다음 달 계획에 반영하세요.",
                emotionCycle = "주기적으로 2주차에 감정 점수가 하락하는 패턴을 보였습니다."
            )
        )

        // Intent에 mock data를 담아 Activity 빌드
        val intent = Intent().apply {
            putExtra("month_summary", mockMonthSummary)
        }

        // ActivityController를 사용하여 Activity 생성 및 생명 주기 메소드 호출
        activity = Robolectric.buildActivity(MonthStatsDetailActivity::class.java, intent).setup().get()
        shadowApplication = Shadows.shadowOf(activity.application)
    }

    // --- 1. 초기화 및 데이터 바인딩 테스트 ---

    @Test
    fun `testActivityInitialization_success`() {
        assertNotNull(activity)
    }

    @Test
    fun `testDataBinding_monthInfoAndDiaryCount`() {
        // 월간 제목
        assertEquals("9월은 발전의 달", activity.findViewById<TextView>(R.id.month_title_text_view).text.toString())

        // 날짜 범위 및 주제
        val expectedRangeText = "2025-09-01 ~ 2025-09-30 | 업무, 여행, 계획"
        assertEquals(expectedRangeText, activity.findViewById<TextView>(R.id.month_range_text_view).text.toString())

        // 일기 작성 횟수 (30일 가정)
        assertEquals("28/30", activity.findViewById<TextView>(R.id.diary_count_ratio).text.toString())
    }

    @Test
    fun `testDataBinding_overviewAndInsights`() {
        // 월간 개요
        assertEquals(mockMonthSummary.summary.overview, activity.findViewById<TextView>(R.id.overview_text_view).text.toString())

        // 통찰/조언
        assertEquals(mockMonthSummary.insights.advice, activity.findViewById<TextView>(R.id.advice_text_view).text.toString())
        assertEquals(mockMonthSummary.insights.emotionCycle, activity.findViewById<TextView>(R.id.emotion_cycle_text_view).text.toString())
    }

    @Test
    fun `testDataBinding_weekSummariesList`() {
        val expectedList = """(09/01~09/07) 긍정적인 1주차 - 😊
키워드: 운동, 공부
개요: 기초 다지기에 집중했습니다.

(09/08~09/14) 평이했던 2주차 - 😐
키워드: 업무, 회의
개요: 업무량이 증가하여 바빴습니다.

(09/15~09/21) 최고의 3주차 - 😄
키워드: 여행, 취미
개요: 휴가를 다녀와서 행복했습니다.

(09/22~09/30) 마무리 4주차 - 🙂
키워드: 정리, 계획
개요: 차분하게 월말을 정리했습니다."""

        assertEquals(expectedList, activity.findViewById<TextView>(R.id.week_summaries_list).text.toString())
    }


    // --- 2. 차트 시각화 테스트 ---

    @Test
    fun `testEmotionBarChartSetup_dataAndLabels`() {
        val barChart = activity.findViewById<BarChart>(R.id.emotion_analysis_bar_chart)
        assertNotNull(barChart.data)

        val dataSet = barChart.data.getDataSetByIndex(0) as BarDataSet
        assertEquals(3, dataSet.entryCount)

        // Negative(15), Neutral(25), Positive(60) 순으로 정렬됨 (keys.sorted() 로직)
        assertEquals(15f, dataSet.getEntryForIndex(0).y, 0.01f) // 부정
        assertEquals(25f, dataSet.getEntryForIndex(1).y, 0.01f) // 중립
        assertEquals(60f, dataSet.getEntryForIndex(2).y, 0.01f) // 긍정

        // X축 라벨 검증
        val labels = listOf("부정", "중립", "긍정")
        val formatter = barChart.xAxis.valueFormatter as IndexAxisValueFormatter
        for (i in labels.indices) {
            assertEquals(labels[i], formatter.getFormattedValue(i.toFloat()))
        }
    }

    @Test
    fun `testWeeklyEmotionLineChartSetup_dataAndLabels`() {
        val lineChart = activity.findViewById<LineChart>(R.id.weekly_emotion_line_chart)
        assertNotNull(lineChart.data)

        val dataSet = lineChart.data.getDataSetByIndex(0) as LineDataSet
        assertEquals(4, dataSet.entryCount) // 4주차 데이터

        // 데이터 값 검증 (Y값: emotionScore)
        assertEquals(0.7f, dataSet.getEntryForIndex(0).y, 0.01f) // 1주차
        assertEquals(0.4f, dataSet.getEntryForIndex(1).y, 0.01f) // 2주차
        assertEquals(0.8f, dataSet.getEntryForIndex(2).y, 0.01f) // 3주차
        assertEquals(0.5f, dataSet.getEntryForIndex(3).y, 0.01f) // 4주차

        // X축 라벨 검증
        val labels = listOf("09/01~09/07", "09/08~09/14", "09/15~09/21", "09/22~09/30")
        val formatter = lineChart.xAxis.valueFormatter as IndexAxisValueFormatter
        for (i in labels.indices) {
            assertEquals(labels[i], formatter.getFormattedValue(i.toFloat()))
        }
    }

    // --- 3. 리스너 및 내비게이션 테스트 ---

    @Test
    fun `testBackButton_finishesActivity`() {
        val backButton = activity.findViewById<ImageButton>(R.id.back_button)
        backButton.performClick()
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `testBtnCalendarClick_startsCalendarActivityAndFinishes`() {
        val btnCalendar: ImageButton = activity.findViewById(R.id.btnCalendar)
        btnCalendar.performClick()

        val actual = shadowApplication.nextStartedActivity
        assertNotNull(actual)
        assertEquals(CalendarActivity::class.java.name, actual.component?.className)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `testBtnDailyClick_startsDailyWriteActivityWithTodayAndFinishes`() {
        val btnDaily: ImageButton = activity.findViewById(R.id.btnDaily)
        val expectedDate = LocalDate.now().toString()
        btnDaily.performClick()

        val actual = shadowApplication.nextStartedActivity
        assertNotNull(actual)
        assertEquals(DailyWriteActivity::class.java.name, actual.component?.className)
        assertEquals(expectedDate, actual.getStringExtra("date"))
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `testBtnInfoClick_startsSettingsActivity`() {
        val btnInfo: ImageButton = activity.findViewById(R.id.btnInfo)
        btnInfo.performClick()

        val actual = shadowApplication.nextStartedActivity
        assertNotNull(actual)
        assertEquals(SettingsActivity::class.java.name, actual.component?.className)
        assertFalse(activity.isFinishing)
    }
}