package yeobaek.backend.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.Sentence;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.book.repository.SentenceRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.domain.Comment;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.comment.dto.CommentResponse;
import yeobaek.backend.comment.repository.CommentLikeRepository;
import yeobaek.backend.comment.repository.CommentLikeSummary;
import yeobaek.backend.comment.repository.CommentReportRepository;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.comment.repository.CommentViewRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberBlockRepository;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;
import yeobaek.backend.support.NotFoundException;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentLikeRepository commentLikeRepository;
    @Mock
    private CommentReportRepository commentReportRepository;
    @Mock
    private CommentViewRepository commentViewRepository;
    @Mock
    private ClubRepository clubRepository;
    @Mock
    private ClubMemberRepository clubMemberRepository;
    @Mock
    private SentenceRepository sentenceRepository;
    @Mock
    private PassageRepository passageRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MemberBlockRepository memberBlockRepository;
    @Mock
    private PublicRoomRepository publicRoomRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CommentService commentService;

    private Book book;
    private Club club;
    private Sentence sentence;
    private Comment comment;

    @BeforeEach
    void setUp() {
        book = new Book(new BookTitle("함께 읽는 책"), null, null, 1, null, null, null, null, null);
        ReflectionTestUtils.setField(book, "id", 10L);
        Chapter chapter = new Chapter(book, new ChapterTitle("첫 장"), 1);
        Passage passage = new Passage(chapter, 1, List.of(new SentenceContent("함께 읽는 문장")));
        sentence = passage.getSentences().getFirst();
        ReflectionTestUtils.setField(sentence, "id", 20L);
        club = new Club(new ClubName("독서 모임"), book, new JoinCode("CODE01"));
        ReflectionTestUtils.setField(club, "id", 30L);
        Member writer = new Member(new Nickname("작성자"));
        ReflectionTestUtils.setField(writer, "id", 1L);
        comment = new Comment(new ClubMember(writer, club), sentence, new CommentContent("원래 내용"));
        ReflectionTestUtils.setField(comment, "id", 40L);
    }

    @Test
    @DisplayName("본인 댓글 수정 응답에는 새 내용, 수정 시각, 좋아요 집계가 포함된다")
    void updateOwnComment() {
        given(commentRepository.findById(40L)).willReturn(Optional.of(comment));
        given(clubMemberRepository.existsJoinedByMemberIdAndClubId(1L, 30L)).willReturn(true);
        given(commentLikeRepository.findSummaryByCommentId(1L, 40L))
                .willReturn(Optional.of(new CommentLikeSummary(40L, 3L, true)));

        CommentResponse response = commentService.update(1L, 40L, new CommentContent("바꾼 내용"));

        assertThat(response.content()).isEqualTo("바꾼 내용");
        assertThat(response.updatedAt()).isNotNull();
        assertThat(response).extracting(CommentResponse::likeCount, CommentResponse::likedByMe)
                .containsExactly(3L, true);
        assertThat(response.mine()).isTrue();
        assertThat(comment.getContent()).isEqualTo("바꾼 내용");
    }

    @Test
    @DisplayName("좋아요가 없는 댓글 수정 응답은 0개와 false를 반환한다")
    void updateWithoutLikes() {
        given(commentRepository.findById(40L)).willReturn(Optional.of(comment));
        given(clubMemberRepository.existsJoinedByMemberIdAndClubId(1L, 30L)).willReturn(true);

        CommentResponse response = commentService.update(1L, 40L, new CommentContent("바꾼 내용"));

        assertThat(response).extracting(CommentResponse::likeCount, CommentResponse::likedByMe)
                .containsExactly(0L, false);
    }

    @Test
    @DisplayName("남의 댓글 수정은 거부하고 원문과 수정 시각을 보존한다")
    void rejectUpdatingOthersComment() {
        given(commentRepository.findById(40L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.update(2L, 40L, new CommentContent("수정 시도")))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_COMMENT_OWNER));
        assertThat(comment.getContent()).isEqualTo("원래 내용");
        assertThat(comment.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("남의 댓글 삭제는 거부하고 삭제 요청을 전달하지 않는다")
    void rejectDeletingOthersComment() {
        given(commentRepository.findById(40L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(2L, 40L))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_COMMENT_OWNER));
        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    @DisplayName("모임 탈퇴자의 댓글 수정과 삭제는 거부한다")
    void rejectChangesAfterLeaving() {
        given(commentRepository.findById(40L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.update(1L, 40L, new CommentContent("수정 시도")))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_CLUB_MEMBER));
        assertThatThrownBy(() -> commentService.delete(1L, 40L))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_CLUB_MEMBER));
        assertThat(comment.getContent()).isEqualTo("원래 내용");
        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    @DisplayName("없는 댓글의 수정과 삭제는 COMMENT_NOT_FOUND로 실패한다")
    void rejectUnknownComment() {
        assertThatThrownBy(() -> commentService.update(1L, 40L, new CommentContent("수정 시도")))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.COMMENT_NOT_FOUND));
        assertThatThrownBy(() -> commentService.delete(1L, 40L))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.COMMENT_NOT_FOUND));
    }

    @Test
    @DisplayName("미가입 또는 탈퇴한 회원은 모임 댓글을 작성하거나 조회할 수 없다")
    void rejectUnavailableMembership() {
        given(clubRepository.findById(30L)).willReturn(Optional.of(club));

        assertThatThrownBy(() -> commentService.create(1L, 30L, 20L, new CommentContent("댓글")))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_CLUB_MEMBER));
        assertThatThrownBy(() -> commentService.findComments(1L, 30L, 20L))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_CLUB_MEMBER));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("존재하지 않는 문장의 댓글 조회는 SENTENCE_NOT_FOUND로 실패한다")
    void rejectUnknownSentence() {
        given(clubRepository.findById(30L)).willReturn(Optional.of(club));
        given(clubMemberRepository.findJoinedByMemberIdAndClubId(1L, 30L))
                .willReturn(Optional.of(new ClubMember(comment.getWriter(), club)));

        assertThatThrownBy(() -> commentService.findComments(1L, 30L, 999L))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.SENTENCE_NOT_FOUND));
    }

    @Test
    @DisplayName("본인 댓글 신고는 CANNOT_REPORT_OWN_COMMENT로 거부한다")
    void rejectOwnCommentReport() {
        given(commentRepository.findByIdForUpdate(40L)).willReturn(Optional.of(comment));
        given(commentRepository.findVisibleWithContextById(1L, 40L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.report(1L, 40L))
                .isInstanceOfSatisfying(BadRequestException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.CANNOT_REPORT_OWN_COMMENT));
        verify(commentReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("삭제된 도서의 댓글 수정은 원문을 보존하며 거부한다")
    void rejectDeletedBookUpdate() {
        book.delete();
        given(commentRepository.findById(40L)).willReturn(Optional.of(comment));
        given(clubMemberRepository.existsJoinedByMemberIdAndClubId(1L, 30L)).willReturn(true);

        assertThatThrownBy(() -> commentService.update(1L, 40L, new CommentContent("수정 시도")))
                .isInstanceOfSatisfying(BadRequestException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE));
        assertThat(comment.getContent()).isEqualTo("원래 내용");
    }
}
