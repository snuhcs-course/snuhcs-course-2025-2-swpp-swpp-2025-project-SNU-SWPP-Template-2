package com.example.sumdays

import android.content.Intent
import android.os.Build
import android.os.Looper
import android.widget.ImageButton
import android.widget.ImageView
import org.hamcrest.CoreMatchers.`is`
import androidx.test.core.app.ApplicationProvider
import com.example.sumdays.daily.diary.AnalysisRepository
import com.example.sumdays.daily.memo.Memo
import com.example.sumdays.daily.memo.MemoMergeAdapter
import io.mockk.*
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class DailySumActivityTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDate = "2025-11-01"


    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockkConstructor(MemoMergeAdapter::class)
        coEvery { anyConstructed<MemoMergeAdapter>().skipMerge() } returns "merged-result-from-test"
        every { anyConstructed<MemoMergeAdapter>().undoLastMerge() } returns true

        mockkObject(DiaryRepository)
        every { DiaryRepository.saveDiary(any(), any()) } just Runs

        mockkObject(AnalysisRepository)
        // ← 함수 시그니처가 AnalysisResponse? 라면 null 반환으로 맞추세요.
        coEvery { AnalysisRepository.requestAnalysis(any()) } returns null
    }


    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    /** Helper: Activity 실행 */
    private fun launchWith(
        date: String = testDate,
        memos: ArrayList<Memo> = arrayListOf()
    ): DailySumActivity {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(ctx, DailySumActivity::class.java).apply {
            putExtra("date", date)
            putParcelableArrayListExtra("memo_list", memos)
        }
        return Robolectric.buildActivity(DailySumActivity::class.java, intent).setup().get()
    }

    @Test
    fun clickingSkip_savesDiary_and_navigatesToRead() {
        val activity = launchWith()

        activity.findViewById<ImageButton>(R.id.skip_icon).performClick()

        // ★ 코루틴 작업 마무리
        testDispatcher.scheduler.advanceUntilIdle()
        // ★ 메인 루퍼 큐도 비우기
        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks()

        // 1) 어댑터의 skipMerge가 실제로 불렸는지 먼저 확인
        coVerify(exactly = 1) { anyConstructed<MemoMergeAdapter>().skipMerge() }

        // 2) 저장 호출 검증
        verify(exactly = 1) { DiaryRepository.saveDiary("2025-11-01", "merged-result-from-test") }

        // 3) 화면 전환 검증
        val started = Shadows.shadowOf(activity).nextStartedActivity
        assert(started != null)
        assert(started.component?.className == DailyReadActivity::class.java.name)
    }

    @Test
    fun clickingUndo_invokesAdapterUndo() {
        val activity = launchWith()

        val undo = activity.findViewById<ImageView>(R.id.undo_icon)
        undo.performClick()

        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks()

        verify(exactly = 1) { anyConstructed<MemoMergeAdapter>().undoLastMerge() }
    }

    @Test
    fun clickingCalendar_opensCalendarActivity() {
        val activity = launchWith()
        activity.findViewById<ImageButton>(R.id.btnCalendar).performClick()

        val next = Shadows.shadowOf(activity).nextStartedActivity
        assertThat(next.component?.className, `is`(CalendarActivity::class.java.name))
    }

    @Test
    fun clickingDaily_opensDailyWriteActivity_withTodayDate() {
        val activity = launchWith()
        activity.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btnDaily).performClick()

        val next = Shadows.shadowOf(activity).nextStartedActivity
        assertThat(next.component?.className, `is`(DailyWriteActivity::class.java.name))
        assertTrue(next.hasExtra("date"))
    }
}
