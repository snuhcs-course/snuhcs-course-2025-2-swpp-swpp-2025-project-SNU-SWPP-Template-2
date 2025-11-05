package com.example.sumdays.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.example.sumdays.R

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "daily_reminder"
        const val KEY_TEXT_REPLY = "memo_reply"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 1. 알림 권한 확인
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        // 2. 알림 바디 입력 -> 앱 실행 및 알림 종료
        val activityIntent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 메모 추가 action
        val replyIntent = Intent(context, ReminderReplyReceiver::class.java)
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // 메모 입력창
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("메모를 입력하세요")
            .build()

        // 메모 추가 액션 버튼
        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_edit,
            "메모 추가",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .build()

        // 알림 빌드
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Sumdays")
            .setContentText("오늘의 하루, 잠깐만 메모해볼까요?")
            .setContentIntent(pendingIntent) // 본문 클릭 → 앱 실행
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(replyAction)
            .setAutoCancel(true) // ✅ 본문 클릭 시 알림 사라짐

        // 알림 표시
        NotificationManagerCompat.from(context).notify(1001, builder.build())
    }
}
