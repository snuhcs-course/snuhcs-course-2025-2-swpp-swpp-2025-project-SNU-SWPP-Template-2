package com.example.librarytogether.feature.library

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.librarytogether.feature.library.data.LibraryRepository
import com.example.librarytogether.feature.library.data.Review
import com.example.librarytogether.feature.library.data.postReview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository
) : ViewModel() {
    private val _myReviews = MutableLiveData<List<Review>>(emptyList())
    val myReviews: LiveData<List<Review>> = _myReviews

    init {
        refreshMyReviews()
    }

    fun refreshMyReviews() {
        viewModelScope.launch {
            val list = repository.getMyReviews()
            _myReviews.postValue(list)
        }
    }

    fun addNewReview(review: postReview) {
        viewModelScope.launch {
            repository.addReview(review)
            refreshMyReviews()
        }
    }
}