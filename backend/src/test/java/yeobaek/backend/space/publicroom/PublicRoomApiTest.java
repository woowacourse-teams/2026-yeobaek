package yeobaek.backend.space.publicroom;

import yeobaek.backend.support.CommentFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import yeobaek.backend.content.book.persistence.Book;
import yeobaek.backend.content.book.domain.Chapter;
import yeobaek.backend.content.book.domain.Passage;
import yeobaek.backend.content.api.value.BookTitle;
import yeobaek.backend.content.api.value.ChapterTitle;
import yeobaek.backend.content.api.value.SentenceContent;
import yeobaek.backend.content.book.repository.BookManagementRepository;
import yeobaek.backend.content.book.repository.ChapterRepository;
import yeobaek.backend.content.book.repository.PassageRepository;
import yeobaek.backend.appreciation.comment.repository.CommentReportRepository;
import yeobaek.backend.appreciation.comment.repository.CommentRepository;
import yeobaek.backend.collaboration.persistence.AppreciationContextRepository;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.publicroom.PublicRoomVisitApi;
import yeobaek.backend.space.club.persistence.Club;
import yeobaek.backend.space.club.domain.ClubMember;
import yeobaek.backend.space.api.club.ClubName;
import yeobaek.backend.space.api.club.JoinCode;
import yeobaek.backend.space.club.repository.ClubMemberRepository;
import yeobaek.backend.space.club.repository.ClubRepository;
import yeobaek.backend.appreciation.comment.persistence.Comment;
import yeobaek.backend.appreciation.api.comment.CommentContent;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.space.publicroom.persistence.PublicRoom;
import yeobaek.backend.space.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.web.v1.CommentService;
import yeobaek.backend.web.v1.PublicRoomService;
import yeobaek.backend.support.IntegrationTest;
import yeobaek.backend.space.publicroom.persistence.PublicRoomVisitRepository;
import yeobaek.backend.reading.persistence.ReadingProgressRepository;
import yeobaek.backend.reading.persistence.ReadingProgressJpaEntity;

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
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private PublicRoomVisitRepository visitRepository;

    @Autowired
    private PublicRoomVisitApi visitApi;

    @Autowired
    private ReadingProgressRepository readingProgressRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private AppreciationContextRepository appreciationContextRepository;

    @Autowired
    private SpaceContentBindingApi bindingService;

    @Autowired
    private CommentReportRepository commentReportRepository;

    @Autowired
    private PublicRoomService publicRoomService;

    @Autowired
    private CommentService commentService;

    @Test
    @DisplayName("같은 공개방 댓글의 동시 반복 신고는 신고 한 건만 저장한다")
    void concurrentDuplicateReports() throws Exception {
        ReadingFixture fixture = createRoom("동시 신고 책", 1);
        Member writer = memberRepository.save(new Member(new Nickname("신고 대상")));
        Member reporter = memberRepository.save(new Member(new Nickname("신고자")));
        Comment comment = saveComment(newPublicRoomComment(fixture.room(), writer,
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
    @DisplayName("도서만 저장해도 시작 시 공개방을 암묵적으로 생성하지 않는다")
    void doesNotBackfillExistingBooksAtStartup() {
        Book first = bookRepository.save(newBook(new BookTitle("기존 책 1"), null, null, 1, null));
        Book second = bookRepository.save(newBook(new BookTitle("기존 책 2"), null, null, 1, null));

        assertThat(bookRepository.findById(first.getId())).isPresent();
        assertThat(bookRepository.findById(second.getId())).isPresent();
        assertThat(publicRoomRepository.count()).isZero();
    }

    @Test
    @DisplayName("공개방 방문 조회는 최근 방문 순서와 공간별 방문자 수를 제공한다")
    void queryVisits() {
        MemberId firstActor = new MemberId(101L);
        MemberId secondActor = new MemberId(102L);
        SpaceId firstRoom = new SpaceId(201L);
        SpaceId secondRoom = new SpaceId(202L);
        visitApi.visit(firstActor, firstRoom, LocalDateTime.of(2026, 1, 1, 10, 0));
        visitApi.visit(firstActor, secondRoom, LocalDateTime.of(2026, 1, 2, 10, 0));
        visitApi.visit(secondActor, secondRoom, LocalDateTime.of(2026, 1, 3, 10, 0));

        assertThat(visitApi.findVisits(firstActor))
                .extracting(visit -> visit.spaceId())
                .containsExactly(secondRoom, firstRoom);
        assertThat(visitApi.findLastVisit(firstActor, firstRoom)).get()
                .extracting(visit -> visit.lastVisitedAt())
                .isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));
        assertThat(visitApi.countVisitors(List.of(firstRoom, secondRoom)))
                .containsEntry(firstRoom, 1L)
                .containsEntry(secondRoom, 2L);
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

        var visit = visitRepository.findOne(member.getId(), fixture.room().getSpaceId()).orElseThrow();
        var savedProgress = readingProgressRepository.findOne(member.getId(), fixture.room().getSpaceId(),
                fixture.book().getContentId()).orElseThrow();
        assertThat(visit.getLastVisitedAt()).isNotNull();
        assertThat(savedProgress.getLocationId()).isNotNull();
        assertThat(visitRepository.count()).isOne();
        assertThat(readingProgressRepository.count()).isOne();
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
        assertThat(visitRepository.count()).isEqualTo(3);

        Passage lastPassage = first.passages().getLast();
        mockMvc.perform(put("/api/public-rooms/{publicRoomId}/progress", first.room().getId())
                        .header("X-Member-Id", member.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passageId\":" + lastPassage.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastReadPassageSequence").value(2))
                .andExpect(jsonPath("$.progressRate").value(100));

        mockMvc.perform(put("/api/public-rooms/{publicRoomId}/progress", first.room().getId())
                        .header("X-Member-Id", member.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passageId\":" + first.passages().getFirst().getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastReadPassageSequence").value(1))
                .andExpect(jsonPath("$.progressRate").value(50));

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
                .andExpect(jsonPath("$.myProgress.lastReadPassageSequence").value(1));
        mockMvc.perform(get("/api/members/me/recent-reading")
                        .header("X-Member-Id", member.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.book.status").value("DELETED"));
        mockMvc.perform(get("/api/public-rooms/{publicRoomId}/passages", first.room().getId())
                        .header("X-Member-Id", member.getId())
                        .queryParam("from", "1").queryParam("to", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONTENT_UNAVAILABLE"));
        mockMvc.perform(get("/api/public-rooms").header("X-Member-Id", member.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicRooms.length()").value(1));
    }

    @Test
    @DisplayName("최근 독서는 JOINED 모임과 공개방 진도만 경쟁하고 삭제 도서의 최신 진도를 보존한다")
    void recentReadingUsesJoinedClubAndPublicProgressOnly() throws Exception {
        Member member = memberRepository.save(new Member(new Nickname("혼합 독자")));
        ClubReadingFixture joined = createClub("참여 모임", "JOIN01", member, false);
        ClubReadingFixture left = createClub("탈퇴 모임", "LEFT02", member, true);
        ReadingFixture progressedRoom = createRoom("진도 공개방", 1);
        ReadingFixture visitOnlyRoom = createRoom("방문 공개방", 1);

        saveProgress(member, joined.club().getSpaceId(), joined.book(), joined.passage(),
                LocalDateTime.of(2026, 10, 2, 10, 0));
        saveProgress(member, progressedRoom.room().getSpaceId(), progressedRoom.book(),
                progressedRoom.passages().getFirst(), LocalDateTime.of(2026, 10, 2, 11, 0));
        saveProgress(member, left.club().getSpaceId(), left.book(), left.passage(),
                LocalDateTime.of(2026, 10, 2, 13, 0));
        visit(visitOnlyRoom.room(), member);

        mockMvc.perform(get("/api/members/me/recent-reading").header("X-Member-Id", member.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.space.type").value("PUBLIC_ROOM"))
                .andExpect(jsonPath("$.space.publicRoomId").value(progressedRoom.room().getId()));

        LocalDateTime tiedAt = LocalDateTime.of(2026, 10, 2, 12, 0);
        saveProgress(member, joined.club().getSpaceId(), joined.book(), joined.passage(), tiedAt);
        saveProgress(member, progressedRoom.room().getSpaceId(), progressedRoom.book(),
                progressedRoom.passages().getFirst(), tiedAt);
        String tiedResponse = mockMvc.perform(get("/api/members/me/recent-reading")
                        .header("X-Member-Id", member.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(tiedResponse).satisfiesAnyOf(
                response -> assertThat(response).contains("\"bookId\":" + joined.book().getId()),
                response -> assertThat(response).contains("\"bookId\":" + progressedRoom.book().getId()));

        saveProgress(member, joined.club().getSpaceId(), joined.book(), joined.passage(),
                LocalDateTime.of(2026, 10, 2, 14, 0));
        bookRepository.delete(joined.book().getId());

        mockMvc.perform(get("/api/members/me/recent-reading").header("X-Member-Id", member.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.space.type").value("CLUB"))
                .andExpect(jsonPath("$.space.clubId").value(joined.club().getId()))
                .andExpect(jsonPath("$.book.status").value("DELETED"));
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
        Book book = bookRepository.save(newBook(new BookTitle("댓글 정렬 책"), null, null, 2, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage firstPassage = passageRepository.save(newPassage(chapter, 1,
                List.of(new SentenceContent("앞 문단 첫 문장"), new SentenceContent("앞 문단 둘째 문장"))));
        Passage secondPassage = passageRepository.save(newPassage(chapter, 2,
                List.of(new SentenceContent("뒤 문단 첫 문장"))));
        PublicRoom room = publicRoomRepository.save(newPublicRoom());
        bindingService.bind(new SpaceId(room.getSpaceId()), new ContentId(book.getContentId()));
        Member writer = memberRepository.save(new Member(new Nickname("정렬 작성자")));
        Member reader = memberRepository.save(new Member(new Nickname("정렬 독자")));
        saveComment(newPublicRoomComment(room, writer,
                firstPassage.getSentences().getFirst(), new CommentContent("가장 먼저 작성된 앞 문단 첫 문장 댓글")));
        saveComment(newPublicRoomComment(room, writer,
                firstPassage.getSentences().getLast(), new CommentContent("나중에 작성된 앞 문단 둘째 문장 댓글")));
        saveComment(newPublicRoomComment(room, writer,
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

        assertThat(visitRepository.findAll()).hasSize(1)
                .allMatch(visit -> visit.getActorId().equals(remaining.getId()));
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
        Book book = bookRepository.save(newBook(new BookTitle(title), null, null, passageCount, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        List<Passage> passages = java.util.stream.IntStream.rangeClosed(1, passageCount)
                .mapToObj(sequence -> passageRepository.save(newPassage(chapter, sequence,
                        List.of(new SentenceContent(sequence + "번 문장")))))
                .toList();
        PublicRoom room = publicRoomRepository.save(newPublicRoom());
        bindingService.bind(new SpaceId(room.getSpaceId()), new ContentId(book.getContentId()));
        return new ReadingFixture(book, room, passages);
    }

    private ClubReadingFixture createClub(String name, String joinCode, Member member, boolean left) {
        Book book = bookRepository.save(newBook(new BookTitle(name + " 책"), null, null, 1, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage passage = passageRepository.save(newPassage(chapter, 1, List.of(new SentenceContent("문장"))));
        Club club = clubRepository.save(newClub(new ClubName(name), new JoinCode(joinCode)));
        bindingService.bind(new SpaceId(club.getSpaceId()), new ContentId(book.getContentId()));
        ClubMember membership = new ClubMember(new yeobaek.backend.shared.identity.MemberId(member.getId()), club);
        if (left) {
            membership.leave();
        }
        clubMemberRepository.save(membership);
        return new ClubReadingFixture(book, club, passage);
    }

    private void saveProgress(Member member, Long spaceId, Book book,
                              Passage passage, LocalDateTime readAt) {
        ReadingProgressJpaEntity progress = readingProgressRepository.findOne(
                        member.getId(), spaceId, book.getContentId())
                .orElseGet(() -> new ReadingProgressJpaEntity(member.getId(), spaceId,
                        book.getContentId(), passage.getLocationId(), readAt));
        progress.update(passage.getLocationId(), readAt);
        readingProgressRepository.save(progress);
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private Comment saveComment(Comment comment) {
        Comment saved = commentRepository.save(comment);
        appreciationContextRepository.save(CommentFixtures.contextOf(saved));
        return saved;
    }

    private record ReadingFixture(Book book, PublicRoom room, List<Passage> passages) {
    }

    private record ClubReadingFixture(Book book, Club club, Passage passage) {
    }
}
