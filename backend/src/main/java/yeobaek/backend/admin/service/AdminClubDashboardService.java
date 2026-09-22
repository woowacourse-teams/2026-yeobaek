package yeobaek.backend.admin.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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
public class AdminClubDashboardService {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final CommentRepository commentRepository;

    @Transactional(readOnly = true)
    public AdminDashboardClubsResponse findClubs() {
        Clubs clubs = new Clubs(clubRepository.findAllWithBookByOrderByIdAsc());
        if (clubs.isEmpty()) {
            return new AdminDashboardClubsResponse(List.of());
        }
        List<Long> clubIds = clubs.ids();
        Map<Long, Long> memberCounts = clubMemberRepository.countJoinedByClubIds(clubIds).stream()
                .collect(Collectors.toMap(ClubMemberCount::getClubId, ClubMemberCount::getMemberCount));
        Map<Long, Long> commentCounts = commentRepository.countByClubIds(clubIds).stream()
                .collect(Collectors.toMap(ClubCommentCount::getClubId, ClubCommentCount::getCommentCount));
        return new AdminDashboardClubsResponse(clubs.asList().stream()
                .map(club -> new AdminDashboardClubResponse(
                        club.getId(),
                        club.getName(),
                        club.getBook().getId(),
                        club.getBook().getTitle().value(),
                        club.getBook().getStatus(),
                        memberCounts.getOrDefault(club.getId(), 0L),
                        commentCounts.getOrDefault(club.getId(), 0L)))
                .toList());
    }
}
