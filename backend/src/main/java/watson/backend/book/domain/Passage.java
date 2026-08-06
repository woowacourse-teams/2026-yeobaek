package watson.backend.book.domain;

import jakarta.persistence.Column;
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

    @Column(nullable = false)
    private int sequence;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String imageUrl;

    public Passage(Chapter chapter, int sequence, String content, String imageUrl) {
        this.chapter = chapter;
        this.sequence = sequence;
        this.content = content;
        this.imageUrl = imageUrl;
    }

    public boolean belongsTo(Book book) {
        return chapter.belongsTo(book);
    }
}
