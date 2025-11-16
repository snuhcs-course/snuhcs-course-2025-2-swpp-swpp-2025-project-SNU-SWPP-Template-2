package com.example.sumdays

import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sumdays.data.DailyEntry
import com.example.sumdays.data.viewModel.DailyEntryViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class DailyReadActivityTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    private val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * 주어진 dateString("yyyy-MM-dd" or null) 으로 DailyReadActivity 를 생성하고,
     * viewModel 을 mock 으로 바꾼 뒤 onCreate 까지 호출한 Activity 와 mock ViewModel 을 반환.
     */
    private fun createActivityWithDate(dateString: String?): Pair<DailyReadActivity, DailyEntryViewModel> {
        val context: Context = ApplicationProvider.getApplicationContext()
        val intent = Intent(context, DailyReadActivity::class.java)
        if (dateString != null) {
            intent.putExtra("date", dateString)
        }

        val controller = Robolectric.buildActivity(DailyReadActivity::class.java, intent)
        val activity = controller.get()

        // ViewModel mock + 가짜 LiveData
        val mockViewModel = mockk<DailyEntryViewModel>(relaxed = true)
        val fakeLiveData = MutableLiveData<DailyEntry?>()
        fakeLiveData.value = null
        every { mockViewModel.getEntry(any()) } returns fakeLiveData

        // viewModel$delegate 를 lazy { mockViewModel } 로 교체
        val delegateField = DailyReadActivity::class.java.getDeclaredField("viewModel\$delegate")
        delegateField.isAccessible = true
        delegateField.set(activity, lazy { mockViewModel })

        controller.create().start().resume().visible()

        return activity to mockViewModel
    }

    /**
     * private fun updateUI(entry: DailyEntry?) 를 reflection 으로 호출
     */
    private fun invokeUpdateUI(activity: DailyReadActivity) {
        val method = DailyReadActivity::class.java.getDeclaredMethod(
            "updateUI",
            DailyEntry::class.java
        )
        method.isAccessible = true
        method.invoke(activity, null) // entry = null
    }

    // ─────────────────────────────────────────────────────────────
    // 1. 날짜 표시 & next 버튼 상태 테스트
    // ─────────────────────────────────────────────────────────────

    @Test
    fun whenDateIsToday_dateTextShows오늘_andNextDayButtonHidden() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, _) = createActivityWithDate(todayString)

        invokeUpdateUI(activity)

        val dateText = activity.findViewById<TextView>(R.id.date_text)
        val nextButton = activity.findViewById<ImageView>(R.id.next_day_button)

        assertEquals("오늘", dateText.text.toString())
        assertEquals(ImageView.INVISIBLE, nextButton.visibility)
        assertFalse(nextButton.isEnabled)
    }

    @Test
    fun whenDateIsPast_dateTextShowsFormattedDate_andNextDayButtonVisible() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -1)
        val yesterdayString = formatter.format(cal.time)

        val (activity, _) = createActivityWithDate(yesterdayString)

        invokeUpdateUI(activity)

        val dateText = activity.findViewById<TextView>(R.id.date_text)
        val nextButton = activity.findViewById<ImageView>(R.id.next_day_button)

        assertEquals(yesterdayString, dateText.text.toString())
        assertEquals(ImageView.VISIBLE, nextButton.visibility)
        assertTrue(nextButton.isEnabled)
    }

    // ─────────────────────────────────────────────────────────────
    // 2. saveDiaryContent 로직 테스트 (viewModel.updateEntry 호출)
    // ─────────────────────────────────────────────────────────────

    @Test
    fun saveDiaryContent_updatesViewModelWithGivenText() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, mockViewModel) = createActivityWithDate(todayString)

        val updatedContent = "테스트용 수정된 일기 내용"

        // private fun saveDiaryContent(updatedContent: String) 호출
        val method = DailyReadActivity::class.java.getDeclaredMethod(
            "saveDiaryContent",
            String::class.java
        )
        method.isAccessible = true
        method.invoke(activity, updatedContent)

        verify(exactly = 1) {
            mockViewModel.updateEntry(date = todayString, diary = updatedContent)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 3. changeDate / isAfterToday 분기 커버: prev / next 버튼
    // ─────────────────────────────────────────────────────────────

    @Test
    fun prevDayButtonClick_movesToPreviousDay_andCallsViewModelWithPrevDate() {
        // 기준: 오늘 날짜로 Activity 시작
        val baseCal = Calendar.getInstance()
        val todayString = formatter.format(baseCal.time)

        val (activity, mockViewModel) = createActivityWithDate(todayString)
        invokeUpdateUI(activity)

        // 기대: prev 버튼 클릭 시 어제 날짜로 getEntry 호출
        val expectedPrevCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -1)
        }
        val expectedPrevString = formatter.format(expectedPrevCal.time)

        val prevButton = activity.findViewById<ImageView>(R.id.prev_day_button)
        prevButton.performClick()
        verify {
            mockViewModel.getEntry(expectedPrevString)
        }
    }

    @Test
    fun nextDayButtonClick_fromPast_movesToNextDay_andCallsViewModelWithNextDate() {
        // 기준: 어제 날짜로 Activity 시작
        val baseCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -1)
        }
        val yesterdayString = formatter.format(baseCal.time)

        val (activity, mockViewModel) = createActivityWithDate(yesterdayString)
        invokeUpdateUI(activity)

        // 기대: next 버튼 클릭 시 오늘 날짜로 getEntry 호출
        val todayCal = Calendar.getInstance()
        val todayString = formatter.format(todayCal.time)

        val nextButton = activity.findViewById<ImageView>(R.id.next_day_button)
        nextButton.performClick()

        verify {
            mockViewModel.getEntry(todayString)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 4. toggleEditMode(true/false)에 따른 뷰 상태 변화 커버
    // ─────────────────────────────────────────────────────────────

    @Test
    fun toggleEditMode_switchesBetweenViewAndEditStates() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, _) = createActivityWithDate(todayString)
        invokeUpdateUI(activity)

        val diaryTextView = activity.findViewById<TextView>(R.id.diary_content_text_view)
        val diaryEditText = activity.findViewById<EditText>(R.id.diary_content_edit_text)
        val editButton = activity.findViewById<ImageView>(R.id.edit_inplace_button)
        val saveButton = activity.findViewById<ImageView>(R.id.save_button)

        assertEquals(TextView.VISIBLE, diaryTextView.visibility)
        assertEquals(EditText.GONE, diaryEditText.visibility)
        assertEquals(ImageView.VISIBLE, editButton.visibility)
        assertEquals(ImageView.GONE, saveButton.visibility)

        editButton.performClick()

        assertEquals(TextView.GONE, diaryTextView.visibility)
        assertEquals(EditText.VISIBLE, diaryEditText.visibility)
        assertEquals(ImageView.GONE, editButton.visibility)
        assertEquals(ImageView.VISIBLE, saveButton.visibility)

        val method = DailyReadActivity::class.java.getDeclaredMethod(
            "toggleEditMode",
            Boolean::class.javaPrimitiveType
        )
        method.isAccessible = true
        method.invoke(activity, false)

        assertEquals(TextView.VISIBLE, diaryTextView.visibility)
        assertEquals(EditText.GONE, diaryEditText.visibility)
        assertEquals(ImageView.VISIBLE, editButton.visibility)
        assertEquals(ImageView.GONE, saveButton.visibility)
    }

    // ─────────────────────────────────────────────────────────────
    // 5. 날짜 텍스트 클릭 → DatePickerDialog 로직 커버
    // ─────────────────────────────────────────────────────────────

    @Test
    fun clickingDateText_opensDatePickerDialog_withoutCrash() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, _) = createActivityWithDate(todayString)
        invokeUpdateUI(activity)

        val dateText = activity.findViewById<TextView>(R.id.date_text)
        dateText.performClick()
    }

    // ─────────────────────────────────────────────────────────────
    // 6. initializeDate 의 "dateString == null" 분기 커버
    // ─────────────────────────────────────────────────────────────

    @Test
    fun whenIntentHasNoDate_initializeDateUsesToday() {
        val (activity, _) = createActivityWithDate(null)
        val field = DailyReadActivity::class.java.getDeclaredField("currentDate")
        field.isAccessible = true
        val currentDateInActivity = field.get(activity) as Calendar

        val today = Calendar.getInstance()
        fun Calendar.yyyymmdd(): Triple<Int, Int, Int> =
            Triple(get(Calendar.YEAR), get(Calendar.MONTH), get(Calendar.DAY_OF_MONTH))

        assertEquals(today.yyyymmdd(), currentDateInActivity.yyyymmdd())
    }

    // ─────────────────────────────────────────────────────────────
    // 7. 내비게이션 바 버튼 클릭: Calendar / DailyWrite / Settings 로 이동
    // ─────────────────────────────────────────────────────────────

    @Test
    fun clickingCalendarButton_startsCalendarActivity() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, _) = createActivityWithDate(todayString)
        invokeUpdateUI(activity)

        val btnCalendar = activity.findViewById<ImageButton>(R.id.btnCalendar)
        val shadowActivity = Shadows.shadowOf(activity)

        btnCalendar.performClick()

        val startedIntent = shadowActivity.nextStartedActivity
        assertEquals(CalendarActivity::class.java.name, startedIntent.component?.className)
    }

    @Test
    fun clickingDailyButton_startsDailyWriteActivityWithTodayDateExtra() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, _) = createActivityWithDate(todayString)
        invokeUpdateUI(activity)

        val btnDaily = activity.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
            R.id.btnDaily
        )
        val shadowActivity = Shadows.shadowOf(activity)

        btnDaily.performClick()

        val startedIntent = shadowActivity.nextStartedActivity
        assertEquals(DailyWriteActivity::class.java.name, startedIntent.component?.className)

        // "date" extra 가 오늘 날짜로 세팅되어 있는지 확인
        val extraDate = startedIntent.getStringExtra("date")
        assertEquals(formatter.format(Calendar.getInstance().time), extraDate)
    }

    @Test
    fun clickingInfoButton_startsSettingsActivity() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, _) = createActivityWithDate(todayString)
        invokeUpdateUI(activity)

        val btnInfo = activity.findViewById<ImageButton>(R.id.btnInfo)
        val shadowActivity = Shadows.shadowOf(activity)

        btnInfo.performClick()

        val startedIntent = shadowActivity.nextStartedActivity
        assertEquals(SettingsActivity::class.java.name, startedIntent.component?.className)
    }
    @Test
    fun showReanalysisDialog_runsWithoutCrash() {
        val today = Calendar.getInstance()
        val todayString = formatter.format(today.time)

        val (activity, _) = createActivityWithDate(todayString)

        val updatedContent = "재분석 다이얼로그 테스트용 내용"

        val method = DailyReadActivity::class.java.getDeclaredMethod(
            "showReanalysisDialog",
            String::class.java
        )
        method.isAccessible = true

        method.invoke(activity, updatedContent)
    }
}
