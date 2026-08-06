package watson.backend.book.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String publisher;

    private Integer publishedYear;

    @Column(nullable = false)
    private int passageCount;

    public Book(String title, String publisher, Integer publishedYear, int passageCount) {
        this.title = title;
        this.publisher = publisher;
        this.publishedYear = publishedYear;
        this.passageCount = passageCount;
    }

    public boolean isSame(Book other) {
        return this.id.equals(other.id);
    }
}
