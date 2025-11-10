package com.example.sumdays

import android.content.Intent
import android.os.Build
import android.os.Looper
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import org.robolectric.Shadows
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import org.junit.*
import org.junit.Assert.*
import io.mockk.*
import com.example.sumdays.daily.diary.AnalysisRepository
import com.example.sumdays.daily.diary.AnalysisResponse
import com.example.sumdays.daily.diary.AnalysisBlock
import java.text.SimpleDateFormat
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE],
    packageName = "com.example.sumdays"
)
class DailyReadActivityTest {

    private val repoKeyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormatter = SimpleDateFormat("MM-dd", Locale.getDefault())

    @Before
    fun setUp() {
        mockkObject(DiaryRepository)
        mockkObject(AnalysisRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun buildActivityWithDate(date: Date? = null): DailyReadActivity {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            if (date != null) putExtra("date", repoKeyFormatter.format(date))
        }
        return Robolectric.buildActivity(DailyReadActivity::class.java, intent)
            .setup()
            .get()
    }

    private data class Views(
        val prev: ImageView,
        val next: ImageView,
        val editInplace: ImageView,
        val save: ImageView,
        val editMemos: ImageView,
        val dateText: TextView,
        val diaryEdit: EditText,
        val diaryText: TextView,
        val commentText: TextView,
        val emotionScore: TextView,
        val keywords: TextView,
        val commentIcon: TextView,
    )

    private fun findViews(activity: DailyReadActivity): Views {
        return Views(
            prev = activity.findViewById(R.id.prev_day_button),
            next = activity.findViewById(R.id.next_day_button),
            editInplace = activity.findViewById(R.id.edit_inplace_button),
            save = activity.findViewById(R.id.save_button),
            editMemos = activity.findViewById(R.id.edit_memos_button),
            dateText = activity.findViewById(R.id.date_text),
            diaryEdit = activity.findViewById(R.id.diary_content_edit_text),
            diaryText = activity.findViewById(R.id.diary_content_text_view),
            commentText = activity.findViewById(R.id.comment_text),
            emotionScore = activity.findViewById(R.id.emotion_score),
            keywords = activity.findViewById(R.id.keywords),
            commentIcon = activity.findViewById(R.id.comment_icon),
        )
    }

    @Test
    fun `onCreate - when intent has date, header shows that date`() {
        val date = GregorianCalendar(2025, Calendar.OCTOBER, 15).time
        every { DiaryRepository.getDiary(any()) } returns null
        every { AnalysisRepository.getAnalysis(any()) } returns null

        val activity = buildActivityWithDate(date)
        val v = findViews(activity)

        assertEquals("< ${displayFormatter.format(date)} >", v.dateText.text.toString())
    }

    @Test
    fun `updateUI - fields populated from repositories`() {
        val date = GregorianCalendar(2025, Calendar.OCTOBER, 20).time
        val dateKey = repoKeyFormatter.format(date)
        val diary = "오늘의 일기 내용"

        val block = mockk<AnalysisBlock>(relaxed = true).apply {
            every { emotionScore } returns 0.73
            every { keywords } returns listOf("공부", "회의")
        }
        val analysis = mockk<AnalysisResponse>(relaxed = true).apply {
            every { aiComment } returns "AI 코멘트"
            every { icon } returns "😊"
            every { this@apply.analysis } returns block
        }

        every { DiaryRepository.getDiary(dateKey) } returns diary
        every { AnalysisRepository.getAnalysis(dateKey) } returns analysis

        val activity = buildActivityWithDate(date)
        val v = findViews(activity)

        assertEquals(diary, v.diaryEdit.text.toString())
        assertEquals(diary, v.diaryText.text.toString())
        assertEquals("AI 코멘트", v.commentText.text.toString())
        assertEquals("감정 점수: 0.73", v.emotionScore.text.toString())
        assertEquals("키워드: 공부, 회의", v.keywords.text.toString())
        assertEquals("😊", v.commentIcon.text.toString())
    }

    @Test
    fun `prev and next - change date label`() {
        every { DiaryRepository.getDiary(any()) } returns null
        every { AnalysisRepository.getAnalysis(any()) } returns null

        val start = GregorianCalendar(2025, Calendar.OCTOBER, 21).time
        val activity = buildActivityWithDate(start)
        val v = findViews(activity)

        v.prev.performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val cal = Calendar.getInstance().apply { time = start; add(Calendar.DAY_OF_MONTH, -1) }
        assertEquals("< ${displayFormatter.format(cal.time)} >", v.dateText.text.toString())

        v.next.performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertEquals("< ${displayFormatter.format(start)} >", v.dateText.text.toString())
    }

    @Test
    fun `editInplace - enter edit mode then save - exit edit mode and persist`() {
        val date = GregorianCalendar(2025, Calendar.OCTOBER, 22).time
        val dateKey = repoKeyFormatter.format(date)
        every { DiaryRepository.getDiary(any()) } returns "기존 일기"
        every { AnalysisRepository.getAnalysis(any()) } returns null
        every { DiaryRepository.saveDiary(any(), any()) } just Runs

        val activity = buildActivityWithDate(date)
        val v = findViews(activity)

        // enter edit mode
        v.editInplace.performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertEquals(TextView.GONE, v.diaryText.visibility)
        assertEquals(TextView.VISIBLE, v.diaryEdit.visibility)
        assertEquals(TextView.GONE, v.editInplace.visibility)
        assertEquals(TextView.VISIBLE, v.save.visibility)

        // edit then save
        v.diaryEdit.setText("수정된 내용")
        v.save.performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // exit edit mode
        assertEquals(TextView.VISIBLE, v.diaryText.visibility)
        assertEquals(TextView.GONE, v.diaryEdit.visibility)
        assertEquals(TextView.VISIBLE, v.editInplace.visibility)
        assertEquals(TextView.GONE, v.save.visibility)
        assertEquals("수정된 내용", v.diaryText.text.toString())

        // 👉 현재 Activity 코드 구조상 saveDiaryContent()가 두 번 호출됩니다.
        verify(exactly = 2) { DiaryRepository.saveDiary(dateKey, "수정된 내용") }
    }

    @Test
    fun `editMemosButton - starts DailyWriteActivity with date and finishes current`() {
        val date = GregorianCalendar(2025, Calendar.OCTOBER, 23).time
        every { DiaryRepository.getDiary(any()) } returns null
        every { AnalysisRepository.getAnalysis(any()) } returns null

        val activity = buildActivityWithDate(date)
        val v = findViews(activity)

        v.editMemos.performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val shadow = Shadows.shadowOf(activity)
        val intent = shadow.nextStartedActivity
        assertNotNull(intent)
        assertEquals("com.example.sumdays.DailyWriteActivity", intent.component?.className)
        assertEquals(repoKeyFormatter.format(date), intent.getStringExtra("date"))
        assertTrue(activity.isFinishing)
    }
}
