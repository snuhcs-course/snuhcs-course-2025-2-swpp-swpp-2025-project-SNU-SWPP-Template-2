package com.example.mindlog.features.statistics.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.statistics.domain.model.Emotion
import com.example.mindlog.features.statistics.domain.model.EmotionRate
import com.example.mindlog.features.statistics.domain.model.EmotionTrend
import com.example.mindlog.features.statistics.domain.model.JournalKeyword
import com.example.mindlog.features.statistics.domain.model.JournalStatistics
import com.example.mindlog.features.statistics.domain.usecase.GetEmotionRatesUseCase
import com.example.mindlog.features.statistics.domain.usecase.GetJournalStatisticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getEmotionRatesUseCase: GetEmotionRatesUseCase,
    private val getJournalStatistics: GetJournalStatisticsUseCase,
    private val dispatcher: DispatcherProvider
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val startDate: LocalDate = LocalDate.now().minusMonths(1),
        val endDate: LocalDate = LocalDate.now(),

        val emotionRatios: List<EmotionRate> = emptyList(),

        // 기간 통계 원본(파생 데이터의 근거)
        val statistics: JournalStatistics? = null,
        // 화면 표시용 파생 값들
        val selectedEmotion: Emotion? = null,
        val emotionTrends: List<EmotionTrend> = emptyList(),
        val emotionEvents: List<String> = emptyList(),
        val journalKeywords: List<JournalKeyword> = emptyList(),

        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    /** 처음 진입/날짜 바뀔 때 호출 */
    fun load() = viewModelScope.launch(dispatcher.io) {
        _state.update { it.copy(isLoading = true, error = null) }

        val start = _state.value.startDate.toString()
        val end = _state.value.endDate.toString()

        // 병렬 로드
        val ratesDeferred = async { getEmotionRatesUseCase(start, end) }
        val statsDeferred = async { getJournalStatistics(start, end) }

        val ratesRes = ratesDeferred.await()
        val statsRes = statsDeferred.await()

        // 에러 메시지 모으기
        val firstError = listOfNotNull(
            (ratesRes as? Result.Error)?.message,
            (statsRes as? Result.Error)?.message
        ).firstOrNull()

        // 성공 데이터 추출
        val ratios = (ratesRes as? Result.Success)?.data ?: emptyList()
        val stats = (statsRes as? Result.Success)?.data

        // 선택 감정 규칙: 1) 이전 선택 유지 → 2) 비율 1위 → 3) 트렌드 첫 감정 → 4) null
        val prevSelected = _state.value.selectedEmotion
        val topFromRatio: Emotion? = ratios.maxByOrNull { it.percentage }?.emotion
        val firstFromTrends: Emotion? = stats?.EmotionTrends?.firstOrNull()?.emotion
        val newSelected: Emotion? = prevSelected
            ?: topFromRatio
            ?: firstFromTrends

        val (events, trends, keywords) = deriveForUI(stats, newSelected)

        _state.update {
            it.copy(
                isLoading = false,
                emotionRatios = ratios,
                statistics = stats,
                selectedEmotion = newSelected,
                emotionEvents = events,
                emotionTrends = trends,
                journalKeywords = keywords,
                error = firstError
            )
        }
    }

    /** 감정 Chip 선택 시 호출 */
    fun setEmotion(emotion: Emotion) = viewModelScope.launch(dispatcher.io) {
        val stats = _state.value.statistics
        val (events, trends, keywords) = deriveForUI(stats, emotion)

        _state.update {
            it.copy(
                selectedEmotion = emotion,
                emotionEvents = events,
                emotionTrends = trends,
                journalKeywords = keywords,
                error = null
            )
        }
    }

    /** 기간 바뀌면 상태만 바꾸고 load 재호출은 화면/호출측에서 */
    fun setDateRange(start: LocalDate, end: LocalDate) {
        _state.update { it.copy(startDate = start, endDate = end) }
    }

    private fun deriveForUI(
        stats: JournalStatistics?,
        selectedEmotion: Emotion?
    ): Triple<List<String>, List<EmotionTrend>, List<JournalKeyword>> {
        if (stats == null) {
            return Triple(emptyList(), emptyList(), emptyList())
        }

        // 감정 이벤트: 선택 감정에 해당하는 이벤트만
        val events = if (selectedEmotion != null) {
            stats.EmotionEvents.firstOrNull { it.emotion == selectedEmotion }?.events ?: emptyList()
        } else emptyList()

        val trends = stats.EmotionTrends
        val keywords = stats.JournalKeywords

        return Triple(events, trends, keywords)
    }
}