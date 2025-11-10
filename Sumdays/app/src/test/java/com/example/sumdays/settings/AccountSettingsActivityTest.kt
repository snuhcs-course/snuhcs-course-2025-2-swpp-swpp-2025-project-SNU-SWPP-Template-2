package com.example.sumdays.settings

import android.content.Intent
import android.os.Build
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.example.sumdays.R
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Robolectric.buildActivity
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class AccountSettingsActivityTest {

    private fun launch(): AccountSettingsActivity {
        val controller = buildActivity(
            AccountSettingsActivity::class.java,
            Intent(getApplicationContext(), AccountSettingsActivity::class.java)
        )
        return controller.setup().get()
    }

    @Test
    fun onCreate_setsHeaderTitle() {
        val activity = launch()

        val titleView = activity.findViewById<TextView>(R.id.headerTitle)
        // 레이아웃에 포함된 헤더의 텍스트가 "계정 설정"으로 설정되는지 확인
        assertThat(titleView.text.toString(), `is`("계정 설정"))
    }

    @Test
    fun clickingBackIcon_finishesActivity() {
        val activity = launch()

        val backIcon = activity.findViewById<ImageView>(R.id.headerBackIcon)
        backIcon.performClick()

        assertThat(activity.isFinishing, `is`(true))
    }

    @Test
    fun activity_startsWithoutCrash_andViewsExist() {
        val activity = launch()
        // 핵심 뷰들이 바인딩되어 있는지 존재 확인
        val titleView = activity.findViewById<TextView>(R.id.headerTitle)
        val backIcon  = activity.findViewById<ImageView>(R.id.headerBackIcon)

        assertThat(titleView != null, `is`(true))
        assertThat(backIcon  != null, `is`(true))

        // 뒤로가기 한번 더 체크
        backIcon.performClick()
        assertThat(activity.isFinishing, `is`(true))
    }
}
