package com.yeobaek.feature.reader

import com.yeobaek.feature.reader.model.PassageUiModel
import com.yeobaek.feature.reader.model.SentenceUiModel

data class ReaderCommentTarget(
    val passageSequence: Int,
    val sentenceId: Long,
) {
    fun findSentence(passages: List<PassageUiModel>): SentenceUiModel? = passages
        .firstOrNull { it.sequence == passageSequence }
        ?.sentences
        ?.firstOrNull { it.sentenceId == sentenceId }

    companion object {
        fun from(passageSequence: Int?, sentenceId: Long?): ReaderCommentTarget? {
            if (passageSequence == null || sentenceId == null) return null
            return ReaderCommentTarget(passageSequence, sentenceId)
        }
    }
}
