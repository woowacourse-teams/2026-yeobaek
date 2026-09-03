package yeobaek.backend.member.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "member_blocks", uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_blocks_blocker_blocked", columnNames = {"blocker_id", "blocked_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member blocker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member blocked;

    public MemberBlock(Member blocker, Member blocked) {
        this.blocker = blocker;
        this.blocked = blocked;
    }
}
