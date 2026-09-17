package com.yeobaek.feature.reader.model

data class CommentedSentencesUiModel(
    val sentences: List<CommentedSentenceUiModel> = emptyList(),
) {
    fun updateCommentCount(
        sentenceId: Long,
        commentCount: Int,
    ): CommentedSentencesUiModel = copy(
        sentences = sentences.mapNotNull { sentence ->
            when {
                sentence.sentenceId != sentenceId -> sentence
                commentCount <= 0 -> null
                else -> sentence.copy(commentCount = commentCount)
            }
        },
    )

    fun markCommentsViewed(sentenceId: Long): CommentedSentencesUiModel = copy(
        sentences = sentences.map { sentence ->
            if (sentence.sentenceId == sentenceId) {
                sentence.copy(unreadCommentCount = 0)
            } else {
                sentence
            }
        },
    )
}
