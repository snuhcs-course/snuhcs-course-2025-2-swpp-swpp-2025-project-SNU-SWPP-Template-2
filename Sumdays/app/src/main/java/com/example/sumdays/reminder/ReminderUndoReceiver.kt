package com.example.sumdays.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.sumdays.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 알림에서 "메모 저장됨" 확인 후 "취소"를 눌렀을 때 호출됩니다.
 * 방금 저장된 메모를 soft-delete 하고 확인 알림을 제거합니다.
 */
class ReminderUndoReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_MEMO_ID = "com.example.sumdays.reminder.EXTRA_MEMO_ID"
        const val EXTRA_CONFIRM_ID = "com.example.sumdays.reminder.EXTRA_CONFIRM_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val memoId = intent.getIntExtra(EXTRA_MEMO_ID, -1)
        val confirmId = intent.getIntExtra(EXTRA_CONFIRM_ID, -1)

        // 1. 확인 알림 제거
        if (confirmId != -1) {
            NotificationManagerCompat.from(context).cancel(confirmId)
        }

        // 2. 메모 id가 없으면 삭제 불가
        if (memoId == -1) {
            Log.e("ReminderUndoReceiver", "memoId가 없어 취소를 수행하지 못했습니다.")
            return
        }

        // 3. DB soft-delete (완료까지 수신 유지)
        val pendingResult = goAsync()
        val memoDao = AppDatabase.getDatabase(context).memoDao()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                memoDao.markAsDeleted(memoId)
                Log.d("ReminderUndoReceiver", "메모 취소(삭제) 완료: id=$memoId")
            } catch (e: Exception) {
                Log.e("ReminderUndoReceiver", "메모 취소 실패", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
