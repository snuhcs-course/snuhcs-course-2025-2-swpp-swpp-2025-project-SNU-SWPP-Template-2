package com.example.mindlog.auth

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.containsString
import com.example.mindlog.R
import com.example.mindlog.core.dispatcher.DispatcherModule
import com.example.mindlog.features.auth.di.AuthBindModule
import com.example.mindlog.features.auth.presentation.main.MainActivity
import com.example.mindlog.features.auth.presentation.signup.SignupActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject


@HiltAndroidTest
@UninstallModules(AuthBindModule::class, DispatcherModule::class)
@RunWith(AndroidJUnit4::class)
class SignupActivityTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var testAuthRepository: TestAuthRepository

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun signup_success_navigates_to_main_activity() {
        // given
        testAuthRepository.shouldSignupSucceed = true

        Intents.init()
        val scenario = ActivityScenario.launch(SignupActivity::class.java)

        onView(withId(R.id.etUsername))
            .perform(typeText("test_username"), closeSoftKeyboard())

        onView(withId(R.id.etLoginId))
            .perform(typeText("test_id"), closeSoftKeyboard())

        onView(withId(R.id.etPassword))
            .perform(typeText("password123"), closeSoftKeyboard())

        onView(withId(R.id.etConfirmPassword))
            .perform(typeText("password123"), closeSoftKeyboard())

        onView(withId(R.id.rbMale)).perform(click())

        onView(withId(R.id.actBirthYear))
            .perform(replaceText("2000"), closeSoftKeyboard())
        onView(withId(R.id.actBirthMonth))
            .perform(replaceText("1"), closeSoftKeyboard())
        onView(withId(R.id.actBirthDay))
            .perform(replaceText("1"), closeSoftKeyboard())

        onView(withId(R.id.btnSignup)).perform(click())

        // then: MainActivity로 이동하는 인텐트가 발생했는지 확인
        Intents.intended(hasComponent(MainActivity::class.java.name))

        scenario.close()
        Intents.release()
    }

    @Test
    fun signup_fail_stays_on_signup_screen() {
        testAuthRepository.shouldSignupSucceed = false

        Intents.init()
        val scenario = ActivityScenario.launch(SignupActivity::class.java)

        // 입력
        onView(withId(R.id.etUsername)).perform(typeText("test_username"), closeSoftKeyboard())
        onView(withId(R.id.etLoginId)).perform(typeText("test_id"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.etConfirmPassword)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.rbMale)).perform(click())
        onView(withId(R.id.actBirthYear)).perform(replaceText("2000"), closeSoftKeyboard())
        onView(withId(R.id.actBirthMonth)).perform(replaceText("1"), closeSoftKeyboard())
        onView(withId(R.id.actBirthDay)).perform(replaceText("1"), closeSoftKeyboard())

        // 클릭
        onView(withId(R.id.btnSignup)).perform(click())

        // 에러 메시지가 버튼 위에 표시되어야 함
        onView(withId(R.id.tvSignupError))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString("signup failed"))))

        // MainActivity로 이동하면 안 됨
        Intents.intended(hasComponent(MainActivity::class.java.name), Intents.times(0))

        // 여전히 Signup 화면이어야 함
        onView(withId(R.id.btnSignup)).check(matches(isDisplayed()))

        scenario.close()
        Intents.release()
    }

    @Test
    fun signup_missing_fields_shows_validation_error() {
        Intents.init()
        val scenario = ActivityScenario.launch(SignupActivity::class.java)

        // 아무 값도 입력하지 않고 바로 회원가입 버튼 클릭
        onView(withId(R.id.btnSignup)).perform(click())

        // 필드 누락 에러 메시지가 표시되어야 함
        onView(withId(R.id.tvSignupError))
            .check(matches(isDisplayed()))
            .check(matches(withText("모든 필드를 입력해주세요.")))

        // MainActivity로 이동하면 안 됨
        Intents.intended(hasComponent(MainActivity::class.java.name), Intents.times(0))

        scenario.close()
        Intents.release()
    }

    @Test
    fun signup_password_mismatch_shows_validation_error() {
        Intents.init()
        val scenario = ActivityScenario.launch(SignupActivity::class.java)

        // 필수 필드들은 입력하되, 비밀번호와 비밀번호 확인은 다르게 입력
        onView(withId(R.id.etUsername)).perform(typeText("test_username"), closeSoftKeyboard())
        onView(withId(R.id.etLoginId)).perform(typeText("test_id"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.etConfirmPassword)).perform(typeText("different123"), closeSoftKeyboard())
        onView(withId(R.id.rbMale)).perform(click())
        onView(withId(R.id.actBirthYear)).perform(replaceText("2000"), closeSoftKeyboard())
        onView(withId(R.id.actBirthMonth)).perform(replaceText("1"), closeSoftKeyboard())
        onView(withId(R.id.actBirthDay)).perform(replaceText("1"), closeSoftKeyboard())

        // 회원가입 버튼 클릭
        onView(withId(R.id.btnSignup)).perform(click())

        // 비밀번호 불일치 에러 메시지가 표시되어야 함
        onView(withId(R.id.tvSignupError))
            .check(matches(isDisplayed()))
            .check(matches(withText("비밀번호가 일치하지 않습니다.")))

        // MainActivity로 이동하면 안 됨
        Intents.intended(hasComponent(MainActivity::class.java.name), Intents.times(0))

        scenario.close()
        Intents.release()
    }
}