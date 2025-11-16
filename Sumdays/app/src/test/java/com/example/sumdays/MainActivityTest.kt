package com.example.sumdays

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.example.sumdays.auth.SessionManager
import com.example.sumdays.reminder.ReminderReceiver
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    @Before
    fun setUp() {
        // SessionManager 모킹
        mockkObject(SessionManager)
        every { SessionManager.init(any()) } just Runs
        // ContextCompat.checkSelfPermission 모킹 (SDK 33+ 테스트에서 사용)
        mockkStatic(ContextCompat::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * SDK < 33 : 권한 요청 없이 schedule + 로그인 여부 따라 네비게이션
     */
    @Test
    @Config(sdk = [Build.VERSION_CODES.R]) // 30
    fun onCreate_pre33_loggedIn_schedulesAlarm_and_navigatesCalendar() {
        every { SessionManager.isLoggedIn() } returns true

        val controller = Robolectric.buildActivity(
            MainActivity::class.java,
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        )
        val activity = controller.setup().get()

        // 알림 채널: SDK 30에서는 채널 생성 분기(O+)는 통과했지만 존재 확인은 O+에서만 가능
        // 알람 스케줄 확인
        val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarm = Shadows.shadowOf(alarmManager)
        val scheduled = shadowAlarm.nextScheduledAlarm
        MatcherAssert.assertThat(scheduled != null, CoreMatchers.`is`(true))

        val pi = scheduled!!.operation                // PendingIntent
        val shadowPI = Shadows.shadowOf(pi)           // ShadowPendingIntent
        val targetClass = shadowPI.savedIntent.component?.className

        MatcherAssert.assertThat(targetClass, CoreMatchers.`is`(ReminderReceiver::class.java.name))

        // 네비게이션: CalendarActivity
        val next = Shadows.shadowOf(activity).nextStartedActivity
        MatcherAssert.assertThat(
            next.component?.className,
            CoreMatchers.`is`(CalendarActivity::class.java.name)
        )
        MatcherAssert.assertThat(activity.isFinishing, CoreMatchers.`is`(true))

        // SessionManager.init 호출 확인
        verify(exactly = 1) { SessionManager.init(ApplicationProvider.getApplicationContext()) }
    }

    /**
     * SDK < 33 : 비로그인 → LoginActivity로 이동
     */
    @Test
    @Config(sdk = [Build.VERSION_CODES.R]) // 30
    fun onCreate_pre33_notLoggedIn_schedulesAlarm_and_navigatesLogin() {
        every { SessionManager.isLoggedIn() } returns false

        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarm = Shadows.shadowOf(alarmManager)
        MatcherAssert.assertThat(shadowAlarm.nextScheduledAlarm != null, CoreMatchers.`is`(true))

        val next = Shadows.shadowOf(activity).nextStartedActivity
        MatcherAssert.assertThat(
            next.component?.className,
            CoreMatchers.`is`(LoginActivity::class.java.name)
        )
        MatcherAssert.assertThat(activity.isFinishing, CoreMatchers.`is`(true))

        verify(exactly = 1) { SessionManager.init(ApplicationProvider.getApplicationContext()) }
    }

    /**
     * SDK 33+ : 권한 GRANTED → schedule + 로그인 여부 따라 네비게이션
     */
    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE]) // 34
    fun onCreate_33plus_permissionGranted_loggedIn_flows() {
        // 권한 허용으로 가정
        every {
            ContextCompat.checkSelfPermission(any(), Manifest.permission.POST_NOTIFICATIONS)
        } returns PackageManager.PERMISSION_GRANTED

        every { SessionManager.isLoggedIn() } returns true

        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        // 알림 채널 생성 확인 (O+ 이므로 가능)
        val nm = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = nm.getNotificationChannel("daily_reminder")
        MatcherAssert.assertThat(channel != null, CoreMatchers.`is`(true))
        MatcherAssert.assertThat(channel!!.name.toString(), CoreMatchers.`is`("Daily Reminder"))

        // 알람 스케줄 확인
        val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarm = Shadows.shadowOf(alarmManager)
        MatcherAssert.assertThat(shadowAlarm.nextScheduledAlarm != null, CoreMatchers.`is`(true))

        // 캘린더로 이동
        val next = Shadows.shadowOf(activity).nextStartedActivity
        MatcherAssert.assertThat(
            next.component?.className,
            CoreMatchers.`is`(CalendarActivity::class.java.name)
        )
        MatcherAssert.assertThat(activity.isFinishing, CoreMatchers.`is`(true))

        verify(exactly = 1) { SessionManager.init(ApplicationProvider.getApplicationContext()) }
    }

    /**
     * SDK 33+ : 권한 GRANTED + 비로그인 → LoginActivity로 이동
     */
    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE]) // 34
    fun onCreate_33plus_permissionGranted_notLoggedIn_flows() {
        every {
            ContextCompat.checkSelfPermission(any(), Manifest.permission.POST_NOTIFICATIONS)
        } returns PackageManager.PERMISSION_GRANTED

        every { SessionManager.isLoggedIn() } returns false

        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        // 알림 채널 존재
        val nm = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = nm.getNotificationChannel("daily_reminder")
        MatcherAssert.assertThat(channel != null, CoreMatchers.`is`(true))

        // 알람 스케줄
        val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarm = Shadows.shadowOf(alarmManager)
        MatcherAssert.assertThat(shadowAlarm.nextScheduledAlarm != null, CoreMatchers.`is`(true))

        // 로그인 화면 이동
        val next = Shadows.shadowOf(activity).nextStartedActivity
        MatcherAssert.assertThat(
            next.component?.className,
            CoreMatchers.`is`(LoginActivity::class.java.name)
        )
        MatcherAssert.assertThat(activity.isFinishing, CoreMatchers.`is`(true))

        verify(exactly = 1) { SessionManager.init(ApplicationProvider.getApplicationContext()) }
    }
}