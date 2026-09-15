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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "passages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Passage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    public Passage(Chapter chapter, int sequence, List<String> sentenceContents) {
        validate(sentenceContents);
        this.chapter = chapter;
        this.sequence = new ContentSequence(sequence);
        for (int index = 0; index < sentenceContents.size(); index++) {
            sentences.add(new Sentence(this, index + 1, sentenceContents.get(index)));
        }
    }

    private static void validate(List<String> sentenceContents) {
        if (sentenceContents == null || sentenceContents.isEmpty()) {
            throw new IllegalArgumentException("문단에는 최소 1개의 문장이 있어야 합니다.");
        }
    }

    public List<Sentence> getSentences() {
        return List.copyOf(sentences);
    }

    public int getSequence() {
        return sequence.value();
    }

    public boolean belongsTo(Book book) {
        return chapter.belongsTo(book);
    }
}
