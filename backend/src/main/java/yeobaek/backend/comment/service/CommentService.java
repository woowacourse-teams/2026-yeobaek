package yeobaek.backend.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.domain.Sentence;
import yeobaek.backend.book.repository.SentenceRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.domain.Comment;
import yeobaek.backend.comment.dto.CommentResponse;
import yeobaek.backend.comment.dto.CommentsResponse;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;
import yeobaek.backend.support.NotFoundException;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final SentenceRepository sentenceRepository;

    @Transactional(readOnly = true)
    public CommentsResponse findComments(Long memberId, Long clubId, Long sentenceId) {
        validateSentenceContext(memberId, clubId, sentenceId);
        return new CommentsResponse(commentRepository
                .findAllVisibleWithWriterByClubIdAndSentenceId(memberId, clubId, sentenceId).stream()
                .map(comment -> CommentResponse.of(comment, memberId))
                .toList());
    }

    @Transactional
    public CommentResponse create(Long memberId, Long clubId, Long sentenceId, String content) {
        SentenceContext context = validateSentenceContext(memberId, clubId, sentenceId);
        Comment comment = commentRepository.save(new Comment(context.clubMember(), context.sentence(), content));
        return CommentResponse.of(comment, memberId);
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
            throw new IllegalArgumentException("모임의 도서에 속하지 않는 문장입니다.");
        }
        club.ensureBookAvailable();
        return new SentenceContext(clubMember, sentence);
    }

    private record SentenceContext(ClubMember clubMember, Sentence sentence) {
    }
}
