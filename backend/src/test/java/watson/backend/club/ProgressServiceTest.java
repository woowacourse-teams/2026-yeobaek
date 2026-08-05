package watson.backend.club;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import watson.backend.book.Book;
import watson.backend.book.BookRepository;
import watson.backend.book.Chapter;
import watson.backend.book.ChapterRepository;
import watson.backend.book.Passage;
import watson.backend.book.PassageRepository;
import watson.backend.member.Member;
import watson.backend.member.MemberRepository;
import watson.backend.support.ForbiddenException;
import watson.backend.support.NotFoundException;
import watson.backend.support.RepositoryTest;

@Import(ProgressService.class)
class ProgressServiceTest extends RepositoryTest {

    @Autowired
    private ProgressService progressService;

    @Autowired
    private BookRepository bookRepository;

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

    private Member reader;
    private Club club;
    private Passage second;
    private Passage fourth;

    @BeforeEach
    void setUp() {
        Book book = bookRepository.save(new Book("운수 좋은 날", null, 1924, 4));
        Chapter chapter = chapterRepository.save(new Chapter(book, "1장", 1));
        passageRepository.save(new Passage(chapter, 1, "본문 1", null));
        second = passageRepository.save(new Passage(chapter, 2, "본문 2", null));
        passageRepository.save(new Passage(chapter, 3, "본문 3", null));
        fourth = passageRepository.save(new Passage(chapter, 4, "본문 4", null));
        reader = memberRepository.save(new Member("민서"));
        club = clubRepository.save(new Club("1기", book, "CODE01"));
        clubMemberRepository.save(new ClubMember(reader, club));
    }

    @Test
    @DisplayName("진도를 보고하면 최근 열람 본문과 진도율이 갱신된다")
    void updateProgress() {
        ProgressResponse response = progressService.updateProgress(reader.getId(), club.getId(), fourth.getId());

        assertThat(response.lastReadPassageSequence()).isEqualTo(4);
        assertThat(response.progressRate()).isEqualTo(100);
        assertThat(response.lastReadAt()).isNotNull();
    }

    @Test
    @DisplayName("앞부분을 다시 읽으면 진도율이 후퇴한다 (PRD 3.4 트레이드오프)")
    void progressCanGoBackward() {
        progressService.updateProgress(reader.getId(), club.getId(), fourth.getId());

        ProgressResponse response = progressService.updateProgress(reader.getId(), club.getId(), second.getId());

        assertThat(response.lastReadPassageSequence()).isEqualTo(2);
        assertThat(response.progressRate()).isEqualTo(50);
    }

    @Test
    @DisplayName("모임 미소속 회원의 진도 보고는 거부된다")
    void rejectOutsider() {
        Member outsider = memberRepository.save(new Member("외부인"));

        assertThatThrownBy(() -> progressService.updateProgress(outsider.getId(), club.getId(), second.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("존재하지 않는 모임·본문의 진도 보고는 실패한다")
    void rejectUnknownTargets() {
        assertThatThrownBy(() -> progressService.updateProgress(reader.getId(), 999L, second.getId()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> progressService.updateProgress(reader.getId(), club.getId(), 999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("모임의 도서에 속하지 않는 본문으로는 진도를 보고할 수 없다")
    void rejectPassageOfOtherBook() {
        Book otherBook = bookRepository.save(new Book("다른 책", null, null, 1));
        Chapter otherChapter = chapterRepository.save(new Chapter(otherBook, "1장", 1));
        Passage otherPassage = passageRepository.save(new Passage(otherChapter, 1, "다른 본문", null));

        assertThatThrownBy(() -> progressService.updateProgress(reader.getId(), club.getId(), otherPassage.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("읽기 기록이 없으면 마지막 읽던 책이 비어 있다")
    void lastReadingEmptyWithoutHistory() {
        assertThat(progressService.findLastReading(reader.getId())).isEmpty();
    }

    @Test
    @DisplayName("여러 모임 중 가장 최근에 읽은 모임이 마지막 읽던 책으로 조회된다")
    void lastReadingPicksMostRecent() {
        Book otherBook = bookRepository.save(new Book("다른 책", null, null, 2));
        Chapter otherChapter = chapterRepository.save(new Chapter(otherBook, "1장", 1));
        Passage otherPassage = passageRepository.save(new Passage(otherChapter, 1, "다른 본문", null));
        Club otherClub = clubRepository.save(new Club("2기", otherBook, "CODE02"));
        clubMemberRepository.save(new ClubMember(reader, otherClub));

        progressService.updateProgress(reader.getId(), club.getId(), second.getId());
        progressService.updateProgress(reader.getId(), otherClub.getId(), otherPassage.getId());

        Optional<LastReadingResponse> response = progressService.findLastReading(reader.getId());

        assertThat(response).isPresent();
        assertThat(response.get().clubId()).isEqualTo(otherClub.getId());
        assertThat(response.get().book().title()).isEqualTo("다른 책");
        assertThat(response.get().lastReadPassageSequence()).isEqualTo(1);
        assertThat(response.get().progressRate()).isEqualTo(50);
    }
}
