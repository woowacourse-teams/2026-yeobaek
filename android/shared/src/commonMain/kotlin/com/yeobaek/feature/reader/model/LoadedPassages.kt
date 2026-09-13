package com.yeobaek.feature.reader.model

data class LoadedPassages(
    val items: List<PassageUiModel> = emptyList(),
) {
    val firstSequence: Int?
        get() = items.firstOrNull()?.sequence

    val lastSequence: Int?
        get() = items.lastOrNull()?.sequence

    val firstPassageId: Long?
        get() = items.firstOrNull()?.passageId

    fun containsSequence(sequence: Int): Boolean =
        items.any { passage -> passage.sequence == sequence }

    fun findBySequence(sequence: Int): PassageUiModel? =
        items.firstOrNull { passage -> passage.sequence == sequence }

    fun findByPassageId(passageId: Long): PassageUiModel? =
        items.firstOrNull { passage -> passage.passageId == passageId }

    fun indexOfSequence(sequence: Int): Int =
        items.indexOfFirst { passage -> passage.sequence == sequence }

    fun indexOfPassageId(passageId: Long): Int =
        items.indexOfFirst { passage -> passage.passageId == passageId }

    fun getOrNull(index: Int): PassageUiModel? = items.getOrNull(index)

    fun findSentence(sentenceId: Long): SentenceUiModel? = items
        .asSequence()
        .flatMap { passage -> passage.sentences.asSequence() }
        .firstOrNull { sentence -> sentence.sentenceId == sentenceId }

    fun findPassageSequenceBySentenceId(sentenceId: Long): Int? = items
        .firstOrNull { passage ->
            passage.sentences.any { sentence -> sentence.sentenceId == sentenceId }
        }
        ?.sequence

    fun addPrevious(previous: List<PassageUiModel>): LoadedPassages =
        merge(previous + items)

    fun addNext(next: List<PassageUiModel>): LoadedPassages =
        merge(items + next)

    fun replaceAll(passages: List<PassageUiModel>): LoadedPassages =
        LoadedPassages(passages)

    fun updateCommentCount(
        sentenceId: Long,
        commentCount: Int,
    ): LoadedPassages = copy(
        items = items.map { passage ->
            if (passage.sentences.none { sentence -> sentence.sentenceId == sentenceId }) {
                passage
            } else {
                passage.copy(
                    sentences = passage.sentences.map { sentence ->
                        if (sentence.sentenceId == sentenceId) {
                            sentence.copy(commentCount = commentCount)
                        } else {
                            sentence
                        }
                    },
                )
            }
        },
    )

    private fun merge(passages: List<PassageUiModel>): LoadedPassages = LoadedPassages(
        items = passages
            .distinctBy(PassageUiModel::sequence)
            .sortedBy(PassageUiModel::sequence),
    )
}
