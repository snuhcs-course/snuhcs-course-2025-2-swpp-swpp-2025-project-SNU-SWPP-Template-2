package com.example.mindlog.features.journal.presentation.list

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.features.journal.domain.usecase.GetJournalByIdUseCase
import com.example.mindlog.features.journal.domain.usecase.GetJournalUseCase
import com.example.mindlog.features.journal.domain.usecase.SearchJournalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class JournalViewModel @Inject constructor(
    private val getJournalsUseCase: GetJournalUseCase,
    private val searchJournalsUseCase: SearchJournalsUseCase,
    private val getJournalByIdUseCase: GetJournalByIdUseCase
) : ViewModel() {

    private val _journals = MutableLiveData<List<JournalEntry>>(emptyList())
    val journals: MutableLiveData<List<JournalEntry>> = _journals
    val isLoading = MutableLiveData(false)
    val errorMessage = MutableLiveData<String?>()

    private var nextCursor: Int? = null
    var isLastPage = false

    val searchQuery = MutableStateFlow<String?>(null)
    val startDate = MutableStateFlow<String?>(null)
    val endDate = MutableStateFlow<String?>(null)

    companion object {
        private const val PAGE_SIZE = 10
    }

    fun startObservingSearchQuery() {
        viewModelScope.launch {
            searchQuery.debounce(500).collectLatest {
                loadJournals()
            }
        }
    }

    fun loadJournals() {
        if (isLoading.value == true) return

        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            _journals.value = emptyList()
            nextCursor = null
            isLastPage = false

            try {
                val response = if (isSearching()) {
                    searchJournalsUseCase(startDate.value, endDate.value, searchQuery.value, PAGE_SIZE, null)
                } else {
                    getJournalsUseCase(PAGE_SIZE, null)
                }
                val newEntries = response.items
                _journals.value = newEntries
                nextCursor = response.nextCursor
                isLastPage = nextCursor == null
            } catch (e: Exception) {
                errorMessage.value = "데이터 로딩 중 오류 발생: ${e.javaClass.simpleName} - ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun loadMoreJournals() {
        if (isLoading.value == true || isLastPage) return

        viewModelScope.launch {
            isLoading.value = true
            try {
                val response = if (isSearching()) {
                    searchJournalsUseCase(startDate.value, endDate.value, searchQuery.value, PAGE_SIZE, nextCursor)
                } else {
                    getJournalsUseCase(PAGE_SIZE, nextCursor)
                }
                val newEntries = response.items
                _journals.value = (_journals.value ?: emptyList()) + newEntries
                nextCursor = response.nextCursor
                isLastPage = nextCursor == null
            } catch (e: Exception) {
                errorMessage.value = "데이터 추가 로딩 중 오류 발생: ${e.javaClass.simpleName} - ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun clearSearchAndReload() {
        searchQuery.value = null
        startDate.value = null
        endDate.value = null
        loadJournals()
    }

    fun updateOrRemoveJournalEntry(updatedId: Int?, deletedId: Int?) {
        viewModelScope.launch {
            val currentList = _journals.value?.toMutableList() ?: return@launch

            if (deletedId != null) {
                val listAfterRemoval = currentList.filterNot { it.id == deletedId }
                if (listAfterRemoval.size < currentList.size) {
                    _journals.value = listAfterRemoval
                }
            } else if (updatedId != null) {
                val index = currentList.indexOfFirst { it.id == updatedId }
                if (index != -1) {
                    try {
                        // ✨ UseCase가 이미 변환된 JournalEntry를 반환함
                        val updatedItem = getJournalByIdUseCase(updatedId)
                        currentList[index] = updatedItem
                        _journals.value = currentList
                    } catch (e: Exception) {
                        loadJournals()
                    }
                }
            }
        }
    }

    fun isSearching(): Boolean {
        return !searchQuery.value.isNullOrBlank() || startDate.value != null
    }

    fun setDateRange(start: String?, end: String?) {
        startDate.value = start
        endDate.value = end
    }

    fun clearSearchConditions() {
        searchQuery.value = null
        startDate.value = null
        endDate.value = null
    }
}
