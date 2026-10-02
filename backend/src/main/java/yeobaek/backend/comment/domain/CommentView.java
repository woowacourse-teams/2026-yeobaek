package yeobaek.backend.comment.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "appreciation_views", indexes = {
        @Index(name = "idx_appreciation_views_actor_appreciation", columnList = "actor_id, appreciation_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private Long actorId;

    @Column(name = "appreciation_id", nullable = false, updatable = false)
    private Long appreciationId;

    public CommentView(Long actorId, Long appreciationId) {
        this.actorId = actorId;
        this.appreciationId = appreciationId;
    }
}
