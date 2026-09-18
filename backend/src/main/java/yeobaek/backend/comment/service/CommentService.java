package yeobaek.backend.comment.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
        validateSentenceContext(memberId, clubId, sentenceId);
        List<Comment> comments = commentRepository
                .findAllVisibleWithWriterByClubIdAndSentenceId(memberId, clubId, sentenceId);
        markAsViewed(memberId, comments);
        return new CommentsResponse(comments.stream()
                .map(comment -> CommentResponse.of(comment, memberId))
                .toList());
    }

    @Transactional
    public CommentResponse create(Long memberId, Long clubId, Long sentenceId, CommentContent content) {
        SentenceContext context = validateSentenceContext(memberId, clubId, sentenceId);
        Comment comment = commentRepository.save(new Comment(context.clubMember(), context.sentence(), content));
        commentViewRepository.save(new CommentView(memberRepository.getReferenceById(memberId), comment));
        return CommentResponse.of(comment, memberId);
    }

    @Transactional(readOnly = true)
    public NewCommentCountResponse countNewComments(Long memberId, Long clubId, Long currentPassageId) {
        Passage currentPassage = validatePassageContext(memberId, clubId, currentPassageId);
        long count = commentRepository.countNewVisibleCommentsWithinProgress(
                memberId, clubId, currentPassage.getSequence().value());
        return new NewCommentCountResponse(count);
    }

    @Transactional(readOnly = true)
    public CommentedSentencesResponse findCommentedSentences(Long memberId, Long clubId, Long currentPassageId) {
        Passage currentPassage = validatePassageContext(memberId, clubId, currentPassageId);
        int currentPassageSequence = currentPassage.getSequence().value();
        List<CommentedSentenceResponse> responses = commentRepository
                .findCommentedSentenceSummaries(memberId, clubId).stream()
                .map(summary -> toResponse(summary, currentPassageSequence))
                .sorted(commentedSentenceComparator())
                .toList();
        return new CommentedSentencesResponse(responses);
    }

    @Transactional
    public CommentResponse update(Long memberId, Long commentId, CommentContent content) {
        Comment comment = findOwnComment(memberId, commentId, "수정");
        comment.ensureBookAvailable();
        comment.updateContent(content);
        return CommentResponse.of(comment, memberId);
    }

    @Transactional
    public void delete(Long memberId, Long commentId) {
        Comment comment = findOwnComment(memberId, commentId, "삭제");
        comment.ensureBookAvailable();
        commentRepository.delete(comment);
    }

    @Transactional
    public void report(Long memberId, Long commentId) {
        Comment comment = commentRepository.findVisibleWithContextById(memberId, commentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.COMMENT_NOT_FOUND,
                        "신고할 댓글이 존재하지 않거나 요청자에게 보이지 않습니다: commentId=" + commentId));
        comment.ensureReportableBy(memberId);
        if (!clubMemberRepository.existsJoinedByMemberIdAndCommentId(memberId, commentId)) {
            throw new ForbiddenException(
                    ErrorCode.NOT_CLUB_MEMBER,
                    "모임에 참여 중인 회원만 댓글을 신고할 수 있습니다: commentId=" + commentId);
        }
        comment.ensureBookAvailable();
        if (!commentReportRepository.existsByReporterIdAndCommentId(memberId, commentId)) {
            commentReportRepository.save(new CommentReport(memberRepository.getReferenceById(memberId), comment));
        }
    }

    private Comment findOwnComment(Long memberId, Long commentId, String action) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.COMMENT_NOT_FOUND,
                        action + "할 댓글이 존재하지 않습니다: commentId=" + commentId));
        if (!comment.isWrittenBy(memberId)) {
            throw new ForbiddenException(
                    ErrorCode.NOT_COMMENT_OWNER,
                    "본인의 댓글만 " + action + "할 수 있습니다: commentId=" + commentId);
        }
        if (!comment.isWriterJoined()) {
            throw new ForbiddenException(
                    ErrorCode.NOT_CLUB_MEMBER,
                    "모임에 참여 중인 작성자만 댓글을 " + action + "할 수 있습니다: commentId=" + commentId);
        }
        return comment;
    }

    private SentenceContext validateSentenceContext(Long memberId, Long clubId, Long sentenceId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.CLUB_NOT_FOUND,
                        "댓글을 조회하거나 작성할 모임이 존재하지 않습니다: clubId=" + clubId));
        ClubMember clubMember = clubMemberRepository.findJoinedByMemberIdAndClubId(memberId, clubId)
                .orElseThrow(() -> new ForbiddenException(
                        ErrorCode.NOT_CLUB_MEMBER,
                        "모임에 참여 중인 회원만 댓글을 조회하거나 작성할 수 있습니다: clubId=" + clubId));
        Sentence sentence = sentenceRepository.findById(sentenceId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.SENTENCE_NOT_FOUND,
                        "댓글을 조회하거나 작성할 문장이 존재하지 않습니다: sentenceId=" + sentenceId));
        if (!club.isReading(sentence)) {
            throw new NotFoundException(
                    ErrorCode.SENTENCE_NOT_FOUND,
                    "해당 모임에서 읽는 문장이 아닙니다: clubId=" + clubId + ", sentenceId=" + sentenceId);
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
                        "댓글 발견 정보를 조회할 모임이 존재하지 않습니다: clubId=" + clubId));
        clubMemberRepository.findJoinedByMemberIdAndClubId(memberId, clubId)
                .orElseThrow(() -> new ForbiddenException(
                        ErrorCode.NOT_CLUB_MEMBER,
                        "모임에 참여 중인 회원만 댓글 발견 정보를 조회할 수 있습니다: clubId=" + clubId));
        Passage passage = passageRepository.findById(passageId)
                .orElseThrow(() -> new BadRequestException(
                        ErrorCode.INVALID_REQUEST,
                        "현재 문단이 존재하지 않습니다: passageId=" + passageId));
        if (!club.isReading(passage)) {
            throw new BadRequestException(
                    ErrorCode.INVALID_REQUEST,
                    "현재 문단이 해당 모임의 도서에 속하지 않습니다: clubId=" + clubId
                            + ", passageId=" + passageId);
        }
        club.ensureBookAvailable();
        return passage;
    }

    private void markAsViewed(Long memberId, List<Comment> comments) {
        if (comments.isEmpty()) {
            return;
        }
        List<Long> commentIds = comments.stream().map(Comment::getId).toList();
        var viewedCommentIds = new HashSet<>(commentViewRepository.findViewedCommentIds(memberId, commentIds));
        var member = memberRepository.getReferenceById(memberId);
        List<CommentView> newViews = comments.stream()
                .filter(comment -> !viewedCommentIds.contains(comment.getId()))
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
                .thenComparing(CommentedSentenceResponse::latestCommentCreatedAt, Comparator.reverseOrder())
                .thenComparing(CommentedSentenceResponse::sentenceId, Comparator.reverseOrder());
    }

    private int sortGroup(CommentedSentenceResponse response) {
        if (response.unreadCommentCount() == 0) {
            return 2;
        }
        return response.future() ? 1 : 0;
    }
}
