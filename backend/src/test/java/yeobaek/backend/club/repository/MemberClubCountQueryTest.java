package yeobaek.backend.club.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.IntegrationTest;

@Transactional
class MemberClubCountQueryTest extends IntegrationTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookManagementRepository bookRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Test
    @DisplayName("조회한 회원의 현재 참여만 집계하며 삭제 도서 모임도 포함한다")
    void countOnlyJoinedMembershipsAcrossBookStates() {
        Book book = bookRepository.save(new Book(new BookTitle("집계 도서"), null, null, 1,
                null, null, null, List.of(), List.of()));
        Club joinedClub = clubRepository.save(new Club(new ClubName("참여 모임"), book, new JoinCode("COUNT1")));
        Club leftClub = clubRepository.save(new Club(new ClubName("탈퇴 모임"), book, new JoinCode("COUNT2")));
        Member joined = memberRepository.save(new Member(new Nickname("참여 회원")));
        Member left = memberRepository.save(new Member(new Nickname("탈퇴 회원")));
        Member outsideQuery = memberRepository.save(new Member(new Nickname("조회 범위 밖 회원")));
        clubMemberRepository.save(new ClubMember(joined, joinedClub));
        clubMemberRepository.save(new ClubMember(outsideQuery, joinedClub));
        ClubMember membership = new ClubMember(left, leftClub);
        membership.leave();
        clubMemberRepository.save(membership);
        bookRepository.delete(book.getId());

        var counts = clubMemberRepository.countJoinedClubsByMemberIds(List.of(joined.getId(), left.getId()));

        assertThat(counts).extracting(MemberClubCount::getMemberId, MemberClubCount::getClubCount)
                .containsExactly(tuple(joined.getId(), 1L));
    }
}
