package yeobaek.backend.comment.service;

import static yeobaek.backend.support.LogField.ATTEMPT;
import static yeobaek.backend.support.LogField.CLUB_ID;
import static yeobaek.backend.support.LogField.COMMENT_ID;
import static yeobaek.backend.support.LogField.MEMBER_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.PHASE;
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

    @Transactional
    public CommentsResponse findComments(Long memberId, Long clubId, Long sentenceId) {
        logAttempt("comment.findAll", memberId, clubId, sentenceId, null);
        validateSentenceContext(memberId, clubId, sentenceId);
        Comments comments = new Comments(
                commentRepository.findAllVisibleWithWriterByClubIdAndSentenceId(memberId, clubId, sentenceId));
        markAsViewed(memberId, comments);
        var response = new CommentsResponse(comments.asList().stream()
                .map(comment -> CommentResponse.of(comment, memberId))
                .toList());
        logSuccess("comment.findAll", memberId, clubId, sentenceId, null, response.comments().size());
        return response;
    }

    @Transactional
    public CommentResponse create(Long memberId, Long clubId, Long sentenceId, CommentContent content) {
        logAttempt("comment.create", memberId, clubId, sentenceId, null);
        SentenceContext context = validateSentenceContext(memberId, clubId, sentenceId);
        Comment comment = commentRepository.save(new Comment(context.clubMember(), context.sentence(), content));
        commentViewRepository.save(new CommentView(memberRepository.getReferenceById(memberId), comment));
        var response = CommentResponse.of(comment, memberId);
        logSuccess("comment.create", memberId, clubId, sentenceId, response.commentId(), 1);
        return response;
    }

    @Transactional(readOnly = true)
    public NewCommentCountResponse countNewComments(Long memberId, Long clubId, Long currentPassageId) {
        logAttempt("comment.countNew", memberId, clubId, null, null);
        Passage currentPassage = validatePassageContext(memberId, clubId, currentPassageId);
        long count = commentRepository.countNewVisibleCommentsWithinProgress(
                memberId, clubId, currentPassage.getSequence().value());
        var response = new NewCommentCountResponse(count);
        logSuccess("comment.countNew", memberId, clubId, null, null, count);
        return response;
    }

    @Transactional(readOnly = true)
    public CommentedSentencesResponse findCommentedSentences(Long memberId, Long clubId, Long currentPassageId) {
        logAttempt("comment.findSentences", memberId, clubId, null, null);
        Passage currentPassage = validatePassageContext(memberId, clubId, currentPassageId);
        int currentPassageSequence = currentPassage.getSequence().value();
        List<CommentedSentenceResponse> responses = commentRepository
                .findCommentedSentenceSummaries(memberId, clubId).stream()
                .map(summary -> toResponse(summary, currentPassageSequence))
                .sorted(commentedSentenceComparator())
                .toList();
        var response = new CommentedSentencesResponse(responses);
        logSuccess("comment.findSentences", memberId, clubId, null, null, responses.size());
        return response;
    }

    @Transactional
    public CommentResponse update(Long memberId, Long commentId, CommentContent content) {
        logAttempt("comment.update", memberId, null, null, commentId);
        Comment comment = findOwnComment(memberId, commentId, "수정");
        comment.ensureBookAvailable();
        comment.updateContent(content);
        var response = CommentResponse.of(comment, memberId);
        logSuccess("comment.update", memberId, null, null, commentId, 1);
        return response;
    }

    @Transactional
    public void delete(Long memberId, Long commentId) {
        logAttempt("comment.delete", memberId, null, null, commentId);
        Comment comment = findOwnComment(memberId, commentId, "삭제");
        comment.ensureBookAvailable();
        commentRepository.delete(comment);
        logSuccess("comment.delete", memberId, null, null, commentId, 1);
    }

    @Transactional
    public void report(Long memberId, Long commentId) {
        logAttempt("comment.report", memberId, null, null, commentId);
        Comment comment = commentRepository.findVisibleWithContextById(memberId, commentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.COMMENT_NOT_FOUND,
                        "신고할 댓글이 존재하지 않거나 요청자에게 보이지 않습니다: commentId=" + commentId,
                        commentContext(memberId, commentId)));
        comment.ensureReportableBy(memberId);
        if (!clubMemberRepository.existsJoinedByMemberIdAndCommentId(memberId, commentId)) {
            throw new ForbiddenException(
                    ErrorCode.NOT_CLUB_MEMBER,
                    "모임에 참여 중인 회원만 댓글을 신고할 수 있습니다: commentId=" + commentId,
                    commentContext(memberId, commentId));
        }
        comment.ensureBookAvailable();
        boolean reportCreated = !commentReportRepository.existsByReporterIdAndCommentId(memberId, commentId);
        if (reportCreated) {
            commentReportRepository.save(new CommentReport(memberRepository.getReferenceById(memberId), comment));
        }
        logSuccess("comment.report", memberId, null, null, commentId, reportCreated ? 1 : 0);
    }

    private Comment findOwnComment(Long memberId, Long commentId, String action) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.COMMENT_NOT_FOUND,
                        action + "할 댓글이 존재하지 않습니다: commentId=" + commentId,
                        commentContext(memberId, commentId)));
        if (!comment.isWrittenBy(memberId)) {
            throw new ForbiddenException(
                    ErrorCode.NOT_COMMENT_OWNER,
                    "본인의 댓글만 " + action + "할 수 있습니다: commentId=" + commentId,
                    commentContext(memberId, commentId));
        }
        if (!comment.isWriterJoined()) {
            throw new ForbiddenException(
                    ErrorCode.NOT_CLUB_MEMBER,
                    "모임에 참여 중인 작성자만 댓글을 " + action + "할 수 있습니다: commentId=" + commentId,
                    commentContext(memberId, commentId));
        }
        return comment;
    }

    private SentenceContext validateSentenceContext(Long memberId, Long clubId, Long sentenceId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.CLUB_NOT_FOUND,
                        "댓글을 조회하거나 작성할 모임이 존재하지 않습니다: clubId=" + clubId,
                        sentenceContext(memberId, clubId, sentenceId)));
        ClubMember clubMember = clubMemberRepository.findJoinedByMemberIdAndClubId(memberId, clubId)
                .orElseThrow(() -> new ForbiddenException(
                        ErrorCode.NOT_CLUB_MEMBER,
                        "모임에 참여 중인 회원만 댓글을 조회하거나 작성할 수 있습니다: clubId=" + clubId,
                        sentenceContext(memberId, clubId, sentenceId)));
        Sentence sentence = sentenceRepository.findById(sentenceId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.SENTENCE_NOT_FOUND,
                        "댓글을 조회하거나 작성할 문장이 존재하지 않습니다: sentenceId=" + sentenceId,
                        sentenceContext(memberId, clubId, sentenceId)));
        if (!club.isReading(sentence)) {
            throw new NotFoundException(
                    ErrorCode.SENTENCE_NOT_FOUND,
                    "해당 모임에서 읽는 문장이 아닙니다: clubId=" + clubId + ", sentenceId=" + sentenceId,
                    sentenceContext(memberId, clubId, sentenceId));
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
                        passageContext(memberId, clubId, passageId)));
        clubMemberRepository.findJoinedByMemberIdAndClubId(memberId, clubId)
                .orElseThrow(() -> new ForbiddenException(
                        ErrorCode.NOT_CLUB_MEMBER,
                        "모임에 참여 중인 회원만 댓글 발견 정보를 조회할 수 있습니다: clubId=" + clubId,
                        passageContext(memberId, clubId, passageId)));
        Passage passage = passageRepository.findById(passageId)
                .orElseThrow(() -> new BadRequestException(
                        ErrorCode.INVALID_REQUEST,
                        "현재 문단이 존재하지 않습니다: passageId=" + passageId,
                        passageContext(memberId, clubId, passageId)));
        if (!club.isReading(passage)) {
            throw new BadRequestException(
                    ErrorCode.INVALID_REQUEST,
                    "현재 문단이 해당 모임의 도서에 속하지 않습니다: clubId=" + clubId
                            + ", passageId=" + passageId,
                    passageContext(memberId, clubId, passageId));
        }
        club.ensureBookAvailable();
        return passage;
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

    private void logAttempt(String operation, Long memberId, Long clubId, Long sentenceId, Long commentId) {
        log.atInfo().addKeyValue(OPERATION, operation).addKeyValue(PHASE, ATTEMPT)
                .addKeyValue(MEMBER_ID, memberId).addKeyValue(CLUB_ID, clubId)
                .addKeyValue("sentenceId", sentenceId).addKeyValue(COMMENT_ID, commentId)
                .log("댓글 작업을 시작합니다.");
    }

    private void logSuccess(String operation, Long memberId, Long clubId, Long sentenceId,
                            Long commentId, long resultCount) {
        log.atInfo().addKeyValue(OPERATION, operation).addKeyValue(PHASE, SUCCESS)
                .addKeyValue(MEMBER_ID, memberId).addKeyValue(CLUB_ID, clubId)
                .addKeyValue("sentenceId", sentenceId).addKeyValue(COMMENT_ID, commentId)
                .addKeyValue("resultCount", resultCount).log("댓글 작업을 완료했습니다.");
    }

    private Map<String, String> commentContext(Long memberId, Long commentId) {
        return Map.of(MEMBER_ID, memberId.toString(), COMMENT_ID, commentId.toString());
    }

    private Map<String, String> sentenceContext(Long memberId, Long clubId, Long sentenceId) {
        return Map.of(MEMBER_ID, memberId.toString(), CLUB_ID, clubId.toString(),
                "sentenceId", sentenceId.toString());
    }

    private Map<String, String> passageContext(Long memberId, Long clubId, Long passageId) {
        return Map.of(MEMBER_ID, memberId.toString(), CLUB_ID, clubId.toString(),
                "passageId", passageId.toString());
    }
}
