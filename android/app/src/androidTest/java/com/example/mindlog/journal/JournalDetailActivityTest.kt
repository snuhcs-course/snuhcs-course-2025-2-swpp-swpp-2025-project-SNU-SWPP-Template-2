package com.example.mindlog.journal

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindlog.R
import com.example.mindlog.core.dispatcher.DispatcherModule
import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.features.journal.presentation.detail.JournalDetailActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(DispatcherModule::class)
@RunWith(AndroidJUnit4::class)
class JournalDetailActivityTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: TestJournalRepository

    private val journalId = 100
    private val journalTitle = "상세 테스트 일기"
    private val journalContent = "상세 테스트 내용입니다."

    @Before
    fun setup() {
        hiltRule.inject()
        repository.clear()

        // 테스트용 데이터 미리 저장
        repository.addDummyData(
            JournalEntry(
                id = journalId,
                title = journalTitle,
                content = journalContent,
                createdAt = Date(),
                imageUrl = null,
                keywords = emptyList(),
                emotions = emptyList(),
                gratitude = "감사합니다"
            )
        )
    }

    @Test
    fun detail_loadsAndDisplaysData() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), JournalDetailActivity::class.java).apply {
            putExtra(JournalDetailActivity.EXTRA_JOURNAL_ID, journalId)
        }
        val scenario = ActivityScenario.launch<JournalDetailActivity>(intent)

        onView(withId(R.id.tv_title)).check(matches(withText(journalTitle)))
        onView(withId(R.id.tv_content)).check(matches(withText(journalContent)))
        onView(withId(R.id.tv_gratitude)).check(matches(withText("감사합니다")))

        scenario.close()
    }

    @Test
    fun detail_clickEdit_opensEditActivity() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), JournalDetailActivity::class.java).apply {
            putExtra(JournalDetailActivity.EXTRA_JOURNAL_ID, journalId)
        }
        val scenario = ActivityScenario.launch<JournalDetailActivity>(intent)

        onView(withId(R.id.btn_detail_edit)).perform(click())

        onView(withText("일기 수정")).check(matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun detail_clickDelete_showsDialog_and_deletes() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), JournalDetailActivity::class.java).apply {
            putExtra(JournalDetailActivity.EXTRA_JOURNAL_ID, journalId)
        }
        val scenario = ActivityScenario.launch<JournalDetailActivity>(intent)

        // 1. 삭제 버튼 클릭
        onView(withId(R.id.btn_detail_delete)).perform(click())

        // 2. 다이얼로그 확인 및 삭제 클릭
        onView(withText("일기 삭제")).check(matches(isDisplayed()))
        onView(withText("삭제")).perform(click())

        // 3. 검증: 시나리오 결과나 상태 대신, 실제 데이터 삭제 여부와 Activity의 finishing 상태를 확인합니다.

        // (1) Repository에서 데이터가 실제로 삭제되었는지 확인
        // 삭제 로직이 비동기일 수 있으므로 약간의 재시도 로직이나 지연이 필요할 수 있지만,
        // Espresso가 perform(click())에서 UI Idle을 대기하므로 대부분 즉시 반영됩니다.
        try {
            runBlocking {
                repository.getJournalById(journalId)
            }
            // 여기까지 오면 데이터가 조회된 것이므로 삭제 실패로 간주
            fail("Journal should have been deleted from repository")
        } catch (e: Exception) {
            // getJournalById가 예외를 던지면 삭제 성공 (Journal not found)
        }

        // (2) Activity가 종료 중인지 확인 (finish() 호출 여부)
        // finish() 호출은 메인 스레드에서 비동기적으로 처리되므로 폴링으로 확인합니다.
        var isFinishing = false
        for (i in 0..20) { // 최대 2초 대기
            scenario.onActivity { activity ->
                if (activity.isFinishing || activity.isDestroyed) {
                    isFinishing = true
                }
            }
            if (isFinishing) break
            Thread.sleep(100)
        }

        assertTrue("Activity should be finishing after deletion", isFinishing)

        scenario.close()
    }
}
