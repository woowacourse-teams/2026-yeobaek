package yeobaek.backend.publicroom.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
@Table(name = "public_room_members", uniqueConstraints = {
        @UniqueConstraint(name = "uk_public_room_members_member_room", columnNames = {"member_id", "public_room_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicRoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "public_room_id", nullable = false)
    private PublicRoom publicRoom;

    @Column
    private LocalDateTime lastVisitedAt;

    @OneToOne(mappedBy = "publicRoomMember", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @Getter(AccessLevel.NONE)
    private PublicRoomMemberLastRead lastRead;

    public PublicRoomMember(Member member, PublicRoom publicRoom) {
        this.member = member;
        this.publicRoom = publicRoom;
    }

    public void visit(LocalDateTime visitedAt) {
        this.lastVisitedAt = visitedAt;
    }

    public void updateProgress(Passage passage, LocalDateTime readAt) {
        if (lastRead == null) {
            lastRead = new PublicRoomMemberLastRead(this, passage, readAt);
            return;
        }
        lastRead.update(passage, readAt);
    }

    public boolean hasProgress() {
        return getLastReadPassage() != null;
    }

    public Passage getLastReadPassage() {
        return lastRead == null ? null : lastRead.getLastReadPassage();
    }

    public LocalDateTime getLastReadAt() {
        return lastRead == null ? null : lastRead.getLastReadAt();
    }

    public int progressRate() {
        return ProgressRate.calculate(getLastReadPassage().getSequence(),
                publicRoom.getBook().getPassageCount()).roundedPercentage();
    }
}
