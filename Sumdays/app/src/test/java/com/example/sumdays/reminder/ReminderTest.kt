package com.example.sumdays.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import androidx.test.core.app.ApplicationProvider
import com.example.sumdays.TestApplication
import com.example.sumdays.data.dao.MemoDao
import com.example.sumdays.data.AppDatabase
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowNotificationManager
import org.threeten.bp.LocalDate


// Reminder 통합 테스트
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33],application = TestApplication::class)
class ReminderIntegrationTest {

    private lateinit var context: Context
    private lateinit var shadowAlarmManager: ShadowAlarmManager
    private lateinit var shadowNotificationManager: ShadowNotificationManager
    private val mockDb = mockk<AppDatabase>()
    private val mockDao = mockk<MemoDao>(relaxed = true)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val app = context as Application

        // 알림 권한 강제 부여
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        // LocalDate Static Mocking
        mockkStatic(LocalDate::class)
        val fixedDate = LocalDate.of(2023, 11, 22) // 테스트용 고정 날짜
        every { LocalDate.now() } returns fixedDate

        // Robolectric Shadows 초기화
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = Shadows.shadowOf(alarmManager)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = Shadows.shadowOf(notificationManager)

        // DB Mocking (ReplyReceiver용)
        mockkObject(AppDatabase.Companion)
        every { AppDatabase.getDatabase(any()) } returns mockDb
        every { mockDb.memoDao() } returns mockDao
    }

    @After
    fun tearDown() {
        unmockkAll()
        unmockkStatic(LocalDate::class)
    }

    // ==========================================
    // 1. ReminderPrefs 테스트
    // ==========================================
    @Test
    fun `ReminderPrefs saves and retrieves data correctly`() {
        val prefs = ReminderPrefs(context)

        prefs.setMasterSwitch(true)
        assertTrue(prefs.isMasterOn())

        prefs.setMasterSwitch(false)
        assertFalse(prefs.isMasterOn())

        val times = listOf("09:00", "20:30")
        prefs.setAlarmTimes(times)

        val retrieved = prefs.getAlarmTimes()
        assertEquals(2, retrieved.size)
        assertEquals("09:00", retrieved[0])
        assertEquals("20:30", retrieved[1])
    }

    // ==========================================
    // 2. ReminderScheduler 테스트
    // ==========================================
    @Test
    fun `Scheduler registers alarms correctly`() {
        val prefs = ReminderPrefs(context)
        prefs.setMasterSwitch(true)
        prefs.setAlarmTimes(listOf("08:00", "22:00"))

        ReminderScheduler.scheduleAllReminders(context)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals(2, scheduledAlarms.size)
        assertNotNull(scheduledAlarms[0])
    }

    @Test
    fun `Scheduler cancels all alarms when master switch is OFF`() {
        val prefs = ReminderPrefs(context)
        prefs.setMasterSwitch(false)
        prefs.setAlarmTimes(listOf("08:00"))

        ReminderScheduler.scheduleAllReminders(context)

        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)
    }

    // ==========================================
    // 3. ReminderReceiver 테스트
    // ==========================================
    @Test
    fun `ReminderReceiver posts notification on receive`() {
        val receiver = ReminderReceiver()
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderScheduler.EXTRA_HOUR, 9)
            putExtra(ReminderScheduler.EXTRA_MINUTE, 0)
            putExtra(ReminderScheduler.EXTRA_REQUEST_CODE, 1)
        }

        // 리시버 실행
        receiver.onReceive(context, intent)

        // 알림 확인
        val notifications = shadowNotificationManager.allNotifications
        assertEquals("알림이 생성되어야 합니다", 1, notifications.size)

        val notification = notifications[0]
        assertEquals("Sumdays", notification.extras.getString("android.title"))
        assertEquals("오늘의 하루, 잠깐만 메모해볼까요?", notification.extras.getString("android.text"))
    }

    // ==========================================
    // 4. ReminderReplyReceiver 테스트
    // ==========================================
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `ReplyReceiver saves memo to database`() {
        val receiver = ReminderReplyReceiver()
        val intent = Intent().apply {
            val remoteInputResults = android.os.Bundle()
            remoteInputResults.putCharSequence(ReminderReceiver.KEY_TEXT_REPLY, "오늘 점심 맛있었다")
            RemoteInput.addResultsToIntent(
                arrayOf(RemoteInput.Builder(ReminderReceiver.KEY_TEXT_REPLY).build()),
                this,
                remoteInputResults
            )
        }

        coEvery { mockDao.getMemoCountByDate(any()) } returns 0
        coJustRun { mockDao.insert(any()) }
        receiver.onReceive(context, intent)
        coVerify(timeout = 2000) {
            mockDao.insert(match { memo ->
                memo.content == "오늘 점심 맛있었다" &&
                        memo.type == "text"
            })
        }
    }
}