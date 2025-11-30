package com.example.mindlog.journal

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.mindlog.R
import com.example.mindlog.features.journal.presentation.write.JournalWriteActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@HiltAndroidTest
class JournalWriteActivityTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(JournalWriteActivity::class.java)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun endToEnd_journalWrite_success() {
        // ✨ [수정] 1. 감정 선택 화면에서 '다음'을 눌러 내용 작성 화면으로 이동
        onView(withId(R.id.btn_next_or_save)).perform(click())

        // 2. 제목과 내용을 입력
        onView(withId(R.id.et_title)).perform(replaceText("UI 테스트 제목"))
        onView(withId(R.id.et_content)).perform(replaceText("UI 테스트 내용입니다."))
        onView(withId(R.id.btn_next_or_save)).perform(click())

        // 3. 토스트 메시지 확인
        onView(withText("일기가 저장되었습니다.")).check(matches(isDisplayed()))

        // 4. 액티비티 종료 확인
        activityRule.scenario.onActivity { activity ->
            assertTrue(activity.isFinishing)
        }
    }

    @Test
    fun saveJournal_failsWithToast_whenTitleIsEmpty() {
        // ✨ [수정] 1. 감정 선택 화면에서 '다음'을 눌러 내용 작성 화면으로 이동
        onView(withId(R.id.btn_next_or_save)).perform(click())

        // 2. 제목은 비우고 내용만 입력 후 '작성' 클릭
        onView(withId(R.id.et_content)).perform(replaceText("내용은 있습니다."))
        onView(withId(R.id.btn_next_or_save)).perform(click())

        // 3. 에러 토스트 확인
        onView(withText("제목을 입력해주세요.")).check(matches(isDisplayed()))
    }

    @Test
    fun saveJournal_failsWithToast_whenContentIsEmpty() {
        // ✨ [수정] 1. 감정 선택 화면에서 '다음'을 눌러 내용 작성 화면으로 이동
        onView(withId(R.id.btn_next_or_save)).perform(click())

        // 2. 내용은 비우고 제목만 입력 후 '작성' 클릭
        onView(withId(R.id.et_title)).perform(replaceText("제목은 있습니다."))
        onView(withId(R.id.btn_next_or_save)).perform(click())

        // 3. 에러 토스트 확인
        onView(withText("오늘의 하루를 입력해주세요.")).check(matches(isDisplayed()))
    }

    @Test
    fun pressBackButton_navigatesFromContentWriteToEmotionSelect() {
        // ✨ [수정] 1. 감정 선택 화면에서 '다음'을 눌러 내용 작성 화면으로 이동
        onView(withId(R.id.btn_next_or_save)).perform(click())

        // 2. 내용 작성 화면에 있는지 확인 (이제 et_title이 보이므로 성공)
        onView(withId(R.id.et_title)).check(matches(isDisplayed()))

        // 3. '뒤로' 버튼 클릭
        onView(withId(R.id.btn_cancel_or_back)).perform(click())

        // 4. 다시 감정 선택 화면으로 돌아왔는지 확인
        onView(withText("오늘 어떤 감정을 느꼈나요?")).check(matches(isDisplayed()))
        onView(withId(R.id.btn_next_or_save)).check(matches(withText("다음")))
    }
}
