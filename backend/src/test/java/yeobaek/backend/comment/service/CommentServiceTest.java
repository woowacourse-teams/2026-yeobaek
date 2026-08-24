package yeobaek.backend.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.repository.BookArchiveRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.domain.Comment;
import yeobaek.backend.comment.dto.CommentResponse;
import yeobaek.backend.comment.dto.CommentsResponse;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;
import yeobaek.backend.support.IntegrationTest;
import yeobaek.backend.support.NotFoundException;

class CommentServiceTest extends IntegrationTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private BookArchiveRepository bookRepository;

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
    private Book book;
    private Member other;
    private Club club;
    private Club otherClub;
    private Passage passage;

    @BeforeEach
    void setUp() {
        book = bookRepository.save(new Book("운수 좋은 날", null, 1924, 2));
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
    @DisplayName("탈퇴 회원의 댓글은 작성자 정보와 내용이 그대로 조회된다")
    void preserveLeftMembersComment() {
        CommentResponse created = commentService.create(writer.getId(), club.getId(), passage.getId(), "남겨진 댓글");
        leaveClub(writer, club);

        CommentsResponse response = commentService.findComments(other.getId(), club.getId(), passage.getId());

        assertThat(response.comments()).singleElement().satisfies(comment -> {
            assertThat(comment.commentId()).isEqualTo(created.commentId());
            assertThat(comment.nickname()).isEqualTo("민서");
            assertThat(comment.content()).isEqualTo("남겨진 댓글");
            assertThat(comment.mine()).isFalse();
        });
    }

    @Test
    @DisplayName("모임 미소속 회원은 댓글을 작성할 수 없다")
    void rejectOutsider() {
        Member outsider = memberRepository.save(new Member("외부인"));

        assertThatThrownBy(() -> commentService.create(outsider.getId(), club.getId(), passage.getId(), "댓글"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("탈퇴 회원은 댓글을 조회하거나 작성할 수 없다")
    void rejectCommentContextForLeftMember() {
        leaveClub(writer, club);

        assertThatThrownBy(() -> commentService.findComments(writer.getId(), club.getId(), passage.getId()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> commentService.create(writer.getId(), club.getId(), passage.getId(), "댓글"))
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
    @DisplayName("탈퇴 회원은 재가입 전까지 기존 댓글을 수정하거나 삭제할 수 없다")
    void rejectChangingCommentForLeftMember() {
        CommentResponse created = commentService.create(writer.getId(), club.getId(), passage.getId(), "보존할 댓글");
        leaveClub(writer, club);

        assertThatThrownBy(() -> commentService.update(writer.getId(), created.commentId(), "수정 시도"))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> commentService.delete(writer.getId(), created.commentId()))
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

    @Test
    @DisplayName("삭제된 도서의 댓글 조회·작성·수정·삭제를 모두 거부하고 기존 댓글을 보존한다")
    void rejectAllCommentOperationsForDeletedBook() {
        CommentResponse existing = commentService.create(writer.getId(), club.getId(), passage.getId(), "원본");
        bookRepository.delete(book.getId());

        assertBookNotAvailable(() -> commentService.findComments(writer.getId(), club.getId(), passage.getId()));
        assertBookNotAvailable(() -> commentService.create(writer.getId(), club.getId(), passage.getId(), "신규"));
        assertBookNotAvailable(() -> commentService.update(writer.getId(), existing.commentId(), "수정"));
        assertBookNotAvailable(() -> commentService.delete(writer.getId(), existing.commentId()));
        assertThat(commentRepository.findById(existing.commentId())).get()
                .extracting(Comment::getContent).isEqualTo("원본");
    }

    private void assertBookNotAvailable(org.assertj.core.api.ThrowableAssert.ThrowingCallable operation) {
        assertThatThrownBy(operation)
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE);
    }

    private void leaveClub(Member member, Club targetClub) {
        ClubMember membership = clubMemberRepository
                .findByMemberIdAndClubId(member.getId(), targetClub.getId()).orElseThrow();
        membership.leave();
        clubMemberRepository.saveAndFlush(membership);
    }
}
