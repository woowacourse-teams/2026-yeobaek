package yeobaek.backend.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.admin.dto.AdminDashboardClubResponse;
import yeobaek.backend.admin.dto.AdminDashboardClubsResponse;
import yeobaek.backend.club.repository.ClubRepository;

@Service
@RequiredArgsConstructor
public class AdminClubDashboardService {

    private final ClubRepository clubRepository;

    @Transactional(readOnly = true)
    public AdminDashboardClubsResponse findClubs() {
        return new AdminDashboardClubsResponse(clubRepository.findAdminDashboardStatistics().stream()
                .map(statistics -> new AdminDashboardClubResponse(
                        statistics.getClubId(),
                        statistics.getName(),
                        statistics.getBookId(),
                        statistics.getBookTitle(),
                        statistics.getBookStatus(),
                        statistics.getMemberCount(),
                        statistics.getCommentCount()))
                .toList());
    }
}
