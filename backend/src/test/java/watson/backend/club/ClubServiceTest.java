package watson.backend.club;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import watson.backend.book.Author;
import watson.backend.book.AuthorBook;
import watson.backend.book.AuthorBookRepository;
import watson.backend.book.AuthorRepository;
import watson.backend.book.Book;
import watson.backend.book.BookRepository;
import watson.backend.member.Member;
import watson.backend.member.MemberRepository;
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
}
