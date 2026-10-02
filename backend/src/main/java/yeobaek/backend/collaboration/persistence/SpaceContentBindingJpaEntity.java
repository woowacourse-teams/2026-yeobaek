package yeobaek.backend.collaboration.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "space_content_bindings")
@IdClass(SpaceContentBindingId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpaceContentBindingJpaEntity {

    @Id
    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Id
    @Column(name = "content_id", nullable = false)
    private Long contentId;

    public SpaceContentBindingJpaEntity(Long spaceId, Long contentId) {
        this.spaceId = spaceId;
        this.contentId = contentId;
    }
}
