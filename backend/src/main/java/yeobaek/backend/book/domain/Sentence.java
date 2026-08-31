package yeobaek.backend.book.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sentences", uniqueConstraints = {
        @UniqueConstraint(name = "uk_sentences_passage_sequence", columnNames = {"passage_id", "sequence"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sentence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passage_id", nullable = false)
    private Passage passage;

    @Column(nullable = false)
    private int sequence;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    Sentence(Passage passage, int sequence, String content) {
        validate(content);
        this.passage = passage;
        this.sequence = sequence;
        this.content = content;
    }

    private static void validate(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("문장 내용은 공백이 아닌 텍스트여야 합니다.");
        }
    }

    public boolean belongsTo(Book book) {
        return passage.belongsTo(book);
    }
}
