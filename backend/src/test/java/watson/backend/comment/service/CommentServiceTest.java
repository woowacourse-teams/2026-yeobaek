package watson.backend.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import watson.backend.book.domain.Book;
import watson.backend.book.domain.Chapter;
import watson.backend.book.domain.Passage;
import watson.backend.book.repository.BookRepository;
import watson.backend.book.repository.ChapterRepository;
import watson.backend.book.repository.PassageRepository;
import watson.backend.club.domain.Club;
import watson.backend.club.domain.ClubMember;
import watson.backend.club.repository.ClubMemberRepository;
import watson.backend.club.repository.ClubRepository;
import watson.backend.comment.domain.Comment;
import watson.backend.comment.dto.CommentResponse;
import watson.backend.comment.dto.CommentsResponse;
import watson.backend.comment.repository.CommentRepository;
import watson.backend.member.domain.Member;
import watson.backend.member.repository.MemberRepository;
import watson.backend.support.ForbiddenException;
import watson.backend.support.NotFoundException;
import watson.backend.support.RepositoryTest;

@Import(CommentService.class)
class CommentServiceTest extends RepositoryTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private PassageRepository passageRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    private Member writer;
    private Member other;
    private Club club;
    private Club otherClub;
    private Passage passage;

    @BeforeEach
    void setUp() {
        Book book = bookRepository.save(new Book("운수 좋은 날", null, 1924, 2));
        Chapter chapter = chapterRepository.save(new Chapter(book, "1장", 1));
        passage = passageRepository.save(new Passage(chapter, 1, "본문 1"));
        writer = memberRepository.save(new Member("민서"));
        other = memberRepository.save(new Member("지수"));
        club = clubRepository.save(new Club("1기", book, "CODE01"));
        otherClub = clubRepository.save(new Club("2기", book, "CODE02"));
        clubMemberRepository.save(new ClubMember(writer, club));
        clubMemberRepository.save(new ClubMember(other, club));
    }

    @Test
    @DisplayName("댓글을 작성하면 작성자 정보와 mine 플래그가 함께 응답된다")
    void createComment() {
        CommentResponse response = commentService.create(writer.getId(), club.getId(), passage.getId(), "멈칫했어요.");

        assertThat(response.nickname()).isEqualTo("민서");
        assertThat(response.mine()).isTrue();
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNull();
        assertThat(commentRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("댓글 목록은 작성일 오름차순이고 본인 여부가 표시되며 같은 모임의 댓글만 보인다")
    void findCommentsOrderedAndIsolated() {
        commentService.create(writer.getId(), club.getId(), passage.getId(), "첫 댓글");
        commentService.create(other.getId(), club.getId(), passage.getId(), "둘째 댓글");
        ClubMember otherClubMembership = clubMemberRepository.save(new ClubMember(writer, otherClub));
        commentRepository.save(new Comment(otherClubMembership, passage, "다른 모임 댓글"));

        CommentsResponse response = commentService.findComments(writer.getId(), club.getId(), passage.getId());

        assertThat(response.comments()).hasSize(2);
        assertThat(response.comments().get(0).content()).isEqualTo("첫 댓글");
        assertThat(response.comments().get(0).mine()).isTrue();
        assertThat(response.comments().get(1).nickname()).isEqualTo("지수");
        assertThat(response.comments().get(1).mine()).isFalse();
    }

    @Test
    @DisplayName("모임 미소속 회원은 댓글을 작성할 수 없다")
    void rejectOutsider() {
        Member outsider = memberRepository.save(new Member("외부인"));

        assertThatThrownBy(() -> commentService.create(outsider.getId(), club.getId(), passage.getId(), "댓글"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("본인 댓글을 수정하면 내용이 바뀌고 수정일이 기록된다")
    void updateOwnComment() {
        CommentResponse created = commentService.create(writer.getId(), club.getId(), passage.getId(), "원본");

        CommentResponse response = commentService.update(writer.getId(), created.commentId(), "수정된 내용");

        assertThat(response.content()).isEqualTo("수정된 내용");
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("남의 댓글은 수정할 수 없다")
    void rejectUpdatingOthersComment() {
        CommentResponse created = commentService.create(writer.getId(), club.getId(), passage.getId(), "원본");

        assertThatThrownBy(() -> commentService.update(other.getId(), created.commentId(), "탈취 시도"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("본인 댓글을 삭제하면 하드 삭제된다")
    void deleteOwnComment() {
        CommentResponse created = commentService.create(writer.getId(), club.getId(), passage.getId(), "삭제될 댓글");

        commentService.delete(writer.getId(), created.commentId());

        assertThat(commentRepository.findById(created.commentId())).isEmpty();
    }

    @Test
    @DisplayName("남의 댓글은 삭제할 수 없다")
    void rejectDeletingOthersComment() {
        CommentResponse created = commentService.create(writer.getId(), club.getId(), passage.getId(), "원본");

        assertThatThrownBy(() -> commentService.delete(other.getId(), created.commentId()))
                .isInstanceOf(ForbiddenException.class);
        assertThat(commentRepository.findById(created.commentId())).isPresent();
    }

    @Test
    @DisplayName("존재하지 않는 댓글의 수정·삭제는 실패한다")
    void rejectUnknownComment() {
        assertThatThrownBy(() -> commentService.update(writer.getId(), 999L, "내용"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> commentService.delete(writer.getId(), 999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("모임의 도서에 속하지 않는 본문에는 댓글을 달 수 없다")
    void rejectPassageOfOtherBook() {
        Book otherBook = bookRepository.save(new Book("다른 책", null, null, 1));
        Chapter otherChapter = chapterRepository.save(new Chapter(otherBook, "1장", 1));
        Passage otherPassage = passageRepository.save(new Passage(otherChapter, 1, "다른 본문"));

        assertThatThrownBy(() -> commentService.create(writer.getId(), club.getId(), otherPassage.getId(), "댓글"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
