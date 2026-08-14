package yeobaek.backend.club.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.auth.AuthMember;
import yeobaek.backend.club.dto.ClubCreateRequest;
import yeobaek.backend.club.dto.ClubCreateResponse;
import yeobaek.backend.club.dto.ClubDetailResponse;
import yeobaek.backend.club.dto.ClubJoinRequest;
import yeobaek.backend.club.dto.ClubJoinResponse;
import yeobaek.backend.club.dto.MyClubsResponse;
import yeobaek.backend.club.service.ClubService;

@Tag(name = "모임")
@SecurityRequirement(name = "memberId")
@RestController
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

    @Operation(summary = "모임 생성",
            description = "책 한 권을 골라 모임을 만든다. 생성자는 자동으로 모임에 참여되고 참여 코드가 발급된다.")
    @PostMapping("/api/clubs")
    @ResponseStatus(HttpStatus.CREATED)
    public ClubCreateResponse create(@AuthMember Long memberId, @RequestBody ClubCreateRequest request) {
        return clubService.create(memberId, request.name(), request.bookId());
    }

    @Operation(summary = "참여 코드로 모임 참여",
            description = "존재하지 않는 코드는 400(JOIN_CODE_NOT_FOUND). 이미 참여한 모임이면 같은 응답을 반환한다(멱등).")
    @PostMapping("/api/clubs/join")
    public ClubJoinResponse join(@AuthMember Long memberId, @RequestBody ClubJoinRequest request) {
        return clubService.join(memberId, request.joinCode());
    }

    @Operation(summary = "내 모임 목록 조회")
    @GetMapping("/api/clubs")
    public MyClubsResponse findMyClubs(@AuthMember Long memberId) {
        return clubService.findMyClubs(memberId);
    }

    @Operation(summary = "모임 상세 조회",
            description = "모임 상세 화면용: 초대 코드, 참여자 목록(참여 시각 오름차순), 내 진도. 모임 미소속은 403(NOT_CLUB_MEMBER).")
    @GetMapping("/api/clubs/{clubId}")
    public ClubDetailResponse findDetail(@AuthMember Long memberId, @Parameter(description = "모임 ID") @PathVariable Long clubId) {
        return clubService.findDetail(memberId, clubId);
    }
}
