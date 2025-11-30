package com.example.mindlog.features.journal.presentation.list

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.features.journal.domain.usecase.SearchByKeywordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalKeywordSearchViewModel @Inject constructor(
    private val searchByKeywordUseCase: SearchByKeywordUseCase
) : ViewModel() {

    private val _journals = MutableLiveData<List<JournalEntry>>(emptyList())
    val journals: MutableLiveData<List<JournalEntry>> = _journals
    val isLoading = MutableLiveData(false)
    val errorMessage = MutableLiveData<String?>()

    private var nextCursor: Int? = null
    var isLastPage = false
    private var currentKeyword: String? = null

    companion object {
        private const val PAGE_SIZE = 10
    }

    fun loadJournals(keyword: String) {
        if (isLoading.value == true || keyword.isBlank()) return
        currentKeyword = keyword

        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            _journals.value = emptyList() // 새 검색이므로 목록 초기화
            nextCursor = null
            isLastPage = false

            try {
                val response = searchByKeywordUseCase(keyword, PAGE_SIZE, null)
                _journals.value = response.items
                nextCursor = response.nextCursor
                isLastPage = nextCursor == null
            } catch (e: Exception) {
                errorMessage.value = "데이터 로딩 중 오류 발생: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun loadMoreJournals() {
        val keyword = currentKeyword
        if (isLoading.value == true || isLastPage || keyword.isNullOrBlank()) return

        viewModelScope.launch {
            isLoading.value = true
            try {
                val response = searchByKeywordUseCase(keyword, PAGE_SIZE, nextCursor)
                _journals.value = (_journals.value ?: emptyList()) + response.items
                nextCursor = response.nextCursor
                isLastPage = nextCursor == null
            } catch (e: Exception) {
                errorMessage.value = "데이터 추가 로딩 중 오류 발생: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }
}
