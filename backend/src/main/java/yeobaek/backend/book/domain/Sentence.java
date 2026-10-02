package yeobaek.backend.book.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import yeobaek.backend.book.domain.vo.ContentSequence;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.foundation.identity.ContentLocationId;

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

    @Column(name = "location_id", nullable = false, unique = true, updatable = false)
    private Long locationId;

    @Column(name = "location_kind", nullable = false, length = 64, updatable = false)
    private String locationKind;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passage_id", nullable = false)
    private Passage passage;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "sequence", nullable = false))
    private ContentSequence sequence;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "content", columnDefinition = "TEXT", nullable = false))
    private SentenceContent content;

    Sentence(ContentLocationId locationId, Passage passage, int sequence, SentenceContent content) {
        if (locationId == null) {
            throw new IllegalArgumentException("문장 위치 식별자는 필수입니다.");
        }
        this.passage = passage;
        this.locationId = locationId.value();
        this.locationKind = "SENTENCE";
        this.sequence = new ContentSequence(sequence);
        if (content == null) {
            throw new IllegalArgumentException("문장 내용은 필수입니다.");
        }
        this.content = content;
    }

    public String getContent() {
        return content.value();
    }

    public ContentSequence getSequence() {
        return sequence;
    }

    public boolean belongsTo(Book book) {
        return passage.belongsTo(book);
    }

    public Long getLocationId() {
        return locationId;
    }
}
