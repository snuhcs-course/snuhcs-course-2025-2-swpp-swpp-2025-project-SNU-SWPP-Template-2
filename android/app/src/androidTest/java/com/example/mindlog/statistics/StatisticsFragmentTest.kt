package com.example.mindlog.statistics

import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindlog.R
import com.example.mindlog.core.dispatcher.DispatcherModule
import com.example.mindlog.features.statistics.di.StatisticsBindModule
import com.example.mindlog.features.statistics.presentation.StatisticsFragment
import com.example.mindlog.utils.launchFragmentInHiltContainer
import com.github.mikephil.charting.charts.LineChart
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.hamcrest.CoreMatchers.containsString
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(StatisticsBindModule::class, DispatcherModule::class)
@RunWith(AndroidJUnit4::class)
class StatisticsFragmentTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun initial_render_shows_happy_events_then_chip_switches_to_sad() {
        launchFragmentInHiltContainer<StatisticsFragment>()

        // 초기 선택: rates에서 HAPPY가 최대 → "최근 행복했던 이유는?" 노출
        onView(withText("최근 행복했던 이유는?"))
            .check(matches(isDisplayed()))

        // SAD 칩 클릭 → "최근 슬픔했던 이유는?"으로 변경
        onView(withId(R.id.chipSad)).perform(click())
        onView(withText("최근 슬픔했던 이유는?"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun clicking_weekly_and_monthly_updates_period_pill() {
        launchFragmentInHiltContainer<StatisticsFragment>()

        // 주간 버튼 클릭 → 기간 pill에 "~" 포함 (yyyy.MM.dd~yyyy.MM.dd 형식)
        onView(withId(R.id.btnWeekly)).perform(click())
        onView(withId(R.id.tvPeriodRange))
            .check(matches(withText(containsString("~"))))

        // 월간 버튼 클릭 → 다시 갱신 확인
        onView(withId(R.id.btnMonthly)).perform(click())
        onView(withId(R.id.tvPeriodRange))
            .check(matches(withText(containsString("~"))))
    }

    @Test
    fun pie_and_line_charts_are_rendered() {
        launchFragmentInHiltContainer<StatisticsFragment>()

        // 차트 뷰 자체가 존재하는지만 확인 (데이터는 페이크 레포가 공급)
        onView(withId(R.id.chartEmotionRates)).check(matches(isDisplayed()))
        onView(withId(R.id.chartEmotionTrend)).check(matches(isDisplayed()))
    }

    @Test
    fun word_cloud_is_rendered_when_keywords_exist() {
        // Given the fragment is launched with the test repository providing non-empty keywords
        launchFragmentInHiltContainer<StatisticsFragment>()

        onView(isRoot()).perform(swipeUp())
        // When: Scroll to the word cloud host view inside NestedScrollView
        onView(withId(R.id.wordCloudView)).perform(scrollTo())
        // Then: The word cloud container is visible
        onView(withId(R.id.wordCloudView)).check(matches(isDisplayed()))
    }

    @Test
    fun chip_selection_syncs_with_viewmodel_and_updates_on_click() {
        launchFragmentInHiltContainer<StatisticsFragment>()

        // 초기 상태: 행복 칩이 체크되어 있는지 확인 (페이크 레포가 행복 최대 비율을 제공한다고 가정)
        onView(withId(R.id.chipHappy)).check(matches(isChecked()))

        // 슬픔 칩 클릭 후 체크 상태 변경 및 제목 문구 변경 확인
        onView(withId(R.id.chipSad)).perform(scrollTo(), click())
        onView(withId(R.id.chipSad)).check(matches(isChecked()))
        onView(withText("최근 슬픔했던 이유는?")).check(matches(isDisplayed()))
    }

    @Test
    fun period_preset_buttons_set_tvPeriodRange_with_correct_format() {
        launchFragmentInHiltContainer<StatisticsFragment>()

        // 주간 버튼 클릭 후 형식 검사: yyyy.MM.dd~yyyy.MM.dd
        onView(withId(R.id.btnWeekly)).perform(scrollTo(), click())
        onView(withId(R.id.tvPeriodRange)).check(matchesTextMatches("\\d{4}\\.\\d{2}\\.\\d{2}~\\d{4}\\.\\d{2}\\.\\d{2}"))

        // 월간 버튼 클릭 후에도 동일 형식 유지
        onView(withId(R.id.btnMonthly)).perform(scrollTo(), click())
        onView(withId(R.id.tvPeriodRange)).check(matchesTextMatches("\\d{4}\\.\\d{2}\\.\\d{2}~\\d{4}\\.\\d{2}\\.\\d{2}"))
    }

    @Test
    fun charts_have_minimal_data_after_render() {
        launchFragmentInHiltContainer<StatisticsFragment>()

        // PieChart 데이터 유효성 (data != null)
        onView(withId(R.id.chartEmotionRates)).check { view, _ ->
            val chart = view as com.github.mikephil.charting.charts.PieChart
            org.junit.Assert.assertNotNull(
                "PieChart data should not be null after render",
                chart.data
            )
        }

        // LineChart 데이터 유효성 (data != null)
        onView(withId(R.id.chartEmotionTrend)).check { view, _ ->
            val chart = view as LineChart
            org.junit.Assert.assertNotNull(
                "LineChart data should not be null after render",
                chart.data
            )
        }
    }

    // ---- helper ----
    private fun matchesTextMatches(regex: String) = ViewAssertion { view, _ ->
        val tv = view as TextView
        val ok = Regex(regex).matches(tv.text?.toString().orEmpty())
        org.junit.Assert.assertTrue("Text '${tv.text}' does not match pattern: $regex", ok)
    }
}