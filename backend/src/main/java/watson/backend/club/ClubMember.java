package watson.backend.club;

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
import watson.backend.book.Passage;
import watson.backend.member.Member;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id")
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_passage_id")
    private Passage lastReadPassage;

    @Column
    private LocalDateTime lastReadAt;

    public ClubMember(Member member, Club club) {
        this.member = member;
        this.club = club;
    }

    public void updateProgress(Passage passage, LocalDateTime readAt) {
        this.lastReadPassage = passage;
        this.lastReadAt = readAt;
    }

    public boolean isMember(Long memberId) {
        return member.getId().equals(memberId);
    }

    /**
     * 진도율(0~100, 반올림) = 최근 열람 본문의 순서 ÷ 도서의 본문 개수 (PRD 3.4).
     */
    public int progressRate() {
        return (int) Math.round(lastReadPassage.getSequence() * 100.0 / club.totalPassageCount());
    }
}
