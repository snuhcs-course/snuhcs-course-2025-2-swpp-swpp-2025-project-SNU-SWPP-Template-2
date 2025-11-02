package com.example.librarytogether.feature.onboarding.ui

import androidx.lifecycle.*
import com.example.librarytogether.feature.onboarding.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repo: OnboardingRepository
): ViewModel() {

    // 검색 결과
    private val _books = MutableLiveData<List<BookDto>>(emptyList())
    val books: LiveData<List<BookDto>> = _books

    private val _authors = MutableLiveData<List<AuthorDto>>(emptyList())
    val authors: LiveData<List<AuthorDto>> = _authors

    private val _genres = MutableLiveData<List<GenreDto>>(emptyList())
    val genres: LiveData<List<GenreDto>> = _genres

    // 선택 상태
    private val _selectedBookIds = MutableLiveData<MutableSet<Int>>(mutableSetOf())
    private val _selectedAuthorIds = MutableLiveData<MutableSet<Int>>(mutableSetOf())
    private val _selectedGenreIds = MutableLiveData<MutableSet<Int>>(mutableSetOf())

    val canProceedBooks: LiveData<Boolean> = Transformations.map(_selectedBookIds) { it.size >= 3 }
    val canProceedAuthors: LiveData<Boolean> = Transformations.map(_selectedAuthorIds) { it.size >= 3 }
    val canProceedGenres: LiveData<Boolean> = Transformations.map(_selectedGenreIds) { it.size >= 3 }

    private var searchJob: Job? = null

    fun toggleBook(id: Int, checked: Boolean) {
        val s = _selectedBookIds.value!!
        if (checked) s.add(id) else s.remove(id)
        _selectedBookIds.value = s
    }
    fun toggleAuthor(id: Int, checked: Boolean) {
        val s = _selectedAuthorIds.value!!
        if (checked) s.add(id) else s.remove(id)
        _selectedAuthorIds.value = s
    }
    fun toggleGenre(id: Int, checked: Boolean) {
        val s = _selectedGenreIds.value!!
        if (checked) s.add(id) else s.remove(id)
        _selectedGenreIds.value = s
    }

    fun searchBooks(q: String?) = debounced {
        _books.value = repo.books(q)
    }
    fun searchAuthors(q: String?) = debounced {
        _authors.value = repo.authors(q)
    }
    fun searchGenres(q: String?) = debounced {
        _genres.value = repo.genres(q)
    }

    private fun debounced(block: suspend () -> Unit) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(180) // 타이핑 디바운스
            block()
        }
    }

    suspend fun submit(): Boolean {
        val req = OnboardingSubmitRequest(
            _selectedBookIds.value!!.toList(),
            _selectedAuthorIds.value!!.toList(),
            _selectedGenreIds.value!!.toList()
        )
        return repo.submit(req)
    }
}
