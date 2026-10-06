package yeobaek.backend.space.club.domain;

import yeobaek.backend.space.club.persistence.Club;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import yeobaek.backend.shared.identity.MemberId;

@Entity
@Table(name = "club_members", uniqueConstraints = {
        @UniqueConstraint(name = "uk_club_members_member_club", columnNames = {"member_id", "club_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id")
    private Club club;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'JOINED'")
    private ClubMemberStatus status = ClubMemberStatus.JOINED;

    public ClubMember(MemberId memberId, Club club) {
        this.memberId = memberId.value();
        this.club = club;
    }

    public void rejoin() {
        this.status = ClubMemberStatus.JOINED;
    }

    public void leave() {
        this.status = ClubMemberStatus.LEFT;
    }

    public boolean isJoined() {
        return status == ClubMemberStatus.JOINED;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }
}
