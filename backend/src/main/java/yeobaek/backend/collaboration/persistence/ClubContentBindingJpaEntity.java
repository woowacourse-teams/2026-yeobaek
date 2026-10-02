package yeobaek.backend.collaboration.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "club_content_bindings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubContentBindingJpaEntity {

    @Id
    @jakarta.persistence.Column(name = "space_id")
    private Long spaceId;

    @jakarta.persistence.Column(name = "content_id", nullable = false)
    private Long contentId;

    public ClubContentBindingJpaEntity(Long spaceId, Long contentId) {
        this.spaceId = spaceId;
        this.contentId = contentId;
    }
}
