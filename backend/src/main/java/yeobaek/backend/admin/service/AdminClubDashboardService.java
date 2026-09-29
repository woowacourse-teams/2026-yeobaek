package yeobaek.backend.admin.service;

import static yeobaek.backend.support.LogField.ATTEMPT;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.PHASE;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.admin.dto.AdminDashboardClubResponse;
import yeobaek.backend.admin.dto.AdminDashboardClubsResponse;
import yeobaek.backend.club.domain.Clubs;
import yeobaek.backend.club.repository.ClubMemberCount;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.repository.ClubCommentCount;
import yeobaek.backend.comment.repository.CommentRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminClubDashboardService {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final CommentRepository commentRepository;

    @Transactional(readOnly = true)
    public AdminDashboardClubsResponse findClubsWithMemberAndCommentCounts() {
        log.atInfo().addKeyValue(OPERATION, "admin.dashboard.clubs").addKeyValue(PHASE, ATTEMPT)
                .log("관리자 모임 현황을 조회합니다.");
        Clubs clubs = new Clubs(clubRepository.findAllWithBookByOrderByIdAsc());
        if (clubs.isEmpty()) {
            logSuccess(0);
            return new AdminDashboardClubsResponse(List.of());
        }
        List<Long> clubIds = clubs.ids();
        Map<Long, Long> memberCounts = clubMemberRepository.countJoinedMembersByClubIds(clubIds).stream()
                .collect(Collectors.toMap(ClubMemberCount::getClubId, ClubMemberCount::getMemberCount));
        Map<Long, Long> commentCounts = commentRepository.countCommentsByClubIds(clubIds).stream()
                .collect(Collectors.toMap(ClubCommentCount::getClubId, ClubCommentCount::getCommentCount));
        var response = new AdminDashboardClubsResponse(clubs.asList().stream()
                .map(club -> new AdminDashboardClubResponse(
                        club.getId(),
                        club.getName(),
                        club.getBook().getId(),
                        club.getBook().getTitle().value(),
                        club.getBook().getStatus(),
                        memberCounts.getOrDefault(club.getId(), 0L),
                        commentCounts.getOrDefault(club.getId(), 0L)))
                .toList());
        logSuccess(response.clubs().size());
        return response;
    }

    private void logSuccess(int resultCount) {
        log.atInfo().addKeyValue(OPERATION, "admin.dashboard.clubs").addKeyValue(PHASE, SUCCESS)
                .addKeyValue("resultCount", resultCount).log("관리자 모임 현황을 조회했습니다.");
    }
}
