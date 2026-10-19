package yeobaek.backend.publicroom.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.publicroom.domain.PublicRoom;
import yeobaek.backend.publicroom.dto.PublicRoomSort;
import yeobaek.backend.publicroom.repository.PublicRoomMemberRepository;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.support.IntegrationTest;

class PublicRoomServiceTest extends IntegrationTest {

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
    private PublicRoomService publicRoomService;

    @Autowired
    private PublicRoomMemberRepository publicRoomMemberRepository;

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

        var membership = publicRoomMemberRepository.findByMemberIdAndPublicRoomId(
                member.getId(), fixture.room().getId()).orElseThrow();
        assertThat(membership.getLastVisitedAt()).isNotNull();
        assertThat(membership.getLastReadPassage()).isNotNull();
        assertThat(publicRoomMemberRepository.count()).isOne();
    }

    @Test
    @DisplayName("공개방 목록은 중복 방문을 제외한 누적 방문 회원 수 내림차순으로 반환한다")
    void findAllByDistinctVisitorCount() {
        ReadingFixture first = createRoom("첫 번째 책", 2);
        ReadingFixture second = createRoom("두 번째 책", 1);
        Member member = memberRepository.save(new Member(new Nickname("독자")));
        Member other = memberRepository.save(new Member(new Nickname("다른 독자")));
        publicRoomService.visit(member.getId(), first.room().getId());
        publicRoomService.visit(member.getId(), first.room().getId());
        publicRoomService.visit(other.getId(), second.room().getId());
        publicRoomService.visit(member.getId(), second.room().getId());

        var response = publicRoomService.findAll(member.getId(), PublicRoomSort.MOST_VISITED);

        assertThat(response.publicRooms()).extracting(room -> room.publicRoomId())
                .containsExactly(second.room().getId(), first.room().getId());
        assertThat(response.publicRooms()).allSatisfy(room -> assertThat(room.myProgress()).isNull());
        assertThat(publicRoomMemberRepository.count()).isEqualTo(3);
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
