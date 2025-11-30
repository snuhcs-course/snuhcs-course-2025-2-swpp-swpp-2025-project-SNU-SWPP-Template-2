package com.example.mindlog.settings

import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.example.mindlog.R
import com.example.mindlog.core.dispatcher.DispatcherModule
import com.example.mindlog.features.settings.presentation.ChangePasswordFragment
import com.example.mindlog.utils.launchFragmentInHiltContainer
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@MediumTest
@HiltAndroidTest
@UninstallModules(DispatcherModule::class)
@RunWith(AndroidJUnit4::class)
class ChangePasswordFragmentTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: TestSettingsRepository

    @Before
    fun setup() {
        hiltRule.inject()
        repository.reset()
    }

    @Test
    fun passwordMismatch_showsErrorOnInputLayout() {
        // Given: 프래그먼트 실행
        launchFragmentInHiltContainer<ChangePasswordFragment>()

        // When: 새 비밀번호와 확인 비밀번호를 다르게 입력
        onView(withId(R.id.et_current_password)).perform(typeText("1234"), closeSoftKeyboard())
        onView(withId(R.id.et_new_password)).perform(typeText("newPass"), closeSoftKeyboard())
        onView(withId(R.id.et_confirm_password)).perform(typeText("wrongPass"), closeSoftKeyboard())

        // 저장 버튼 클릭
        onView(withId(R.id.btn_save)).perform(click())

        // Then: 비밀번호 확인 TextInputLayout에 에러 메시지가 표시되는지 확인
        onView(withId(R.id.til_confirm_password))
            .check(matches(hasDescendant(withText("비밀번호가 일치하지 않습니다."))))
    }

    @Test
    fun validPassword_showsConfirmDialog_and_updatesRepository_onSuccess() {
        // Given: NavController 설정
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        launchFragmentInHiltContainer<ChangePasswordFragment> {
            navController.setGraph(R.navigation.nav_graph_home)
            navController.setCurrentDestination(R.id.changePasswordFragment)
            Navigation.setViewNavController(requireView(), navController)
        }

        // When: 올바른 비밀번호 입력 후 저장 클릭
        val newPassword = "newPass123"
        onView(withId(R.id.et_current_password)).perform(replaceText("1234"))
        onView(withId(R.id.et_new_password)).perform(replaceText(newPassword))
        onView(withId(R.id.et_confirm_password)).perform(replaceText(newPassword))

        // 키보드 닫기
        onView(withId(R.id.et_confirm_password)).perform(closeSoftKeyboard())
        onView(withId(R.id.btn_save)).perform(click())

        // Then 1: 확인 다이얼로그 표시 확인
        onView(withText("비밀번호 변경"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))

        onView(withText("정말로 비밀번호를 변경하시겠습니까?"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))

        // When 2: 다이얼로그의 '확인' 버튼 클릭
        onView(withText("확인"))
            .inRoot(isDialog())
            .perform(click())

        // Then 2: Repository에 변경된 비밀번호가 전달되었는지 확인
        assertEquals(newPassword, repository.lastUpdatedPassword)
    }
}
