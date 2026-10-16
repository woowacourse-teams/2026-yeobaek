package yeobaek.backend.comment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.publicroom.domain.PublicRoom;

class CommentTest {

    private final Member writer = new Member(new Nickname("민서"));
    private final Book book = new Book(new BookTitle("제목"), null, null, 1, null);
    private final ClubMember clubMember = new ClubMember(writer,
            new Club(new ClubName("모임"), book, new JoinCode("CODE01")));
    private final PublicRoom publicRoom = new PublicRoom(book);

    @Test
    @DisplayName("1~1000자 내용으로 댓글을 생성할 수 있다")
    void createWithValidContent() {
        assertThatCode(() -> new Comment(clubMember, null, new CommentContent("가".repeat(1000))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("모임 댓글을 생성하면 모임 회원의 작성자와 연결된다")
    void createClubComment() {
        Comment comment = new Comment(clubMember, null, new CommentContent("내용"));

        assertThat(comment.getWriter()).isSameAs(writer);
        assertThat(comment.isPublicRoomComment()).isFalse();
        assertThatCode(comment::ensureBookAvailable).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("공개방 댓글을 생성하면 전달한 작성자와 연결된다")
    void createPublicRoomComment() {
        Comment comment = new Comment(publicRoom, writer, null, new CommentContent("내용"));

        assertThat(comment.getWriter()).isSameAs(writer);
        assertThat(comment.isPublicRoomComment()).isTrue();
    }

    @Test
    @DisplayName("모임 회원이 없으면 모임 댓글 생성에 실패한다")
    void rejectNullClubMember() {
        assertThatThrownBy(() -> new Comment((ClubMember) null, null, new CommentContent("내용")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("모임 회원은 필수입니다.");
    }

    @Test
    @DisplayName("공개방이 없으면 공개방 댓글 생성에 실패한다")
    void rejectNullPublicRoom() {
        assertThatThrownBy(() -> new Comment(null, writer, null, new CommentContent("내용")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("공개방은 필수입니다.");
    }

    @Test
    @DisplayName("댓글을 생성하면 생성 시각이 즉시 채워지고 수정 시각은 비어있다")
    void createFillsCreatedAtOnly() {
        Comment comment = new Comment(clubMember, null, new CommentContent("내용"));

        assertThat(comment.getCreatedAt()).isNotNull();
        assertThat(comment.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("내용을 수정하면 수정 시각이 채워지고 생성 시각은 바뀌지 않는다")
    void updateContentFillsUpdatedAt() {
        Comment comment = new Comment(clubMember, null, new CommentContent("원본"));
        var createdAt = comment.getCreatedAt();

        comment.updateContent(new CommentContent("수정된 내용"));

        assertThat(comment.getContent()).isEqualTo("수정된 내용");
        assertThat(comment.getUpdatedAt()).isNotNull();
        assertThat(comment.getCreatedAt()).isEqualTo(createdAt);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("내용이 없거나 공백뿐이면 댓글 생성에 실패한다")
    void rejectBlankContent(String content) {
        assertThatThrownBy(() -> new Comment(clubMember, null, new CommentContent(content)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("내용이 1000자를 넘으면 댓글 생성에 실패한다")
    void rejectTooLongContent() {
        assertThatThrownBy(() -> new Comment(clubMember, null, new CommentContent("가".repeat(1001))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
