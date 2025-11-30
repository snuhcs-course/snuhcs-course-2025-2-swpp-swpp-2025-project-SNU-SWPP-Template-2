package com.example.mindlog.features.selfaware.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.selfaware.domain.model.QAItem
import com.example.mindlog.features.selfaware.domain.usecase.GetQAHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class SelfAwareHistoryViewModel @Inject constructor(
    private val getHistoryUseCase: GetQAHistoryUseCase,
    private val dispatcher: DispatcherProvider
) : ViewModel() {

    data class UiState(
        val nextCursor: Int? = null,
        val limit: Int = 10,
        val items: List<QAItem> = emptyList(),
        val isRefreshing: Boolean = false,  // 최초/새로고침 로딩
        val isLoading: Boolean = false,     // 다음 페이지 로딩
        val isEnd: Boolean = false,
        val error: String? = null
    )
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private var loadMutex = kotlinx.coroutines.sync.Mutex()
    private var refreshJob: Job? = null
    private var loadJob: Job? = null

    fun refresh() {
        if (refreshJob?.isActive == true) return
        loadJob?.cancel()

        refreshJob = viewModelScope.launch(dispatcher.io) {
            loadMutex.withLock {
                // refresh
                _state.update { it.copy(nextCursor = null, isRefreshing = true, isEnd = false, error = null) }

                val s = _state.value
                when (val res = getHistoryUseCase(limit = s.limit, cursor = s.nextCursor)) {
                    is Result.Success -> {
                        val page = res.data
                        val items = page.items
                        val nextCursor = page.cursor

                        _state.update {
                            it.copy(
                                isRefreshing = false,
                                items = items,
                                nextCursor = nextCursor,
                                isEnd = items.isEmpty() || nextCursor == null,
                                error = null
                            )
                        }
                    }
                    is Result.Error -> {
                        _state.update { it.copy(isRefreshing = false, error = res.message) }
                    }
                }
            }
        }
    }

    fun loadNext()  {
        val s = _state.value
        if (s.isRefreshing || s.isLoading || s.isEnd || s.nextCursor == null) return
        if (loadJob?.isActive == true) return

        loadJob = viewModelScope.launch(dispatcher.io) {
            loadMutex.withLock {
                _state.update { it.copy(isLoading = true, error = null) }

                val s = _state.value
                val nextCursor = s.nextCursor ?: run {
                    _state.update { it.copy(isLoading = false, isEnd = true) }
                    return@withLock
                }

                when (val res = getHistoryUseCase(limit = s.limit, cursor = nextCursor)) {
                    is Result.Success -> {
                        val page = res.data
                        val more = page.items
                        val nextCursor = page.cursor

                        _state.update {
                            val merged = (it.items + more)
                            it.copy (
                                items = merged,
                                nextCursor = nextCursor,
                                isLoading = false,
                                isEnd = more.isEmpty() || nextCursor == null,
                                error = null
                            )
                        }
                    }
                    is Result.Error -> {
                        _state.update { it.copy(isLoading = false, error = res.message) }
                    }
                }
            }
        }
    }

}