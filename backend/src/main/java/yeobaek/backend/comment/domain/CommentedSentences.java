package yeobaek.backend.comment.domain;

import java.util.Comparator;
import java.util.List;
import yeobaek.backend.book.domain.Passage;

public record CommentedSentences(List<CommentedSentence> values) {

    public CommentedSentences {
        values = List.copyOf(values);
    }

    public CommentedSentences withUnreadCommentsUpTo(Passage currentPassage) {
        return new CommentedSentences(values.stream()
                .filter(sentence -> sentence.hasNewVisibleComments(currentPassage))
                .toList());
    }

    public List<CommentedSentence> sortedByPositionInBook() {
        return values.stream()
                .sorted(Comparator.comparingInt((CommentedSentence sentence) -> sentence.passageSequence().value())
                        .thenComparingInt(sentence -> sentence.sentenceSequence().value()))
                .toList();
    }
}
