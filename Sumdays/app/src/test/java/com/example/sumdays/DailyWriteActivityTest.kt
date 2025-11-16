package com.example.sumdays

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.widget.*
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.daily.memo.Memo
import com.example.sumdays.daily.memo.MemoAdapter
import com.example.sumdays.daily.memo.MemoViewModel
import com.example.sumdays.daily.memo.MemoViewModelFactory
import com.example.sumdays.audio.AudioRecorderHelper
import io.mockk.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import org.robolectric.shadows.ShadowApplication
import java.time.LocalDate
import androidx.appcompat.app.AlertDialog
import android.content.DialogInterface
import org.robolectric.shadows.ShadowDialog
import android.view.View

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [Build.VERSION_CODES.O], // LocalDate 사용 가능
    application = MyApplication::class,
    packageName = "com.example.sumdays"
)
class DailyWriteActivityTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var activity: DailyWriteActivity
    private lateinit var shadowApplication: ShadowApplication

    private val mockMemoViewModel: MemoViewModel = mockk(relaxed = true)
    private val mockMemoListLiveData: MutableLiveData<List<Memo>> = MutableLiveData()
    private val mockAudioRecorderHelper: AudioRecorderHelper = mockk(relaxUnitFun = true)

    private lateinit var memoInputEditText: EditText
    private lateinit var sendIcon: ImageView
    private lateinit var micIcon: ImageView
    private lateinit var dateTextView: TextView
    private lateinit var readDiaryButton: Button
    private lateinit var memoListView: RecyclerView
    private lateinit var controller: org.robolectric.android.controller.ActivityController<DailyWriteActivity>

    @Before
    fun setUp() {
        // MemoViewModelFactory -> mockMemoViewModel 반환하게
        mockkConstructor(MemoViewModelFactory::class)
        every { anyConstructed<MemoViewModelFactory>().create(MemoViewModel::class.java) } returns mockMemoViewModel

        // MemoViewModel LiveData
        every { mockMemoViewModel.getMemosForDate(any()) } returns mockMemoListLiveData

        // AudioRecorderHelper 생성자 mocking
        mockkConstructor(AudioRecorderHelper::class)
        every { anyConstructed<AudioRecorderHelper>().checkPermissionAndToggleRecording() } just Runs
        every { anyConstructed<AudioRecorderHelper>().release() } just Runs

        // Activity 생성
        val intent = Intent(Intent.ACTION_MAIN).apply {
            putExtra("date", LocalDate.now().toString())
        }
        controller = Robolectric.buildActivity(DailyWriteActivity::class.java, intent)
        activity = controller.create().get()   // onCreate까지
        shadowApplication = Shadows.shadowOf(activity.application)

        memoInputEditText = activity.findViewById(R.id.memo_input_edittext)
        sendIcon = activity.findViewById(R.id.send_icon)
        micIcon = activity.findViewById(R.id.mic_icon)
        dateTextView = activity.findViewById(R.id.date_text_view)
        readDiaryButton = activity.findViewById(R.id.read_diary_button)
        memoListView = activity.findViewById(R.id.memo_list_view)

        controller.start().resume()
    }

    @After
    fun tearDown() {
        if (::controller.isInitialized) {
            try {
                controller.pause().stop().destroy()
            } catch (e: Exception) {
                println("WARN: tearDown 중 액티비티 상태 오류: ${e.message}")
            }
        }
        unmockkAll()
    }

    // --- 1. 액티비티 초기화 ---

    @Test
    fun `testActivitySetup_withIntentDate`() {
        val testDate = "2025-10-25"
        val intent = Intent(Intent.ACTION_MAIN).apply {
            putExtra("date", testDate)
        }
        val newActivity = Robolectric.buildActivity(DailyWriteActivity::class.java, intent).setup().get()
        val dateText: TextView = newActivity.findViewById(R.id.date_text_view)

        assertEquals(testDate, dateText.text.toString())
        verify(exactly = 1) { mockMemoViewModel.getMemosForDate(testDate) }
    }

    @Test
    fun `testActivitySetup_noIntentDate_usesToday`() {
        val today = LocalDate.now().toString()
        assertEquals(today, dateTextView.text.toString())
        verify(exactly = 1) { mockMemoViewModel.getMemosForDate(today) }
    }

    @Test
    fun `testActivitySetup_audioHelperRelease`() {
        controller.destroy()
        verify(exactly = 1) { anyConstructed<AudioRecorderHelper>().release() }
    }

    // --- 2. LiveData & RecyclerView ---

    @Test
    fun `testMemoListUpdate_submitsToAdapter`() {
        val memo1 = Memo(1, "테스트 메모 1", "09:00", "2025-10-25", 0)
        val memo2 = Memo(2, "테스트 메모 2", "10:00", "2025-10-25", 1)
        val testList = listOf(memo1, memo2)

        mockMemoListLiveData.postValue(testList)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val adapter = memoListView.adapter as MemoAdapter
        assertEquals(2, adapter.itemCount)
        assertEquals(memo1.content, adapter.currentList[0].content)
        assertEquals(memo2.content, adapter.currentList[1].content)
    }

    // --- 3. UI 상호작용 ---

    @Test
    fun `testSendIcon_validMemo_insertsAndClears`() {
        val memoContent = "새로운 일기 메모"
        val today = LocalDate.now().toString()
        val adapter = memoListView.adapter as MemoAdapter
        adapter.submitList(emptyList())

        memoInputEditText.setText(memoContent)

        sendIcon.performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        verify(exactly = 1) {
            mockMemoViewModel.insert(match {
                it.content == memoContent && it.date == today && it.order == 0
            })
        }
        assertEquals("", memoInputEditText.text.toString())
    }

    @Test
    fun `testSendIcon_emptyMemo_showsToast`() {
        memoInputEditText.setText("")

        sendIcon.performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        verify(exactly = 0) { mockMemoViewModel.insert(any()) }
        val latestToast = ShadowToast.getTextOfLatestToast()
        assertEquals("메모 내용을 입력해 주세요.", latestToast)
    }

    @Test
    fun `testMicIconClick_callsHelper`() {
        micIcon.performClick()
        verify(exactly = 1) { anyConstructed<AudioRecorderHelper>().checkPermissionAndToggleRecording() }
    }

    @Test
    fun `testReadDiaryButtonClick_startsDailyReadActivity`() {
        // 현재 구현에서는 "일기가 있을 때만" 버튼을 활성화하므로
        // 여기서는 단순히 네비게이션만 검증하기 위해 버튼을 강제로 활성화
        val today = LocalDate.now().toString()
        readDiaryButton.isEnabled = true

        readDiaryButton.performClick()

        val actual = shadowApplication.nextStartedActivity
        assertNotNull(actual)
        assertEquals(DailyReadActivity::class.java.name, actual.component?.className)
        assertEquals(today, actual.getStringExtra("date"))
    }

    // --- 4. Nav bar 버튼 (Sum, Info, Calendar) ---

    @Test
    fun `testBtnSumClick_startsDailySumActivityWithMemos`() {
        val memo1 = Memo(1, "요약할 메모", "10:00", LocalDate.now().toString(), 0)
        val testList = listOf(memo1)
        mockMemoListLiveData.postValue(testList)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val btnSum: ImageButton = activity.findViewById(R.id.btnSum)
        btnSum.performClick()

        val actual = shadowApplication.nextStartedActivity
        assertNotNull(actual)
        assertEquals(DailySumActivity::class.java.name, actual.component?.className)
        assertEquals(LocalDate.now().toString(), actual.getStringExtra("date"))

        val receivedMemos = actual.getParcelableArrayListExtra<Memo>("memo_list")
        assertNotNull(receivedMemos)
        assertEquals(1, receivedMemos?.size)
        assertEquals(memo1.content, receivedMemos?.get(0)?.content)
    }

    @Test
    fun `testBtnInfoClick_startsSettingsActivity`() {
        val btnInfo: ImageButton = activity.findViewById(R.id.btnInfo)

        btnInfo.performClick()

        val actual = shadowApplication.nextStartedActivity
        assertNotNull(actual)
        assertEquals(SettingsActivity::class.java.name, actual.component?.className)
    }

    @Test
    fun `testBtnCalendarClick_startsCalendarActivity`() {
        val btnCalendar: ImageButton = activity.findViewById(R.id.btnCalendar)

        btnCalendar.performClick()

        val actual = shadowApplication.nextStartedActivity
        assertNotNull(actual)
        assertEquals(CalendarActivity::class.java.name, actual.component?.className)
    }

    // ★★★ 여기서부터가 메모 수정 다이얼로그 관련 테스트들 ★★★

    @Test
    fun `testShowEditMemoDialog_positiveButton_updatesMemo`() {
        val originalMemo = Memo(id = 1, content = "수정 전", timestamp = "09:00", date = LocalDate.now().toString(), order = 0)
        val newContent = "수정 후 내용"

        controller.start().resume()

        activity.showEditMemoDialog(originalMemo)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val dialog = ShadowDialog.getLatestDialog()
        assertNotNull("다이얼로그가 열리지 않았습니다.", dialog)

        val alertDialog = dialog as androidx.appcompat.app.AlertDialog
        val editText = alertDialog.findViewById<EditText>(R.id.edit_text_memo_content)
        assertNotNull("EditText를 찾을 수 없습니다.", editText)
        editText!!.setText(newContent)

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        verify(exactly = 1) {
            mockMemoViewModel.update(match {
                it.content == newContent && it.id == originalMemo.id
            })
        }
    }

    @Test
    fun `testShowEditMemoDialog_negativeButton_deletesMemo`() {
        // 현재 구현에서 NEGATIVE = "삭제"
        val originalMemo = Memo(id = 1, content = "수정 전", timestamp = "09:00", date = LocalDate.now().toString(), order = 0)
        controller.start().resume()

        activity.showEditMemoDialog(originalMemo)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val dialog = ShadowDialog.getLatestDialog()
        assertNotNull("다이얼로그가 열리지 않았습니다.", dialog)
        val alertDialog = dialog as AlertDialog

        // "삭제" 버튼 클릭 (NEGATIVE)
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // update는 호출되지 않고 delete가 호출되어야 함
        verify(exactly = 0) { mockMemoViewModel.update(any()) }
        verify(exactly = 1) { mockMemoViewModel.delete(originalMemo) }
        assertFalse(alertDialog.isShowing)
    }

    @Test
    fun `testShowEditMemoDialog_neutralButton_cancels`() {
        val originalMemo = Memo(id = 1, content = "수정 전", timestamp = "09:00", date = LocalDate.now().toString(), order = 0)
        controller.start().resume()

        activity.showEditMemoDialog(originalMemo)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val dialog = ShadowDialog.getLatestDialog()
        assertNotNull("다이얼로그가 열리지 않았습니다.", dialog)
        val alertDialog = dialog as AlertDialog

        // "취소" 버튼 클릭 (NEUTRAL)
        alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        verify(exactly = 0) { mockMemoViewModel.update(any()) }
        verify(exactly = 0) { mockMemoViewModel.delete(any()) }
        assertFalse(alertDialog.isShowing)
    }

    @Test
    fun `onNewIntent_updatesDate_and_reobserves`() {
        val today = LocalDate.now().toString()
        val newIntent = Intent(Intent.ACTION_MAIN).apply { putExtra("date", today) }
        controller.newIntent(newIntent)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val dateText: TextView = activity.findViewById(R.id.date_text_view)
        assertEquals(today, dateText.text.toString())
        verify(atLeast = 1) { mockMemoViewModel.getMemosForDate(today) }
    }

    @Test
    fun `waveAnimation_start_and_stop_resetScale_and_clearAnimator`() {
        val start = DailyWriteActivity::class.java.getDeclaredMethod("startWaveAnimation").apply { isAccessible = true }
        val stop  = DailyWriteActivity::class.java.getDeclaredMethod("stopWaveAnimation").apply { isAccessible = true }

        val bar1: View = activity.findViewById(R.id.wave_bar_1)
        val bar2: View = activity.findViewById(R.id.wave_bar_2)
        val bar3: View = activity.findViewById(R.id.wave_bar_3)

        start.invoke(activity)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val field = DailyWriteActivity::class.java.getDeclaredField("waveAnimatorSet").apply { isAccessible = true }
        assertNotNull(field.get(activity))

        stop.invoke(activity)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1.0f, bar1.scaleY)
        assertEquals(1.0f, bar2.scaleY)
        assertEquals(1.0f, bar3.scaleY)
        assertNull(field.get(activity))
    }

    @Test
    fun `removeDummyMemoAndAddFinal_replacesDummy_and_insertsFinal`() {
        val pendingId = 777
        val pendingField = DailyWriteActivity::class.java.getDeclaredField("pendingAudioMemoId").apply { isAccessible = true }
        pendingField.set(activity, pendingId)

        val adapter = activity.findViewById<RecyclerView>(R.id.memo_list_view).adapter as MemoAdapter
        val dummy = Memo(id = pendingId, content = "음성 인식 중...", timestamp = "11:11", date = LocalDate.now().toString(), order = 0, type = "audio")
        adapter.submitList(listOf(dummy))
        assertEquals(1, adapter.itemCount)

        val method = DailyWriteActivity::class.java.getDeclaredMethod("removeDummyMemoAndAddFinal", String::class.java, String::class.java).apply { isAccessible = true }
        val finalText = "최종 변환 텍스트"
        val finalType = "audio"

        method.invoke(activity, finalText, finalType)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertTrue(adapter.currentList.none { it.id == pendingId })
        verify(exactly = 1) { mockMemoViewModel.insert(match { it.content == finalText && it.type == finalType }) }
        assertNull(pendingField.get(activity))
    }

    @Test
    fun `handleIntent_keepsAdapterList_when_pending_not_in_livedata`() {
        val pendingId = 999
        val pendingField = DailyWriteActivity::class.java.getDeclaredField("pendingAudioMemoId").apply { isAccessible = true }
        pendingField.set(activity, pendingId)

        val adapter = activity.findViewById<RecyclerView>(R.id.memo_list_view).adapter as MemoAdapter
        val dummy = Memo(id = pendingId, content = "임시", timestamp = "12:34", date = LocalDate.now().toString(), order = 0)
        adapter.submitList(listOf(dummy))
        assertEquals(1, adapter.itemCount)

        val liveDataList = listOf(
            Memo(id = 1, content = "DB의 메모", timestamp = "13:00", date = LocalDate.now().toString(), order = 0)
        )
        mockMemoListLiveData.postValue(liveDataList)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, adapter.itemCount)
        assertEquals(pendingId, adapter.currentList.first().id)
    }
    @Test
    fun `stopIconClick_callsRecorderHelper`() {
        val stopIcon: ImageView = activity.findViewById(R.id.stop_icon)

        // WHEN
        stopIcon.performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // THEN
        verify(exactly = 1) {
            anyConstructed<AudioRecorderHelper>().checkPermissionAndToggleRecording()
        }
    }
    @Test
    fun `memoInput_focusGain_hidesStopIcon`() {
        val stopIcon: ImageView = activity.findViewById(R.id.stop_icon)

        // 포커스 받기 전에는 일단 보이게 만들어 둠
        stopIcon.visibility = View.VISIBLE

        val listener = memoInputEditText.onFocusChangeListener
        assertNotNull("포커스 리스너가 설정되어 있어야 합니다", listener)

        // WHEN: 포커스를 받으면
        listener!!.onFocusChange(memoInputEditText, true)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // THEN: stopIcon은 GONE이어야 함
        assertEquals(View.GONE, stopIcon.visibility)
    }
    @Test
    fun `memoInput_focusLoss_notRecording_showsMic_andHidesStopIcon`() {
        val stopIcon: ImageView = activity.findViewById(R.id.stop_icon)
        val micIconLocal: ImageView = activity.findViewById(R.id.mic_icon)

        // isRecording = false 로 설정
        val recField = DailyWriteActivity::class.java
            .getDeclaredField("isRecording")
            .apply { isAccessible = true }
        recField.setBoolean(activity, false)

        val listener = memoInputEditText.onFocusChangeListener
        assertNotNull(listener)

        // WHEN: 포커스를 잃음
        listener!!.onFocusChange(memoInputEditText, false)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // THEN: 녹음 안 하는 상태이므로 micIcon VISIBLE, stopIcon GONE
        assertEquals(View.VISIBLE, micIconLocal.visibility)
        assertEquals(View.GONE, stopIcon.visibility)
    }
    @Test
    fun `memoInput_focusLoss_recording_showsStop_andHidesMic`() {
        val stopIcon: ImageView = activity.findViewById(R.id.stop_icon)
        val micIconLocal: ImageView = activity.findViewById(R.id.mic_icon)

        // isRecording = true 로 설정
        val recField = DailyWriteActivity::class.java
            .getDeclaredField("isRecording")
            .apply { isAccessible = true }
        recField.setBoolean(activity, true)

        val listener = memoInputEditText.onFocusChangeListener
        assertNotNull(listener)

        // WHEN: 포커스를 잃음
        listener!!.onFocusChange(memoInputEditText, false)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // THEN: 녹음 중이므로 stopIcon VISIBLE, micIcon GONE
        assertEquals(View.VISIBLE, stopIcon.visibility)
        assertEquals(View.GONE, micIconLocal.visibility)
    }
    @Test
    fun `removeDummyMemo_clearsPendingDummyId_withoutCrash`() {
        val pendingId = 1234

        // pendingAudioMemoId 세팅
        val pendingField = DailyWriteActivity::class.java
            .getDeclaredField("pendingAudioMemoId")
            .apply { isAccessible = true }
        pendingField.set(activity, pendingId)

        // 어댑터에 임시 메모 주입 (실제 제거 여부는 DiffUtil 타이밍에 따라 다를 수 있으므로
        // 여기서는 '예외 없이 동작 + pendingAudioMemoId 초기화'만 확인)
        val adapter = activity.findViewById<RecyclerView>(R.id.memo_list_view).adapter as MemoAdapter
        val dummy = Memo(
            id = pendingId,
            content = "임시 음성 메모",
            timestamp = "11:11",
            date = LocalDate.now().toString(),
            order = 0,
            type = "audio"
        )
        adapter.submitList(listOf(dummy))
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // private fun removeDummyMemo(errorContent: String, memoType: String) 호출
        val method = DailyWriteActivity::class.java
            .getDeclaredMethod("removeDummyMemo", String::class.java, String::class.java)
            .apply { isAccessible = true }

        method.invoke(activity, "[오류: 테스트]", "audio")
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // THEN: 최소한 pendingAudioMemoId 는 null 로 초기화 되어야 한다.
        assertNull(pendingField.get(activity))
    }
    @Test
    fun `micIconClick_whenApiProcessing_showsToast_andDoesNotCallRecorder`() {
        // isApiProcessingAudio = true 로 세팅 (private 필드 리플렉션)
        val apiField = DailyWriteActivity::class.java
            .getDeclaredField("isApiProcessingAudio")
            .apply { isAccessible = true }
        apiField.setBoolean(activity, true)

        // WHEN: 마이크 아이콘 클릭
        micIcon.performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // THEN: 토스트 메시지 확인
        assertEquals("이전 음성을 처리 중입니다...", ShadowToast.getTextOfLatestToast())

        // 그리고 녹음 토글은 호출되면 안 됨
        verify(exactly = 0) {
            anyConstructed<AudioRecorderHelper>().checkPermissionAndToggleRecording()
        }
    }


}
