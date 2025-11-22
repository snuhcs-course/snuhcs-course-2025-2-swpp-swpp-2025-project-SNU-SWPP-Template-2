package com.example.mindlog.features.analysis.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.analysis.domain.model.ComprehensiveAnalysis
import com.example.mindlog.features.analysis.domain.model.PersonalizedAdvice
import com.example.mindlog.features.analysis.domain.model.UserType
import com.example.mindlog.features.analysis.domain.usecase.GetComprehensiveAnalysisUseCase
import com.example.mindlog.features.analysis.domain.usecase.GetPersonalizedAdviceUseCase
import com.example.mindlog.features.analysis.domain.usecase.GetUserTypeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val getUserTypeUseCase: GetUserTypeUseCase,
    private val getComprehensiveAnalysisUseCase: GetComprehensiveAnalysisUseCase,
    private val getPersonalizedAdviceUseCase: GetPersonalizedAdviceUseCase,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,

        val userType: UserType? = null,
        val comprehensiveAnalysis: ComprehensiveAnalysis? = null,
        val advice: PersonalizedAdvice? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun load() {
        viewModelScope.launch(dispatchers.main) {
            _state.update { it.copy(isLoading = true, error = null) }

            coroutineScope {
                val userType = async { getUserTypeUseCase() }
                val comp = async { getComprehensiveAnalysisUseCase() }
                val advice = async { getPersonalizedAdviceUseCase() }

                when (val res = userType.await()) {
                    is Result.Success -> _state.update { it.copy(userType = res.data) }
                    is Result.Error -> setError(res.message)
                }
                when (val res = comp.await()) {
                    is Result.Success -> _state.update { it.copy(comprehensiveAnalysis = res.data) }
                    is Result.Error -> setError(res.message)
                }
                when (val res = advice.await()) {
                    is Result.Success -> _state.update { it.copy(advice = res.data) }
                    is Result.Error -> setError(res.message)
                }
            }

            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun setError(message: String?) {
        _state.update {
            it.copy(
                error = message ?: "Unknown Error",
                isLoading = false
            )
        }
    }
}