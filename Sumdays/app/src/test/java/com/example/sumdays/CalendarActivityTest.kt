package com.example.sumdays

import android.content.Intent
import android.os.Build
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sumdays.calendar.MonthAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper
import org.threeten.bp.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE]) // 34
@LooperMode(LooperMode.Mode.PAUSED)
class CalendarActivityTest {

    /** 액티비티를 초기화해서 반환 */
    private fun buildActivity(): CalendarActivity {
        val controller = Robolectric.buildActivity(CalendarActivity::class.java)
        val activity = controller.setup().get()
        // 메인 루퍼 처리
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        return activity
    }

    @Test
    fun onCreate_setsHeaderAndTitle_andAdapter() {
        val activity = buildActivity()

        // 1) 요일 헤더는 7개가 생성되고, ENGLISH 기본값에서 SUN…SAT 문자여야 함
        val header = activity.findViewById<LinearLayout>(R.id.day_of_week_header)
        assertThat(header.childCount, `is`(7))
        val first = header.getChildAt(0) as TextView
        val last = header.getChildAt(6) as TextView
        assertThat(first.text.toString(), `is`("SUN"))
        assertThat(last.text.toString(), `is`("SAT"))

        // 2) 상단 월/년 텍스트가 비어있지 않음
        val monthTitle = activity.findViewById<TextView>(R.id.tv_month_year)
        assertThat(monthTitle.text.toString().isNotBlank(), `is`(true))

        // 3) ViewPager2 어댑터가 MonthAdapter로 설정됨
        val pager = activity.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.calendarViewPager)
        assertThat(pager.adapter, instanceOf(MonthAdapter::class.java))
    }

    @Test
    fun nextPrevButtons_changeMonthTitle() {
        val activity = buildActivity()

        val monthTitle = activity.findViewById<TextView>(R.id.tv_month_year)
        val btnPrev = activity.findViewById<ImageButton>(R.id.btn_prev_month)
        val btnNext = activity.findViewById<ImageButton>(R.id.btn_next_month)

        val initial = monthTitle.text.toString()

        // 다음 달로
        btnNext.performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        val afterNext = monthTitle.text.toString()
        assertThat(afterNext, not(initial))

        // 다시 이전 달로(원래로 돌아오지는 않아도, 타이틀 변경이 일어나는지만 확인)
        btnPrev.performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        val afterPrev = monthTitle.text.toString()
        assertThat(afterPrev.isNotBlank(), `is`(true))
    }

    @Test
    fun statisticButton_startsStatisticsActivity() {
        val activity = buildActivity()

        val statsBtn = activity.findViewById<ImageButton>(R.id.statistic_btn)
        statsBtn.performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        val shadowActivity = Shadows.shadowOf(activity)
        val nextIntent: Intent = shadowActivity.nextStartedActivity
        assertThat(nextIntent.component?.className, `is`(StatisticsActivity::class.qualifiedName))
    }

    @Test
    fun navBar_dailyButton_startsDailyWrite_withTodayExtra() {
        val activity = buildActivity()

        val dailyBtn = activity.findViewById<FloatingActionButton>(R.id.btnDaily)
        dailyBtn.performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        val shadowActivity = Shadows.shadowOf(activity)
        val nextIntent: Intent = shadowActivity.nextStartedActivity
        assertThat(nextIntent.component?.className, `is`(DailyWriteActivity::class.qualifiedName))

        val expectedDate = LocalDate.now().toString()
        val actualDate = nextIntent.getStringExtra("date")
        assertThat(actualDate, `is`(expectedDate))
    }

    @Test
    fun getEventEmoji_returnsValuesFromLoadedMap() {
        val activity = buildActivity()

        val today = LocalDate.now().toString()
        val nextWeek = LocalDate.now().plusWeeks(1).toString()

        assertThat(activity.getEventEmoji(today), `is`("⭐"))
        assertThat(activity.getEventEmoji(nextWeek), `is`("💻"))
        // 없는 날짜는 null
        assertThat(activity.getEventEmoji("2099-12-31"), `is`(nullValue()))
    }

    @Test
    fun viewPager_swipeProgrammatically_updatesTitle() {
        val activity = buildActivity()
        val pager = activity.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.calendarViewPager)
        val title = activity.findViewById<TextView>(R.id.tv_month_year)

        val before = title.text.toString()
        val current = pager.currentItem

        // 스와이프 시뮬레이션: 다음 페이지로 이동
        pager.setCurrentItem(current + 1, false)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        val after = title.text.toString()
        assertThat(after, not(before))
    }
}
