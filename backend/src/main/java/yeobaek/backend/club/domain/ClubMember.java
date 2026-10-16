package yeobaek.backend.club.domain;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.club.domain.vo.ProgressRate;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;

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

    @OneToOne(mappedBy = "clubMember", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @Getter(AccessLevel.NONE)
    private ClubMemberLastRead lastRead;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'JOINED'")
    private ClubMemberStatus status = ClubMemberStatus.JOINED;

    public ClubMember(Member member, Club club) {
        this.member = member;
        this.club = club;
    }

    public void updateProgress(Passage passage, LocalDateTime readAt) {
        if (lastRead == null) {
            lastRead = new ClubMemberLastRead(this, passage, readAt);
            return;
        }
        lastRead.update(passage, readAt);
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
        return member.getId().equals(memberId);
    }

    /**
     * 진도율(0~100, 반올림) = 최근 열람 본문의 순서 ÷ 도서의 본문 개수 (PRD 3.4).
     */
    public int progressRate() {
        return ProgressRate.calculate(getLastReadPassage().getSequence(), club.totalPassageCount()).roundedPercentage();
    }

    public Passage getLastReadPassage() {
        return lastRead == null ? null : lastRead.getLastReadPassage();
    }

    public LocalDateTime getLastReadAt() {
        return lastRead == null ? null : lastRead.getLastReadAt();
    }

    public void ensureBookAvailable() {
        club.ensureBookAvailable();
    }

    public void ensureCanRead(Passage passage) {
        if (!isJoined()) {
            throw new ForbiddenException(
                    ErrorCode.NOT_CLUB_MEMBER,
                    "모임에 참여 중인 회원이 아닙니다.",
                    readingLogContext(passage));
        }
        if (!club.isReading(passage)) {
            throw new BadRequestException(
                    ErrorCode.INVALID_REQUEST,
                    "해당 모임의 도서에 속한 문단이 아닙니다.",
                    readingLogContext(passage));
        }
        club.ensureBookAvailable();
    }

    private Map<String, String> readingLogContext(Passage passage) {
        Map<String, String> context = new HashMap<>();
        if (member.getId() != null) {
            context.put("memberId", member.getId().toString());
        }
        if (club.getId() != null) {
            context.put("clubId", club.getId().toString());
        }
        if (passage.getId() != null) {
            context.put("passageId", passage.getId().toString());
        }
        return context;
    }
}
