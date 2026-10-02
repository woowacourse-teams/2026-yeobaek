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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yeobaek.backend.book.domain.vo.ContentSequence;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.foundation.identity.ContentLocationId;

@Entity
@Table(name = "passages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Passage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "location_id", nullable = false, unique = true, updatable = false)
    private Long locationId;

    @Column(name = "location_kind", nullable = false, length = 64, updatable = false)
    private String locationKind;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "sequence", nullable = false))
    private ContentSequence sequence;

    @OneToMany(mappedBy = "passage", fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.ALL,
            orphanRemoval = true)
    @OrderBy("sequence.value ASC")
    private List<Sentence> sentences = new ArrayList<>();

    public Passage(ContentLocationId passageLocationId, List<ContentLocationId> sentenceLocationIds,
                   Chapter chapter, int sequence, List<SentenceContent> sentenceContents) {
        validate(passageLocationId, sentenceLocationIds, sentenceContents);
        this.chapter = chapter;
        this.locationId = passageLocationId.value();
        this.locationKind = "PASSAGE";
        this.sequence = new ContentSequence(sequence);
        for (int index = 0; index < sentenceContents.size(); index++) {
            sentences.add(new Sentence(sentenceLocationIds.get(index), this, index + 1, sentenceContents.get(index)));
        }
    }

    private void validate(ContentLocationId passageLocationId, List<ContentLocationId> sentenceLocationIds,
                          List<SentenceContent> sentenceContents) {
        if (passageLocationId == null) {
            throw new IllegalArgumentException("문단 위치 식별자는 필수입니다.");
        }
        if (sentenceContents == null || sentenceContents.isEmpty()) {
            throw new IllegalArgumentException("문단에는 최소 1개의 문장이 있어야 합니다.");
        }
        if (sentenceLocationIds == null || sentenceLocationIds.size() != sentenceContents.size()
                || sentenceLocationIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("문장마다 컨텐츠 위치 식별자가 필요합니다.");
        }
        if (sentenceContents.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("문장 내용은 필수입니다.");
        }
    }

    public List<Sentence> getSentences() {
        return List.copyOf(sentences);
    }

    public ContentSequence getSequence() {
        return sequence;
    }

    public boolean belongsTo(Book book) {
        return chapter.belongsTo(book);
    }

    public Long getLocationId() {
        return locationId;
    }

    public Book getBook() {
        return chapter.getBook();
    }
}
