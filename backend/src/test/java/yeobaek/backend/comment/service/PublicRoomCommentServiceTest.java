package yeobaek.backend.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.comment.domain.Comment;
import yeobaek.backend.comment.domain.CommentLike;
import yeobaek.backend.comment.domain.CommentView;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.comment.dto.CommentResponse;
import yeobaek.backend.comment.repository.CommentLikeRepository;
import yeobaek.backend.comment.repository.CommentReportRepository;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.comment.repository.CommentViewRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.MemberBlock;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberBlockRepository;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.publicroom.domain.PublicRoom;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.support.IntegrationTest;

class PublicRoomCommentServiceTest extends IntegrationTest {

    @Autowired
    private BookManagementRepository bookRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private PassageRepository passageRepository;

    @Autowired
    private PublicRoomRepository publicRoomRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private CommentReportRepository commentReportRepository;

    @Autowired
    private CommentViewRepository commentViewRepository;

    @Autowired
    private MemberBlockRepository memberBlockRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("같은 공개방 댓글의 동시 반복 신고는 신고 한 건만 저장한다")
    void concurrentDuplicateReports() throws Exception {
        ReadingFixture fixture = createRoom("동시 신고 책", 1);
        Member writer = memberRepository.save(new Member(new Nickname("신고 대상")));
        Member reporter = memberRepository.save(new Member(new Nickname("신고자")));
        Comment comment = commentRepository.save(new Comment(fixture.room(), writer,
                fixture.passages().getFirst().getSentences().getFirst(), new CommentContent("댓글")));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                await(start);
                commentService.report(reporter.getId(), comment.getId());
            });
            var second = executor.submit(() -> {
                await(start);
                commentService.report(reporter.getId(), comment.getId());
            });
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        }

        assertThat(commentReportRepository.count()).isOne();
    }

    @Test
    @DisplayName("공개방의 같은 댓글 상태 그룹은 최신 댓글 시각보다 책의 문단·문장 순서로 정렬한다")
    void commentedSentencesUseBookOrderWithinGroup() {
        Book book = bookRepository.save(new Book(new BookTitle("댓글 정렬 책"), null, null, 2, null, null, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage firstPassage = passageRepository.save(new Passage(chapter, 1,
                List.of(new SentenceContent("앞 문단 첫 문장"), new SentenceContent("앞 문단 둘째 문장"))));
        Passage secondPassage = passageRepository.save(new Passage(chapter, 2,
                List.of(new SentenceContent("뒤 문단 첫 문장"))));
        PublicRoom room = publicRoomRepository.save(new PublicRoom(book));
        Member writer = memberRepository.save(new Member(new Nickname("정렬 작성자")));
        Member reader = memberRepository.save(new Member(new Nickname("정렬 독자")));
        commentRepository.save(new Comment(room, writer,
                firstPassage.getSentences().getFirst(), new CommentContent("가장 먼저 작성된 앞 문단 첫 문장 댓글")));
        commentRepository.save(new Comment(room, writer,
                firstPassage.getSentences().getLast(), new CommentContent("나중에 작성된 앞 문단 둘째 문장 댓글")));
        commentRepository.save(new Comment(room, writer,
                secondPassage.getSentences().getFirst(), new CommentContent("가장 나중에 작성된 뒤 문단 댓글")));

        var response = commentService.findPublicRoomCommentedSentences(
                reader.getId(), room.getId(), secondPassage.getId());

        assertThat(response.commentedSentences())
                .extracting(summary -> tuple(summary.passageSequence(), summary.sentenceSequence()))
                .containsExactly(tuple(1, 1), tuple(1, 2), tuple(2, 1));
    }

    @Test
    @DisplayName("공개방 댓글 문장 조회는 차단 댓글을 제외하고 확인 여부와 최신 시각을 도메인에서 집계한다")
    void commentedSentencesExcludeBlockedWritersAndAggregateVisibleComments() {
        Book book = bookRepository.save(new Book(new BookTitle("공개방 댓글 집계 책"), null, null, 1, null, null, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage passage = passageRepository.save(new Passage(chapter, 1,
                List.of(new SentenceContent("혼합 댓글 문장"), new SentenceContent("차단 댓글만 있는 문장"))));
        PublicRoom room = publicRoomRepository.save(new PublicRoom(book));
        Member visibleWriter = memberRepository.save(new Member(new Nickname("보이는 작성자")));
        Member blockedWriter = memberRepository.save(new Member(new Nickname("차단 작성자")));
        Member reader = memberRepository.save(new Member(new Nickname("집계 독자")));
        Comment visible = commentRepository.saveAndFlush(new Comment(room, visibleWriter,
                passage.getSentences().getFirst(), new CommentContent("보이는 댓글")));
        Comment blockedInSameSentence = commentRepository.saveAndFlush(new Comment(room, blockedWriter,
                passage.getSentences().getFirst(), new CommentContent("더 나중에 작성된 차단 댓글")));
        Comment blockedInOtherSentence = commentRepository.saveAndFlush(new Comment(room, blockedWriter,
                passage.getSentences().getLast(), new CommentContent("차단 댓글만 존재")));
        LocalDateTime visibleCreatedAt = LocalDateTime.of(2026, 10, 15, 10, 0);
        jdbcTemplate.update("update comments set created_at = ? where id = ?",
                Timestamp.valueOf(visibleCreatedAt), visible.getId());
        jdbcTemplate.update("update comments set created_at = ? where id = ?",
                Timestamp.valueOf(visibleCreatedAt.plusHours(1)), blockedInSameSentence.getId());
        jdbcTemplate.update("update comments set created_at = ? where id = ?",
                Timestamp.valueOf(visibleCreatedAt.plusHours(2)), blockedInOtherSentence.getId());
        commentViewRepository.saveAll(List.of(
                new CommentView(reader, visible),
                new CommentView(reader, visible)));
        memberBlockRepository.save(new MemberBlock(reader, blockedWriter));
        long viewCountBeforeQuery = commentViewRepository.count();

        var response = commentService.findPublicRoomCommentedSentences(
                reader.getId(), room.getId(), passage.getId());

        assertThat(response.commentedSentences()).singleElement().satisfies(summary -> {
            assertThat(summary.sentenceId()).isEqualTo(passage.getSentences().getFirst().getId());
            assertThat(summary.commentCount()).isEqualTo(1);
            assertThat(summary.unreadCommentCount()).isZero();
            assertThat(summary.latestCommentCreatedAt()).isEqualTo(visibleCreatedAt);
        });
        assertThat(commentViewRepository.count()).isEqualTo(viewCountBeforeQuery);
    }

    @Test
    @DisplayName("공개방 댓글 작성 응답은 좋아요 0개와 false로 시작한다")
    void createCommentWithoutLikes() {
        ReadingFixture fixture = createRoom("좋아요 초기값", 1);
        Member writer = memberRepository.save(new Member(new Nickname("작성자")));

        var response = commentService.createInPublicRoom(writer.getId(), fixture.room().getId(),
                fixture.passages().getFirst().getSentences().getFirst().getId(), new CommentContent("댓글"));

        assertThat(response).extracting(CommentResponse::likeCount, CommentResponse::likedByMe)
                .containsExactly(0L, false);
        assertThat(commentLikeRepository.count()).isZero();
    }

    @Test
    @DisplayName("공개방 댓글 목록에도 댓글별 좋아요 집계를 반환하며 좋아요가 없으면 0과 false다")
    void findCommentsWithLikeSummaries() {
        ReadingFixture fixture = createRoom("공개방 좋아요 집계", 1);
        Member writer = memberRepository.save(new Member(new Nickname("작성자")));
        Member reader = memberRepository.save(new Member(new Nickname("독자")));
        var sentence = fixture.passages().getFirst().getSentences().getFirst();
        Comment first = commentRepository.save(new Comment(fixture.room(), writer, sentence, new CommentContent("둘이 좋아요")));
        Comment second = commentRepository.save(new Comment(fixture.room(), writer, sentence, new CommentContent("작성자만 좋아요")));
        Comment third = commentRepository.save(new Comment(fixture.room(), writer, sentence, new CommentContent("좋아요 없음")));
        commentLikeRepository.saveAll(List.of(new CommentLike(writer, first), new CommentLike(reader, first),
                new CommentLike(writer, second)));

        var response = commentService.findPublicRoomComments(reader.getId(), fixture.room().getId(), sentence.getId());

        assertThat(response.comments())
                .extracting(CommentResponse::commentId, CommentResponse::likeCount, CommentResponse::likedByMe)
                .containsExactly(tuple(first.getId(), 2L, true), tuple(second.getId(), 1L, false),
                        tuple(third.getId(), 0L, false));
    }

    private ReadingFixture createRoom(String title, int passageCount) {
        Book book = bookRepository.save(new Book(new BookTitle(title), null, null, passageCount, null, null, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        List<Passage> passages = java.util.stream.IntStream.rangeClosed(1, passageCount)
                .mapToObj(sequence -> passageRepository.save(new Passage(chapter, sequence,
                        List.of(new SentenceContent(sequence + "번 문장")))))
                .toList();
        PublicRoom room = publicRoomRepository.save(new PublicRoom(book));
        return new ReadingFixture(book, room, passages);
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private record ReadingFixture(Book book, PublicRoom room, List<Passage> passages) {
    }
}
