package yeobaek.backend.publicroom.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.club.domain.vo.ProgressRate;
import yeobaek.backend.member.domain.Member;

@Entity
@Table(name = "public_room_activities", uniqueConstraints = {
        @UniqueConstraint(name = "uk_public_room_activities_member_room", columnNames = {"member_id", "public_room_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicRoomActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "public_room_id")
    private PublicRoom publicRoom;

    @Column
    private LocalDateTime lastVisitedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_passage_id")
    private Passage lastReadPassage;

    @Column
    private LocalDateTime lastReadAt;

    public PublicRoomActivity(Member member, PublicRoom publicRoom) {
        this.member = member;
        this.publicRoom = publicRoom;
    }

    public void visit(LocalDateTime visitedAt) {
        this.lastVisitedAt = visitedAt;
    }

    public void updateProgress(Passage passage, LocalDateTime readAt) {
        this.lastReadPassage = passage;
        this.lastReadAt = readAt;
    }

    public boolean hasProgress() {
        return lastReadPassage != null;
    }

    public int progressRate() {
        return ProgressRate.calculate(lastReadPassage.getSequence(),
                publicRoom.getBook().getPassageCount()).roundedPercentage();
    }
}
