package com.example.sumdays.daily.memo

import kotlinx.coroutines.flow.Flow

// 데이터베이스 접근을 위한 저장소(Repository) 클래스
class MemoRepository(private val memoDao: MemoDao) {

    // 새로운 메모를 삽입하는 함수
    suspend fun insert(memo: Memo) {
        memoDao.insert(memo)
    }

    // 특정 날짜의 메모를 가져오는 함수
    fun getMemosForDate(date: String): Flow<List<Memo>> {
        return memoDao.getMemosForDate(date)
    }

    // 메모를 수정하는 함수
    suspend fun update(memo: Memo) {
        memoDao.update(memo)
    }

    // 메모 목록을 업데이트
    suspend fun updateAll(memos: List<Memo>) {
        memoDao.updateAll(memos)
    }

    // 메모를 삭제
    suspend fun delete(memo: Memo) {
        memoDao.delete(memo)
    }
}