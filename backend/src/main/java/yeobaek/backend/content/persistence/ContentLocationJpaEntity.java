package yeobaek.backend.content.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yeobaek.backend.content.api.location.LocationKind;

@Entity
@Table(name = "content_locations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_content_locations_id_kind", columnNames = {"id", "kind"}),
        @UniqueConstraint(name = "uk_content_locations_id_content", columnNames = {"id", "content_id"})
}, indexes = @Index(name = "idx_content_locations_content", columnList = "content_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentLocationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private ContentJpaEntity content;

    @Column(nullable = false, length = 64)
    private String kind;

    public ContentLocationJpaEntity(ContentJpaEntity content, LocationKind kind) {
        this.content = content;
        this.kind = kind.value();
    }

    public LocationKind locationKind() {
        return new LocationKind(kind);
    }
}
