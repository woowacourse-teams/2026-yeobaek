package yeobaek.backend.comment.service;

import static yeobaek.backend.support.LogField.CLUB_ID;
import static yeobaek.backend.support.LogField.COMMENT_ID;
import static yeobaek.backend.support.LogField.CURRENT_PASSAGE_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SENTENCE_ID;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.Sentence;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.book.repository.SentenceRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.domain.Comment;
import yeobaek.backend.comment.domain.CommentReport;
import yeobaek.backend.comment.domain.CommentView;
import yeobaek.backend.comment.domain.Comments;
import yeobaek.backend.comment.domain.ContentVisibility;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.comment.dto.CommentResponse;
import yeobaek.backend.comment.dto.CommentedSentenceResponse;
import yeobaek.backend.comment.dto.CommentedSentencesResponse;
import yeobaek.backend.comment.dto.CommentsResponse;
import yeobaek.backend.comment.dto.NewCommentCountResponse;
import yeobaek.backend.comment.repository.CommentReportRepository;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.comment.repository.CommentViewRepository;
import yeobaek.backend.comment.repository.CommentedSentenceSummary;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.publicroom.domain.PublicRoom;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;
import yeobaek.backend.support.NotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentReportRepository commentReportRepository;
    private final CommentViewRepository commentViewRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final SentenceRepository sentenceRepository;
    private final PassageRepository passageRepository;
    private final MemberRepository memberRepository;
    private final PublicRoomRepository publicRoomRepository;

    @Transactional
    public CommentsResponse findComments(Long memberId, Long clubId, Long sentenceId) {
        log.atInfo().addKeyValue(OPERATION, "comment.findComments")
                .addKeyValue(CLUB_ID, clubId).addKeyValue(SENTENCE_ID, sentenceId)
                .log("댓글 목록을 조회합니다.");
        validateSentenceContext(memberId, clubId, sentenceId);
        Comments comments = new Comments(
                commentRepository.findAllVisibleWithWriterByClubIdAndSentenceId(memberId, clubId, sentenceId));
        markAsViewed(memberId, comments);
        var response = new CommentsResponse(comments.asList().stream()
                .map(comment -> CommentResponse.of(comment, memberId))
                .toList());
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
        SentenceContext context = validateSentenceContext(memberId, clubId, sentenceId);
        Comment comment = commentRepository.save(new Comment(context.clubMember(), context.sentence(), content));
        commentViewRepository.save(new CommentView(memberRepository.getReferenceById(memberId), comment));
        var response = CommentResponse.of(comment, memberId);
        log.atInfo().addKeyValue(OPERATION, "comment.create").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(CLUB_ID, clubId).addKeyValue(SENTENCE_ID, sentenceId)
                .addKeyValue(COMMENT_ID, response.commentId()).log("댓글을 작성했습니다.");
        return response;
    }

    @Transactional
    public CommentsResponse findPublicRoomComments(Long memberId, Long publicRoomId, Long sentenceId) {
        PublicRoom room = validatePublicRoomSentenceContext(publicRoomId, sentenceId);
        Comments comments = new Comments(commentRepository.findAllVisibleInPublicRoom(
                memberId, room.getId(), sentenceId));
        markAsViewed(memberId, comments);
        return new CommentsResponse(comments.asList().stream()
                .map(comment -> CommentResponse.of(comment, memberId))
                .toList());
    }

    @Transactional
    public CommentResponse createInPublicRoom(Long memberId, Long publicRoomId, Long sentenceId,
                                               CommentContent content) {
        PublicRoom room = validatePublicRoomSentenceContext(publicRoomId, sentenceId);
        Comment comment = commentRepository.save(new Comment(room,
                memberRepository.getReferenceById(memberId),
                sentenceRepository.getReferenceById(sentenceId), content));
        commentViewRepository.save(new CommentView(memberRepository.getReferenceById(memberId), comment));
        return CommentResponse.of(comment, memberId);
    }

    @Transactional(readOnly = true)
    public NewCommentCountResponse countNewPublicRoomComments(Long memberId, Long publicRoomId,
                                                               Long currentPassageId) {
        Passage passage = validatePublicRoomPassageContext(publicRoomId, currentPassageId);
        long count = commentRepository.countNewVisibleCommentsInPublicRoom(
                memberId, publicRoomId, passage.getSequence().value());
        return new NewCommentCountResponse(count);
    }

    @Transactional(readOnly = true)
    public CommentedSentencesResponse findPublicRoomCommentedSentences(Long memberId, Long publicRoomId,
                                                                       Long currentPassageId) {
        Passage passage = validatePublicRoomPassageContext(publicRoomId, currentPassageId);
        int currentPassageSequence = passage.getSequence().value();
        return new CommentedSentencesResponse(commentRepository
                .findPublicRoomCommentedSentenceSummaries(memberId, publicRoomId).stream()
                .map(summary -> toResponse(summary, currentPassageSequence))
                .sorted(commentedSentenceComparator())
                .toList());
    }

    @Transactional(readOnly = true)
    public NewCommentCountResponse countNewComments(Long memberId, Long clubId, Long currentPassageId) {
        log.atInfo().addKeyValue(OPERATION, "comment.countNewComments")
                .addKeyValue(CLUB_ID, clubId).addKeyValue(CURRENT_PASSAGE_ID, currentPassageId)
                .log("새 댓글 수를 조회합니다.");
        Passage currentPassage = validatePassageContext(memberId, clubId, currentPassageId);
        long count = commentRepository.countNewVisibleCommentsWithinProgress(
                memberId, clubId, currentPassage.getSequence().value());
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
        Passage currentPassage = validatePassageContext(memberId, clubId, currentPassageId);
        int currentPassageSequence = currentPassage.getSequence().value();
        List<CommentedSentenceResponse> responses = commentRepository
                .findCommentedSentenceSummaries(memberId, clubId).stream()
                .map(summary -> toResponse(summary, currentPassageSequence))
                .sorted(commentedSentenceComparator())
                .toList();
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
        Comment comment = findOwnComment(memberId, commentId, "수정");
        comment.ensureBookAvailable();
        comment.updateContent(content);
        var response = CommentResponse.of(comment, memberId);
        log.atInfo().addKeyValue(OPERATION, "comment.update").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(COMMENT_ID, commentId).log("댓글을 수정했습니다.");
        return response;
    }

    @Transactional
    public void delete(Long memberId, Long commentId) {
        log.atInfo().addKeyValue(OPERATION, "comment.delete").addKeyValue(COMMENT_ID, commentId)
                .log("댓글을 삭제합니다.");
        Comment comment = findOwnComment(memberId, commentId, "삭제");
        comment.ensureBookAvailable();
        commentRepository.delete(comment);
        log.atInfo().addKeyValue(OPERATION, "comment.delete").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(COMMENT_ID, commentId).log("댓글을 삭제했습니다.");
    }

    @Transactional
    public void report(Long memberId, Long commentId) {
        log.atInfo().addKeyValue(OPERATION, "comment.report").addKeyValue(COMMENT_ID, commentId)
                .log("댓글을 신고합니다.");
        commentRepository.findByIdForUpdate(commentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.COMMENT_NOT_FOUND,
                        "신고할 댓글이 존재하지 않습니다: commentId=" + commentId,
                        commentContext(commentId)));
        Comment comment = commentRepository.findVisibleWithContextById(memberId, commentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.COMMENT_NOT_FOUND,
                        "신고할 댓글이 존재하지 않거나 요청자에게 보이지 않습니다: commentId=" + commentId,
                        commentContext(commentId)));
        comment.ensureReportableBy(memberId);
        if (!comment.isPublicRoomComment()
                && !clubMemberRepository.existsJoinedByMemberIdAndCommentId(memberId, commentId)) {
            throw new ForbiddenException(
                    ErrorCode.NOT_CLUB_MEMBER,
                    "모임에 참여 중인 회원만 댓글을 신고할 수 있습니다: commentId=" + commentId,
                    commentContext(commentId));
        }
        comment.ensureBookAvailable();
        boolean reportCreated = !commentReportRepository.existsByReporterIdAndCommentId(memberId, commentId);
        if (reportCreated) {
            commentReportRepository.save(new CommentReport(memberRepository.getReferenceById(memberId), comment));
        }
        log.atInfo().addKeyValue(OPERATION, "comment.report").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(COMMENT_ID, commentId)
                .addKeyValue("reportCreated", reportCreated)
                .log("댓글 작업을 완료했습니다.");
    }

    private Comment findOwnComment(Long memberId, Long commentId, String action) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.COMMENT_NOT_FOUND,
                        action + "할 댓글이 존재하지 않습니다: commentId=" + commentId,
                        commentContext(commentId)));
        if (!comment.isWrittenBy(memberId)) {
            throw new ForbiddenException(
                    ErrorCode.NOT_COMMENT_OWNER,
                    "본인의 댓글만 " + action + "할 수 있습니다: commentId=" + commentId,
                    commentContext(commentId));
        }
        if (!comment.isWriterJoined()) {
            throw new ForbiddenException(
                    ErrorCode.NOT_CLUB_MEMBER,
                    "모임에 참여 중인 작성자만 댓글을 " + action + "할 수 있습니다: commentId=" + commentId,
                    commentContext(commentId));
        }
        return comment;
    }

    private SentenceContext validateSentenceContext(Long memberId, Long clubId, Long sentenceId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.CLUB_NOT_FOUND,
                        "댓글을 조회하거나 작성할 모임이 존재하지 않습니다: clubId=" + clubId,
                        sentenceContext(clubId, sentenceId)));
        ClubMember clubMember = clubMemberRepository.findJoinedByMemberIdAndClubId(memberId, clubId)
                .orElseThrow(() -> new ForbiddenException(
                        ErrorCode.NOT_CLUB_MEMBER,
                        "모임에 참여 중인 회원만 댓글을 조회하거나 작성할 수 있습니다: clubId=" + clubId,
                        sentenceContext(clubId, sentenceId)));
        Sentence sentence = sentenceRepository.findById(sentenceId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.SENTENCE_NOT_FOUND,
                        "댓글을 조회하거나 작성할 문장이 존재하지 않습니다: sentenceId=" + sentenceId,
                        sentenceContext(clubId, sentenceId)));
        if (!club.isReading(sentence)) {
            throw new NotFoundException(
                    ErrorCode.SENTENCE_NOT_IN_CLUB_BOOK,
                    "해당 모임에서 읽는 문장이 아닙니다: clubId=" + clubId + ", sentenceId=" + sentenceId,
                    sentenceContext(clubId, sentenceId));
        }
        club.ensureBookAvailable();
        return new SentenceContext(clubMember, sentence);
    }

    private record SentenceContext(ClubMember clubMember, Sentence sentence) {
    }

    private Passage validatePassageContext(Long memberId, Long clubId, Long passageId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.CLUB_NOT_FOUND,
                        "댓글 발견 정보를 조회할 모임이 존재하지 않습니다: clubId=" + clubId,
                        passageContext(clubId, passageId)));
        clubMemberRepository.findJoinedByMemberIdAndClubId(memberId, clubId)
                .orElseThrow(() -> new ForbiddenException(
                        ErrorCode.NOT_CLUB_MEMBER,
                        "모임에 참여 중인 회원만 댓글 발견 정보를 조회할 수 있습니다: clubId=" + clubId,
                        passageContext(clubId, passageId)));
        Passage passage = passageRepository.findById(passageId)
                .orElseThrow(() -> new BadRequestException(
                        ErrorCode.INVALID_REQUEST,
                        "현재 문단이 존재하지 않습니다: passageId=" + passageId,
                        passageContext(clubId, passageId)));
        if (!club.isReading(passage)) {
            throw new BadRequestException(
                    ErrorCode.INVALID_REQUEST,
                    "현재 문단이 해당 모임의 도서에 속하지 않습니다: clubId=" + clubId
                            + ", passageId=" + passageId,
                    passageContext(clubId, passageId));
        }
        club.ensureBookAvailable();
        return passage;
    }

    private PublicRoom validatePublicRoomSentenceContext(Long publicRoomId, Long sentenceId) {
        PublicRoom room = findPublicRoom(publicRoomId);
        Sentence sentence = sentenceRepository.findById(sentenceId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.SENTENCE_NOT_FOUND,
                        "댓글을 조회하거나 작성할 문장이 존재하지 않습니다: sentenceId=" + sentenceId));
        if (!room.isReading(sentence)) {
            throw new NotFoundException(
                    ErrorCode.SENTENCE_NOT_FOUND,
                    "해당 공개방에서 읽는 문장이 아닙니다: publicRoomId=" + publicRoomId
                            + ", sentenceId=" + sentenceId);
        }
        room.ensureBookAvailable();
        return room;
    }

    private Passage validatePublicRoomPassageContext(Long publicRoomId, Long passageId) {
        PublicRoom room = findPublicRoom(publicRoomId);
        Passage passage = passageRepository.findById(passageId)
                .orElseThrow(() -> new BadRequestException(
                        ErrorCode.INVALID_REQUEST,
                        "현재 문단이 존재하지 않습니다: passageId=" + passageId));
        if (!room.isReading(passage)) {
            throw new BadRequestException(
                    ErrorCode.INVALID_REQUEST,
                    "현재 문단이 해당 공개방의 도서에 속하지 않습니다: publicRoomId=" + publicRoomId
                            + ", passageId=" + passageId);
        }
        room.ensureBookAvailable();
        return passage;
    }

    private PublicRoom findPublicRoom(Long publicRoomId) {
        return publicRoomRepository.findWithBookById(publicRoomId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.PUBLIC_ROOM_NOT_FOUND,
                        "공개방이 존재하지 않습니다: publicRoomId=" + publicRoomId));
    }

    private void markAsViewed(Long memberId, Comments comments) {
        if (comments.isEmpty()) {
            return;
        }
        var viewedCommentIds = new HashSet<>(commentViewRepository.findViewedCommentIds(memberId, comments.ids()));
        var member = memberRepository.getReferenceById(memberId);
        List<CommentView> newViews = comments.excludingIds(viewedCommentIds).stream()
                .map(comment -> new CommentView(member, comment))
                .toList();
        commentViewRepository.saveAll(newViews);
    }

    private CommentedSentenceResponse toResponse(CommentedSentenceSummary summary, int currentPassageSequence) {
        boolean future = summary.getPassageSequence() > currentPassageSequence;
        return new CommentedSentenceResponse(
                summary.getSentenceId(),
                summary.getContent(),
                summary.getPassageId(),
                summary.getPassageSequence(),
                summary.getSentenceSequence(),
                future,
                summary.getCommentCount(),
                summary.getUnreadCommentCount(),
                ContentVisibility.from(future, summary.getUnreadCommentCount()),
                summary.getLatestCommentCreatedAt());
    }

    private Comparator<CommentedSentenceResponse> commentedSentenceComparator() {
        return Comparator.comparingInt(this::sortGroup)
                .thenComparingInt(CommentedSentenceResponse::passageSequence)
                .thenComparingInt(CommentedSentenceResponse::sentenceSequence)
                .thenComparing(CommentedSentenceResponse::sentenceId, Comparator.reverseOrder());
    }

    private int sortGroup(CommentedSentenceResponse response) {
        if (response.unreadCommentCount() == 0) {
            return 2;
        }
        return response.future() ? 1 : 0;
    }

    private Map<String, String> commentContext(Long commentId) {
        return Map.of(COMMENT_ID, commentId.toString());
    }

    private Map<String, String> sentenceContext(Long clubId, Long sentenceId) {
        return Map.of(CLUB_ID, clubId.toString(), SENTENCE_ID, sentenceId.toString());
    }

    private Map<String, String> passageContext(Long clubId, Long passageId) {
        return Map.of(CLUB_ID, clubId.toString(), "passageId", passageId.toString());
    }
}
