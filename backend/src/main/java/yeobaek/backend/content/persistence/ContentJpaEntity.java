package yeobaek.backend.content.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contents", uniqueConstraints = {
        @UniqueConstraint(name = "uk_contents_id_kind", columnNames = {"id", "kind"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String kind;

    public ContentJpaEntity(String kind) {
        this.kind = kind;
    }
}
