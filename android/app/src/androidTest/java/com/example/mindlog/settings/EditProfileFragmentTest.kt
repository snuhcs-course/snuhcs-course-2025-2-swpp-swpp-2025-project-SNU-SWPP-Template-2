package com.example.mindlog.settings

import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.example.mindlog.R
import com.example.mindlog.core.dispatcher.DispatcherModule
import com.example.mindlog.features.settings.presentation.EditProfileFragment
import com.example.mindlog.utils.launchFragmentInHiltContainer
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.After
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
class EditProfileFragmentTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: TestSettingsRepository

    @Before
    fun setup() {
        hiltRule.inject()
        repository.reset()
    }

    @After
    fun teardown() {
        repository.reset()
    }

    @Test
    fun loadUserInfo_populatesUiFieldsCorrectly() {
        // Given: Repository 초기 상태 (reset() 호출됨) -> username: "기존 사용자"

        // When: 프래그먼트 실행
        launchFragmentInHiltContainer<EditProfileFragment>()

        // Then: UI에 데이터가 올바르게 표시되는지 확인
        onView(withId(R.id.et_username)).check(matches(withText("기존 사용자")))

        // 생년월일 (1990-01-01)
        onView(withId(R.id.act_birth_year)).check(matches(withText("1990")))
        onView(withId(R.id.act_birth_month)).check(matches(withText("01")))
        onView(withId(R.id.act_birth_day)).check(matches(withText("01")))

        // 성별 (M -> 남성)
        onView(withId(R.id.rb_male)).check(matches(isChecked()))

        // 외모
        onView(withId(R.id.et_appearance)).check(matches(withText("기존 외모")))
    }

    @Test
    fun updateProfile_updatesFakeRepositoryCorrectly() {
        // NavController 설정
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        launchFragmentInHiltContainer<EditProfileFragment> {
            navController.setGraph(R.navigation.nav_graph_home)
            navController.setCurrentDestination(R.id.editProfileFragment)
            Navigation.setViewNavController(requireView(), navController)
        }

        // 1. UI 조작: 정보 수정
        onView(withId(R.id.et_username)).perform(replaceText("새로운 이름"))
        onView(withId(R.id.et_appearance)).perform(replaceText("새로운 스타일"))

        // 성별 변경 (여성)
        onView(withId(R.id.rb_female)).perform(click())

        // 생년월일 변경
        onView(withId(R.id.act_birth_year)).perform(replaceText("2000"))

        // 2. 저장 버튼 클릭
        onView(withId(R.id.btn_save)).perform(closeSoftKeyboard(), click())

        // 3. 검증: Repository 데이터가 변경되었는지 확인
        val updatedUser = repository.getCurrentUserInfoSync()

        assertEquals("새로운 이름", updatedUser.username)
        assertEquals("새로운 스타일", updatedUser.appearance)
        assertEquals("F", updatedUser.gender)
        // 변경된 년도와 기존 월/일 조합 확인
        assertEquals("2000-01-01", updatedUser.birthdate)
    }
}
