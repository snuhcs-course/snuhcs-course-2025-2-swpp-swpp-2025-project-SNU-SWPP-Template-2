package com.example.sumdays.data.style

// com.example.sumdays.data.style.StyleDatabase.kt

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [UserStyle::class], version = 2, exportSchema = false)
@TypeConverters(StyleConverters::class) // 타입 변환기 연결
abstract class StyleDatabase : RoomDatabase() {

    abstract fun userStyleDao(): UserStyleDao

    // 싱글톤 패턴
    companion object {
        @Volatile
        private var INSTANCE: StyleDatabase? = null

        fun getDatabase(context: Context): StyleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StyleDatabase::class.java,
                    "user_style_database" // DB 파일 이름
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}