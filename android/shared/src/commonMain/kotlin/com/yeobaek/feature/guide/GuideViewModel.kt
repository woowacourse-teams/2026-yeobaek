package com.yeobaek.feature.guide

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.feature.guide.model.SentenceGuideUiModel

class GuideViewModel : ViewModel() {
    var uiState by mutableStateOf(GuideUiState())
        private set

    init {
        initSentences()
    }

    fun initSentences() {
        uiState = uiState.copy(sentences = mockSentences)
    }

    fun onCurrentPage(
        onEvent: () -> Unit,
    ) {
        when (uiState.currentPage) {
            1 -> {
                uiState = uiState.copy(nextEnabled = true)
            }

            2 -> {
                uiState = uiState.copy(nextEnabled = true)
            }

            3 -> uiState = uiState.copy(nextEnabled = true)

            4 -> {
                uiState = uiState.copy(
                    isSuccessGuide = false,
                    nextEnabled = false,
                    isClickCommentSentence = false,
                    isClickUnCommentSentence = false,
                )
            }

            else -> {
                onEvent()
                uiState = uiState.copy(
                    nextEnabled = false,
                    previousEnabled = false,
                )
            }
        }
    }

    fun onSuccessGuide() {
        if (uiState.isSuccessGuide) uiState = uiState.copy(nextEnabled = true)
    }

    fun onClickPrevious() {
        if (uiState.currentPage > 1) uiState = uiState.copy(currentPage = uiState.currentPage - 1)
        if (uiState.currentPage == 1) uiState = uiState.copy(previousEnabled = false)
        if (uiState.currentPage < TOTAL_PAGES) uiState = uiState.copy(nextEnabled = true)
    }

    fun onClickNext() {
        uiState = uiState.copy(currentPage = uiState.currentPage + 1)
        if (uiState.currentPage > 1) uiState = uiState.copy(previousEnabled = true)
        if (uiState.currentPage == TOTAL_PAGES) uiState = uiState.copy(nextEnabled = false)
    }

    fun onClickCommentSentence() {
        uiState = uiState.copy(isClickCommentSentence = true)
    }

    fun onClickUnCommentSentence() {
        uiState = uiState.copy(isClickUnCommentSentence = true)
    }

    fun onCancel() {
        uiState = uiState.copy(isSuccessGuide = true, isClickCommentSentence = false)
    }

    fun isLast(): Boolean = uiState.currentPage == TOTAL_PAGES

    fun currentPageText(): String = "${minOf(uiState.currentPage, TOTAL_PAGES)} / $TOTAL_PAGES"

    companion object {
        private const val TOTAL_PAGES = 4

        private val mockSentences = listOf(
            SentenceGuideUiModel(
                sentenceId = 1,
                content = "처음 찰스 스트릭랜드를 알게 되었을 때, 나는 그에게 범상치 않은 구석이 있으리라고는 단 한순간도 알아보지 못했음을 고백한다. ",
                isComment = false,
            ),
            SentenceGuideUiModel(
                sentenceId = 2,
                content = "그러나 이제 그의 위대함을 부정할 사람은 거의 없을 것이다. ",
                isComment = true,
            ),
            SentenceGuideUiModel(
                sentenceId = 3,
                content = "내가 말하는 위대함은 운 좋은 청지가나 승승장구한 군인이 얻는 그런 것이 아니다. ",
                isComment = false,
            ),
            SentenceGuideUiModel(
                sentenceId = 4,
                content = "그것은 당사자보다 그가 차지한 자리에 딸린 속성이어서, 형편이 바뀌면 놀라울 만큼 초라해지고 만다. ",
                isComment = false,
            ),
        )

        fun guideViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                GuideViewModel()
            }
        }
    }
}
