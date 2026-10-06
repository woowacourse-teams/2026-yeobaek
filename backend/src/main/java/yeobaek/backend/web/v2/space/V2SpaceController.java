package yeobaek.backend.web.v2.space;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.application.club.ClubMembershipWorkflow;
import yeobaek.backend.application.publicroom.PublicRoomVisitWorkflow;
import yeobaek.backend.application.space.query.SpaceQueryService;
import yeobaek.backend.auth.AuthMember;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.SpaceKind;

@Tag(name = "v2 공간")
@SecurityRequirement(name = "memberId")
@RestController
@RequiredArgsConstructor
public class V2SpaceController {

    private final SpaceWebAdapterRegistry adapters;
    private final SpaceQueryService spaces;
    private final ClubMembershipWorkflow memberships;
    private final PublicRoomVisitWorkflow visits;

    @Operation(summary = "공간 생성")
    @PostMapping("/api/v2/spaces")
    @ResponseStatus(HttpStatus.CREATED)
    public SpaceResponses.Space create(@AuthMember MemberId memberId,
                                       @Valid @RequestBody SpaceRequests.Create request) {
        return adapters.get(request.kind()).create(memberId, request.data());
    }

    @Operation(summary = "참여 코드로 공간 가입")
    @PostMapping("/api/v2/spaces/join")
    public SpaceResponses.Space join(@AuthMember MemberId memberId, @Valid @RequestBody SpaceRequests.Join request) {
        return adapters.get(request.kind()).join(memberId, request.data());
    }

    @Operation(summary = "공간 탈퇴")
    @DeleteMapping("/api/v2/spaces/{spaceId}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@AuthMember MemberId memberId,
                      @Parameter(description = "공간 ID", schema = @Schema(implementation = Long.class))
                      @PathVariable SpaceId spaceId) {
        memberships.leave(memberId, spaceId);
    }

    @Operation(summary = "내 공간 목록 조회")
    @GetMapping("/api/v2/members/me/spaces")
    public SpaceResponses.Spaces findMine(@AuthMember MemberId memberId,
                                           @Parameter(description = "공간 종류") @RequestParam SpaceKind kind) {
        SpaceWebAdapter adapter = adapters.get(kind);
        return new SpaceResponses.Spaces(spaces.findMine(memberId, kind).stream()
                .map(adapter::map)
                .toList());
    }

    @Operation(summary = "공간 상세 조회")
    @GetMapping("/api/v2/spaces/{spaceId}")
    public SpaceResponses.Space findDetail(@AuthMember MemberId memberId,
                                            @Parameter(description = "공간 ID",
                                                    schema = @Schema(implementation = Long.class))
                                            @PathVariable SpaceId spaceId) {
        var result = spaces.findDetail(memberId, spaceId);
        return adapters.get(result.kind()).map(result);
    }

    @Operation(summary = "공개 공간 목록 조회")
    @GetMapping("/api/v2/spaces")
    public SpaceResponses.Spaces findPublic(
            @AuthMember MemberId memberId,
            @Parameter(description = "공간 종류") @RequestParam SpaceKind kind,
            @Parameter(description = "정렬. 공개방은 MOST_VISITED만 지원")
            @RequestParam(required = false) String sort) {
        SpaceWebAdapter adapter = adapters.get(kind);
        return new SpaceResponses.Spaces(adapter.findPublic(memberId, sort).stream()
                .map(adapter::map)
                .toList());
    }

    @Operation(summary = "공간 방문 기록")
    @PostMapping("/api/v2/spaces/{spaceId}/visits")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void visit(@AuthMember MemberId memberId,
                      @Parameter(description = "공간 ID", schema = @Schema(implementation = Long.class))
                      @PathVariable SpaceId spaceId) {
        visits.visit(memberId, spaceId);
    }
}
