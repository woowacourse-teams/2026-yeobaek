package yeobaek.backend.publicroom.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
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
import yeobaek.backend.publicroom.service.PublicRoomService;
import yeobaek.backend.support.IntegrationTest;

class PublicRoomMemberRepositoryTest extends IntegrationTest {

    @Autowired
    private PublicRoomMemberRepository publicRoomMemberRepository;

    @Autowired
    private PublicRoomService publicRoomService;

    @Autowired
    private PublicRoomRepository publicRoomRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookManagementRepository bookRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private PassageRepository passageRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("조회 대상 공개방들의 진도 정보를 한 번의 SQL로 읽는다")
    void findAllMembershipsFetchesProgressInOneQuery() {
        ReadingFixture firstProgressed = createRoom("첫 번째 진도 책", 1);
        ReadingFixture secondProgressed = createRoom("두 번째 진도 책", 2);
        ReadingFixture visitedOnly = createRoom("진도 없는 책", 1);
        Member member = memberRepository.save(new Member(new Nickname("목록 독자")));
        publicRoomService.updateProgress(member.getId(), firstProgressed.room().getId(),
                firstProgressed.passages().getFirst().getId());
        publicRoomService.updateProgress(member.getId(), secondProgressed.room().getId(),
                secondProgressed.passages().getLast().getId());
        publicRoomService.visit(member.getId(), visitedOnly.room().getId());

        long sqlCount = countSqlStatements(() -> {
            List<Integer> progressSequences = publicRoomMemberRepository
                    .findAllByMemberIdAndPublicRoomIdIn(member.getId(), List.of(
                            firstProgressed.room().getId(), secondProgressed.room().getId(),
                            visitedOnly.room().getId()))
                    .stream()
                    .map(membership -> membership.getLastReadAt() == null
                            ? null : membership.getLastReadPassage().getSequence().value())
                    .toList();

            assertThat(progressSequences).containsExactlyInAnyOrder(1, 2, null);
        });

        assertThat(sqlCount).isOne();
    }

    private long countSqlStatements(Runnable action) {
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean statisticsEnabled = statistics.isStatisticsEnabled();
        try {
            statistics.setStatisticsEnabled(true);
            long preparedStatementsBefore = statistics.getPrepareStatementCount();

            transactionTemplate.executeWithoutResult(status -> action.run());

            return statistics.getPrepareStatementCount() - preparedStatementsBefore;
        } finally {
            statistics.setStatisticsEnabled(statisticsEnabled);
        }
    }

    private ReadingFixture createRoom(String title, int passageCount) {
        Book book = bookRepository.save(new Book(new BookTitle(title), null, null, passageCount, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        List<Passage> passages = java.util.stream.IntStream.rangeClosed(1, passageCount)
                .mapToObj(sequence -> passageRepository.save(new Passage(chapter, sequence,
                        List.of(new SentenceContent(sequence + "번 문장")))))
                .toList();
        PublicRoom room = publicRoomRepository.save(new PublicRoom(book));
        return new ReadingFixture(room, passages);
    }

    private record ReadingFixture(PublicRoom room, List<Passage> passages) {
    }
}
