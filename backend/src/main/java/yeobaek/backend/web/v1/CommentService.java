package yeobaek.backend.web.v1;

import static yeobaek.backend.support.LogField.CLUB_ID;
import static yeobaek.backend.support.LogField.ACTOR_ID;
import static yeobaek.backend.support.LogField.COMMENT_ID;
import static yeobaek.backend.support.LogField.CURRENT_PASSAGE_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SENTENCE_ID;
import static yeobaek.backend.support.LogField.SUCCESS;
import static yeobaek.backend.support.LogField.SPACE_ID;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.application.appreciation.CommentModificationWorkflow;
import yeobaek.backend.application.appreciation.CommentQueryWorkflow;
import yeobaek.backend.application.appreciation.CommentQueryWorkflow.CommentResult;
import yeobaek.backend.application.appreciation.CommentQueryWorkflow.DiscoveryResult;
import yeobaek.backend.application.appreciation.CommentReportWorkflow;
import yeobaek.backend.application.appreciation.CommentSharingWorkflow;
import yeobaek.backend.web.comment.dto.ContentVisibility;
import yeobaek.backend.appreciation.api.comment.CommentContent;
import yeobaek.backend.web.comment.dto.CommentResponse;
import yeobaek.backend.web.comment.dto.CommentedSentenceResponse;
import yeobaek.backend.web.comment.dto.CommentedSentencesResponse;
import yeobaek.backend.web.comment.dto.CommentsResponse;
import yeobaek.backend.web.comment.dto.NewCommentCountResponse;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.content.api.location.ContentLegacyLocationQueryApi;
import yeobaek.backend.space.api.club.ClubApi;
import yeobaek.backend.space.api.access.SpaceAccessApi;
import yeobaek.backend.space.api.publicroom.PublicRoomApi;
import yeobaek.backend.shared.exception.BadRequestException;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.ForbiddenException;
import yeobaek.backend.shared.exception.NotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "yeobaek.backend.appreciation.comment.service.CommentService")
public class CommentService {

    private final CommentSharingWorkflow sharingWorkflow;
    private final CommentReportWorkflow reportWorkflow;
    private final CommentModificationWorkflow modificationWorkflow;
    private final CommentQueryWorkflow queryWorkflow;
    private final ClubApi clubs;
    private final PublicRoomApi rooms;
    private final SpaceContentBindingApi bindings;
    private final ContentLegacyLocationQueryApi locations;
    private final SpaceAccessApi spaces;

    @Transactional
    public CommentsResponse findComments(Long memberId, Long clubId, Long sentenceId) {
        log.atInfo().addKeyValue(OPERATION, "comment.findComments")
                .addKeyValue(CLUB_ID, clubId).addKeyValue(SENTENCE_ID, sentenceId)
                .log("댓글 목록을 조회합니다.");
        var target = resolveClubTarget(new MemberId(memberId), clubId, sentenceId);
        var response = commentsResponse(memberId, queryWorkflow.findComments(new MemberId(memberId),
                target.spaceId(), target.contentId(), target.locationId()));
        log.atInfo().addKeyValue(OPERATION, "comment.findComments").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(CLUB_ID, clubId).addKeyValue(SENTENCE_ID, sentenceId)
                .addKeyValue("resultCount", response.comments().size()).log("댓글 목록을 조회했습니다.");
        return response;
    }

    @Transactional
    public CommentResponse create(Long memberId, Long clubId, Long sentenceId, CommentContent content) {
        log.atInfo().addKeyValue(OPERATION, "comment.create")
                .addKeyValue(CLUB_ID, clubId).addKeyValue(SENTENCE_ID, sentenceId)
                .log("댓글을 작성합니다.");
        var target = resolveClubTarget(new MemberId(memberId), clubId, sentenceId);
        var shared = sharingWorkflow.share(new MemberId(memberId),
                target.spaceId(), target.contentId(), target.locationId(), content);
        var response = CommentResponse.of(shared.comment(), shared.author());
        log.atInfo().addKeyValue(OPERATION, "comment.create").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(CLUB_ID, clubId).addKeyValue(SENTENCE_ID, sentenceId)
                .addKeyValue(COMMENT_ID, response.commentId()).log("댓글을 작성했습니다.");
        return response;
    }

    @Transactional
    public CommentsResponse findPublicRoomComments(Long memberId, Long publicRoomId, Long sentenceId) {
        var target = resolvePublicRoomTarget(new MemberId(memberId), publicRoomId, sentenceId);
        return commentsResponse(memberId, queryWorkflow.findComments(new MemberId(memberId),
                target.spaceId(), target.contentId(), target.locationId()));
    }

    @Transactional
    public CommentResponse createInPublicRoom(Long memberId, Long publicRoomId, Long sentenceId,
                                               CommentContent content) {
        var target = resolvePublicRoomTarget(new MemberId(memberId), publicRoomId, sentenceId);
        var shared = sharingWorkflow.share(new MemberId(memberId),
                target.spaceId(), target.contentId(), target.locationId(), content);
        return CommentResponse.of(shared.comment(), shared.author());
    }

