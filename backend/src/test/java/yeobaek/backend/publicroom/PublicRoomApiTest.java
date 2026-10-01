package yeobaek.backend.publicroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.comment.repository.CommentReportRepository;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.comment.service.CommentService;
import yeobaek.backend.comment.domain.Comment;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.publicroom.domain.PublicRoom;
import yeobaek.backend.publicroom.repository.PublicRoomActivityRepository;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.publicroom.service.PublicRoomBackfill;
import yeobaek.backend.publicroom.service.PublicRoomService;
import yeobaek.backend.support.IntegrationTest;
import org.springframework.boot.DefaultApplicationArguments;

class PublicRoomApiTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookManagementRepository bookRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private PassageRepository passageRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PublicRoomRepository publicRoomRepository;

    @Autowired
    private PublicRoomActivityRepository activityRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentReportRepository commentReportRepository;

    @Autowired
    private PublicRoomService publicRoomService;

    @Autowired
    private PublicRoomBackfill publicRoomBackfill;

    @Autowired
    private CommentService commentService;

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
    @DisplayName("기존 도서 backfill은 재실행해도 도서마다 공개방 하나를 유지한다")
    void backfillExistingBooks() {
        Book first = bookRepository.save(new Book(new BookTitle("기존 책 1"), null, null, 1, null));
        Book second = bookRepository.save(new Book(new BookTitle("기존 책 2"), null, null, 1, null));

        publicRoomBackfill.run(new DefaultApplicationArguments());
        publicRoomBackfill.run(new DefaultApplicationArguments());

        assertThat(publicRoomRepository.findByBookId(first.getId())).isPresent();
        assertThat(publicRoomRepository.findByBookId(second.getId())).isPresent();
        assertThat(publicRoomRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("동시 최초 방문과 진도 저장은 활동 한 건에 두 상태를 모두 보존한다")
    void concurrentFirstWrites() throws Exception {
        ReadingFixture fixture = createRoom("동시성 책", 1);
        Member member = memberRepository.save(new Member(new Nickname("동시 독자")));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var visit = executor.submit(() -> {
                await(start);
                publicRoomService.visit(member.getId(), fixture.room().getId());
            });
            var progress = executor.submit(() -> {
                await(start);
                publicRoomService.updateProgress(member.getId(), fixture.room().getId(),
                        fixture.passages().getFirst().getId());
            });
            start.countDown();
            visit.get(10, TimeUnit.SECONDS);
            progress.get(10, TimeUnit.SECONDS);
        }

        var activity = activityRepository.findByMemberIdAndPublicRoomId(
                member.getId(), fixture.room().getId()).orElseThrow();
        assertThat(activity.getLastVisitedAt()).isNotNull();
        assertThat(activity.getLastReadPassage()).isNotNull();
        assertThat(activityRepository.count()).isOne();
    }

    @Test
    @DisplayName("공개방 방문·진도·목록과 삭제 도서 경계를 계약대로 처리한다")
    void readingFlow() throws Exception {
        ReadingFixture first = createRoom("첫 번째 책", 2);
        ReadingFixture second = createRoom("두 번째 책", 1);
        Member member = memberRepository.save(new Member(new Nickname("독자")));
        Member other = memberRepository.save(new Member(new Nickname("다른 독자")));

        visit(first.room(), member);
        visit(first.room(), member);
        visit(second.room(), other);
        visit(second.room(), member);

        mockMvc.perform(get("/api/public-rooms")
                        .header("X-Member-Id", member.getId())
                        .queryParam("sort", "MOST_VISITED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicRooms[0].publicRoomId").value(second.room().getId()))
                .andExpect(jsonPath("$.publicRooms[1].publicRoomId").value(first.room().getId()))
                .andExpect(jsonPath("$.publicRooms[1].myProgress").value((Object) null));
        assertThat(activityRepository.count()).isEqualTo(3);

        Passage lastPassage = first.passages().getLast();
        mockMvc.perform(put("/api/public-rooms/{publicRoomId}/progress", first.room().getId())
                        .header("X-Member-Id", member.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passageId\":" + lastPassage.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastReadPassageSequence").value(2))
                .andExpect(jsonPath("$.progressRate").value(100));

        mockMvc.perform(get("/api/members/me/recent-reading")
                        .header("X-Member-Id", member.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.space.type").value("PUBLIC_ROOM"))
                .andExpect(jsonPath("$.space.publicRoomId").value(first.room().getId()))
                .andExpect(jsonPath("$.space.clubId").doesNotExist());

        bookRepository.delete(first.book().getId());

        mockMvc.perform(get("/api/public-rooms/{publicRoomId}", first.room().getId())
                        .header("X-Member-Id", member.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.book.status").value("DELETED"))
                .andExpect(jsonPath("$.myProgress.lastReadPassageSequence").value(2));
        mockMvc.perform(get("/api/members/me/recent-reading")
                        .header("X-Member-Id", member.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.book.status").value("DELETED"));
        mockMvc.perform(get("/api/public-rooms/{publicRoomId}/passages", first.room().getId())
                        .header("X-Member-Id", member.getId())
                        .queryParam("from", "1").queryParam("to", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_AVAILABLE"));
        mockMvc.perform(get("/api/public-rooms").header("X-Member-Id", member.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicRooms.length()").value(1));
    }

    @Test
    @DisplayName("공개방 댓글은 발견·직접 확인·차단·공통 수정과 신고 계약을 따른다")
    void commentFlow() throws Exception {
        ReadingFixture fixture = createRoom("댓글 책", 1);
        Member writer = memberRepository.save(new Member(new Nickname("작성자")));
        Member reader = memberRepository.save(new Member(new Nickname("독자")));
        Long sentenceId = fixture.passages().getFirst().getSentences().getFirst().getId();
        Long passageId = fixture.passages().getFirst().getId();

        String created = mockMvc.perform(post(
                        "/api/public-rooms/{publicRoomId}/sentences/{sentenceId}/comments",
                        fixture.room().getId(), sentenceId)
                        .header("X-Member-Id", writer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"첫 댓글\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mine").value(true))
                .andReturn().getResponse().getContentAsString();
        Long commentId = Long.valueOf(created.replaceAll(".*\\\"commentId\\\":(\\d+).*", "$1"));

        mockMvc.perform(get("/api/public-rooms/{publicRoomId}/comments/new-count", fixture.room().getId())
                        .header("X-Member-Id", reader.getId())
                        .queryParam("currentPassageId", passageId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newCommentCount").value(1));
        mockMvc.perform(get("/api/public-rooms/{publicRoomId}/passages", fixture.room().getId())
                        .header("X-Member-Id", reader.getId())
                        .queryParam("from", "1").queryParam("to", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passages[0].sentences[0].commentCount").value(1));
        mockMvc.perform(post(
                        "/api/public-rooms/{publicRoomId}/sentences/{sentenceId}/comment-detail-views",
                        fixture.room().getId(), sentenceId)
                        .header("X-Member-Id", reader.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].commentId").value(commentId));
        mockMvc.perform(get("/api/public-rooms/{publicRoomId}/comments/new-count", fixture.room().getId())
                        .header("X-Member-Id", reader.getId())
                        .queryParam("currentPassageId", passageId.toString()))
                .andExpect(jsonPath("$.newCommentCount").value(0));

        mockMvc.perform(put("/api/comments/{commentId}", commentId)
                        .header("X-Member-Id", writer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정 댓글\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("수정 댓글"));
        mockMvc.perform(post("/api/comments/{commentId}/reports", commentId)
                        .header("X-Member-Id", reader.getId()))
                .andExpect(status().isNoContent());
        assertThat(commentReportRepository.count()).isOne();

        mockMvc.perform(put("/api/members/me/blocks/{memberId}", writer.getId())
                        .header("X-Member-Id", reader.getId()))
                .andExpect(status().isNoContent());
        mockMvc.perform(post(
                        "/api/public-rooms/{publicRoomId}/sentences/{sentenceId}/comment-detail-views",
                        fixture.room().getId(), sentenceId)
                        .header("X-Member-Id", reader.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isEmpty());
        mockMvc.perform(post("/api/comments/{commentId}/reports", commentId)
                        .header("X-Member-Id", reader.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("공개방의 같은 댓글 상태 그룹은 최신 댓글 시각보다 책의 문단·문장 순서로 정렬한다")
    void commentedSentencesUseBookOrderWithinGroup() throws Exception {
        Book book = bookRepository.save(new Book(new BookTitle("댓글 정렬 책"), null, null, 2, null));
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

        mockMvc.perform(get("/api/public-rooms/{publicRoomId}/commented-sentences", room.getId())
                        .header("X-Member-Id", reader.getId())
                        .queryParam("currentPassageId", secondPassage.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentedSentences.length()").value(3))
                .andExpect(jsonPath("$.commentedSentences[0].passageSequence").value(1))
                .andExpect(jsonPath("$.commentedSentences[0].sentenceSequence").value(1))
                .andExpect(jsonPath("$.commentedSentences[1].passageSequence").value(1))
                .andExpect(jsonPath("$.commentedSentences[1].sentenceSequence").value(2))
                .andExpect(jsonPath("$.commentedSentences[2].passageSequence").value(2))
                .andExpect(jsonPath("$.commentedSentences[2].sentenceSequence").value(1));
    }

    @Test
    @DisplayName("계정 삭제는 공개방 활동과 댓글을 제거해 인기 집계에서도 제외한다")
    void memberDeletion() throws Exception {
        ReadingFixture fixture = createRoom("탈퇴 책", 1);
        Member leaving = memberRepository.save(new Member(new Nickname("탈퇴자")));
        Member remaining = memberRepository.save(new Member(new Nickname("잔여자")));
        visit(fixture.room(), leaving);
        visit(fixture.room(), remaining);
        Long sentenceId = fixture.passages().getFirst().getSentences().getFirst().getId();
        mockMvc.perform(post("/api/public-rooms/{roomId}/sentences/{sentenceId}/comments",
                        fixture.room().getId(), sentenceId)
                        .header("X-Member-Id", leaving.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"삭제될 댓글\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/members/me").header("X-Member-Id", leaving.getId()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(activityRepository.findAll()).hasSize(1)
                .allMatch(activity -> activity.getMember().getId().equals(remaining.getId()));
        assertThat(commentRepository.findAll()).isEmpty();
        assertThat(publicRoomRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("빈 문자열과 지원하지 않는 공개방 정렬은 INVALID_REQUEST다")
    void rejectInvalidSort() throws Exception {
        Member member = memberRepository.save(new Member(new Nickname("정렬 독자")));
        for (String sort : List.of("", "RECENT")) {
            mockMvc.perform(get("/api/public-rooms")
                            .header("X-Member-Id", member.getId())
                            .queryParam("sort", sort))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
    }

    private void visit(PublicRoom room, Member member) throws Exception {
        mockMvc.perform(post("/api/public-rooms/{publicRoomId}/visits", room.getId())
                        .header("X-Member-Id", member.getId()))
                .andExpect(status().isNoContent());
    }

    private ReadingFixture createRoom(String title, int passageCount) {
        Book book = bookRepository.save(new Book(new BookTitle(title), null, null, passageCount, null));
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
