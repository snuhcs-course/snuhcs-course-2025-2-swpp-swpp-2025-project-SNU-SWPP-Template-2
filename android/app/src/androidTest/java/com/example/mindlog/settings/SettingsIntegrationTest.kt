package com.example.mindlog.settings

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.mindlog.R
import com.example.mindlog.core.dispatcher.DispatcherModule
import com.example.mindlog.features.home.presentation.HomeActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.endsWith
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@LargeTest
@HiltAndroidTest
@UninstallModules(DispatcherModule::class)
@RunWith(AndroidJUnit4::class)
class SettingsIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var settingsRepository: TestSettingsRepository

    @Before
    fun setup() {
        hiltRule.inject()
        settingsRepository.reset()
    }

    @Test
    fun settings_EndToEnd_UserJourney() {
        // 1. 홈 화면 진입
        val scenario = ActivityScenario.launch(HomeActivity::class.java)

        // 2. 설정 화면 진입
        onView(withId(R.id.btn_settings)).perform(click())
        onView(withText("설정")).check(matches(isDisplayed()))
        onView(withId(R.id.tv_username)).check(matches(withText("기존 사용자")))

        // ------------------------------------------------------------
        // 3. 프로필 수정 시나리오
        // ------------------------------------------------------------
        onView(withId(R.id.btnEditProfile)).perform(click())
        onView(withId(R.id.et_username)).perform(replaceText("변경된 사용자"))
        onView(withId(R.id.rb_female)).perform(click())
        onView(withId(R.id.btn_save)).perform(closeSoftKeyboard(), click())
        onView(withId(R.id.tv_username)).check(matches(withText("변경된 사용자")))

        // ------------------------------------------------------------
        // 4. 비밀번호 변경 시나리오
        // ------------------------------------------------------------
        onView(withId(R.id.btnChangePassword)).perform(click())
        onView(withId(R.id.et_current_password)).perform(typeText("1234"), closeSoftKeyboard())
        onView(withId(R.id.et_new_password)).perform(typeText("newPass!"), closeSoftKeyboard())
        onView(withId(R.id.et_confirm_password)).perform(typeText("newPass!"), closeSoftKeyboard())
        onView(withId(R.id.btn_save)).perform(click())

        // 비밀번호 변경 다이얼로그의 '확인' 버튼을 더 명확하게 지정
        onView(allOf(withText("확인"), withClassName(endsWith("Button"))))
            .inRoot(isDialog())
            .perform(click())

        // 복귀 확인
        onView(withText("설정")).check(matches(isDisplayed()))
        assertEquals("newPass!", settingsRepository.lastUpdatedPassword)

        // ------------------------------------------------------------
        // 5. 로그아웃 시나리오
        // ------------------------------------------------------------
        onView(withId(R.id.btnLogout)).perform(click())

        onView(withText("정말로 로그아웃 하시겠습니까?"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))

        onView(allOf(withText("로그아웃"), withClassName(endsWith("MaterialButton"))))
            .inRoot(isDialog())
            .perform(click())

        scenario.close()
    }
}
