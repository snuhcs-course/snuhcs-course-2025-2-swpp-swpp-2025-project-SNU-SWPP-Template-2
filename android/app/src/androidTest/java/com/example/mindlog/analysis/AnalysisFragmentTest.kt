package com.example.mindlog.analysis

import android.content.Context
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.example.mindlog.R
import com.example.mindlog.core.dispatcher.DispatcherModule
import com.example.mindlog.features.analysis.di.AnalysisBindModule
import com.example.mindlog.features.analysis.presentation.AnalysisFragment
import com.example.mindlog.utils.launchFragmentInHiltContainer
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.hamcrest.CoreMatchers.containsString
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(AnalysisBindModule::class, DispatcherModule::class)
@RunWith(AndroidJUnit4::class)
class AnalysisFragmentTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    /**
     * Hilt의 TestAnalysisRepository 에서 미리 넣어둔 더미 데이터가
     * 실제 화면에 잘 렌더링되는지 확인.
     */
    @Test
    fun loadsData_and_showsAllCards_withNewUI() {
        launchFragmentInHiltContainer<AnalysisFragment>()

        // ---------- 1) 사용자 유형 카드 ----------
        onView(withId(R.id.cardUserType))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        // 텍스트는 "탐험가형", "탐험가형 (Explorer)" 등일 수 있으니 부분 매칭
        onView(withId(R.id.tvUserTypeName))
            .perform(scrollTo())
            .check(matches(withText(containsString("탐험가형"))))

        onView(withId(R.id.tvUserTypeDescription))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        // 캐릭터 이미지 표시
        onView(withId(R.id.ivUserTypeCharacter))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        // ---------- 2) Five Factor 카드뉴스(ViewPager2) ----------
        onView(withId(R.id.cardComprehensive))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(withId(R.id.vpFiveFactor))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(withId(R.id.dotsIndicator))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        // ---------- 3) Personalized Advice 카드 ----------
        onView(withId(R.id.cardAdvice))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        // TestAnalysisRepository 에서 넣어둔 타이틀에 맞춰 검증
        onView(withId(R.id.tvAdviceType))
            .perform(scrollTo())
            .check(matches(withText("오늘의 탐험 가이드")))

        onView(withId(R.id.tvAdviceBody))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        // Lottie 캐릭터 애니메이션 뷰도 보이는지 체크
        onView(withId(R.id.lottieAdviceCharacter))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    /**
     * 네비게이션 그래프에 제대로 붙어 있는지만 확인하는 간단한 테스트
     */
    @Test
    fun navigation_check_from_analysis() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = TestNavHostController(context)

        launchFragmentInHiltContainer<AnalysisFragment> {
            navController.setGraph(R.navigation.nav_graph_home)
            navController.setCurrentDestination(R.id.analysisFragment)
            Navigation.setViewNavController(requireView(), navController)
        }

        // Fragment 가 잘 띄워졌는지만 확인
        onView(withId(R.id.cardUserType))
            .check(matches(isDisplayed()))

        assertThat(navController.currentDestination?.id)
            .isEqualTo(R.id.analysisFragment)
    }
}