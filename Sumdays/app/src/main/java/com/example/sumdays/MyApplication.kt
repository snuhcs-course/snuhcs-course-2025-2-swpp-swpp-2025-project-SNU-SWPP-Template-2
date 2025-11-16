package com.example.sumdays

import android.app.Application
import com.example.sumdays.daily.memo.MemoDatabase
import com.example.sumdays.daily.memo.MemoRepository
//데이터베이스와 저장소(Repository)를 초기화, 싱글톤 패턴을 위한 애플리케이션 클래스
class MyApplication : Application() {
    // Lazy를 사용하여 데이터베이스와 리포지토리를 필요할 때만 초기화
    private val database by lazy { MemoDatabase.getDatabase(this) }
    val repository by lazy { MemoRepository(database.memoDao()) }
}