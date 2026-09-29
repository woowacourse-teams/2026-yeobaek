package yeobaek.backend.admin.service;

import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.PHASE;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.admin.dto.AdminDashboardClubCountDistributionResponse;
import yeobaek.backend.admin.dto.AdminDashboardMemberResponse;
import yeobaek.backend.admin.dto.AdminDashboardMembersResponse;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.MemberClubCount;
import yeobaek.backend.member.domain.Members;
import yeobaek.backend.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminMemberDashboardService {

    private static final int AVERAGE_SCALE = 2;

    private final MemberRepository memberRepository;
    private final ClubMemberRepository clubMemberRepository;

    @Transactional(readOnly = true)
    public AdminDashboardMembersResponse findMemberClubParticipationStatistics() {
        log.atInfo().addKeyValue(OPERATION, "admin.dashboard.findMemberClubParticipationStatistics")
                .log("관리자 회원 현황을 조회합니다.");
        Members allMembers = new Members(memberRepository.findAll());
        if (allMembers.isEmpty()) {
            logSuccess(0);
            return new AdminDashboardMembersResponse(
                    List.of(), BigDecimal.ZERO.setScale(AVERAGE_SCALE), List.of());
        }
        Map<Long, Long> clubCounts = clubMemberRepository.countJoinedClubsByMemberIds(allMembers.ids()).stream()
                .collect(Collectors.toMap(MemberClubCount::getMemberId, MemberClubCount::getClubCount));
        List<AdminDashboardMemberResponse> members = allMembers.asList().stream()
                .map(member -> new AdminDashboardMemberResponse(
                        member.getId(),
                        member.getNickname(),
                        clubCounts.getOrDefault(member.getId(), 0L)))
                .sorted(Comparator.comparingLong(AdminDashboardMemberResponse::clubCount).reversed()
                        .thenComparing(AdminDashboardMemberResponse::memberId))
                .toList();
        var response = new AdminDashboardMembersResponse(
                members,
                calculateAverageJoinedClubCount(members),
                calculateMemberDistributionByClubCount(members));
        logSuccess(response.members().size());
        return response;
    }

    private void logSuccess(int memberCount) {
        log.atInfo().addKeyValue(OPERATION, "admin.dashboard.findMemberClubParticipationStatistics").addKeyValue(PHASE, SUCCESS)
                .addKeyValue("memberCount", memberCount).log("관리자 회원 현황을 조회했습니다.");
    }

    private BigDecimal calculateAverageJoinedClubCount(List<AdminDashboardMemberResponse> members) {
        long totalClubCount = members.stream()
                .mapToLong(AdminDashboardMemberResponse::clubCount)
                .sum();
        return BigDecimal.valueOf(totalClubCount)
                .divide(BigDecimal.valueOf(members.size()), AVERAGE_SCALE, RoundingMode.HALF_UP);
    }

    private List<AdminDashboardClubCountDistributionResponse> calculateMemberDistributionByClubCount(
            List<AdminDashboardMemberResponse> members
    ) {
        Map<Long, Long> memberCountsByClubCount = new TreeMap<>();
        for (AdminDashboardMemberResponse member : members) {
            memberCountsByClubCount.merge(member.clubCount(), 1L, Long::sum);
        }
        return memberCountsByClubCount.entrySet().stream()
                .map(entry -> new AdminDashboardClubCountDistributionResponse(entry.getKey(), entry.getValue()))
                .toList();
    }
}
