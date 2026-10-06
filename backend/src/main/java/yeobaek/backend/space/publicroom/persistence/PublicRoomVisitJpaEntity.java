package yeobaek.backend.space.publicroom.persistence;

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
@Table(name = "public_room_visits", uniqueConstraints = {
        @UniqueConstraint(name = "uk_public_room_visits_actor_space", columnNames = {"actor_id", "space_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicRoomVisitJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Column(precision = 6)
    private LocalDateTime lastVisitedAt;

    public PublicRoomVisitJpaEntity(Long actorId, Long spaceId) {
        this.actorId = actorId;
        this.spaceId = spaceId;
    }

    public void visit(LocalDateTime visitedAt) {
        if (visitedAt == null) {
            throw new IllegalArgumentException("방문 시각은 필수입니다.");
        }
        lastVisitedAt = visitedAt;
    }
}
