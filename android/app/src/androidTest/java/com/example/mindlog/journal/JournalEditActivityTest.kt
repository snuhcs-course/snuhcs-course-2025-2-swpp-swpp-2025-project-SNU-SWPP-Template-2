package com.example.mindlog.journal

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindlog.R
import com.example.mindlog.core.dispatcher.DispatcherModule
import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.features.journal.presentation.write.JournalEditActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(DispatcherModule::class)
@RunWith(AndroidJUnit4::class)
class JournalEditActivityTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: TestJournalRepository

    private val journalId = 999

    @Before
    fun setup() {
        hiltRule.inject()
        repository.clear()

        // 테스트용 초기 데이터 저장
        repository.addDummyData(
            JournalEntry(
                id = journalId,
                title = "기존 제목",
                content = "기존 내용",
                createdAt = Date(),
                imageUrl = null,
                keywords = emptyList(),
                emotions = emptyList(),
                gratitude = "기존 감사"
            )
        )
    }

    @Test
    fun editJournal_loadsData_and_saves_updates() = runBlocking {
        // 1. Intent에 ID를 담아 Activity 실행
        val intent = Intent(ApplicationProvider.getApplicationContext(), JournalEditActivity::class.java).apply {
            putExtra(JournalEditActivity.EXTRA_JOURNAL_ID, journalId)
        }
        val scenario = ActivityScenario.launch<JournalEditActivity>(intent)

        // 2. 초기 데이터가 잘 로드되었는지 확인 (EditText에 텍스트가 채워져 있어야 함)
        onView(withId(R.id.et_title)).check(matches(withText("기존 제목")))
        onView(withId(R.id.et_content)).check(matches(withText("기존 내용")))

        // 3. 내용 수정
        onView(withId(R.id.et_title)).perform(replaceText("수정된 제목"))

        // 4. 저장 버튼 클릭 (클릭 즉시 finish() 호출됨)
        onView(withId(R.id.btn_edit_save)).perform(click())

        // 5. 검증
        val updated = repository.getJournalById(journalId)
        assert(updated.title == "수정된 제목")

        scenario.onActivity { activity ->
            assert(activity.isFinishing)
        }

        scenario.close()
    }

    @Test
    fun editJournal_delete_finishes_activity() = runBlocking {
        val intent = Intent(ApplicationProvider.getApplicationContext(), JournalEditActivity::class.java).apply {
            putExtra(JournalEditActivity.EXTRA_JOURNAL_ID, journalId)
        }
        val scenario = ActivityScenario.launch<JournalEditActivity>(intent)

        // 1. 삭제 버튼 클릭
        onView(withId(R.id.btn_edit_delete)).perform(click())

        // 2. 다이얼로그 확인 클릭 (클릭 즉시 finish() 호출됨)
        onView(withText("삭제"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
            .perform(click())

        // 3. 검증
        val exists = try {
            repository.getJournalById(journalId)
            true
        } catch (e: Exception) {
            false
        }
        assert(!exists)

        scenario.onActivity { activity ->
            assert(activity.isFinishing)
        }

        scenario.close()
    }
}
