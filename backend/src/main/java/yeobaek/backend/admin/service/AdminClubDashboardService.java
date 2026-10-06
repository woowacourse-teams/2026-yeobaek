package yeobaek.backend.admin.service;

import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.admin.dto.AdminDashboardClubResponse;
import yeobaek.backend.admin.dto.AdminDashboardClubsResponse;
import yeobaek.backend.space.api.club.ClubApi;
import yeobaek.backend.space.api.club.ClubMembershipApi;
import yeobaek.backend.space.api.club.ClubResponse;
import yeobaek.backend.readmodel.book.SpaceBookReadModel;
import yeobaek.backend.web.common.dto.BookResponse.Status;
import yeobaek.backend.readmodel.admin.AdminClubStatisticsReadModel;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminClubDashboardService {

    private final ClubApi clubsApi;
    private final ClubMembershipApi memberships;
    private final AdminClubStatisticsReadModel statisticsReadModel;
    private final SpaceBookReadModel bookReadModel;

    @Transactional(readOnly = true)
    public AdminDashboardClubsResponse findClubsWithMemberAndCommentCounts() {
        log.atInfo().addKeyValue(OPERATION, "admin.dashboard.findClubsWithMemberAndCommentCounts")
                .log("관리자 모임 현황을 조회합니다.");
        List<ClubResponse> clubs = clubsApi.findAll();
        if (clubs.isEmpty()) {
            logSuccess(0);
            return new AdminDashboardClubsResponse(List.of());
        }
        List<Long> clubIds = clubs.stream().map(ClubResponse::clubId).toList();
        Map<Long, Long> memberCounts = memberships.countJoinedMembers(clubIds);
        Map<Long, Long> commentCounts = statisticsReadModel.countComments(clubIds);
        var response = new AdminDashboardClubsResponse(clubs.stream()
                .map(club -> {
                    var book = bookReadModel.findBook(club.id()).orElseThrow();
                    return new AdminDashboardClubResponse(
                        club.clubId(),
                        club.name(),
                        book.bookId(),
                        book.title(),
                        Status.valueOf(book.status()),
                        memberCounts.getOrDefault(club.clubId(), 0L),
                        commentCounts.getOrDefault(club.clubId(), 0L));
                })
                .toList());
        logSuccess(response.clubs().size());
        return response;
    }

    private void logSuccess(int resultCount) {
        log.atInfo().addKeyValue(OPERATION, "admin.dashboard.findClubsWithMemberAndCommentCounts").addKeyValue(RESULT, SUCCESS)
                .addKeyValue("resultCount", resultCount).log("관리자 모임 현황을 조회했습니다.");
    }
}
