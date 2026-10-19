package yeobaek.backend.publicroom.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.auth.AuthMember;
import yeobaek.backend.book.dto.PassagesResponse;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.comment.dto.CommentCreateRequest;
import yeobaek.backend.comment.dto.CommentResponse;
import yeobaek.backend.comment.dto.CommentedSentencesResponse;
import yeobaek.backend.comment.dto.CommentsResponse;
import yeobaek.backend.comment.dto.NewCommentCountResponse;
import yeobaek.backend.comment.service.CommentService;
import yeobaek.backend.publicroom.dto.PublicRoomDetailResponse;
import yeobaek.backend.publicroom.dto.PublicRoomProgressResponse;
import yeobaek.backend.publicroom.dto.PublicRoomProgressUpdateRequest;
import yeobaek.backend.publicroom.dto.PublicRoomSort;
import yeobaek.backend.publicroom.dto.PublicRoomsResponse;
import yeobaek.backend.publicroom.dto.VisitedPublicRoomsResponse;
import yeobaek.backend.publicroom.service.PublicRoomService;

@Tag(name = "공개방")
@SecurityRequirement(name = "memberId")
@RestController
@RequiredArgsConstructor
public class PublicRoomController {

    private final PublicRoomService publicRoomService;
    private final CommentService commentService;

    @Operation(summary = "공개방 목록 조회 · 검색",
            description = "이용 가능한 책의 공개방을 전체 반환한다. keyword를 주면 제목 또는 작가 이름 부분 일치로 검색한다. 현재 기본 정렬과 MOST_VISITED는 중복을 제외한 누적 방문 회원 수 내림차순이며 동률 순서는 보장하지 않는다. 삭제 도서의 공개방은 제외한다.")
    @GetMapping("/api/public-rooms")
    public PublicRoomsResponse findAll(@AuthMember Long memberId,
                                       @Parameter(description = "정렬 기준. 생략하면 서비스 기본 정렬, 지원 값은 MOST_VISITED. 빈 문자열·미지원 값은 400(INVALID_REQUEST).")
                                       @RequestParam(required = false) String sort,
                                       @Parameter(description = "제목 또는 작가 이름 부분 일치 검색어. 미지정·빈 문자열·공백만 있으면 전체 목록")
                                       @RequestParam(required = false) String keyword) {
        return publicRoomService.findAll(memberId, PublicRoomSort.from(sort), keyword);
    }

    @GetMapping("/api/members/me/public-rooms")
    public VisitedPublicRoomsResponse findVisited(@AuthMember Long memberId) {
        return publicRoomService.findVisited(memberId);
    }

    @GetMapping("/api/public-rooms/{publicRoomId}")
    public PublicRoomDetailResponse findDetail(@AuthMember Long memberId,
                                               @PathVariable Long publicRoomId) {
        return publicRoomService.findDetail(memberId, publicRoomId);
    }

    @PostMapping("/api/public-rooms/{publicRoomId}/visits")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void visit(@AuthMember Long memberId, @PathVariable Long publicRoomId) {
        publicRoomService.visit(memberId, publicRoomId);
    }

    @GetMapping("/api/public-rooms/{publicRoomId}/passages")
    public PassagesResponse findPassages(@AuthMember Long memberId,
                                         @PathVariable Long publicRoomId,
                                         @RequestParam int from,
                                         @RequestParam int to) {
        return publicRoomService.findPassages(memberId, publicRoomId, from, to);
    }

    @PutMapping("/api/public-rooms/{publicRoomId}/progress")
    public PublicRoomProgressResponse updateProgress(@AuthMember Long memberId,
                                                      @PathVariable Long publicRoomId,
                                                      @Valid @RequestBody PublicRoomProgressUpdateRequest request) {
        return publicRoomService.updateProgress(memberId, publicRoomId, request.passageId());
    }

    @GetMapping("/api/public-rooms/{publicRoomId}/comments/new-count")
    public NewCommentCountResponse countNewComments(@AuthMember Long memberId,
                                                     @PathVariable Long publicRoomId,
                                                     @RequestParam Long currentPassageId) {
        return commentService.countNewPublicRoomComments(memberId, publicRoomId, currentPassageId);
    }

    @GetMapping("/api/public-rooms/{publicRoomId}/commented-sentences")
    public CommentedSentencesResponse findCommentedSentences(@AuthMember Long memberId,
                                                             @PathVariable Long publicRoomId,
                                                             @RequestParam Long currentPassageId) {
        return commentService.findPublicRoomCommentedSentences(memberId, publicRoomId, currentPassageId);
    }

    @PostMapping("/api/public-rooms/{publicRoomId}/sentences/{sentenceId}/comment-detail-views")
    public CommentsResponse findCommentDetails(@AuthMember Long memberId,
                                               @PathVariable Long publicRoomId,
                                               @PathVariable Long sentenceId) {
        return commentService.findPublicRoomComments(memberId, publicRoomId, sentenceId);
    }

    @PostMapping("/api/public-rooms/{publicRoomId}/sentences/{sentenceId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(@AuthMember Long memberId,
                                         @PathVariable Long publicRoomId,
                                         @PathVariable Long sentenceId,
                                         @Valid @RequestBody CommentCreateRequest request) {
        CommentContent content = request.content();
        return commentService.createInPublicRoom(memberId, publicRoomId, sentenceId, content);
    }
}
