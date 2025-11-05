package com.example.sumdays.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.example.sumdays.R
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.example.sumdays.daily.memo.Memo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import android.util.Log
import com.example.sumdays.daily.memo.MemoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
class ReminderReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val replyText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(ReminderReceiver.KEY_TEXT_REPLY).toString()
        addMemo(context, replyText)

        // 바디 클릭 -> 앱 실행 및 종료
        val activityIntent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        //  “메모 추가” 액션 생성
        val replyIntent = Intent(context, ReminderReplyReceiver::class.java)
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val remoteInput = RemoteInput.Builder(ReminderReceiver.KEY_TEXT_REPLY)
            .setLabel("메모를 입력하세요")
            .build()

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_edit,
            "메모 추가",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .build()

        val updatedNotification = NotificationCompat.Builder(context, ReminderReceiver.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Sumdays")
            .setContentText("오늘의 하루, 잠깐만 메모해볼까요?")
            .setContentIntent(pendingIntent) // 본문 클릭 시 앱 실행
            .addAction(replyAction)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // ✅ 본문 클릭 시 닫힘
            .build()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        NotificationManagerCompat.from(context).notify(1001, updatedNotification)
    }
    private fun addMemo(context: Context, memoText : String) {

        val db = MemoDatabase.getDatabase(context)
        val memoDao = db.memoDao()
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().time)
        val date = LocalDate.now().toString()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 🔹 현재 날짜의 메모 개수를 가져와 order 계산
                val count = memoDao.getMemoCountByDate(date)

                val newMemo = Memo(
                    id = 0,
                    content = memoText,
                    timestamp = currentTime,
                    date = date,
                    order = count,
                    type = "text"
                )
                memoDao.insert(newMemo)
                Log.d("ReminderReplyReceiver", "메모 추가 완료: ${newMemo.content}")

            } catch (e: Exception) {
                Log.e("ReminderReplyReceiver", "메모 추가 실패", e)
            }
        }
    }
}
