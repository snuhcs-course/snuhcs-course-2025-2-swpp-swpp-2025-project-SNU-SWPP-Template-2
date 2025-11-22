package com.example.mindlog.journal

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindlog.R
import com.example.mindlog.core.dispatcher.DispatcherModule
import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.features.journal.presentation.list.JournalFragment
import com.example.mindlog.utils.launchFragmentInHiltContainer
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(DispatcherModule::class)
@RunWith(AndroidJUnit4::class)
class JournalFragmentTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: TestJournalRepository

    @Before
    fun setup() {
        hiltRule.inject()
        repository.clear() // 테스트 전 초기화
    }

    @Test
    fun displayJournalList_whenDataExists() {
        // Given: 가짜 저장소에 데이터 추가
        val entry = JournalEntry(
            id = 1,
            title = "테스트 일기 제목",
            content = "테스트 내용입니다.",
            createdAt = Date(),
            imageUrl = null,
            keywords = emptyList(),
            emotions = emptyList(),
            gratitude = "감사합니다"
        )
        repository.addDummyData(entry)

        // When: 프래그먼트 실행
        launchFragmentInHiltContainer<JournalFragment>()

        // Then: 리사이클러뷰와 아이템 내용 표시 확인
        onView(withId(R.id.rv_diary_feed)).check(matches(isDisplayed()))
        onView(withText("테스트 일기 제목")).check(matches(isDisplayed()))
        onView(withText("테스트 내용입니다.")).check(matches(isDisplayed()))
    }

    @Test
    fun displayEmptyList_whenNoData() {
        // Given: 데이터 없음 (clear 호출됨)

        // When: 프래그먼트 실행
        launchFragmentInHiltContainer<JournalFragment>()

        // Then: 리사이클러뷰는 표시되지만 내용은 없음 (혹은 EmptyView가 있다면 그것을 체크)
        onView(withId(R.id.rv_diary_feed)).check(matches(isDisplayed()))
        // 만약 "작성된 일기가 없습니다" 같은 텍스트 뷰가 있다면:
        // onView(withText("일기가 없습니다")).check(matches(isDisplayed()))
    }
}
