package com.example.sumdays.data.viewModel

import android.R
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.sumdays.data.AppDatabase
import kotlinx.coroutines.launch
import androidx.lifecycle.LiveData
import com.example.sumdays.data.EmojiData
import kotlinx.coroutines.flow.map

class DailyEntryViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.Companion.getDatabase(application).dailyEntryDao()

    fun getEntry(date: String) =
        dao.getEntry(date).asLiveData()

    fun getMonthlyEmojis(fromDate: String, toDate: String): LiveData<Map<String, Pair<Boolean, String?>>> {
        return dao.getMonthlyEmojis(fromDate, toDate)
            .map { list: List<EmojiData> ->
                list.associate {
                    it.date to Pair(!it.diary.isNullOrEmpty(), it.themeIcon)
                }
            }
            .asLiveData()
    }

    // ✅ 2️⃣ 부분 업데이트 (선택 인자 방식)
    fun updateEntry(
        date: String,
        diary: String? = null,
        keywords: String? = null,
        aiComment: String? = null,
        emotionScore: Double? = null,
        emotionIcon: String? = null,
        themeIcon: String? = null
    ) = viewModelScope.launch {
        dao.updateEntry(
            date = date,
            diary = diary,
            keywords = keywords,
            aiComment = aiComment,
            emotionScore = emotionScore,
            emotionIcon = emotionIcon,
            themeIcon = themeIcon
        )
    }
    fun deleteEntry(date: String) = viewModelScope.launch {
        dao.deleteEntry(date)
    }

}