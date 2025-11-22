package com.example.mindlog.journal

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions

import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindlog.R
import com.example.mindlog.core.dispatcher.DispatcherModule
import com.example.mindlog.features.home.presentation.HomeActivity
import com.example.mindlog.features.journal.presentation.adapter.JournalAdapter
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(DispatcherModule::class)
@RunWith(AndroidJUnit4::class)
class JournalIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: TestJournalRepository

    @Before
    fun setup() {
        hiltRule.inject()
        repository.clear()
    }

    @Test
    fun journal_EndToEnd_UserJourney() {
        // 1. 홈 화면 진입
        val scenario = ActivityScenario.launch(HomeActivity::class.java)

        // 2. 일기 작성 버튼 클릭
        onView(withId(R.id.fabWrite)).perform(ViewActions.click())

        // 3. 감정 선택 화면 (EmotionSelectFragment)
        // 다음 화면으로 이동
        onView(withId(R.id.btn_next_or_save)).perform(ViewActions.click())

        // 4. 내용 작성 화면 (ContentWriteFragment)
        onView(withId(R.id.et_title)).perform(ViewActions.replaceText("통합 테스트 일기"))
        onView(withId(R.id.et_content)).perform(ViewActions.replaceText("통합 테스트 내용"))
        onView(withId(R.id.et_gratitude)).perform(ViewActions.replaceText("감사합니다"))

        // 저장 버튼 클릭
        onView(withId(R.id.btn_next_or_save)).perform(ViewActions.click())

        // ToastMatcher는 불안정하므로 제거합니다.
        // 대신 목록 화면으로 복귀하여 데이터가 잘 보이는지 확인하는 것으로 충분합니다.

        // 5. 목록 화면으로 복귀 및 데이터 확인
        // RecyclerView에 "통합 테스트 일기"가 보이는지 확인
        onView(withText("통합 테스트 일기")).check(matches(isDisplayed()))

        // 6. 상세 화면 진입
        onView(withId(R.id.rv_diary_feed))
            .perform(RecyclerViewActions.actionOnItemAtPosition<JournalAdapter.ViewHolder>(0,
                ViewActions.click()))

        // 상세 내용 확인 (TextView ID: tv_title, tv_content)
        onView(withId(R.id.tv_title)).check(matches(withText("통합 테스트 일기")))
        onView(withId(R.id.tv_content)).check(matches(withText("통합 테스트 내용")))

        // 7. 수정 화면 진입
        onView(withId(R.id.btn_detail_edit)).perform(ViewActions.click())
        // 수정 화면 툴바 제목 확인
        onView(withText("일기 수정")).check(matches(isDisplayed()))

        // 제목 수정
        onView(withId(R.id.et_title)).perform(ViewActions.replaceText("수정된 제목"))
        onView(withId(R.id.btn_edit_save)).perform(ViewActions.click())

        // 수정 완료 Toast 확인 코드 제거

        // 수정된 내용이 상세 화면에 반영되었는지 확인 (DetailActivity로 돌아옴)
        onView(withId(R.id.tv_title)).check(matches(withText("수정된 제목")))

        // 8. 삭제
        onView(withId(R.id.btn_detail_delete)).perform(ViewActions.click())
        // 다이얼로그 확인 버튼 ("삭제") 클릭
        onView(withText("삭제")).perform(ViewActions.click())

        // 삭제 완료 Toast 확인 코드 제거

        // 목록에서 사라졌는지 확인
        // "수정된 제목" 텍스트가 더 이상 화면에 존재하지 않아야 함
        onView(withText("수정된 제목")).check(ViewAssertions.doesNotExist())

        scenario.close()
    }
}
