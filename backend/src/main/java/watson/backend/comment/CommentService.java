package watson.backend.comment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import watson.backend.book.Passage;
import watson.backend.book.PassageRepository;
import watson.backend.club.Club;
import watson.backend.club.ClubMember;
import watson.backend.club.ClubMemberRepository;
import watson.backend.club.ClubRepository;
import watson.backend.support.ForbiddenException;
import watson.backend.support.NotFoundException;

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
                .orElseThrow(() -> new NotFoundException("존재하지 않는 댓글입니다."));
        if (!comment.isWrittenBy(memberId)) {
            throw new ForbiddenException(forbiddenMessage);
        }
        return comment;
    }

    private ClubMember validatePassageContext(Long memberId, Long clubId, Long passageId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 모임입니다."));
        ClubMember clubMember = clubMemberRepository.findByMemberIdAndClubId(memberId, clubId)
                .orElseThrow(() -> new ForbiddenException("모임에 참여하지 않은 회원입니다."));
        Passage passage = passageRepository.findById(passageId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 본문입니다."));
        if (!passage.getChapter().getBook().getId().equals(club.getBook().getId())) {
            throw new IllegalArgumentException("모임의 도서에 속하지 않는 본문입니다.");
        }
        return clubMember;
    }
}
