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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chapters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "title", nullable = false, length = ChapterTitle.MAX_LENGTH))
    private ChapterTitle title;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "sequence", nullable = false))
    private ContentSequence sequence;

    public Chapter(Book book, String title, int sequence) {
        this.book = book;
        this.title = new ChapterTitle(title);
        this.sequence = new ContentSequence(sequence);
    }

    public String getTitle() {
        return title.value();
    }

    public int getSequence() {
        return sequence.value();
    }

    public boolean belongsTo(Book other) {
        return book.isSame(other);
    }
}
