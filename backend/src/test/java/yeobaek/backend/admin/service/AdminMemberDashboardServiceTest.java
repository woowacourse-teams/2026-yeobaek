package yeobaek.backend.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.admin.dto.AdminDashboardClubCountDistributionResponse;
import yeobaek.backend.admin.dto.AdminDashboardMemberResponse;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.MemberClubCount;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class AdminMemberDashboardServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @InjectMocks
    private AdminMemberDashboardService service;

    @Test
    @DisplayName("모임이 없는 회원을 평균과 분포에 포함하고 참여 수 내림차순과 회원 ID 순으로 표시한다")
    void includeZeroParticipationInStatistics() {
        when(memberRepository.findAll()).thenReturn(List.of(
                member(4L), member(2L), member(1L), member(3L)));
        when(clubMemberRepository.countJoinedClubsByMemberIds(List.of(4L, 2L, 1L, 3L)))
                .thenReturn(List.of(new Participation(4L, 1), new Participation(2L, 1),
                        new Participation(3L, 3)));

        var response = service.findMemberClubParticipationStatistics();

        assertThat(response.members())
                .extracting(AdminDashboardMemberResponse::memberId, AdminDashboardMemberResponse::clubCount)
                .containsExactly(tuple(3L, 3L), tuple(2L, 1L), tuple(4L, 1L), tuple(1L, 0L));
        assertThat(response.averageClubCount()).isEqualByComparingTo(new BigDecimal("1.25"));
        assertThat(response.distribution()).containsExactly(
                new AdminDashboardClubCountDistributionResponse(0, 1),
                new AdminDashboardClubCountDistributionResponse(1, 2),
                new AdminDashboardClubCountDistributionResponse(3, 1));
    }

    @Test
    @DisplayName("평균 0.125는 소수 둘째 자리까지 반올림해 0.13으로 반환한다")
    void roundAverageHalfUp() {
        List<Member> members = LongStream.rangeClosed(1, 8).mapToObj(this::member).toList();
        when(memberRepository.findAll()).thenReturn(members);
        when(clubMemberRepository.countJoinedClubsByMemberIds(
                List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L)))
                .thenReturn(List.of(new Participation(1L, 1)));

        assertThat(service.findMemberClubParticipationStatistics().averageClubCount())
                .isEqualByComparingTo(new BigDecimal("0.13"));
    }

    @Test
    @DisplayName("회원이 없으면 빈 목록과 분포 및 0.00 평균을 반환한다")
    void emptyPopulation() {
        when(memberRepository.findAll()).thenReturn(List.of());

        var response = service.findMemberClubParticipationStatistics();

        assertThat(response.members()).isEmpty();
        assertThat(response.distribution()).isEmpty();
        assertThat(response.averageClubCount()).isEqualTo(new BigDecimal("0.00"));
    }

    private Member member(long id) {
        Member member = new Member(new Nickname("회원" + id));
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private record Participation(Long memberId, long clubCount) implements MemberClubCount {
        @Override
        public Long getMemberId() {
            return memberId;
        }

        @Override
        public long getClubCount() {
            return clubCount;
        }
    }
}
