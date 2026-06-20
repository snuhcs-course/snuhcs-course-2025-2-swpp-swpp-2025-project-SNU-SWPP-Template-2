package com.example.sumdays.settings

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.sumdays.TestApplication
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.intArrayOf


@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [Build.VERSION_CODES.S],
    application = TestApplication::class,
    manifest = Config.NONE
)
class NotificationSettingsActivityCheckPermissionTest {

    @Test
    fun checkExactAlarmPermission_falseBranch() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val act = Robolectric.buildActivity(NotificationSettingsActivity::class.java)
            .setup().get()

        val method = NotificationSettingsActivity::class.java
            .getDeclaredMethod("checkExactAlarmPermission", Context::class.java)
        method.isAccessible = true

        // 결과는 false가 되어야 한다 (AlarmManager.canScheduleExactAlarms == false)
        val result = method.invoke(act, context) as Boolean
        assertFalse(result)
    }
}