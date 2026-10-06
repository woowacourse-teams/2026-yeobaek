package yeobaek.backend.web.v2.reading;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.application.content.ContentReadingQueryService;
import yeobaek.backend.application.reading.ReadingActivityQueryService;
import yeobaek.backend.application.reading.ReadingActivityResult;
import yeobaek.backend.application.reading.ReadingProgressCommandService;
import yeobaek.backend.auth.AuthMember;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.web.v2.content.ContentWebAdapterRegistry;
import yeobaek.backend.web.v2.space.SpaceWebAdapterRegistry;

@Tag(name = "v2 독서")
@SecurityRequirement(name = "memberId")
@RestController
@RequiredArgsConstructor
public class V2ReadingController {

    private final ContentReadingQueryService bodies;
    private final ReadingProgressCommandService progress;
    private final ReadingActivityQueryService activities;
    private final ContentWebAdapterRegistry contentAdapters;
    private final SpaceWebAdapterRegistry spaceAdapters;

    @Operation(summary = "공간의 컨텐츠 본문 조회")
    @GetMapping("/api/v2/spaces/{spaceId}/contents/{contentId}/body")
    public ReadingResponses.Body findBody(
            @AuthMember MemberId memberId,
            @Parameter(description = "공간 ID", schema = @Schema(implementation = Long.class))
            @PathVariable SpaceId spaceId,
            @Parameter(description = "컨텐츠 ID", schema = @Schema(implementation = Long.class))
            @PathVariable ContentId contentId,
            @Parameter(description = "첫 문단 순서. 1부터 시작하며 양끝을 포함") @RequestParam int from,
            @Parameter(description = "마지막 문단 순서. 양끝을 포함") @RequestParam int to) {
        return ReadingResponses.from(bodies.findBody(memberId, spaceId, contentId, from, to));
    }

    @Operation(summary = "독서 진도 갱신")
    @PutMapping("/api/v2/spaces/{spaceId}/contents/{contentId}/reading-progress")
    public ReadingResponses.Progress updateProgress(
            @AuthMember MemberId memberId,
            @Parameter(description = "공간 ID", schema = @Schema(implementation = Long.class))
            @PathVariable SpaceId spaceId,
            @Parameter(description = "컨텐츠 ID", schema = @Schema(implementation = Long.class))
            @PathVariable ContentId contentId,
            @Valid @RequestBody UpdateProgressRequest request) {
        return ReadingResponses.from(progress.update(memberId, spaceId, contentId, request.lastReadLocationId()));
    }

    @Operation(summary = "마지막으로 읽은 모임 조회")
    @GetMapping("/api/v2/members/me/last-reading")
    public ResponseEntity<ReadingResponses.Reading> findLastReading(@AuthMember MemberId memberId) {
        return response(activities.findLastClub(memberId));
    }

    @Operation(summary = "가장 최근 독서 기록 조회")
    @GetMapping("/api/v2/members/me/recent-reading")
    public ResponseEntity<ReadingResponses.Reading> findRecentReading(@AuthMember MemberId memberId) {
        return response(activities.findRecent(memberId));
    }

    private ResponseEntity<ReadingResponses.Reading> response(Optional<ReadingActivityResult> result) {
        return result.map(reading -> ReadingResponses.from(reading,
                        spaceAdapters.get(reading.space().kind()).mapReading(reading.space()),
                        contentAdapters.card(reading.content())))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    public record UpdateProgressRequest(
            @NotNull @Schema(description = "마지막으로 읽은 canonical PASSAGE 위치 ID",
                    implementation = Long.class)
            ContentLocationId lastReadLocationId) {
    }
}
