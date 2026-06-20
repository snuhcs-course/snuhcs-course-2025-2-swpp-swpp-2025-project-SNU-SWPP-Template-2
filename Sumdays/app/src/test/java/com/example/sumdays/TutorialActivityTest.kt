package com.example.sumdays

import android.os.Build
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import androidx.viewpager.widget.ViewPager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowActivity

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [24],
    application = TestApplication::class  // 🔥 이미 있는 TestApplication 재사용
)
class TutorialActivityTest {

    /** 공통: Activity 생성 헬퍼 */
    private fun createActivity(): TutorialActivity {
        val controller = Robolectric.buildActivity(
            TutorialActivity::class.java
        )
        return controller.setup().get()
    }

    // ---------------------------------------------------------------------
    // 1. onCreate: ViewPager, dots, 버튼들이 잘 초기화되는지
    // ---------------------------------------------------------------------
    @Test
    fun onCreate_setsUpViewPagerAndDots() {
        val activity = createActivity()

        val viewPager = activity.findViewById<ViewPager>(R.id.view_pager)
        val dotsLayout = activity.findViewById<LinearLayout>(R.id.layoutDots)
        val btnSkip = activity.findViewById<ImageButton>(R.id.btn_skip)
        val btnNext = activity.findViewById<ImageButton>(R.id.btn_next)
        val btnPrev = activity.findViewById<ImageButton>(R.id.btn_prev)

        assertNotNull(viewPager)
        assertNotNull(viewPager.adapter)
        assertTrue(viewPager.adapter is TutorialActivity.TutorialPagerAdapter)

        // layouts.size == adapter.count 이어야 함
        assertEquals(viewPager.adapter!!.count, dotsLayout.childCount)

        assertNotNull(btnSkip)
        assertNotNull(btnNext)
        assertNotNull(btnPrev)
    }

    // ---------------------------------------------------------------------
    // 2. Skip 버튼: Activity 가 finish() 되는지
    // ---------------------------------------------------------------------
    @Test
    fun clickingSkip_finishesActivity() {
        val activity = createActivity()
        val btnSkip = activity.findViewById<ImageButton>(R.id.btn_skip)

        btnSkip.performClick()

        val shadow = Shadow.extract<ShadowActivity>(activity)
        assertTrue("Skip 클릭 후 Activity 가 종료 상태여야 함", activity.isFinishing)
    }

    // ---------------------------------------------------------------------
    // 3. Next 버튼: 마지막 페이지 전까지는 페이지 +1
    // ---------------------------------------------------------------------
    @Test
    fun clickingNext_movesToNextPage() {
        val activity = createActivity()
        val viewPager = activity.findViewById<ViewPager>(R.id.view_pager)
        val btnNext = activity.findViewById<ImageButton>(R.id.btn_next)

        // 초기 페이지는 0이라고 가정
        assertEquals(0, viewPager.currentItem)

        btnNext.performClick()
        assertEquals(1, viewPager.currentItem)

        btnNext.performClick()
        assertEquals(2, viewPager.currentItem)
    }

    // ---------------------------------------------------------------------
    // 4. Next 버튼: 마지막 페이지에서 클릭 시 Activity 종료
    // ---------------------------------------------------------------------
    @Test
    fun clickingNextOnLastPage_finishesActivity() {
        val activity = createActivity()
        val viewPager = activity.findViewById<ViewPager>(R.id.view_pager)
        val btnNext = activity.findViewById<ImageButton>(R.id.btn_next)

        val lastIndex = viewPager.adapter!!.count - 1
        viewPager.currentItem = lastIndex

        btnNext.performClick()

        val shadow = Shadow.extract<ShadowActivity>(activity)
        assertTrue("마지막 페이지에서 Next 클릭 시 Activity 종료되어야 함", activity.isFinishing)
    }

    // ---------------------------------------------------------------------
    // 5. Prev 버튼: 첫 페이지에서는 아무 일도 안 일어나는지
    // ---------------------------------------------------------------------
    @Test
    fun clickingPrevOnFirstPage_doesNothing() {
        val activity = createActivity()
        val viewPager = activity.findViewById<ViewPager>(R.id.view_pager)
        val btnPrev = activity.findViewById<ImageButton>(R.id.btn_prev)

        viewPager.currentItem = 0
        btnPrev.performClick()

        assertEquals(
            "첫 페이지에서 Prev 클릭해도 페이지가 음수로 가지 않아야 함",
            0,
            viewPager.currentItem
        )
    }

    // ---------------------------------------------------------------------
    // 6. Prev 버튼: 두 번째 페이지에서 이전으로 잘 가는지
    // ---------------------------------------------------------------------
    @Test
    fun clickingPrevOnSecondPage_movesToFirstPage() {
        val activity = createActivity()
        val viewPager = activity.findViewById<ViewPager>(R.id.view_pager)
        val btnPrev = activity.findViewById<ImageButton>(R.id.btn_prev)

        viewPager.currentItem = 1
        btnPrev.performClick()

        assertEquals(0, viewPager.currentItem)
    }

    // ---------------------------------------------------------------------
    // 7. Back 버튼(제스처 포함): OnBackPressedCallback 때문에 finish() 안 되는지
    // ---------------------------------------------------------------------
    @Test
    fun backPress_isDisabledByCallback() {
        val activity = createActivity()

        // 뒤로 가기 호출
        activity.onBackPressedDispatcher.onBackPressed()

        val shadow = Shadow.extract<ShadowActivity>(activity)
        assertFalse("OnBackPressedCallback 때문에 뒤로 가기 시 Activity 가 종료되면 안 됨", activity.isFinishing)
    }
}
