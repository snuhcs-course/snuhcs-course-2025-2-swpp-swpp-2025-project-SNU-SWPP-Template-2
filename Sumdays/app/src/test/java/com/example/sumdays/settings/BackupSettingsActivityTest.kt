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
class BackupSettingsActivityTest {

    private fun launch(): BackupSettingsActivity {
        val controller = buildActivity(
            BackupSettingsActivity::class.java,
            Intent(getApplicationContext(), BackupSettingsActivity::class.java)
        )
        return controller.setup().get()
    }

    @Test
    fun onCreate_setsHeaderTitle() {
        val activity = launch()
        val titleView = activity.findViewById<TextView>(R.id.headerTitle)
        // 액티비티 코드에서 "알림 설정"으로 설정하고 있음
        assertThat(titleView.text.toString(), `is`("알림 설정"))
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
