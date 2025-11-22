package com.example.mindlog.features.selfaware.presentation.viewmodel

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.common.Time
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.selfaware.domain.model.CategoryScore
import com.example.mindlog.features.selfaware.domain.model.ValueScore
import com.example.mindlog.features.selfaware.domain.usecase.GetTodayQAUseCase
import com.example.mindlog.features.selfaware.domain.usecase.SubmitAnswerUseCase
import com.example.mindlog.features.selfaware.domain.usecase.GetValueMapUseCase
import com.example.mindlog.features.selfaware.domain.usecase.GetTopValueScoresUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import javax.inject.Inject


@HiltViewModel
class SelfAwareViewModel @Inject constructor(
    private val getTodayQAUseCase: GetTodayQAUseCase,
    private val submitAnswerUseCase: SubmitAnswerUseCase,
    private val getValueMapUseCase: GetValueMapUseCase,
    private val getTopValueScoresUseCase: GetTopValueScoresUseCase,
    private val dispatcher: DispatcherProvider
) : ViewModel() {

    data class UiState(
        val date: LocalDate = Time.todayKST(),
        val isAnsweredToday: Boolean = false,
        val questionId: Int? = null,
        val questionText: String? = null,
        val answerText: String = "",

        val valueMap: List<CategoryScore> = emptyList(),
        val topValueScores: List<ValueScore> = emptyList(),

        val isLoadingQuestion: Boolean = true,
        val isSubmitting: Boolean = false,
        val isLoading: Boolean = true,
        val showCompletionOverlay: Boolean = false,

        val isQuestionError: Boolean = false,
        val questionErrorMessage: String? = null,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private var pollJob: Job? = null

    fun load() = viewModelScope.launch(dispatcher.io) {
        // 화면 전체 스피너는 해제, 질문 섹션만 로딩 상태 유지
        val date = _state.value.date
        _state.update {
            it.copy(
                isLoadingQuestion = true,
                isLoading = true,
                showCompletionOverlay = false,
                isQuestionError = false,
                questionErrorMessage = null,
                error = null
            )
        }

        var errorMessage: String? = null
        pollJob?.cancel()

        try {
            coroutineScope {
                val valueMap = async { getValueMapUseCase(Unit) }
                val topValues = async { getTopValueScoresUseCase(Unit) }
                val todayQA = async { getTodayQAUseCase(date) }

                // 결과 병합
                val valueMapRes = valueMap.await()
                val topRes = topValues.await()
                _state.update {
                    it.copy(
                        valueMap = (valueMapRes as? Result.Success)?.data?.categoryScores ?: emptyList(),
                        topValueScores = (topRes as? Result.Success)?.data?.valueScores ?: emptyList(),
                        isLoading = false,
                        // valueMap / topValues 관련 에러만 우선 반영
                        error = listOfNotNull(
                            (valueMapRes as? Result.Error)?.message,
                            (topRes as? Result.Error)?.message,
                        ).firstOrNull()
                    )
                }

                when (val todayQARes = todayQA.await()) {
                    is Result.Success -> {
                        val qaRes = todayQARes.data
                        if (qaRes?.question != null) {
                            _state.update {
                                it.copy(
                                    questionId = qaRes.question.id,
                                    questionText = qaRes.question.text,
                                    isAnsweredToday = qaRes.answer != null,
                                    isLoadingQuestion = false,
                                    showCompletionOverlay = qaRes.answer != null,
                                    isQuestionError = false,
                                    questionErrorMessage = null
                                )
                            }
                        } else startPolling()
                    }
                    is Result.Error -> {
                        val code = todayQARes.code
                        if (code == null) {
                            startPolling()
                        } else {
                            _state.update {
                                it.copy(
                                    questionId = null,
                                    questionText = "질문 생성에 문제가 있습니다. 잠시 후 다시 시도해주세요.",
                                    isLoadingQuestion = false,
                                    isAnsweredToday = false,
                                    showCompletionOverlay = false,
                                    isQuestionError = true,
                                    questionErrorMessage = "질문 생성에 문제가 있습니다. 잠시 후 다시 시도해주세요.",
                                    error = todayQARes.message
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            _state.update { it.copy(isLoadingQuestion = false, error = e.message ?: "self aware data loading error") }
        }
    }

    /** polling 로직 분리 */
    private fun startPolling(
        maxWaitMs: Long = 60_000L,
        initialDelayMs: Long = 1_500L,
        intervalMs: Long = 2_000L,
        maxIntervalMs: Long = 6_000L
    ) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch(dispatcher.io) {
            // 폴링 시작: 질문 섹션만 로딩 유지
            _state.update {
                it.copy(
                    isLoadingQuestion = true,
                    showCompletionOverlay = false,
                    isQuestionError = false,
                    questionErrorMessage = null,
                    error = null
                )
            }

            try {
                withTimeout(maxWaitMs) {
                    delay(initialDelayMs)
                    var next = intervalMs

                    while (isActive) {
                        val date = _state.value.date
                        when (val res = getTodayQAUseCase(date)) {
                            is Result.Success -> {
                                val qa = res.data
                                if (qa?.question != null) {
                                    _state.update {
                                        it.copy(
                                            questionId = qa.question.id,
                                            questionText = qa.question.text,
                                            isAnsweredToday = qa.answer != null,
                                            isLoadingQuestion = false,
                                            showCompletionOverlay = qa.answer != null,
                                            isQuestionError = false,
                                            questionErrorMessage = null,
                                            error = null
                                        )
                                    }
                                    return@withTimeout
                                }
                            }
                            is Result.Error -> {
                                val code = res.code
                                if (code != null) {
                                    _state.update {
                                        it.copy(
                                            questionId = null,
                                            questionText = "질문 생성에 문제가 있습니다. 잠시 후 다시 시도해주세요.",
                                            isLoadingQuestion = false,
                                            isAnsweredToday = false,
                                            showCompletionOverlay = false,
                                            isQuestionError = true,
                                            questionErrorMessage = "질문 생성에 문제가 있습니다. 잠시 후 다시 시도해주세요.",
                                            error = res.message
                                        )
                                    }
                                    return@withTimeout
                                }
                            }
                        }

                        delay(next)
                        next = (next * 1.5f).toLong().coerceAtMost(maxIntervalMs) // 지수 백오프
                    }
                }
            } catch (_: TimeoutCancellationException) {
                _state.update {
                    it.copy(
                        isLoadingQuestion = false,
                        isQuestionError = true,
                        questionErrorMessage = "질문 생성이 지연되고 있어요. 잠시 후 다시 시도해 주세요.",
                        error = "질문 생성이 지연되고 있어요. 잠시 후 다시 시도해 주세요."
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }

    fun updateAnswerText(text: String) = _state.update { it.copy(answerText = text) }

    fun submit() = viewModelScope.launch(dispatcher.io) {
        val s = _state.value

        if (s.questionId == null) {
            _state.update { it.copy(error = "질문을 불러오는 중 입니다.") }
            return@launch
        }
        if (s.answerText.isBlank()) {
            _state.update { it.copy(error = "답변을 입력해주세요.") }
            return@launch
        }

        _state.update {
            it.copy(
                isSubmitting = true,
                showCompletionOverlay = true
            )
        }

        when (val res = submitAnswerUseCase(s.questionId, s.answerText)) {
            is Result.Success -> {
                pollJob?.cancel()
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        isAnsweredToday = true,
                        isLoading = false,
                        isLoadingQuestion = false,
                        showCompletionOverlay = true,
                        answerText = ""
                    )
                }
            }

            is Result.Error -> {
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        showCompletionOverlay = false,
                        error = res.message
                    )
                }
            }
        }
    }
}