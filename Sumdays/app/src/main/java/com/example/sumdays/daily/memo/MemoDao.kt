package com.example.sumdays.daily.memo

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Room의 DAO(Data Access Object) 인터페이스
@Dao
interface MemoDao {
    // 새로운 메모를 데이터베이스에 삽입
    @Insert
    suspend fun insert(memo: Memo)

    // 특정 날짜의 메모만 조회
    // Flow를 반환하여 데이터 변경 시 실시간으로 알림을 받음
    @Query("SELECT * FROM memo_table WHERE date = :date ORDER BY memo_order ASC")
    fun getMemosForDate(date: String): Flow<List<Memo>>

    // 메모를 수정
    @Update
    suspend fun update(memo: Memo)

    // 여러 메모의 순서를 한 번에 업데이트
    @Update
    suspend fun updateAll(memos: List<Memo>)

    // 특정 메모를 삭제
    @Delete
    suspend fun delete(memo: Memo)

    // reminder에서 memo를 추가할 떄 order 알기 위해 사용
    @Query("SELECT COUNT(*) FROM memo_table WHERE date = :date")
    suspend fun getMemoCountByDate(date: String): Int
}