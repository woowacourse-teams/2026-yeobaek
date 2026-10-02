package yeobaek.backend.appreciation.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "appreciations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_appreciations_id_kind", columnNames = {"id", "kind"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppreciationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String kind;

    @Column(name = "author_id", nullable = false, updatable = false)
    private Long authorId;

    @Column(nullable = false, precision = 6)
    private LocalDateTime createdAt;

    @Column(precision = 6)
    private LocalDateTime updatedAt;

    public AppreciationJpaEntity(String kind, Long authorId, LocalDateTime createdAt) {
        this.kind = kind;
        this.authorId = authorId;
        this.createdAt = createdAt;
    }

    public void markUpdated(LocalDateTime changedAt) {
        updatedAt = changedAt;
    }
}
