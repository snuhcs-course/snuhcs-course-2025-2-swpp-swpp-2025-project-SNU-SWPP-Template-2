package com.example.sumdays.statistics

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * WeekSummary 데이터 클래스 및 모든 내부 클래스에 대한 유닛 테스트입니다.
 * 목표: 모든 데이터 클래스의 인스턴스화, 동등성, 복사, 그리고 Parcelable 라운드 트립을 테스트하여
 * Line Coverage 100%를 달성합니다.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE]) // 샘플 코드와 동일한 SDK 설정
class WeekSummaryTest {

    // --- 헬퍼 함수: 테스트용 샘플 데이터 생성 ---

    private fun createSampleEmotionAnalysis(trend: String? = "Increasing") = EmotionAnalysis(
        distribution = mapOf("positive" to 5, "neutral" to 2, "negative" to 0),
        dominantEmoji = "😃",
        emotionScore = 0.8f,
        trend = trend // null 포함 테스트를 위해 파라미터화
    )

    private fun createSampleHighlight(date: String = "2025-10-15") = Highlight(
        date = date,
        summary = "가장 즐거웠던 날은 캘린더 기능을 구현한 날입니다."
    )

    private fun createSampleInsights() = Insights(
        advice = "목표를 세분화하여 달성 가능성을 높여보세요.",
        emotionCycle = "주 초반에 긍정적이었으나, 주 후반에 중립적인 감정이 우세합니다."
    )

    private fun createSampleSummaryDetails() = SummaryDetails(
        emergingTopics = listOf("프로젝트", "야근", "운동"),
        overview = "이번 주는 프로젝트 마감으로 인해 스트레스와 성취감이 공존했습니다.",
        title = "성장과 도전의 한 주"
    )

    private fun createSampleWeekSummary(startDate: String = "2025-10-13") = WeekSummary(
        startDate = startDate,
        endDate = "2025-10-19",
        diaryCount = 5,
        emotionAnalysis = createSampleEmotionAnalysis(),
        highlights = listOf(createSampleHighlight()),
        insights = createSampleInsights(),
        summary = createSampleSummaryDetails()
    )

    /**
     * Parcelable 객체를 Parcel에 쓰고 다시 읽어와서 동일한지 확인하는 라운드 트립 테스트 헬퍼입니다.
     */
    private fun <T : Parcelable> T.roundTrip(): T {
        // 1. Parcel 객체 얻기 (가상)
        val parcel = Parcel.obtain()

        // 2. 객체를 Parcel에 쓰기
        parcel.writeParcelable(this, 0)

        // 3. 읽기 시작 위치로 포인터 이동
        parcel.setDataPosition(0)

        // 4. Parcel에서 객체 읽기
        // CREATOR를 사용하지 않고 writeParcelable/readParcelable을 사용하면 클래스 로더를 명시해야 합니다.
        val result = parcel.readParcelable<T>(this.javaClass.classLoader)

        // 5. Parcel 반납 및 결과 반환
        parcel.recycle()
        return result!!
    }

    // --- 1. WeekSummary 테스트 ---

    @Test
    fun weekSummary_instantiationAndGetters_workCorrectly() {
        val summary = createSampleWeekSummary()

        assertThat(summary.startDate, `is`("2025-10-13"))
        assertThat(summary.diaryCount, `is`(5))
        assertThat(summary.highlights.size, `is`(1))
        assertThat(summary.emotionAnalysis, `is`(notNullValue()))
        assertThat(summary.summary.title, `is`("성장과 도전의 한 주"))
    }

    @Test
    fun weekSummary_equality_hashCode_andCopy_workCorrectly() {
        val summary1 = createSampleWeekSummary()
        val summary2 = createSampleWeekSummary()
        val summaryDiff = createSampleWeekSummary(startDate = "2025-10-20")
        val summaryCopy = summary1.copy(diaryCount = 6)

        // 동등성 테스트
        assertThat(summary1, `is`(summary2))
        assertThat(summary1.hashCode(), `is`(summary2.hashCode()))
        assertThat(summary1, not(summaryDiff))

        // copy 테스트
        assertThat(summaryCopy.diaryCount, `is`(6))
        assertThat(summaryCopy.endDate, `is`(summary1.endDate)) // 다른 필드는 유지
    }

    @Test
    fun weekSummary_parcelableRoundTrip_isSuccessful() {
        val original = createSampleWeekSummary()
        val parceled = original.roundTrip()

        // Parcelable을 거쳤음에도 모든 필드가 동일한지 확인 (100% Coverage 핵심)
        assertThat(parceled, `is`(original))
        assertThat(parceled.insights.advice, `is`(original.insights.advice))
        assertThat(parceled.summary.overview, `is`(original.summary.overview))
        assertThat(parceled.emotionAnalysis.emotionScore, `is`(original.emotionAnalysis.emotionScore))
    }

    // --- 2. EmotionAnalysis 테스트 (Nullable 필드 포함) ---

    @Test
    fun emotionAnalysis_instantiation_with_nonNullTrend() {
        val analysis = createSampleEmotionAnalysis("Stable")

        assertThat(analysis.dominantEmoji, `is`("😃"))
        assertThat(analysis.emotionScore, `is`(0.8f))
        assertThat(analysis.trend, `is`("Stable"))
        assertThat(analysis.distribution.keys.size, `is`(3))
    }

    @Test
    fun emotionAnalysis_instantiation_with_nullTrend() {
        val analysis = createSampleEmotionAnalysis(trend = null)

        assertThat(analysis.trend, `is`(nullValue())) // Nullable 필드 테스트
    }

    @Test
    fun emotionAnalysis_parcelableRoundTrip_isSuccessful_with_nullTrend() {
        val original = createSampleEmotionAnalysis(trend = null)
        val parceled = original.roundTrip()

        assertThat(parceled, `is`(original))
        assertThat(parceled.trend, `is`(nullValue())) // Null 값이 잘 보존되는지 확인
    }

    // --- 3. Highlight 테스트 ---

    @Test
    fun highlight_instantiationAndParceling_isSuccessful() {
        val original = createSampleHighlight()
        val parceled = original.roundTrip()

        assertThat(original.date, `is`("2025-10-15"))
        assertThat(parceled.summary, `is`(original.summary))
        assertThat(parceled, `is`(original))
    }

    // --- 4. Insights 테스트 ---

    @Test
    fun insights_instantiationAndParceling_isSuccessful() {
        val original = createSampleInsights()
        val parceled = original.roundTrip()

        assertThat(original.advice, containsString("세분화"))
        assertThat(parceled.emotionCycle, `is`(original.emotionCycle))
        assertThat(parceled, `is`(original))
    }

    // --- 5. SummaryDetails 테스트 ---

    @Test
    fun summaryDetails_instantiationAndParceling_isSuccessful() {
        val original = createSampleSummaryDetails()
        val parceled = original.roundTrip()

        assertThat(original.emergingTopics.size, `is`(3))
        assertThat(original.overview, containsString("마감"))
        assertThat(parceled.title, `is`(original.title))
        assertThat(parceled, `is`(original))
    }
}