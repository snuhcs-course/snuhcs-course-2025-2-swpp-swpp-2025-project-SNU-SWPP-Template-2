package com.example.mindlog.settings

import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.example.mindlog.R
import com.example.mindlog.core.dispatcher.DispatcherModule
import com.example.mindlog.features.settings.presentation.SettingsFragment
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
class SettingsFragmentTest {

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
    fun displayUserInfo_showsCorrectDataFromRepository() {
        // Given: Repository 초기 데이터 (TestSettingsRepository 참조)
        // username: "기존 사용자", loginId: "test_login_id", birthdate: "1990-01-01", gender: "M"

        // When
        launchFragmentInHiltContainer<SettingsFragment>()

        // Then
        onView(withId(R.id.tv_username)).check(matches(withText("기존 사용자")))
        onView(withId(R.id.tv_login_id)).check(matches(withText("ID: test_login_id")))

        // birthdate와 gender가 합쳐져서 표시됨
        onView(withId(R.id.tv_additional_info)).check(matches(withText("1990-01-01 · M")))
    }

    @Test
    fun navigateToEditProfile_whenButtonClicked() {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        launchFragmentInHiltContainer<SettingsFragment> {
            navController.setGraph(R.navigation.nav_graph_home)
            navController.setCurrentDestination(R.id.settingsFragment)
            Navigation.setViewNavController(requireView(), navController)
        }

        // 프로필 수정 버튼 클릭
        onView(withId(R.id.btnEditProfile)).perform(click())

        // 내비게이션 이동 확인
        assertEquals(R.id.editProfileFragment, navController.currentDestination?.id)
    }
}
