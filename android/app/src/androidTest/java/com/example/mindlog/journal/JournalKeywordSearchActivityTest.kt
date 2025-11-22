package com.example.mindlog.journal

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindlog.R
import com.example.mindlog.core.dispatcher.DispatcherModule
import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.core.model.Keyword
import com.example.mindlog.features.journal.presentation.adapter.JournalAdapter
import com.example.mindlog.features.journal.presentation.detail.JournalDetailActivity
import com.example.mindlog.features.journal.presentation.list.JournalKeywordSearchActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(DispatcherModule::class)
@RunWith(AndroidJUnit4::class)
class JournalKeywordSearchActivityTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: TestJournalRepository

    private val keyword = "여행"

    @Before
    fun setup() {
        hiltRule.inject()
        repository.clear()
        Intents.init()

        // 검색될 더미 데이터 추가 (키워드 포함)
        val entry = JournalEntry(
            id = 200,
            title = "여행 일기",
            content = "즐거운 여행이었다.",
            createdAt = Date(),
            imageUrl = null,
            keywords = listOf(Keyword(keyword, "happy", "summary", 1.0f)),
            emotions = emptyList(),
            gratitude = "여행"
        )
        repository.addDummyData(entry)
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun searchActivity_loadsResults_and_displaysInRecyclerView() {
        // 1. 키워드를 담아 Activity 실행
        val intent = Intent(ApplicationProvider.getApplicationContext(), JournalKeywordSearchActivity::class.java).apply {
            putExtra(JournalKeywordSearchActivity.EXTRA_KEYWORD, keyword)
        }

        val scenario = ActivityScenario.launch<JournalKeywordSearchActivity>(intent)

        // 2. 툴바 타이틀 확인 ("키워드: 여행")
        // Toolbar 내부의 TextView를 찾기 위해 hasDescendant 사용
        onView(withId(R.id.toolbar))
            .check(matches(hasDescendant(withText("키워드: $keyword"))))

        // 3. 리사이클러뷰 표시 확인
        onView(ViewMatchers.withId(R.id.rvKeywordSearchResults))
            .check(matches(isDisplayed()))

        // 4. 아이템 내용("여행 일기") 확인
        onView(withText("여행 일기")).check(matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun searchActivity_clickItem_navigatesToDetail() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), JournalKeywordSearchActivity::class.java).apply {
            putExtra(JournalKeywordSearchActivity.EXTRA_KEYWORD, keyword)
        }
        val scenario = ActivityScenario.launch<JournalKeywordSearchActivity>(intent)

        // 리스트 아이템 클릭
        onView(ViewMatchers.withId(R.id.rvKeywordSearchResults))
            .perform(RecyclerViewActions.actionOnItemAtPosition<JournalAdapter.ViewHolder>(0, click()))

        // DetailActivity로 이동하는 Intent 검증
        intended(allOf(
            hasComponent(JournalDetailActivity::class.java.name),
            hasExtra(JournalDetailActivity.EXTRA_JOURNAL_ID, 200)
        ))

        scenario.close()
    }

    @Test
    fun searchActivity_finishes_whenNoKeyword() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), JournalKeywordSearchActivity::class.java)
        val scenario = ActivityScenario.launch<JournalKeywordSearchActivity>(intent)

        Thread.sleep(500)
        assert(scenario.state == Lifecycle.State.DESTROYED)

        scenario.close()
    }
}
