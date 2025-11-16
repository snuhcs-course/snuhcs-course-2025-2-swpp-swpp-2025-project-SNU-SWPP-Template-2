package com.example.sumdays.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.sumdays.data.dao.*
import androidx.room.TypeConverters
import com.example.sumdays.data.converter.WeekSummaryConverter

@Database(
    entities = [
        DailyEntry::class,
        WeekSummaryEntity::class // ✅ WeekSummary 테이블 추가
    ],
    version = 4, // ⚠️ 기존 버전보다 +1 해야 함 (ex. 3 → 4)
    exportSchema = false
)
@TypeConverters(WeekSummaryConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyEntryDao(): DailyEntryDao
    abstract fun weekSummaryDao(): WeekSummaryDao

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
