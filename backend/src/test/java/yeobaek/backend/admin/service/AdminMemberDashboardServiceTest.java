package yeobaek.backend.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.admin.dto.AdminDashboardClubCountDistributionResponse;
import yeobaek.backend.admin.dto.AdminDashboardMemberResponse;
import yeobaek.backend.admin.dto.AdminDashboardMembersResponse;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.IntegrationTest;

class AdminMemberDashboardServiceTest extends IntegrationTest {

    @Autowired
    private AdminMemberDashboardService adminMemberDashboardService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookManagementRepository bookRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Test
    @DisplayName("현재 회원의 JOINED 모임만 집계해 평균과 오름차순 분포를 계산한다")
    void findMembersWithAverageAndDistribution() {
        Book book = bookRepository.save(new Book(new BookTitle("회원 통계 도서"), null, null, 1, null));
        Club first = saveClub("첫 모임", book, "MEM001");
        Club second = saveClub("둘째 모임", book, "MEM002");
        Club third = saveClub("셋째 모임", book, "MEM003");
        Member zeroClubs = saveMember("모임 없는 회원");
        Member oneClubFirst = saveMember("한 모임 회원 A");
        Member threeClubs = saveMember("세 모임 회원");
        Member oneClubSecond = saveMember("한 모임 회원 B");
        join(threeClubs, first);
        join(threeClubs, second);
        join(threeClubs, third);
        join(oneClubFirst, first);
        join(oneClubSecond, second);
        ClubMember leftMembership = new ClubMember(zeroClubs, first);
        leftMembership.leave();
        clubMemberRepository.save(leftMembership);
        bookRepository.delete(book.getId());

        AdminDashboardMembersResponse response = adminMemberDashboardService.findMembers();

        assertThat(response.members())
                .extracting(
                        AdminDashboardMemberResponse::memberId,
                        AdminDashboardMemberResponse::nickname,
                        AdminDashboardMemberResponse::clubCount)
                .containsExactly(
                        tuple(threeClubs.getId(), "세 모임 회원", 3L),
                        tuple(oneClubFirst.getId(), "한 모임 회원 A", 1L),
                        tuple(oneClubSecond.getId(), "한 모임 회원 B", 1L),
                        tuple(zeroClubs.getId(), "모임 없는 회원", 0L));
        assertThat(response.averageClubCount()).isEqualByComparingTo(new BigDecimal("1.25"));
        assertThat(response.distribution())
                .extracting(
                        AdminDashboardClubCountDistributionResponse::clubCount,
                        AdminDashboardClubCountDistributionResponse::memberCount)
                .containsExactly(tuple(0L, 1L), tuple(1L, 2L), tuple(3L, 1L));
    }

    @Test
    @DisplayName("평균 참여 모임 수를 소수 둘째 자리에서 HALF_UP으로 반올림한다")
    void roundAverageHalfUp() {
        Book book = bookRepository.save(new Book(new BookTitle("반올림 도서"), null, null, 1, null));
        Club club = saveClub("반올림 모임", book, "ROUND1");
        join(saveMember("첫 회원"), club);
        saveMember("둘째 회원");
        saveMember("셋째 회원");
        saveMember("넷째 회원");
        saveMember("다섯째 회원");
        saveMember("여섯째 회원");
        saveMember("일곱째 회원");
        saveMember("여덟째 회원");

        assertThat(adminMemberDashboardService.findMembers().averageClubCount())
                .isEqualByComparingTo(new BigDecimal("0.13"));
    }

    @Test
    @DisplayName("회원이 없으면 빈 목록과 빈 분포 및 0.00 평균을 반환한다")
    void findMembersWhenEmpty() {
        AdminDashboardMembersResponse response = adminMemberDashboardService.findMembers();

        assertThat(response.members()).isEmpty();
        assertThat(response.distribution()).isEmpty();
        assertThat(response.averageClubCount()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    private Club saveClub(String name, Book book, String joinCode) {
        return clubRepository.save(new Club(new ClubName(name), book, new JoinCode(joinCode)));
    }

    private Member saveMember(String nickname) {
        return memberRepository.save(new Member(new Nickname(nickname)));
    }

    private void join(Member member, Club club) {
        clubMemberRepository.save(new ClubMember(member, club));
    }
}
