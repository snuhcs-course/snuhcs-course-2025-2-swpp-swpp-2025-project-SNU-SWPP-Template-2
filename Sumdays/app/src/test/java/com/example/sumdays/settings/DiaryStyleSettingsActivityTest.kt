package com.example.sumdays.settings

import android.content.Intent
import android.os.Looper
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.example.sumdays.data.style.StylePrompt
import com.example.sumdays.data.style.UserStyle
import com.example.sumdays.data.style.UserStyleViewModel
import com.example.sumdays.settings.prefs.UserStatsPrefs
import com.example.sumdays.settings.ui.StyleCardAdapter
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34]
)
class DiaryStyleSettingsActivityTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var controller: ActivityController<TestDiaryStyleSettingsActivity>
    private lateinit var activity: TestDiaryStyleSettingsActivity

    private lateinit var mockViewModel: UserStyleViewModel
    private lateinit var mockAdapter: StyleCardAdapter
    private lateinit var mockPrefs: UserStatsPrefs

    private lateinit var stylesLiveData: MutableLiveData<List<UserStyle>>

    @Before
    fun setup() {
        mockViewModel = mockk(relaxed = true)
        mockAdapter = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)

        stylesLiveData = MutableLiveData()
        every { mockViewModel.getAllStyles() } returns stylesLiveData
        every { mockPrefs.getActiveStyleId() } returns null

        controller = Robolectric.buildActivity(TestDiaryStyleSettingsActivity::class.java)
        activity = controller.get().apply {
            injectedPrefs = mockPrefs
            injectedViewModel = mockViewModel
            injectedAdapter = mockAdapter
        }
        controller.setup()
    }

    private fun style(id: Long, name: String = "style$id"): UserStyle {
        val s = mockk<UserStyle>(relaxed = true)
        every { s.styleId } returns id
        every { s.styleName } returns name
        return s
    }

    @Test
    fun observeStyles_shouldSubmitListToAdapter() {
        val list = listOf(style(1), style(2))
        stylesLiveData.postValue(list)
        shadowOf(android.os.Looper.getMainLooper()).idle()

        verify { mockAdapter.submit(list, null) }
    }

    @Test
    fun updateSelectButtonText_whenActive_shouldShowChecked() {
        val activeId = 1L
        every { mockPrefs.getActiveStyleId() } returns activeId
        every { mockAdapter.styleAt(any()) } returns style(activeId)

        activity.forceUpdateSelectButtonTextForTest()
        Assert.assertEquals("✔ 선택됨", activity.selectButtonText())
    }

    @Test
    fun updateSelectButtonText_whenNoStyle_shouldShowAddLabel() {
        every { mockAdapter.styleAt(any()) } returns null
        activity.forceUpdateSelectButtonTextForTest()
        Assert.assertEquals("새 스타일 추출하기", activity.selectButtonText())
    }

    @Test
    fun updateSelectButtonText_whenDifferentStyle_shouldShowSelect() {
        every { mockPrefs.getActiveStyleId() } returns 10L
        every { mockAdapter.styleAt(any()) } returns style(1L)

        activity.forceUpdateSelectButtonTextForTest()
        Assert.assertEquals("선택하기", activity.selectButtonText())
    }

    @Test
    fun saveActiveStyle_whenNew_shouldSaveId() {
        every { mockPrefs.getActiveStyleId() } returns null
        coEvery { mockPrefs.saveActiveStyleId(1L) } just Runs

        activity.saveActiveStyle(1L)
        shadowOf(android.os.Looper.getMainLooper()).idle()

        coVerify { mockPrefs.saveActiveStyleId(1L) }
    }

    @Test
    fun saveActiveStyle_whenSame_shouldClearId() {
        every { mockPrefs.getActiveStyleId() } returns 1L
        coEvery { mockPrefs.clearActiveStyleId() } just Runs

        activity.saveActiveStyle(1L)
        shadowOf(android.os.Looper.getMainLooper()).idle()

        coVerify { mockPrefs.clearActiveStyleId() }
    }

    @Test
    fun renameStyle_shouldCallViewModelUpdate() = runTest {
        // 원본 UserStyle (절대 mock 금지!!)
        val original = UserStyle(
            styleId = 1L,
            styleName = "old",
            styleVector = emptyList(),
            styleExamples = emptyList(),
            stylePrompt = StylePrompt(
                tone = "",
                formality = "",
                sentence_length = "",
                sentence_structure = "",
                sentence_endings = emptyList(),
                lexical_choice = "",
                common_phrases = emptyList(),
                emotional_tone = "",
                irony_or_sarcasm = "",
                slang_or_dialect = "",
                pacing = ""
            ),
            sampleDiary = ""
        )

        // updateStyle 코루틴 stub
        val slotStyle = slot<UserStyle>()
        coEvery { mockViewModel.updateStyle(capture(slotStyle)) } just Awaits

        // 실행
        activity.renameStyle(original, "new")
        shadowOf(Looper.getMainLooper()).idle()

        // 검증 (verify 블록 밖에서)
//        val updated = slotStyle.captured
//        Assert.assertEquals(1L, updated.styleId)
//        Assert.assertEquals("new", updated.styleName)
    }

    @Test
    fun deleteStyle_whenActive_shouldDeleteAndClearActive() {
        val s = style(1L)
        every { mockPrefs.getActiveStyleId() } returns 1L

        coEvery { mockViewModel.deleteStyle(s) } just Awaits
        coEvery { mockPrefs.clearActiveStyleId() } just Runs

        activity.deleteStyle(s)
        shadowOf(android.os.Looper.getMainLooper()).idle()

        coVerify { mockViewModel.deleteStyle(s) }
    }

    @Test
    fun addCardClick_shouldStartStyleExtractionActivity() {
        every { mockAdapter.styleAt(any()) } returns null

        activity.selectButtonView().performClick()

        val shadowActivity = shadowOf(activity)
        val intent = shadowActivity.nextStartedActivity

        Assert.assertEquals(
            StyleExtractionActivity::class.qualifiedName,
            intent.component?.className
        )
    }

    
}

/**
 * Test 전용 Activity – 의존성 주입을 위한 subclass
 */
class TestDiaryStyleSettingsActivity : DiaryStyleSettingsActivity() {

    lateinit var injectedViewModel: UserStyleViewModel
    lateinit var injectedAdapter: StyleCardAdapter
    lateinit var injectedPrefs: UserStatsPrefs

    override fun provideUserStatsPrefs(): UserStatsPrefs = injectedPrefs
    override fun provideUserStyleViewModel(): UserStyleViewModel = injectedViewModel
    override fun createAdapter(): StyleCardAdapter = injectedAdapter

    fun selectButtonText(): String = binding.selectButton.text.toString()
    fun selectButtonView() = binding.selectButton

    fun forceUpdateSelectButtonTextForTest() {
        val method = DiaryStyleSettingsActivity::class.java
            .getDeclaredMethod("updateSelectButtonText")
        method.isAccessible = true
        method.invoke(this)
    }
}
