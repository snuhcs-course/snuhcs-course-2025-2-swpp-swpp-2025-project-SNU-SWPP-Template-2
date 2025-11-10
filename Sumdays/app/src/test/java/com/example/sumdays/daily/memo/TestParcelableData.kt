package com.example.sumdays.daily.memo

import kotlinx.android.parcel.Parcelize

class TestParcelableData {
    // src/test/java/com/example/sumdays/statistics/TestParcelableData.kt (혹은 공통 파일)에 추가

    @Parcelize
    data class Memo(
        val id: Int = 0,
        val content: String,
        val timestamp: String,
        val date: String,
        val order: Int
    ) : android.os.Parcelable // ⭐️ Parcelable 추가
}