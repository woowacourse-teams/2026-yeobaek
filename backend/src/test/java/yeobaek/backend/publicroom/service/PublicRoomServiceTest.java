package yeobaek.backend.publicroom.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.book.domain.Author;
import yeobaek.backend.book.domain.AuthorBook;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.AuthorName;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.repository.AuthorRepository;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.publicroom.domain.PublicRoom;
import yeobaek.backend.publicroom.dto.PublicRoomSort;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.support.IntegrationTest;

class PublicRoomServiceTest extends IntegrationTest {

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private AuthorBookRepository authorBookRepository;

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

    @ParameterizedTest
    @ValueSource(strings = {"운수", "현진"})
    @DisplayName("제목 또는 여러 작가의 이름에 부분 일치하는 공개방을 중복 없이 검색한다")
    void searchByTitleOrAuthor(String keyword) {
        ReadingFixture matched = createRoom("운수 좋은 날", 1);
        addAuthor(matched.book(), "현진건");
        addAuthor(matched.book(), "현진영");
        addAuthor(matched.book(), "다른 작가");
        createRoom("무관한 책", 1);
        ReadingFixture deleted = createRoom("운수 삭제 도서", 1);
        addAuthor(deleted.book(), "현진수");
        bookRepository.delete(deleted.book().getId());
        Member reader = memberRepository.save(new Member(new Nickname("검색 독자")));

        var response = publicRoomService.findAll(reader.getId(), PublicRoomSort.MOST_VISITED, keyword);

        assertThat(response.publicRooms()).singleElement().satisfies(room -> {
            assertThat(room.publicRoomId()).isEqualTo(matched.room().getId());
            assertThat(room.book().authors()).containsExactlyInAnyOrder("현진건", "현진영", "다른 작가");
            assertThat(room.myProgress()).isNull();
        });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("검색어가 없거나 비어 있으면 삭제 도서를 제외한 전체 공개방을 조회한다")
    void searchWithoutKeyword(String keyword) {
        ReadingFixture first = createRoom("첫 번째 책", 1);
        ReadingFixture second = createRoom("두 번째 책", 1);
        ReadingFixture deleted = createRoom("삭제 도서", 1);
        bookRepository.delete(deleted.book().getId());
        Member reader = memberRepository.save(new Member(new Nickname("목록 독자")));
        visit(second.room(), reader);
        var response = publicRoomService.findAll(reader.getId(), PublicRoomSort.MOST_VISITED, keyword);

        assertThat(response.publicRooms()).extracting(room -> room.publicRoomId())
                .containsExactly(second.room().getId(), first.room().getId());
    }

    @Test
    @DisplayName("제목·작가 검색 결과에도 누적 방문순 정렬과 내 진도를 유지한다")
    void searchKeepsSortAndProgress() {
        ReadingFixture titleMatch = createRoom("검색 제목", 1);
        ReadingFixture authorMatch = createRoom("다른 제목", 2);
        addAuthor(authorMatch.book(), "검색 작가");
        ReadingFixture unrelated = createRoom("무관한 책", 1);
        Member reader = memberRepository.save(new Member(new Nickname("정렬 검색 독자")));
        Member other = memberRepository.save(new Member(new Nickname("다른 독자")));
        visit(titleMatch.room(), reader);
        visit(authorMatch.room(), reader);
        visit(authorMatch.room(), other);
        visit(authorMatch.room(), other);
        visit(unrelated.room(), reader);
        publicRoomService.updateProgress(reader.getId(), authorMatch.room().getId(),
                authorMatch.passages().getFirst().getId());

        var response = publicRoomService.findAll(reader.getId(), PublicRoomSort.MOST_VISITED, "검색");

        assertThat(response.publicRooms()).extracting(room -> room.publicRoomId())
                .containsExactly(authorMatch.room().getId(), titleMatch.room().getId());
        assertThat(response.publicRooms().getFirst().myProgress()).satisfies(progress -> {
            assertThat(progress.lastReadPassageSequence()).isEqualTo(1);
            assertThat(progress.progressRate()).isEqualTo(50);
            assertThat(progress.lastReadAt()).isNotNull();
        });
    }

    @Test
    @DisplayName("일치하는 공개방이 없으면 빈 배열을 반환한다")
    void searchWithoutMatches() {
        createRoom("다른 책", 1);
        Member reader = memberRepository.save(new Member(new Nickname("빈 검색 독자")));

        var response = publicRoomService.findAll(reader.getId(), PublicRoomSort.MOST_VISITED, "없는 검색어");

        assertThat(response.publicRooms()).isEmpty();
    }

    private void addAuthor(Book book, String name) {
        Author author = authorRepository.save(new Author(new AuthorName(name)));
        authorBookRepository.save(new AuthorBook(author, book));
    }

    private void visit(PublicRoom room, Member member) {
        publicRoomService.visit(member.getId(), room.getId());
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

    private record ReadingFixture(Book book, PublicRoom room, List<Passage> passages) {
    }
}
