package yeobaek.backend.admin.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.admin.dto.AdminDashboardClubCountDistributionResponse;
import yeobaek.backend.admin.dto.AdminDashboardMemberResponse;
import yeobaek.backend.admin.dto.AdminDashboardMembersResponse;
import yeobaek.backend.member.repository.AdminMemberDashboardStatistics;
import yeobaek.backend.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class AdminMemberDashboardService {

    private static final int AVERAGE_SCALE = 2;

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public AdminDashboardMembersResponse findMembers() {
        List<AdminMemberDashboardStatistics> statistics = memberRepository.findAdminDashboardStatistics();
        List<AdminDashboardMemberResponse> members = statistics.stream()
                .map(statistic -> new AdminDashboardMemberResponse(
                        statistic.getMemberId(),
                        statistic.getNickname(),
                        statistic.getClubCount()))
                .toList();
        return new AdminDashboardMembersResponse(members, averageClubCount(statistics), distribution(statistics));
    }

    private BigDecimal averageClubCount(List<AdminMemberDashboardStatistics> statistics) {
        if (statistics.isEmpty()) {
            return BigDecimal.ZERO.setScale(AVERAGE_SCALE);
        }
        long totalClubCount = statistics.stream()
                .mapToLong(AdminMemberDashboardStatistics::getClubCount)
                .sum();
        return BigDecimal.valueOf(totalClubCount)
                .divide(BigDecimal.valueOf(statistics.size()), AVERAGE_SCALE, RoundingMode.HALF_UP);
    }

    private List<AdminDashboardClubCountDistributionResponse> distribution(
            List<AdminMemberDashboardStatistics> statistics
    ) {
        Map<Long, Long> memberCountsByClubCount = new TreeMap<>();
        for (AdminMemberDashboardStatistics statistic : statistics) {
            memberCountsByClubCount.merge(statistic.getClubCount(), 1L, Long::sum);
        }
        return memberCountsByClubCount.entrySet().stream()
                .map(entry -> new AdminDashboardClubCountDistributionResponse(entry.getKey(), entry.getValue()))
                .toList();
    }
}
