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
import yeobaek.backend.comment.dto.CommentedSentenceResponse;
import yeobaek.backend.comment.dto.CommentedSentencesResponse;
import yeobaek.backend.comment.dto.CommentResponse;
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
    public CommentResponse create(Long memberId, Long clubId, Long sentenceId, String content) {
        SentenceContext context = validateSentenceContext(memberId, clubId, sentenceId);
        Comment comment = commentRepository.save(new Comment(context.clubMember(), context.sentence(), content));
        commentViewRepository.save(new CommentView(memberRepository.getReferenceById(memberId), comment));
        return CommentResponse.of(comment, memberId);
    }

    @Transactional(readOnly = true)
    public NewCommentCountResponse countNewComments(Long memberId, Long clubId, Long currentPassageId) {
        Passage currentPassage = validatePassageContext(memberId, clubId, currentPassageId);
        long count = commentRepository.countNewVisibleCommentsWithinProgress(
                memberId, clubId, currentPassage.getSequence());
        return new NewCommentCountResponse(count);
    }

    @Transactional(readOnly = true)
    public CommentedSentencesResponse findCommentedSentences(Long memberId, Long clubId, Long currentPassageId) {
        Passage currentPassage = validatePassageContext(memberId, clubId, currentPassageId);
        int currentPassageSequence = currentPassage.getSequence();
        List<CommentedSentenceResponse> responses = commentRepository
                .findCommentedSentenceSummaries(memberId, clubId).stream()
                .map(summary -> toResponse(summary, currentPassageSequence))
                .sorted(commentedSentenceComparator())
                .toList();
        return new CommentedSentencesResponse(responses);
    }

    @Transactional
    public CommentResponse update(Long memberId, Long commentId, String content) {
        Comment comment = findOwnComment(memberId, commentId, "본인의 댓글만 수정할 수 있습니다.");
        comment.ensureBookAvailable();
        comment.updateContent(content);
        return CommentResponse.of(comment, memberId);
    }

    @Transactional
    public void delete(Long memberId, Long commentId) {
        Comment comment = findOwnComment(memberId, commentId, "본인의 댓글만 삭제할 수 있습니다.");
        comment.ensureBookAvailable();
        commentRepository.delete(comment);
    }

    @Transactional
    public void report(Long memberId, Long commentId) {
        Comment comment = commentRepository.findVisibleWithContextById(memberId, commentId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.COMMENT_NOT_FOUND));
        comment.ensureReportableBy(memberId);
        if (!clubMemberRepository.existsJoinedByMemberIdAndCommentId(memberId, commentId)) {
            throw new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER);
        }
        comment.ensureBookAvailable();
        if (!commentReportRepository.existsByReporterIdAndCommentId(memberId, commentId)) {
            commentReportRepository.save(new CommentReport(memberRepository.getReferenceById(memberId), comment));
        }
    }

    private Comment findOwnComment(Long memberId, Long commentId, String forbiddenMessage) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.isWrittenBy(memberId)) {
            throw new ForbiddenException(ErrorCode.NOT_COMMENT_OWNER, forbiddenMessage);
        }
        if (!comment.isWriterJoined()) {
            throw new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER);
        }
        return comment;
    }

    private SentenceContext validateSentenceContext(Long memberId, Long clubId, Long sentenceId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CLUB_NOT_FOUND));
        ClubMember clubMember = clubMemberRepository.findJoinedByMemberIdAndClubId(memberId, clubId)
                .orElseThrow(() -> new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER));
        Sentence sentence = sentenceRepository.findById(sentenceId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SENTENCE_NOT_FOUND));
        if (!club.isReading(sentence)) {
            throw new NotFoundException(ErrorCode.SENTENCE_NOT_FOUND);
        }
        club.ensureBookAvailable();
        return new SentenceContext(clubMember, sentence);
    }

    private record SentenceContext(ClubMember clubMember, Sentence sentence) {
    }

    private Passage validatePassageContext(Long memberId, Long clubId, Long passageId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CLUB_NOT_FOUND));
        clubMemberRepository.findJoinedByMemberIdAndClubId(memberId, clubId)
                .orElseThrow(() -> new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER));
        Passage passage = passageRepository.findById(passageId)
                .orElseThrow(() -> new BadRequestException(ErrorCode.INVALID_REQUEST));
        if (!club.isReading(passage)) {
            throw new BadRequestException(ErrorCode.INVALID_REQUEST);
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
