package yeobaek.backend.admin.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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
public class AdminMemberDashboardService {

    private static final int AVERAGE_SCALE = 2;

    private final MemberRepository memberRepository;
    private final ClubMemberRepository clubMemberRepository;

    @Transactional(readOnly = true)
    public AdminDashboardMembersResponse findMembers() {
        Members allMembers = new Members(memberRepository.findAll());
        if (allMembers.isEmpty()) {
            return new AdminDashboardMembersResponse(
                    List.of(), BigDecimal.ZERO.setScale(AVERAGE_SCALE), List.of());
        }
        Map<Long, Long> clubCounts = clubMemberRepository.countJoinedByMemberIds(allMembers.ids()).stream()
                .collect(Collectors.toMap(MemberClubCount::getMemberId, MemberClubCount::getClubCount));
        List<AdminDashboardMemberResponse> members = allMembers.asList().stream()
                .map(member -> new AdminDashboardMemberResponse(
                        member.getId(),
                        member.getNickname(),
                        clubCounts.getOrDefault(member.getId(), 0L)))
                .sorted(Comparator.comparingLong(AdminDashboardMemberResponse::clubCount).reversed()
                        .thenComparing(AdminDashboardMemberResponse::memberId))
                .toList();
        return new AdminDashboardMembersResponse(members, averageClubCount(members), distribution(members));
    }

    private BigDecimal averageClubCount(List<AdminDashboardMemberResponse> members) {
        long totalClubCount = members.stream()
                .mapToLong(AdminDashboardMemberResponse::clubCount)
                .sum();
        return BigDecimal.valueOf(totalClubCount)
                .divide(BigDecimal.valueOf(members.size()), AVERAGE_SCALE, RoundingMode.HALF_UP);
    }

    private List<AdminDashboardClubCountDistributionResponse> distribution(
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
