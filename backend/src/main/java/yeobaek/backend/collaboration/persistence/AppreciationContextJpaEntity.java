package yeobaek.backend.collaboration.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "appreciation_contexts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppreciationContextJpaEntity {

    @Id
    private Long appreciationId;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    public AppreciationContextJpaEntity(Long appreciationId, Long spaceId,
                                        Long contentId, Long locationId) {
        this.appreciationId = appreciationId;
        this.spaceId = spaceId;
        this.contentId = contentId;
        this.locationId = locationId;
    }
}
