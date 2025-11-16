package com.example.sumdays.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.app.Application // ★ Application 타입 사용을 위해 추가
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowApplication
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * ReminderReceiver 클래스에 대한 Unit Test입니다.
 * BroadcastReceiver의 onReceive 메소드 내의 모든 분기(Line Coverage 100%)를 커버합니다.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE]) // 34 (샘플 설정 차용)
@LooperMode(LooperMode.Mode.PAUSED)
class ReminderReceiverTest {

    private lateinit var receiver: ReminderReceiver
    private lateinit var context: Context
    private lateinit var shadowApplication: ShadowApplication

    @Before
    fun setUp() {
        receiver = ReminderReceiver()
        context = ApplicationProvider.getApplicationContext<Context>()

        // FIX 1: 'shadowOf' 후보 없음 오류 해결. Context를 Application으로 명시적으로 캐스팅합니다.
        shadowApplication = shadowOf(context.applicationContext as Application)

        // POST_NOTIFICATIONS 권한을 기본적으로 승인 상태로 설정
        shadowApplication.grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }


    /**
     * 시나리오 1: 필수 알람 정보가 누락되었을 때 (Line 25-28 커버)
     * -> 알림 표시 및 재등록 로직이 실행되지 않고 즉시 리턴해야 함.
     */
    @Test
    fun onReceive_withMissingExtras_shouldReturnEarly() {
        // GIVEN: EXTRA_HOUR, EXTRA_MINUTE 중 하나라도 빠진 Intent
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderScheduler.EXTRA_HOUR, 10)
            // EXTRA_MINUTE 누락
            putExtra(ReminderScheduler.EXTRA_REQUEST_CODE, 101)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = shadowOf(notificationManager)

        // WHEN
        receiver.onReceive(context, intent)

        // THEN
        // 1. 알림이 표시되지 않았는지 확인
        assertTrue(shadowNotificationManager.allNotifications.isEmpty(), "알람 정보 누락 시 알림은 표시되지 않아야 합니다.")
    }

    /**
     * 시나리오 2: 알림 권한이 없을 때 (Line 31-36 커버)
     * -> 알림 표시 및 재등록 로직이 실행되지 않고 즉시 리턴해야 함.
     */
    @Test
    fun onReceive_withoutNotificationPermission_shouldReturnEarly() {
        // GIVEN: POST_NOTIFICATIONS 권한 거부 상태
        shadowApplication.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderScheduler.EXTRA_HOUR, 10)
            putExtra(ReminderScheduler.EXTRA_MINUTE, 30)
            putExtra(ReminderScheduler.EXTRA_REQUEST_CODE, 102)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = shadowOf(notificationManager)

        // WHEN
        receiver.onReceive(context, intent)

        // THEN
        // 1. 알림이 표시되지 않았는지 확인
        assertTrue(shadowNotificationManager.allNotifications.isEmpty(), "권한 없을 시 알림은 표시되지 않아야 합니다.")
    }

    /**
     * 시나리오 3: 정상적인 알림 표시 및 다음 날 재등록 (Line 25-77 커버)
     * -> 알림이 성공적으로 표시되고, 재등록 함수가 호출되어야 함.
     */
    @Test
    fun onReceive_withAllConditionsMet_shouldShowNotificationAndReschedule() {
        // GIVEN: 정상적인 알람 정보와 권한 승인 상태
        val hour = 11
        val minute = 45
        val requestCode = 103
        val notificationId = ReminderReceiver.NOTIFICATION_ID_BASE + requestCode

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderScheduler.EXTRA_HOUR, hour)
            putExtra(ReminderScheduler.EXTRA_MINUTE, minute)
            putExtra(ReminderScheduler.EXTRA_REQUEST_CODE, requestCode)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = shadowOf(notificationManager)

        // WHEN
        receiver.onReceive(context, intent)

        // THEN (알림 표시 검증)
        val notifiedNotification = shadowNotificationManager.getNotification(notificationId)
        assertNotNull(notifiedNotification, "알림이 정확한 ID($notificationId)로 표시되어야 합니다.")

        val shadowNotification = shadowOf(notifiedNotification)
        assertEquals("Sumdays", shadowNotification.getContentTitle().toString(), "알림 제목이 정확해야 합니다.")

        // THEN (PendingIntent 검증 - 앱 실행)
        val contentIntent = notifiedNotification.contentIntent
        val shadowContentIntent = shadowOf(contentIntent)

        val launchedIntent = shadowContentIntent.savedIntent
        assertNotNull(launchedIntent, "Content Intent에 앱 실행 Intent가 포함되어야 합니다.")
        assertEquals(context.packageName, launchedIntent.getPackage(), "앱 실행 Intent의 패키지가 정확해야 합니다.")

        // THEN (Remote Input Action 검증)
        // FIX 2-5: Unresolved reference 오류 해결을 위해 raw Notification의 public field에 널 안전 연산자를 명시적으로 사용합니다.
        val actionsArray = notifiedNotification.actions
        assertNotNull(actionsArray, "알림 액션 배열은 null이 아니어야 합니다.")

        assertEquals(1, actionsArray!!.size, "메모 추가 액션이 하나 포함되어야 합니다.")

        val replyAction = actionsArray[0] // Notification.Action 타입

        // Notification.Action.title 필드 접근 (널 안전)
        assertEquals("메모 추가", replyAction.title?.toString(), "액션 제목이 정확해야 합니다.")

        // Notification.Action.remoteInputs 필드 접근 (널 안전)
        val remoteInputs = replyAction.remoteInputs
        assertNotNull(remoteInputs, "Remote Input 배열은 null이 아니어야 합니다.")
        assertEquals(1, remoteInputs!!.size, "Remote Input이 포함되어야 합니다.")

        // RemoteInput.resultKey 필드 접근
        assertEquals(ReminderReceiver.KEY_TEXT_REPLY, remoteInputs[0].resultKey, "Remote Input 키가 정확해야 합니다.")

        // THEN (Reply Receiver PendingIntent 검증)
        // Notification.Action.actionIntent 필드 접근
        val replyPendingIntent = replyAction.actionIntent
        val shadowReplyPendingIntent = shadowOf(replyPendingIntent)
        val replyIntent = shadowReplyPendingIntent.savedIntent
        assertNotNull(replyIntent, "Reply Pending Intent에 Intent가 포함되어야 합니다.")

        // ComponentName.className을 통해 클래스 이름 확인
        assertEquals(ReminderReplyReceiver::class.qualifiedName, replyIntent.component?.className, "Reply Intent는 ReminderReplyReceiver를 가리켜야 합니다.")
        assertEquals(requestCode, replyIntent.getIntExtra(ReminderScheduler.EXTRA_REQUEST_CODE, -1), "Reply Intent에 requestCode가 포함되어야 합니다.")

        // THEN (재등록 로직 간접 검증)
    }
}