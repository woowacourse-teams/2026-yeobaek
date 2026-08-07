package watson.backend.club.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import watson.backend.auth.AuthMember;
import watson.backend.club.dto.ClubCreateRequest;
import watson.backend.club.dto.ClubCreateResponse;
import watson.backend.club.dto.ClubDetailResponse;
import watson.backend.club.dto.ClubJoinRequest;
import watson.backend.club.dto.ClubJoinResponse;
import watson.backend.club.dto.MyClubsResponse;
import watson.backend.club.service.ClubService;

@RestController
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

    @PostMapping("/api/clubs")
    @ResponseStatus(HttpStatus.CREATED)
    public ClubCreateResponse create(@AuthMember Long memberId, @RequestBody ClubCreateRequest request) {
        return clubService.create(memberId, request.name(), request.bookId());
    }

    @PostMapping("/api/clubs/join")
    public ClubJoinResponse join(@AuthMember Long memberId, @RequestBody ClubJoinRequest request) {
        return clubService.join(memberId, request.joinCode());
    }

    @GetMapping("/api/clubs")
    public MyClubsResponse findMyClubs(@AuthMember Long memberId) {
        return clubService.findMyClubs(memberId);
    }

    @GetMapping("/api/clubs/{clubId}")
    public ClubDetailResponse findDetail(@AuthMember Long memberId, @PathVariable Long clubId) {
        return clubService.findDetail(memberId, clubId);
    }
}
