package com.example.sumdays

import android.content.Intent
import android.os.Build
import android.widget.ImageButton
import android.widget.Toast
import androidx.viewpager2.widget.ViewPager2
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import org.robolectric.shadows.ShadowApplication
import com.example.sumdays.statistics.StatisticsMonthAdapter
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [Build.VERSION_CODES.O], // LocalDate 사용을 위해 API 26 (Oreo) 이상으로 설정
    packageName = "com.example.sumdays"
)
class StatisticsActivityTest {

    private lateinit var activity: StatisticsActivity
    private lateinit var shadowApplication: ShadowApplication

    // R.id 값은 테스트 코드 내에서 임시로 설정하거나, 실제 프로젝트의 R 클래스를 참조해야 합니다.
    // 여기서는 실제 ID가 존재한다고 가정하고 findViewByID를 사용합니다.

    @Before
    fun setUp() {
        // ActivityController를 사용하여 Activity 생성 및 생명 주기 메소드 호출
        activity = Robolectric.buildActivity(StatisticsActivity::class.java).setup().get()
        shadowApplication = Shadows.shadowOf(activity.application)
    }

    // --- 1. 초기화 및 뷰 설정 테스트 ---

    @Test
    fun `testActivityInitialization_success`() {
        // 액티비티가 null이 아니며 성공적으로 생성되었는지 확인
        assertNotNull(activity)
    }

    @Test
    fun `testStatisticsRecyclerViewSetup_adapterBoundAndDataLoaded`() {
        // 뷰페이저 참조
        val monthViewPager: ViewPager2 = activity.findViewById(R.id.month_statistics_recycler_view)

        // ViewPager2가 Null이 아닌지 확인
        assertNotNull(monthViewPager)

        // 어댑터가 올바르게 연결되었는지 확인
        val adapter = monthViewPager.adapter
        assertNotNull(adapter)
        assertTrue(adapter is StatisticsMonthAdapter)

        // createDummyData()가 반환하는 데이터 수(3개)가 어댑터에 로드되었는지 확인
        assertEquals(3, adapter?.itemCount)
    }

    // --- 2. 내비게이션 바 클릭 이벤트 테스트 ---

    @Test
    fun `testBtnCalendarClick_startsCalendarActivityAndFinishes`() {
        // GIVEN
        val btnCalendar: ImageButton = activity.findViewById(R.id.btnCalendar)

        // WHEN
        btnCalendar.performClick()

        // THEN: Intent 검증
        val expectedIntent = Intent(activity, CalendarActivity::class.java)
        val actual = shadowApplication.nextStartedActivity

        // 올바른 액티비티로 전환되는지 확인
        assertEquals(expectedIntent.component, actual.component)

        // finish()가 호출되어 액티비티가 종료되었는지 확인
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `testBtnDailyClick_startsDailyWriteActivityWithDateAndFinishes`() {
        // GIVEN
        val btnDaily: ImageButton = activity.findViewById(R.id.btnDaily)
        val expectedDate = LocalDate.now().toString()

        // WHEN
        btnDaily.performClick()

        // THEN: Intent 검증
        val actual = shadowApplication.nextStartedActivity

        // 올바른 액티비티로 전환되는지 확인
        assertEquals(DailyWriteActivity::class.java.name, actual.component?.className)

        // date Extra에 오늘 날짜가 올바르게 담겨 있는지 확인
        assertEquals(expectedDate, actual.getStringExtra("date"))

        // finish()가 호출되어 액티비티가 종료되었는지 확인
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

        // 1. 액티비티가 시작되었는지 확인
        assertNotNull(actual)

        // 2. 시작된 액티비티가 SettingsActivity인지 확인
        assertEquals(SettingsActivity::class.java.name, actual.component?.className)
    }
}