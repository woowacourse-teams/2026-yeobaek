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

    private static final String PUBLIC_ROOM_ID_DESCRIPTION = "공개방 ID (도서·모임 ID와 별개)";

    private final PublicRoomService publicRoomService;
    private final CommentService commentService;

    @Operation(summary = "전체 공개방 목록 조회 · 검색",
            description = "이용 가능한 책의 공개방을 전체 반환한다. keyword를 주면 제목 또는 작가 이름 부분 일치로 검색한다. 현재 기본 정렬과 MOST_VISITED는 중복을 제외한 누적 방문 회원 수 내림차순이며 동률 순서는 보장하지 않는다. 삭제 도서의 공개방은 제외한다.")
    @GetMapping("/api/public-rooms")
    public PublicRoomsResponse findAll(@AuthMember Long memberId,
                                       @Parameter(description = "정렬 기준. 생략하면 서비스 기본 정렬, 지원 값은 MOST_VISITED. 빈 문자열·미지원 값은 400(INVALID_REQUEST).")
                                       @RequestParam(required = false) String sort,
                                       @Parameter(description = "제목 또는 작가 이름 부분 일치 검색어. 미지정·빈 문자열·공백만 있으면 전체 목록")
                                       @RequestParam(required = false) String keyword) {
        return publicRoomService.findAll(memberId, PublicRoomSort.from(sort), keyword);
    }

    @Operation(summary = "내가 방문한 공개방 목록 조회",
            description = "최근 방문 시각 내림차순으로 전체 반환하며 삭제 도서의 공개방은 제외한다. 조회는 방문 시각·진도·댓글 확인 상태를 변경하지 않는다.")
    @GetMapping("/api/members/me/public-rooms")
    public VisitedPublicRoomsResponse findVisited(@AuthMember Long memberId) {
        return publicRoomService.findVisited(memberId);
    }

    @Operation(summary = "공개방 정보 조회",
            description = "방문 여부와 관계없이 도서 정보·내 진도·최근 방문 시각을 조회한다. 진도와 방문 기록이 없으면 각각 null이다. 삭제 도서도 book.status=DELETED로 반환하며 조회는 방문·진도·댓글 확인 상태를 변경하지 않는다.")
    @GetMapping("/api/public-rooms/{publicRoomId}")
    public PublicRoomDetailResponse findDetail(@AuthMember Long memberId,
                                               @Parameter(description = PUBLIC_ROOM_ID_DESCRIPTION) @PathVariable Long publicRoomId) {
        return publicRoomService.findDetail(memberId, publicRoomId);
    }

    @Operation(summary = "공개방 방문 기록",
            description = "독서 화면 진입 시 호출한다. 호출마다 최근 방문 시각을 갱신하며 재방문해도 방문 회원 수는 중복 집계하지 않는다. 진도·댓글 확인 상태는 변경하지 않는다. 삭제 도서는 400(BOOK_NOT_AVAILABLE).")
    @PostMapping("/api/public-rooms/{publicRoomId}/visits")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void visit(@AuthMember Long memberId,
                      @Parameter(description = PUBLIC_ROOM_ID_DESCRIPTION) @PathVariable Long publicRoomId) {
        publicRoomService.visit(memberId, publicRoomId);
    }

    @Operation(summary = "공개방 본문 범위 조회",
            description = "전체 순서 기준 범위(양 끝 포함)의 본문을 순서대로 반환한다. 1 ≤ from ≤ to, to-from+1 ≤ 100을 만족하지 않으면 400(INVALID_REQUEST). 댓글 수는 이 공개방에서 요청 회원에게 보이는 댓글만 집계한다. 방문·진도·댓글 확인 상태는 변경하지 않는다.")
    @GetMapping("/api/public-rooms/{publicRoomId}/passages")
    public PassagesResponse findPassages(@AuthMember Long memberId,
                                         @Parameter(description = PUBLIC_ROOM_ID_DESCRIPTION) @PathVariable Long publicRoomId,
                                         @Parameter(description = "시작 본문의 전체 순서 (1 이상)")
                                         @RequestParam int from,
                                         @Parameter(description = "끝 본문의 전체 순서 (양 끝 포함, to-from+1 ≤ 100)")
                                         @RequestParam int to) {
        return publicRoomService.findPassages(memberId, publicRoomId, from, to);
    }

    @Operation(summary = "공개방 진도 갱신 (최근 열람 보고)",
            description = "최근 열람 문단으로 덮어쓰며 앞부분 재열람 시 진도율은 후퇴할 수 있다. 이 공개방의 내 진도와 lastReadAt만 갱신하고 통합 최근 읽기에 반영한다. 없거나 다른 책의 문단이면 400(PASSAGE_NOT_FOUND).")
    @PutMapping("/api/public-rooms/{publicRoomId}/progress")
    public PublicRoomProgressResponse updateProgress(@AuthMember Long memberId,
                                                      @Parameter(description = PUBLIC_ROOM_ID_DESCRIPTION) @PathVariable Long publicRoomId,
                                                      @Valid @RequestBody PublicRoomProgressUpdateRequest request) {
        return publicRoomService.updateProgress(memberId, publicRoomId, request.passageId());
    }

    @Operation(summary = "공개방 새 댓글 개수 조회",
            description = "현재 문단까지 요청 회원에게 보이는 NEW 댓글 수를 반환한다. 댓글 확인 상태·진도·방문 시각은 변경하지 않는다. 없거나 다른 책의 현재 문단이면 400(INVALID_REQUEST).")
    @GetMapping("/api/public-rooms/{publicRoomId}/comments/new-count")
    public NewCommentCountResponse countNewComments(@AuthMember Long memberId,
                                                     @Parameter(description = PUBLIC_ROOM_ID_DESCRIPTION) @PathVariable Long publicRoomId,
                                                     @Parameter(description = "현재 읽고 있는 문단 ID (저장 진도와 별개인 댓글 발견·미래 문장 판단 기준)")
                                                     @RequestParam Long currentPassageId) {
        return commentService.countNewPublicRoomComments(memberId, publicRoomId, currentPassageId);
    }

    @Operation(summary = "공개방 댓글 문장 전체 목록 조회",
            description = "보이는 댓글이 있는 책 전체의 문장을 새 댓글 문장 → 미래 문장 → 확인한 문장 순으로 반환한다. 미래 문장에 미확인 댓글이 있으면 REVEAL_REQUIRED이며 본문은 응답에 포함한다. 조회는 댓글 확인 상태·진도·방문 시각을 변경하지 않는다. 없거나 다른 책의 현재 문단이면 400(INVALID_REQUEST).")
    @GetMapping("/api/public-rooms/{publicRoomId}/commented-sentences")
    public CommentedSentencesResponse findCommentedSentences(@AuthMember Long memberId,
                                                             @Parameter(description = PUBLIC_ROOM_ID_DESCRIPTION) @PathVariable Long publicRoomId,
                                                             @Parameter(description = "현재 읽고 있는 문단 ID (저장 진도와 별개인 댓글 발견·미래 문장 판단 기준)")
                                                             @RequestParam Long currentPassageId) {
        return commentService.findPublicRoomCommentedSentences(memberId, publicRoomId, currentPassageId);
    }

    @Operation(summary = "공개방 문장의 댓글 상세 조회와 직접 확인",
            description = "보이는 댓글을 작성일 오름차순으로 반환하고 VIEWED로 전환한다. 보이는 댓글이 없으면 빈 배열을 반환한다. 없거나 다른 책의 문장이면 400(SENTENCE_NOT_FOUND).")
    @PostMapping("/api/public-rooms/{publicRoomId}/sentences/{sentenceId}/comment-detail-views")
    public CommentsResponse findCommentDetails(@AuthMember Long memberId,
                                               @Parameter(description = PUBLIC_ROOM_ID_DESCRIPTION) @PathVariable Long publicRoomId,
                                               @Parameter(description = "문장 ID") @PathVariable Long sentenceId) {
        return commentService.findPublicRoomComments(memberId, publicRoomId, sentenceId);
    }

    @Operation(summary = "공개방 댓글 작성",
            description = "1~1000자의 댓글을 작성한다. 작성자에게는 즉시 VIEWED로 처리한다. 없거나 다른 책의 문장이면 400(SENTENCE_NOT_FOUND).")
    @PostMapping("/api/public-rooms/{publicRoomId}/sentences/{sentenceId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(@AuthMember Long memberId,
                                         @Parameter(description = PUBLIC_ROOM_ID_DESCRIPTION) @PathVariable Long publicRoomId,
                                         @Parameter(description = "문장 ID") @PathVariable Long sentenceId,
                                         @Valid @RequestBody CommentCreateRequest request) {
        CommentContent content = request.content();
        return commentService.createInPublicRoom(memberId, publicRoomId, sentenceId, content);
    }
}
