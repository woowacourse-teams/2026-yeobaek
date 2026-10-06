package yeobaek.backend.web.v2.content;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.application.content.ContentMetadataQueryService;
import yeobaek.backend.auth.AuthMember;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.MemberId;

@Tag(name = "v2 컨텐츠")
@SecurityRequirement(name = "memberId")
@RestController
@RequiredArgsConstructor
public class V2ContentController {

    private final ContentMetadataQueryService contents;
    private final ContentWebAdapterRegistry adapters;

    @Operation(summary = "컨텐츠 목록 조회 · 검색")
    @GetMapping("/api/v2/contents")
    public ContentResponses.Contents findContents(
            @AuthMember MemberId memberId,
            @Parameter(description = "컨텐츠 종류") @RequestParam ContentKind kind,
            @Parameter(description = "제목 또는 제작자 이름 부분 일치 검색어")
            @RequestParam(required = false) String keyword) {
        return new ContentResponses.Contents(contents.search(kind, keyword).stream()
                .map(adapters::summary)
                .toList());
    }

    @Operation(summary = "컨텐츠 상세와 목차 조회")
    @GetMapping("/api/v2/contents/{contentId}")
    public ContentResponses.DetailResponse findContent(
            @AuthMember MemberId memberId,
            @Parameter(description = "컨텐츠 ID", schema = @Schema(implementation = Long.class))
            @PathVariable ContentId contentId) {
        return adapters.detail(contents.getDetail(contentId));
    }
}
