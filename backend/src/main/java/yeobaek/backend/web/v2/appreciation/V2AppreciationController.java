package yeobaek.backend.web.v2.appreciation;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.application.appreciation.CommentModificationWorkflow;
import yeobaek.backend.application.appreciation.CommentQueryWorkflow;
import yeobaek.backend.application.appreciation.CommentReportWorkflow;
import yeobaek.backend.appreciation.api.AppreciationKind;
import yeobaek.backend.auth.AuthMember;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;

@Tag(name = "v2 감상")
@SecurityRequirement(name = "memberId")
@RestController
@RequiredArgsConstructor
public class V2AppreciationController {

    private static final String SPACE_ID_DESCRIPTION = "공간 ID";
    private static final String CONTENT_ID_DESCRIPTION = "컨텐츠 ID";

    private final AppreciationWebAdapterRegistry adapters;
    private final CommentQueryWorkflow comments;
    private final CommentModificationWorkflow modifications;
    private final CommentReportWorkflow reports;

    @Operation(summary = "컨텐츠 위치의 감상 목록 조회", description = "보이는 감상을 반환하고 확인 처리한다.")
    @GetMapping("/api/v2/spaces/{spaceId}/contents/{contentId}/locations/{locationId}/appreciations")
    public AppreciationResponses.Appreciations find(
            @AuthMember MemberId memberId,
            @Parameter(description = SPACE_ID_DESCRIPTION, schema = @Schema(implementation = Long.class))
            @PathVariable SpaceId spaceId,
            @Parameter(description = CONTENT_ID_DESCRIPTION, schema = @Schema(implementation = Long.class))
            @PathVariable ContentId contentId,
            @Parameter(description = "컨텐츠 위치 ID", schema = @Schema(implementation = Long.class))
            @PathVariable ContentLocationId locationId,
            @Parameter(description = "감상 종류") @RequestParam AppreciationKind kind) {
        return new AppreciationResponses.Appreciations(adapters.get(kind).find(memberId, spaceId,
                contentId, locationId));
    }

    @Operation(summary = "컨텐츠 위치의 감상 상세 조회와 확인")
    @PostMapping("/api/v2/spaces/{spaceId}/contents/{contentId}/locations/{locationId}/appreciation-views")
    public AppreciationResponses.Appreciations view(
            @AuthMember MemberId memberId,
            @Parameter(description = SPACE_ID_DESCRIPTION, schema = @Schema(implementation = Long.class))
            @PathVariable SpaceId spaceId,
            @Parameter(description = CONTENT_ID_DESCRIPTION, schema = @Schema(implementation = Long.class))
            @PathVariable ContentId contentId,
            @Parameter(description = "컨텐츠 위치 ID", schema = @Schema(implementation = Long.class))
            @PathVariable ContentLocationId locationId) {
        return new AppreciationResponses.Appreciations(adapters.get(AppreciationKind.COMMENT).find(memberId,
                spaceId, contentId, locationId));
    }

    @Operation(summary = "감상 공유")
    @PostMapping("/api/v2/spaces/{spaceId}/contents/{contentId}/locations/{locationId}/appreciations")
    @ResponseStatus(HttpStatus.CREATED)
    public AppreciationResponses.Appreciation create(
            @AuthMember MemberId memberId,
            @Parameter(description = SPACE_ID_DESCRIPTION, schema = @Schema(implementation = Long.class))
            @PathVariable SpaceId spaceId,
            @Parameter(description = CONTENT_ID_DESCRIPTION, schema = @Schema(implementation = Long.class))
            @PathVariable ContentId contentId,
            @Parameter(description = "컨텐츠 위치 ID", schema = @Schema(implementation = Long.class))
            @PathVariable ContentLocationId locationId,
            @Valid @RequestBody AppreciationRequests.Write request) {
        return adapters.get(request.kind()).create(memberId, spaceId, contentId, locationId, request.data());
    }

    @Operation(summary = "새 감상 개수 조회")
    @GetMapping("/api/v2/spaces/{spaceId}/contents/{contentId}/appreciations/unread-count")
    public AppreciationResponses.UnreadCount countUnread(
            @AuthMember MemberId memberId,
            @Parameter(description = SPACE_ID_DESCRIPTION, schema = @Schema(implementation = Long.class))
            @PathVariable SpaceId spaceId,
            @Parameter(description = CONTENT_ID_DESCRIPTION, schema = @Schema(implementation = Long.class))
            @PathVariable ContentId contentId,
            @Parameter(description = "현재 문단 위치 ID", schema = @Schema(implementation = Long.class))
            @RequestParam ContentLocationId currentLocationId) {
        long count = comments.countNewComments(memberId, spaceId, contentId, currentLocationId);
        return new AppreciationResponses.UnreadCount(count);
    }

    @Operation(summary = "감상이 있는 컨텐츠 위치 목록 조회")
    @GetMapping("/api/v2/spaces/{spaceId}/contents/{contentId}/appreciation-discovery")
    public AppreciationResponses.Discovery findDiscovery(
            @AuthMember MemberId memberId,
            @Parameter(description = SPACE_ID_DESCRIPTION, schema = @Schema(implementation = Long.class))
            @PathVariable SpaceId spaceId,
            @Parameter(description = CONTENT_ID_DESCRIPTION, schema = @Schema(implementation = Long.class))
            @PathVariable ContentId contentId,
            @Parameter(description = "현재 문단 위치 ID", schema = @Schema(implementation = Long.class))
            @RequestParam ContentLocationId currentLocationId) {
        return new AppreciationResponses.Discovery(comments.findDiscovery(memberId,
                        spaceId, contentId, currentLocationId)
                .stream().map(AppreciationResponses::from).toList());
    }

    @Operation(summary = "감상 수정")
    @PutMapping("/api/v2/appreciations/{appreciationId}")
    public AppreciationResponses.Appreciation update(
            @AuthMember MemberId memberId,
            @Parameter(description = "감상 ID", schema = @Schema(implementation = Long.class))
            @PathVariable AppreciationId appreciationId,
            @Valid @RequestBody AppreciationRequests.Write request) {
        return adapters.get(request.kind()).update(memberId, appreciationId, request.data());
    }

    @Operation(summary = "감상 삭제")
    @DeleteMapping("/api/v2/appreciations/{appreciationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthMember MemberId memberId,
                       @Parameter(description = "감상 ID", schema = @Schema(implementation = Long.class))
                       @PathVariable AppreciationId appreciationId) {
        modifications.delete(memberId, appreciationId);
    }

    @Operation(summary = "감상 신고")
    @PostMapping("/api/v2/appreciations/{appreciationId}/reports")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@AuthMember MemberId memberId,
                       @Parameter(description = "감상 ID", schema = @Schema(implementation = Long.class))
                       @PathVariable AppreciationId appreciationId) {
        reports.report(memberId, appreciationId);
    }
}
