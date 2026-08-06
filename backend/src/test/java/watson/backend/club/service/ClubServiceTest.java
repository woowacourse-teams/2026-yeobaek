package watson.backend.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import watson.backend.book.domain.Author;
import watson.backend.book.domain.AuthorBook;
import watson.backend.book.domain.Book;
import watson.backend.book.domain.Chapter;
import watson.backend.book.domain.Passage;
import watson.backend.book.repository.AuthorBookRepository;
import watson.backend.book.repository.AuthorRepository;
import watson.backend.book.repository.BookRepository;
import watson.backend.book.repository.ChapterRepository;
import watson.backend.book.repository.PassageRepository;
import watson.backend.club.domain.ClubMember;
import watson.backend.club.domain.JoinCodeGenerator;
import watson.backend.club.dto.ClubCreateResponse;
import watson.backend.club.dto.ClubJoinResponse;
import watson.backend.club.dto.MyClubResponse;
import watson.backend.club.dto.MyClubsResponse;
import watson.backend.club.repository.ClubMemberRepository;
import watson.backend.club.repository.ClubRepository;
import watson.backend.member.domain.Member;
import watson.backend.member.repository.MemberRepository;
import watson.backend.support.NotFoundException;
import watson.backend.support.RepositoryTest;

@Import({ClubService.class, JoinCodeGenerator.class})
class ClubServiceTest extends RepositoryTest {

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private AuthorBookRepository authorBookRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private PassageRepository passageRepository;

    private Member creator;
    private Book book;

    @BeforeEach
    void setUp() {
        creator = memberRepository.save(new Member("민서"));
        book = bookRepository.save(new Book("운수 좋은 날", "자체 제작", 1924, 312));
        Author author = authorRepository.save(new Author("현진건"));
        authorBookRepository.save(new AuthorBook(author, book));
    }

    @Test
    @DisplayName("모임을 생성하면 참여 코드가 발급되고 생성자가 자동으로 참여한다")
    void createClub() {
        ClubCreateResponse response = clubService.create(creator.getId(), "교환독서 1기", book.getId());

        assertThat(response.joinCode()).matches("^[A-Z0-9]{6}$");
        assertThat(response.book().authors()).containsExactly("현진건");
        assertThat(clubRepository.findById(response.clubId())).isPresent();
        assertThat(clubMemberRepository.existsByMemberIdAndClubId(creator.getId(), response.clubId())).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 도서로는 모임을 생성할 수 없다")
    void rejectUnknownBook() {
        assertThatThrownBy(() -> clubService.create(creator.getId(), "교환독서 1기", 999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("참여 코드로 모임에 참여한다")
    void joinByCode() {
        ClubCreateResponse created = clubService.create(creator.getId(), "교환독서 1기", book.getId());
        Member joiner = memberRepository.save(new Member("지수"));

        ClubJoinResponse response = clubService.join(joiner.getId(), created.joinCode());

        assertThat(response.clubId()).isEqualTo(created.clubId());
        assertThat(clubMemberRepository.existsByMemberIdAndClubId(joiner.getId(), created.clubId())).isTrue();
    }

    @Test
    @DisplayName("이미 참여한 모임에 다시 참여해도 같은 응답을 반환한다 (멱등)")
    void joinIsIdempotent() {
        ClubCreateResponse created = clubService.create(creator.getId(), "교환독서 1기", book.getId());

        ClubJoinResponse response = clubService.join(creator.getId(), created.joinCode());

        assertThat(response.clubId()).isEqualTo(created.clubId());
        assertThat(clubMemberRepository.countByClubIds(List.of(created.clubId())).getFirst().getMemberCount())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 참여 코드는 거부된다")
    void rejectUnknownJoinCode() {
        assertThatThrownBy(() -> clubService.join(creator.getId(), "NOCODE"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("내 모임 목록에 회원 수와 진도가 함께 조회된다")
    void findMyClubsWithProgress() {
        Book smallBook = bookRepository.save(new Book("작은 책", null, null, 3));
        Chapter chapter = chapterRepository.save(new Chapter(smallBook, "1장", 1));
        Passage second = passageRepository.save(new Passage(chapter, 2, "본문 2", null));
        ClubCreateResponse first = clubService.create(creator.getId(), "1기", book.getId());
        ClubCreateResponse secondClub = clubService.create(creator.getId(), "2기", smallBook.getId());
        Member joiner = memberRepository.save(new Member("지수"));
        clubService.join(joiner.getId(), first.joinCode());
        ClubMember myMembership = clubMemberRepository.findAllWithClubAndBookByMemberId(creator.getId()).stream()
                .filter(clubMember -> clubMember.getClub().getId().equals(secondClub.clubId()))
                .findFirst().orElseThrow();
        myMembership.updateProgress(second, LocalDateTime.of(2026, 8, 5, 14, 30));
        clubMemberRepository.saveAndFlush(myMembership);

        MyClubsResponse response = clubService.findMyClubs(creator.getId());

        assertThat(response.clubs()).hasSize(2);
        MyClubResponse firstClub = response.clubs().stream()
                .filter(club -> club.clubId().equals(first.clubId())).findFirst().orElseThrow();
        MyClubResponse progressClub = response.clubs().stream()
                .filter(club -> club.clubId().equals(secondClub.clubId())).findFirst().orElseThrow();
        assertThat(firstClub.memberCount()).isEqualTo(2);
        assertThat(firstClub.myProgress()).isNull();
        assertThat(progressClub.myProgress().lastReadPassageSequence()).isEqualTo(2);
        assertThat(progressClub.myProgress().progressRate()).isEqualTo(67);
    }
}
