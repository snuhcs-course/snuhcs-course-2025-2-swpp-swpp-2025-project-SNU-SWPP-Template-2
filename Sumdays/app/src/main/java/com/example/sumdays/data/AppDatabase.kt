package com.example.sumdays.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.sumdays.data.dao.DailyEntryDao

import com.example.sumdays.data.DailyEntry

@Database(
    entities = [DailyEntry::class /*, 다른 Entity들 추가 가능 */],
    version = 3
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyEntryDao(): DailyEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // ✅ DB는 최초 호출 시 자동 생성 (onCreate 불필요)
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sumdays_1.db"
                )
                    .fallbackToDestructiveMigration() // 필요 시만 사용
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
