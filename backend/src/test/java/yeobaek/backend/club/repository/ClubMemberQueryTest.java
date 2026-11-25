package yeobaek.backend.club.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.IntegrationTest;

@Transactional
class ClubMemberQueryTest extends IntegrationTest {

    @Autowired
    private BookManagementRepository bookRepository;
    @Autowired
    private ChapterRepository chapterRepository;
    @Autowired
    private PassageRepository passageRepository;
    @Autowired
    private ClubRepository clubRepository;
    @Autowired
    private ClubMemberRepository clubMemberRepository;
    @Autowired
    private MemberRepository memberRepository;


    @Test
    @DisplayName("최근 읽기 쿼리는 탈퇴와 미열람 모임을 제외하고 삭제 도서 기록도 시간순으로 반환한다")
    void lastReadingCandidates() {
        Member reader = memberRepository.save(new Member(new Nickname("독자")));
        Reading older = reading(reader, "OLD001");
        Reading latest = reading(reader, "NEW001");
        Reading left = reading(reader, "LEFT01");
        Reading unread = reading(reader, "NONE01");
        LocalDateTime baseline = LocalDateTime.of(2026, 1, 1, 12, 0);
        update(older, baseline);
        update(latest, baseline.plusHours(1));
        left.membership().updateProgress(left.passage(), baseline.plusHours(2));
        left.membership().leave();
        clubMemberRepository.saveAndFlush(left.membership());
        bookRepository.delete(latest.book().getId());

        var candidates = clubMemberRepository.findAllJoinedWithLastReadingByMemberId(reader.getId());

        assertThat(candidates).extracting(membership -> membership.getClub().getId())
                .containsExactly(latest.club().getId(), older.club().getId());
        assertThat(clubMemberRepository.findAllJoinedWithClubAndBookByMemberId(reader.getId()))
                .extracting(membership -> membership.getClub().getId())
                .containsExactlyInAnyOrder(older.club().getId(), latest.club().getId(), unread.club().getId());
    }

    @Test
    @DisplayName("모임의 참여자 목록과 인원 집계는 탈퇴 회원을 제외한다")
    void joinedMembersOnly() {
        Member reader = memberRepository.save(new Member(new Nickname("독자")));
        Member left = memberRepository.save(new Member(new Nickname("탈퇴 독자")));
        Book book = bookRepository.save(new Book(new BookTitle("집계 도서"), null, null, 1,
                null, null, null, null, null));
        Club club = clubRepository.save(new Club(new ClubName("독서 모임"), book, new JoinCode("COUNT1")));
        clubMemberRepository.save(new ClubMember(reader, club));
        ClubMember leftMembership = new ClubMember(left, club);
        leftMembership.leave();
        clubMemberRepository.saveAndFlush(leftMembership);

        assertThat(clubMemberRepository.findAllJoinedWithMemberByClubId(club.getId()))
                .extracting(membership -> membership.getMember().getId()).containsExactly(reader.getId());
        assertThat(clubMemberRepository.countJoinedMembersByClubIds(List.of(club.getId())))
                .singleElement().satisfies(count -> assertThat(count.getMemberCount()).isEqualTo(1));
    }

    private Reading reading(Member reader, String code) {
        Book book = bookRepository.save(new Book(new BookTitle(code), null, null, 4, null, null, null, null, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("첫 장"), 1));
        Passage passage = passageRepository.save(new Passage(chapter, 2, List.of(new SentenceContent("본문"))));
        Club club = clubRepository.save(new Club(new ClubName("독서 모임"), book, new JoinCode(code)));
        ClubMember membership = clubMemberRepository.save(new ClubMember(reader, club));
        return new Reading(book, club, membership, passage);
    }

    private void update(Reading reading, LocalDateTime readAt) {
        reading.membership().updateProgress(reading.passage(), readAt);
        clubMemberRepository.saveAndFlush(reading.membership());
    }

    private record Reading(Book book, Club club, ClubMember membership, Passage passage) {
    }
}
