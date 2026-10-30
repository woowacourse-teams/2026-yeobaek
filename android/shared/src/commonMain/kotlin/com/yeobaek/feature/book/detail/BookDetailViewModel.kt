package com.yeobaek.feature.book.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.data.repository.BookRepository
import com.yeobaek.feature.book.detail.model.toUiModel
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class BookDetailViewModel(
    private val bookId: Long,
    private val bookRepository: BookRepository,
) : ViewModel() {
    var state: BookDetailState by mutableStateOf(BookDetailState.Loading)
        private set

    init {
        loadBookDetail()
    }

    fun retry() {
        if (state is BookDetailState.Loading) return
        loadBookDetail()
    }

    private fun loadBookDetail() {
        state = BookDetailState.Loading
        viewModelScope.launch {
            try {
                val book = bookRepository.getBookDetail(bookId).toUiModel()
                state = BookDetailState.Success(book)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                state = BookDetailState.Failure(exception.message ?: "책 정보를 불러오지 못했습니다.")
            }
        }
    }

    companion object {
        fun bookDetailViewModelFactory(bookId: Long, bookRepository: BookRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    BookDetailViewModel(bookId = bookId, bookRepository = bookRepository)
                }
            }
    }
}
