package com.example.sumdays

import android.content.Intent
import android.os.Build
import android.widget.ImageButton
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.example.sumdays.settings.*
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
class SettingsActivityTest {

    private fun launch(): SettingsActivity {
        val controller = buildActivity(
            SettingsActivity::class.java,
            Intent(getApplicationContext(), SettingsActivity::class.java)
        )
        return controller.setup().get()
    }

    // --- Settings blocks ---

    @Test
    fun clickingNotificationBlock_navigatesToNotificationSettings() {
        val activity = launch()
        activity.findViewById<android.view.View>(R.id.notificationBlock).performClick()

        val next = Shadows.shadowOf(activity).nextStartedActivity
        assertThat(next.component?.className, `is`(NotificationSettingsActivity::class.java.name))
    }

    @Test
    fun clickingDiaryStyleBlock_navigatesToDiaryStyleSettings() {
        val activity = launch()
        activity.findViewById<android.view.View>(R.id.diaryStyleBlock).performClick()

        val next = Shadows.shadowOf(activity).nextStartedActivity
        assertThat(next.component?.className, `is`(DiaryStyleSettingsActivity::class.java.name))
    }

    @Test
    fun clickingAccountBlock_navigatesToAccountSettings() {
        val activity = launch()
        activity.findViewById<android.view.View>(R.id.accountBlock).performClick()

        val next = Shadows.shadowOf(activity).nextStartedActivity
        assertThat(next.component?.className, `is`(AccountSettingsActivity::class.java.name))
    }

    // --- Navigation bar buttons ---

    @Test
    fun clickingCalendar_navigatesToCalendarActivity() {
        val activity = launch()
        activity.findViewById<ImageButton>(R.id.btnCalendar).performClick()

        val next = Shadows.shadowOf(activity).nextStartedActivity
        assertThat(next.component?.className, `is`(CalendarActivity::class.java.name))
    }
}
