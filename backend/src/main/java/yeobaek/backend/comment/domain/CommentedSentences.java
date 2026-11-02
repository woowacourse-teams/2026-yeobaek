package yeobaek.backend.comment.domain;

import java.util.Comparator;
import java.util.List;

public record CommentedSentences(List<CommentedSentence> values) {

    public CommentedSentences {
        values = List.copyOf(values);
    }

    public CommentedSentences filteredBy(CommentedSentenceFilter filter) {
        return new CommentedSentences(values.stream()
                .filter(filter::matches)
                .toList());
    }

    public List<CommentedSentence> sortedInBookOrder() {
        return values.stream()
                .sorted(Comparator.comparingInt((CommentedSentence sentence) -> sentence.passageSequence().value())
                        .thenComparingInt(sentence -> sentence.sentenceSequence().value())
                        .thenComparing(CommentedSentence::sentenceId, Comparator.reverseOrder()))
                .toList();
    }
}
