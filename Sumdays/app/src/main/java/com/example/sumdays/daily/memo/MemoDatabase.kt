package com.example.sumdays.daily.memo

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Room 데이터베이스 추상 클래스
@Database(entities = [Memo::class], version = 2, exportSchema = false)
abstract class MemoDatabase : RoomDatabase() {

    // MemoDao를 사용하여 데이터베이스에 접근
    abstract fun memoDao(): MemoDao

    companion object {
        // 싱글톤(Singleton) 패턴을 사용하여 하나의 데이터베이스 인스턴스만 유지
        @Volatile
        private var INSTANCE: MemoDatabase? = null

        // 데이터베이스 인스턴스를 가져오는 함수
        fun getDatabase(context: Context): MemoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MemoDatabase::class.java,
                    "memo_database"
                )
                    .fallbackToDestructiveMigration() // 스키마 변경 시 데이터베이스 재생성
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}