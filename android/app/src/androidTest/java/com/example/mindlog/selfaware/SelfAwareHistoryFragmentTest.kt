package com.example.mindlog.selfaware

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindlog.R
import com.example.mindlog.core.dispatcher.DispatcherModule
import com.example.mindlog.features.selfaware.di.SelfAwareBindModule
import com.example.mindlog.features.selfaware.presentation.fragment.SelfAwareHistoryFragment
import com.example.mindlog.utils.launchFragmentInHiltContainer
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals

@HiltAndroidTest
@UninstallModules(SelfAwareBindModule::class, DispatcherModule::class)
@RunWith(AndroidJUnit4::class)
class SelfAwareHistoryFragmentTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp()  {
        hiltRule.inject()
    }

    @Test
    fun list_shows10_then_paginates_to15_on_scroll() {
        // 프래그먼트 띄우기
        launchFragmentInHiltContainer<SelfAwareHistoryFragment>()

        // 초기 목록 표시 (첫 호출 10개)
        onView(withId(R.id.recyclerHistory))
            .check(matches(isDisplayed()))
        onView(withId(R.id.recyclerHistory))
            .check(RecyclerViewItemCountAssertion(10))

        // 마지막 아이템으로 스크롤 → 페이징 트리거(loadNext)
        onView(withId(R.id.recyclerHistory)).perform(
            RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(9) // 0..9
        )

        // 추가 아이템이 로드될 수 있도록 한 번 더 아래로 스크롤 (15번 인덱스로)
        onView(withId(R.id.recyclerHistory)).perform(
            RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(14)
        )

        // 최종 아이템 수 15 확인 (두 번째 호출에서 5개 더)
        onView(withId(R.id.recyclerHistory))
            .check(RecyclerViewItemCountAssertion(15))
    }
}

class RecyclerViewItemCountAssertion(
    private val expectedCount: Int
) : ViewAssertion {
    override fun check(view: View?, noViewFoundException: NoMatchingViewException?) {
        if (noViewFoundException != null) throw noViewFoundException
        val rv = view as? RecyclerView
            ?: throw AssertionError("The asserted view is not a RecyclerView")
        val adapter = rv.adapter
            ?: throw AssertionError("RecyclerView has no adapter set")
        assertEquals("RecyclerView itemCount not matched.", expectedCount, adapter.itemCount)
    }
}