    @Transactional(readOnly = true)
    public NewCommentCountResponse countNewPublicRoomComments(Long memberId, Long publicRoomId,
                                                               Long currentPassageId) {
        var target = resolvePublicRoomPassage(new MemberId(memberId), publicRoomId, currentPassageId);
        long count = queryWorkflow.countNewComments(new MemberId(memberId), target.spaceId(),
                target.contentId(), target.locationId());
        return new NewCommentCountResponse(count);
    }

    @Transactional(readOnly = true)
    public CommentedSentencesResponse findPublicRoomCommentedSentences(Long memberId, Long publicRoomId,
                                                                       Long currentPassageId) {
        var target = resolvePublicRoomPassage(new MemberId(memberId), publicRoomId, currentPassageId);
        return discoveryResponse(queryWorkflow.findDiscovery(new MemberId(memberId), target.spaceId(),
                target.contentId(), target.locationId()));
    }

    @Transactional(readOnly = true)
    public NewCommentCountResponse countNewComments(Long memberId, Long clubId, Long currentPassageId) {
        log.atInfo().addKeyValue(OPERATION, "comment.countNewComments")
                .addKeyValue(CLUB_ID, clubId).addKeyValue(CURRENT_PASSAGE_ID, currentPassageId)
                .log("새 댓글 수를 조회합니다.");
        var target = resolveClubPassage(new MemberId(memberId), clubId, currentPassageId);
        long count = queryWorkflow.countNewComments(new MemberId(memberId), target.spaceId(),
                target.contentId(), target.locationId());
        var response = new NewCommentCountResponse(count);
        log.atInfo().addKeyValue(OPERATION, "comment.countNewComments").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(CLUB_ID, clubId).addKeyValue(CURRENT_PASSAGE_ID, currentPassageId)
                .addKeyValue("resultCount", count).log("새 댓글 수를 조회했습니다.");
        return response;
    }

    @Transactional(readOnly = true)
    public CommentedSentencesResponse findCommentedSentences(Long memberId, Long clubId, Long currentPassageId) {
        log.atInfo().addKeyValue(OPERATION, "comment.findCommentedSentences")
                .addKeyValue(CLUB_ID, clubId).addKeyValue(CURRENT_PASSAGE_ID, currentPassageId)
                .log("댓글이 있는 문장 목록을 조회합니다.");
        var target = resolveClubPassage(new MemberId(memberId), clubId, currentPassageId);
        List<CommentedSentenceResponse> responses = discoveryResponse(queryWorkflow.findDiscovery(
                new MemberId(memberId), target.spaceId(), target.contentId(), target.locationId()))
                .commentedSentences();
        var response = new CommentedSentencesResponse(responses);
        log.atInfo().addKeyValue(OPERATION, "comment.findCommentedSentences").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(CLUB_ID, clubId).addKeyValue(CURRENT_PASSAGE_ID, currentPassageId)
                .addKeyValue("resultCount", responses.size()).log("댓글이 있는 문장 목록을 조회했습니다.");
        return response;
    }

    @Transactional
    public CommentResponse update(Long memberId, Long commentId, CommentContent content) {
        log.atInfo().addKeyValue(OPERATION, "comment.update").addKeyValue(COMMENT_ID, commentId)
                .log("댓글을 수정합니다.");
        var changed = modificationWorkflow.update(
                new MemberId(memberId), new AppreciationId(commentId), content);
        var response = CommentResponse.of(changed.comment(), changed.author());
        log.atInfo().addKeyValue(OPERATION, "comment.update").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(COMMENT_ID, commentId).log("댓글을 수정했습니다.");
        return response;
    }

    @Transactional
    public void delete(Long memberId, Long commentId) {
        log.atInfo().addKeyValue(OPERATION, "comment.delete").addKeyValue(COMMENT_ID, commentId)
                .log("댓글을 삭제합니다.");
        modificationWorkflow.delete(new MemberId(memberId), new AppreciationId(commentId));
        log.atInfo().addKeyValue(OPERATION, "comment.delete").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(COMMENT_ID, commentId).log("댓글을 삭제했습니다.");
    }

    @Transactional
    public void report(Long memberId, Long commentId) {
        log.atInfo().addKeyValue(OPERATION, "comment.report").addKeyValue(COMMENT_ID, commentId)
                .log("댓글을 신고합니다.");
        boolean reportCreated = reportWorkflow.report(
                new MemberId(memberId), new AppreciationId(commentId));
        log.atInfo().addKeyValue(OPERATION, "comment.report").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(COMMENT_ID, commentId)
                .addKeyValue("reportCreated", reportCreated)
                .log("댓글 작업을 완료했습니다.");
    }

    private CommentedSentenceResponse toResponse(DiscoveryResult summary) {
        var sentence = locations.findSentenceInfo(List.of(summary.locationId())).stream().findFirst().orElseThrow();
        long passageId = locations.findByLocation(summary.passageLocationId()).orElseThrow().passageId();
        return new CommentedSentenceResponse(
                sentence.sentenceId(), summary.content(), passageId, summary.passageSequence(),
                summary.sentenceSequence(), summary.future(), summary.commentCount(), summary.unreadCommentCount(),
                summary.revealRequired() ? ContentVisibility.REVEAL_REQUIRED : ContentVisibility.VISIBLE,
                summary.latestCommentCreatedAt());
    }

