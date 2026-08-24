package yeobaek.backend.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.repository.PassageRepository;
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
    private final PassageRepository passageRepository;

    @Transactional(readOnly = true)
    public CommentsResponse findComments(Long memberId, Long clubId, Long passageId) {
        validatePassageContext(memberId, clubId, passageId);
        return new CommentsResponse(commentRepository.findAllWithWriterByClubIdAndPassageId(clubId, passageId).stream()
                .map(comment -> CommentResponse.of(comment, memberId))
                .toList());
    }

    @Transactional
    public CommentResponse create(Long memberId, Long clubId, Long passageId, String content) {
        ClubMember clubMember = validatePassageContext(memberId, clubId, passageId);
        Passage passage = passageRepository.findById(passageId).orElseThrow();
        Comment comment = commentRepository.save(new Comment(clubMember, passage, content));
        return CommentResponse.of(comment, memberId);
    }

    @Transactional
    public CommentResponse update(Long memberId, Long commentId, String content) {
        Comment comment = findOwnComment(memberId, commentId, "본인의 댓글만 수정할 수 있습니다.");
        comment.updateContent(content);
        return CommentResponse.of(comment, memberId);
    }

    @Transactional
    public void delete(Long memberId, Long commentId) {
        Comment comment = findOwnComment(memberId, commentId, "본인의 댓글만 삭제할 수 있습니다.");
        commentRepository.delete(comment);
    }

    private Comment findOwnComment(Long memberId, Long commentId, String forbiddenMessage) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.isWrittenBy(memberId)) {
            throw new ForbiddenException(ErrorCode.NOT_COMMENT_OWNER, forbiddenMessage);
        }
        if (!comment.isWriterActive()) {
            throw new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER);
        }
        return comment;
    }

    private ClubMember validatePassageContext(Long memberId, Long clubId, Long passageId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CLUB_NOT_FOUND));
        ClubMember clubMember = clubMemberRepository.findActiveByMemberIdAndClubId(memberId, clubId)
                .orElseThrow(() -> new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER));
        Passage passage = passageRepository.findById(passageId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PASSAGE_NOT_FOUND));
        if (!club.isReading(passage)) {
            throw new IllegalArgumentException("모임의 도서에 속하지 않는 본문입니다.");
        }
        return clubMember;
    }
}
