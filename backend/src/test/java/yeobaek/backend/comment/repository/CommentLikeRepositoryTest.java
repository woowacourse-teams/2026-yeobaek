package yeobaek.backend.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.domain.Comment;
import yeobaek.backend.comment.domain.CommentLike;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.IntegrationTest;

class CommentLikeRepositoryTest extends IntegrationTest {

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookManagementRepository bookRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private PassageRepository passageRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    private Member writer;
    private Member reader;
    private Comment comment;
    private Comment otherComment;

    @BeforeEach
    void setUp() {
        Book book = bookRepository.save(new Book(new BookTitle("좋아요 테스트 도서"), null, 1924, 1, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage passage = passageRepository.save(new Passage(chapter, 1, List.of(new SentenceContent("본문"))));
        writer = memberRepository.save(new Member(new Nickname("작성자")));
        reader = memberRepository.save(new Member(new Nickname("독자")));
        Club club = clubRepository.save(new Club(new ClubName("좋아요 모임"), book, new JoinCode("LIKE01")));
        ClubMember membership = clubMemberRepository.save(new ClubMember(writer, club));
        comment = commentRepository.saveAndFlush(
                new Comment(membership, passage.getSentences().getFirst(), new CommentContent("첫 댓글")));
        otherComment = commentRepository.saveAndFlush(
                new Comment(membership, passage.getSentences().getFirst(), new CommentContent("다른 댓글")));
    }

    @Test
    @DisplayName("본인 댓글에도 좋아요를 저장할 수 있다")
    void saveOwnCommentLike() {
        commentLikeRepository.saveAndFlush(new CommentLike(writer, comment));

        assertThat(commentLikeRepository.existsByMemberIdAndCommentId(writer.getId(), comment.getId())).isTrue();
        assertThat(commentLikeRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 회원과 댓글의 좋아요는 DB에서 중복 저장을 거부한다")
    void rejectDuplicateLike() {
        commentLikeRepository.saveAndFlush(new CommentLike(reader, comment));

        assertThatThrownBy(() -> commentLikeRepository.saveAndFlush(new CommentLike(reader, comment)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(commentLikeRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("서로 다른 회원은 같은 댓글에 각각 좋아요를 저장할 수 있다")
    void saveLikesFromDifferentMembers() {
        commentLikeRepository.saveAndFlush(new CommentLike(reader, comment));
        commentLikeRepository.saveAndFlush(new CommentLike(writer, comment));

        assertThat(commentLikeRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("같은 회원은 서로 다른 댓글에 각각 좋아요를 저장할 수 있다")
    void saveLikesOnDifferentComments() {
        commentLikeRepository.saveAndFlush(new CommentLike(reader, comment));
        commentLikeRepository.saveAndFlush(new CommentLike(reader, otherComment));

        assertThat(commentLikeRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("회원 삭제 시 그 회원의 좋아요만 DB cascade로 삭제한다")
    void deleteByMemberCascade() {
        commentLikeRepository.saveAndFlush(new CommentLike(reader, comment));
        commentLikeRepository.saveAndFlush(new CommentLike(writer, comment));

        memberRepository.deleteById(reader.getId());
        memberRepository.flush();

        assertThat(commentLikeRepository.count()).isEqualTo(1);
        assertThat(commentLikeRepository.existsByMemberIdAndCommentId(writer.getId(), comment.getId())).isTrue();
    }

    @Test
    @DisplayName("댓글 삭제 시 해당 댓글의 좋아요만 DB cascade로 삭제한다")
    void deleteByCommentCascade() {
        commentLikeRepository.saveAndFlush(new CommentLike(reader, comment));
        commentLikeRepository.saveAndFlush(new CommentLike(writer, comment));
        commentLikeRepository.saveAndFlush(new CommentLike(reader, otherComment));

        commentRepository.deleteById(comment.getId());
        commentRepository.flush();

        assertThat(commentLikeRepository.count()).isEqualTo(1);
        assertThat(commentLikeRepository.existsByMemberIdAndCommentId(reader.getId(), otherComment.getId())).isTrue();
    }
}
