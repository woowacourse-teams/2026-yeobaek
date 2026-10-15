package yeobaek.backend.comment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.Sentence;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;

class CommentsTest {

    @Test
    @DisplayName("댓글 id를 순서대로 반환한다")
    void ids() {
        Comment first = commentWithId(1L);
        Comment second = commentWithId(2L);
        Comments comments = new Comments(List.of(first, second));

        assertThat(comments.ids()).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("제외할 id에 포함되지 않은 댓글만 반환한다")
    void excludingIds() {
        Comment first = commentWithId(1L);
        Comment second = commentWithId(2L);
        Comments comments = new Comments(List.of(first, second));

        assertThat(comments.excludingIds(Set.of(1L))).containsExactly(second);
    }

    @Test
    @DisplayName("댓글이 없으면 빈 컬렉션이다")
    void isEmpty() {
        Comments comments = new Comments(List.of());

        assertThat(comments.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("제외할 작성자가 없으면 원래 순서대로 모든 댓글을 반환한다")
    void excludingWrittenByNobody() {
        Comment first = commentWithWriter(1L, memberWithId(10L));
        Comment second = commentWithWriter(2L, memberWithId(20L));
        Comments comments = new Comments(List.of(first, second));

        Comments filtered = comments.excludingWrittenBy(Set.of());

        assertThat(filtered.asList()).containsExactly(first, second);
    }

    @Test
    @DisplayName("모든 댓글 작성자를 제외하면 빈 댓글 목록을 반환한다")
    void excludingAllWriters() {
        Member writer = memberWithId(10L);
        Comments comments = new Comments(List.of(
                commentWithWriter(1L, writer),
                commentWithWriter(2L, writer)));

        Comments filtered = comments.excludingWrittenBy(Set.of(10L));

        assertThat(filtered.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("특정 작성자의 여러 댓글만 제외하고 나머지 댓글 순서를 유지한다")
    void excludingSomeWriters() {
        Member blockedWriter = memberWithId(10L);
        Comment firstBlocked = commentWithWriter(1L, blockedWriter);
        Comment visible = commentWithWriter(2L, memberWithId(20L));
        Comment secondBlocked = commentWithWriter(3L, blockedWriter);
        Comments comments = new Comments(List.of(firstBlocked, visible, secondBlocked));

        Comments filtered = comments.excludingWrittenBy(Set.of(10L));

        assertThat(filtered.asList()).containsExactly(visible);
        assertThat(comments.asList()).containsExactly(firstBlocked, visible, secondBlocked);
    }

    @Test
    @DisplayName("문장별 댓글 수, 미확인 수, 최신 작성 시각을 집계한다")
    void summarizeBySentence() {
        Passage passage = passageWithTwoSentences();
        Sentence firstSentence = passage.getSentences().get(0);
        Sentence secondSentence = passage.getSentences().get(1);
        LocalDateTime firstCreatedAt = LocalDateTime.of(2026, 10, 14, 10, 0);
        LocalDateTime latestCreatedAt = LocalDateTime.of(2026, 10, 14, 11, 0);
        Comment first = comment(1L, memberWithId(10L), firstSentence, firstCreatedAt);
        Comment latest = comment(2L, memberWithId(20L), firstSentence, latestCreatedAt);
        Comment secondSentenceComment = comment(
                3L, memberWithId(10L), secondSentence, firstCreatedAt.minusHours(1));

        List<CommentedSentence> summaries = new Comments(List.of(first, latest, secondSentenceComment))
                .summarizeBySentence(Set.of(1L));

        assertThat(summaries).hasSize(2);
        assertThat(summaries.get(0)).satisfies(summary -> {
            assertThat(summary.sentenceId()).isEqualTo(firstSentence.getId());
            assertThat(summary.commentCount()).isEqualTo(2);
            assertThat(summary.unreadCommentCount()).isEqualTo(1);
            assertThat(summary.latestCommentCreatedAt()).isEqualTo(latestCreatedAt);
        });
        assertThat(summaries.get(1)).satisfies(summary -> {
            assertThat(summary.sentenceId()).isEqualTo(secondSentence.getId());
            assertThat(summary.commentCount()).isEqualTo(1);
            assertThat(summary.unreadCommentCount()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("댓글이 없으면 문장별 집계도 비어 있다")
    void summarizeEmptyComments() {
        assertThat(new Comments(List.of()).summarizeBySentence(Set.of())).isEmpty();
    }

    private Comment commentWithId(Long id) {
        Comment comment = new Comment();
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    private Comment commentWithWriter(Long id, Member writer) {
        Comment comment = commentWithId(id);
        ReflectionTestUtils.setField(comment, "writer", writer);
        return comment;
    }

    private Comment comment(Long id, Member writer, Sentence sentence, LocalDateTime createdAt) {
        Comment comment = commentWithWriter(id, writer);
        ReflectionTestUtils.setField(comment, "sentence", sentence);
        ReflectionTestUtils.setField(comment, "createdAt", createdAt);
        return comment;
    }

    private Member memberWithId(Long id) {
        Member member = new Member(new Nickname("회원" + id));
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Passage passageWithTwoSentences() {
        Book book = new Book(new BookTitle("도서"), null, null, 1, null);
        Chapter chapter = new Chapter(book, new ChapterTitle("장"), 1);
        Passage passage = new Passage(chapter, 1, List.of(
                new SentenceContent("첫 문장"),
                new SentenceContent("둘째 문장")));
        ReflectionTestUtils.setField(passage.getSentences().get(0), "id", 100L);
        ReflectionTestUtils.setField(passage.getSentences().get(1), "id", 200L);
        return passage;
    }
}
