package com.example.sumdays.data.style

// com.example.sumdays.data.UserStyleDao.kt (수정됨)

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface UserStyleDao {

    // 1. 새 스타일 저장 (AI 추출 성공 시)
    @Insert
    suspend fun insertStyle(style: UserStyle): Long

    // 2. 모든 스타일 목록 조회 (SettingsActivity에서 사용)
    // LiveData<List<UserStyle>>을 반환하여 단일 사용자에게 속한 모든 스타일을 표시
    @Query("SELECT * FROM user_style ORDER BY styleId DESC")
    fun getAllStyles(): LiveData<List<UserStyle>>

    // 3. 스타일 삭제
    @Delete
    suspend fun deleteStyle(style: UserStyle)

    // 4. 활성 스타일 조회 (UserStats의 activeStyleId를 통해 조회)
    @Query("SELECT * FROM user_style WHERE styleId = :styleId")
    suspend fun getStyleById(styleId: Long): UserStyle?

    // 5. 모든 스타일 삭제 (계정 탈퇴 시 등 - userId 없이 전체 삭제)
    @Query("DELETE FROM user_style")
    suspend fun deleteAllStyles()
}