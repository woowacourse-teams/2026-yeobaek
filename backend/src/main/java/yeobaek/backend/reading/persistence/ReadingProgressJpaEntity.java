package yeobaek.backend.reading.persistence;

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
@Table(name = "reading_progresses", uniqueConstraints = {
        @UniqueConstraint(name = "uk_reading_progresses_actor_space_content",
                columnNames = {"actor_id", "space_id", "content_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadingProgressJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(nullable = false, precision = 6)
    private LocalDateTime lastReadAt;

    public ReadingProgressJpaEntity(Long actorId, Long spaceId, Long contentId,
                                    Long locationId, LocalDateTime lastReadAt) {
        this.actorId = actorId;
        this.spaceId = spaceId;
        this.contentId = contentId;
        this.locationId = locationId;
        this.lastReadAt = lastReadAt;
    }

    public void update(Long changedLocationId, LocalDateTime readAt) {
        locationId = changedLocationId;
        lastReadAt = readAt;
    }
}
