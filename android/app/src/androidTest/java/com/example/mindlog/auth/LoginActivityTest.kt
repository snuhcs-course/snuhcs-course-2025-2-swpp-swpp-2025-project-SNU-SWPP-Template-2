package com.example.mindlog.auth

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import com.example.mindlog.features.auth.presentation.login.LoginActivity
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.UninstallModules
import com.example.mindlog.features.auth.di.AuthBindModule
import com.example.mindlog.core.dispatcher.DispatcherModule
import com.example.mindlog.features.auth.presentation.main.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(AuthBindModule::class, DispatcherModule::class)
@RunWith(AndroidJUnit4::class)
@LargeTest
class LoginActivityTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var testAuthRepository: TestAuthRepository

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun login_success_navigates_to_main_activity() {
        // given
        testAuthRepository.shouldLoginSucceed = true

        Intents.init()
        val scenario = ActivityScenario.launch(LoginActivity::class.java)

        // when: 입력 + 로그인 버튼
        onView(withId(com.example.mindlog.R.id.etLoginId))
            .perform(typeText("test_id"), closeSoftKeyboard())
        onView(withId(com.example.mindlog.R.id.etPassword))
            .perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(com.example.mindlog.R.id.btnLogin)).perform(click())

        // then: MainActivity로 이동 인텐트 확인
        Intents.intended(hasComponent(MainActivity::class.java.name))

        scenario.close()
        Intents.release()
    }

    @Test
    fun login_fail_stays_on_login_screen() {
        // given
        testAuthRepository.shouldLoginSucceed = false

        Intents.init()
        val scenario = ActivityScenario.launch(LoginActivity::class.java)

        onView(withId(com.example.mindlog.R.id.etLoginId))
            .perform(typeText("wrong_id"), closeSoftKeyboard())
        onView(withId(com.example.mindlog.R.id.etPassword))
            .perform(typeText("wrong_pass"), closeSoftKeyboard())
        onView(withId(com.example.mindlog.R.id.btnLogin)).perform(click())

        Intents.intended(hasComponent(MainActivity::class.java.name), Intents.times(0))

        // then: 여전히 로그인 화면에 머물러 있는지 확인
        onView(withId(com.example.mindlog.R.id.etLoginId))
            .check(matches(isDisplayed()))

        scenario.close()
        Intents.release()
    }
}