    private CommentsResponse commentsResponse(Long requesterId, List<CommentResult> results) {
        return new CommentsResponse(results.stream().map(result -> new CommentResponse(
                result.comment().id().value(), result.author().id().value(), result.author().nickname(),
                result.comment().content(), result.comment().createdAt(), result.comment().updatedAt(),
                result.comment().isWrittenBy(new MemberId(requesterId)))).toList());
    }

    private CommentedSentencesResponse discoveryResponse(List<DiscoveryResult> results) {
        return new CommentedSentencesResponse(results.stream().map(this::toResponse).toList());
    }

    private CommentTarget resolveClubTarget(MemberId actorId, Long clubId, Long sentenceId) {
        var club = clubs.findById(clubId).orElseThrow(() -> new NotFoundException(ErrorCode.CLUB_NOT_FOUND,
                "댓글을 조회하거나 작성할 모임이 존재하지 않습니다: clubId=" + clubId,
                Map.of(ACTOR_ID, Long.toString(actorId.value()), "clubId", clubId.toString())));
        return target(actorId, club.id(), sentenceId, true);
    }

    private CommentTarget resolvePublicRoomTarget(MemberId actorId, Long publicRoomId, Long sentenceId) {
        var room = rooms.findById(publicRoomId).orElseThrow(() -> new NotFoundException(
                ErrorCode.PUBLIC_ROOM_NOT_FOUND, "공개방이 존재하지 않습니다: publicRoomId=" + publicRoomId,
                Map.of(ACTOR_ID, Long.toString(actorId.value()), "publicRoomId", publicRoomId.toString())));
        return target(actorId, room.id(), sentenceId, true);
    }

    private CommentTarget resolveClubPassage(MemberId actorId, Long clubId, Long passageId) {
        var club = clubs.findById(clubId).orElseThrow(() -> new NotFoundException(ErrorCode.CLUB_NOT_FOUND,
                "댓글 발견 정보를 조회할 모임이 존재하지 않습니다: clubId=" + clubId,
                Map.of(ACTOR_ID, Long.toString(actorId.value()), "clubId", clubId.toString())));
        return target(actorId, club.id(), passageId, false);
    }

    private CommentTarget resolvePublicRoomPassage(MemberId actorId, Long publicRoomId, Long passageId) {
        var room = rooms.findById(publicRoomId).orElseThrow(() -> new NotFoundException(
                ErrorCode.PUBLIC_ROOM_NOT_FOUND, "공개방이 존재하지 않습니다: publicRoomId=" + publicRoomId,
                Map.of(ACTOR_ID, Long.toString(actorId.value()), "publicRoomId", publicRoomId.toString())));
        return target(actorId, room.id(), passageId, false);
    }

    private CommentTarget target(MemberId actorId, SpaceId spaceId, Long legacyLocationId, boolean sentence) {
        if (!spaces.canAccess(actorId, spaceId)) {
            throw new ForbiddenException(ErrorCode.SPACE_ACCESS_DENIED, "공간에 접근할 수 없습니다.",
                    Map.of(ACTOR_ID, Long.toString(actorId.value()),
                            SPACE_ID, Long.toString(spaceId.value()),
                            "legacyLocationId", legacyLocationId.toString()));
        }
        ContentId contentId = requireContent(spaceId);
        var location = sentence ? locations.findSentence(legacyLocationId) : locations.findPassage(legacyLocationId);
        var found = location.orElseThrow(() -> new NotFoundException(
                sentence ? ErrorCode.SENTENCE_NOT_FOUND : ErrorCode.PASSAGE_NOT_FOUND,
                "컨텐츠 위치가 존재하지 않습니다: locationId=" + legacyLocationId,
                Map.of(ACTOR_ID, Long.toString(actorId.value()),
                        SPACE_ID, Long.toString(spaceId.value()),
                        "contentId", Long.toString(contentId.value()),
                        "legacyLocationId", legacyLocationId.toString())));
        if (!found.contentId().equals(contentId)) {
            throw new BadRequestException(ErrorCode.INVALID_REQUEST, "공간의 컨텐츠에 속하지 않는 위치입니다.",
                    Map.of(ACTOR_ID, Long.toString(actorId.value()),
                            SPACE_ID, Long.toString(spaceId.value()),
                            "contentId", Long.toString(contentId.value()),
                            "legacyLocationId", legacyLocationId.toString(),
                            "locationId", Long.toString(found.locationId().value())));
        }
        return new CommentTarget(spaceId, contentId, found.locationId());
    }

    private ContentId requireContent(SpaceId spaceId) {
        return bindings.findContents(spaceId).stream().findFirst()
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOOK_NOT_FOUND,
                        "공간에서 읽는 도서가 존재하지 않습니다.",
                        Map.of(SPACE_ID, Long.toString(spaceId.value()))));
    }

    private record CommentTarget(SpaceId spaceId, ContentId contentId, ContentLocationId locationId) {
    }
}
