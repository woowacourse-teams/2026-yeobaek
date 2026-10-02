package yeobaek.backend.publicroom.controller;

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
import yeobaek.backend.publicroom.dto.PublicRoomDetailResponse;
import yeobaek.backend.publicroom.dto.PublicRoomProgressResponse;
import yeobaek.backend.publicroom.dto.PublicRoomProgressUpdateRequest;
import yeobaek.backend.publicroom.dto.PublicRoomSort;
import yeobaek.backend.publicroom.dto.PublicRoomsResponse;
import yeobaek.backend.publicroom.dto.VisitedPublicRoomsResponse;
import yeobaek.backend.web.compatibility.CommentService;
import yeobaek.backend.web.compatibility.PublicRoomService;

@RestController
@RequiredArgsConstructor
public class PublicRoomController {

    private final PublicRoomService publicRoomService;
    private final CommentService commentService;

    @GetMapping("/api/public-rooms")
    public PublicRoomsResponse findAll(@AuthMember Long memberId,
                                       @RequestParam(required = false) String sort) {
        return publicRoomService.findAll(memberId, PublicRoomSort.from(sort));
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
