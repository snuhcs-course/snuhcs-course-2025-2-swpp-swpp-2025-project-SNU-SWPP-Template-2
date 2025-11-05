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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class DiaryStyleSettingsActivityTest {

    private fun launch(): DiaryStyleSettingsActivity {
        val controller = buildActivity(
            DiaryStyleSettingsActivity::class.java,
            Intent(getApplicationContext(), DiaryStyleSettingsActivity::class.java)
        )
        return controller.setup().get()
    }

    @Test
    fun onCreate_setsHeaderTitle() {
        val activity = launch()
        val titleView = activity.findViewById<TextView>(R.id.headerTitle)
        assertThat(titleView.text.toString(), `is`("일기 생성 스타일 설정"))
    }

    @Test
    fun clickingBackIcon_finishesActivity() {
        val activity = launch()
        val backIcon = activity.findViewById<ImageView>(R.id.headerBackIcon)

        backIcon.performClick()

        assertThat(activity.isFinishing, `is`(true))
    }

    @Test
    fun views_exist_and_activity_doesNotCrash() {
        val activity = launch()
        val titleView = activity.findViewById<TextView>(R.id.headerTitle)
        val backIcon  = activity.findViewById<ImageView>(R.id.headerBackIcon)

        assertThat(titleView != null, `is`(true))
        assertThat(backIcon  != null, `is`(true))
    }
}
