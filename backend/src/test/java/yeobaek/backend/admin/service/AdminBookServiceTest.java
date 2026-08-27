package yeobaek.backend.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.book.domain.Author;
import yeobaek.backend.book.domain.AuthorBook;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.BookStatus;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.repository.AuthorRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.domain.Comment;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.IntegrationTest;
import yeobaek.backend.support.NotFoundException;

class AdminBookServiceTest extends IntegrationTest {

    private static final String COVER_KEY = "yeobaek/book-covers/123e4567-e89b-12d3-a456-426614174000.jpg";

    @Autowired
    private AdminBookService adminBookService;

    @Autowired
    private BookManagementRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private AuthorBookRepository authorBookRepository;

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

    @Autowired
    private CommentRepository commentRepository;

    @Test
    @DisplayName("도서를 삭제해도 기존 모임과 독서 활동 기록의 연결은 보존된다")
    void preservesExistingClubAndActivityRecords() {
        Book book = bookRepository.save(new Book("운수 좋은 날", null, 1924, 1));
        Author author = authorRepository.save(new Author("현진건"));
        AuthorBook authorBook = authorBookRepository.save(new AuthorBook(author, book));
        Chapter chapter = chapterRepository.save(new Chapter(book, "1장", 1));
        Passage passage = passageRepository.save(new Passage(chapter, 1, "본문"));
        Member member = memberRepository.save(new Member("민서"));
        Club club = clubRepository.save(new Club("1기", book, "CODE01"));
        ClubMember membership = clubMemberRepository.save(new ClubMember(member, club));
        Comment comment = commentRepository.save(new Comment(membership, passage, "댓글"));

        adminBookService.delete(book.getId());

        assertThat(bookRepository.findById(book.getId()).orElseThrow().getStatus()).isEqualTo(BookStatus.DELETED);
        assertThat(clubRepository.findById(club.getId())).get()
                .extracting(found -> found.getBook().getId()).isEqualTo(book.getId());
        assertThat(passageRepository.findById(passage.getId())).isPresent();
        assertThat(commentRepository.findById(comment.getId())).isPresent();
        assertThat(authorBookRepository.findById(authorBook.getId())).isPresent();
    }

    @Test
    @DisplayName("존재하지 않는 도서는 삭제할 수 없다")
    void cannotDeleteMissingBook() {
        assertThatThrownBy(() -> adminBookService.delete(999L))
                .isInstanceOf(NotFoundException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    @DisplayName("삭제된 도서는 다시 삭제할 수 없다")
    void cannotDeleteBookTwice() {
        Book book = bookRepository.save(new Book("제목", null, null, 1));
        adminBookService.delete(book.getId());

        assertThatThrownBy(() -> adminBookService.delete(book.getId()))
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("동시에 같은 도서를 삭제하면 한 요청만 성공한다")
    void allowOnlyOneConcurrentDeletion() throws Exception {
        Book book = bookRepository.save(new Book("동시 삭제", null, null, 1));
        CyclicBarrier start = new CyclicBarrier(2);
        Callable<String> deletion = () -> {
            start.await();
            try {
                adminBookService.delete(book.getId());
                return "SUCCESS";
            } catch (BadRequestException exception) {
                return exception.getCode().name();
            }
        };

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<String> first = executor.submit(deletion);
            Future<String> second = executor.submit(deletion);

            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("SUCCESS", "BOOK_NOT_AVAILABLE");
        }
    }

    @Test
    @DisplayName("활성 도서의 표지를 교체하고 제거한다")
    void replaceAndRemoveCoverImage() {
        Book book = bookRepository.save(new Book("표지 도서", null, null, 1));

        adminBookService.replaceCoverImage(book.getId(), COVER_KEY);
        assertThat(bookRepository.getById(book.getId()).getCoverImageKey()).isEqualTo(COVER_KEY);

        adminBookService.removeCoverImage(book.getId());
        assertThat(bookRepository.getById(book.getId()).getCoverImageKey()).isNull();
    }

    @Test
    @DisplayName("삭제된 도서의 표지는 교체할 수 없다")
    void cannotReplaceCoverOfDeletedBook() {
        Book book = bookRepository.save(new Book("삭제 표지 도서", null, null, 1));
        adminBookService.delete(book.getId());

        assertThatThrownBy(() -> adminBookService.replaceCoverImage(book.getId(), COVER_KEY))
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE);
    }
}
