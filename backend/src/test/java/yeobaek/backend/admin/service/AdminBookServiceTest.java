package yeobaek.backend.admin.service;

import yeobaek.backend.support.CommentFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Collections;
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
import yeobaek.backend.admin.dto.AdminBookAuthorResponse;
import yeobaek.backend.admin.dto.AdminBookResponse;
import yeobaek.backend.admin.dto.AdminBooksResponse;
import yeobaek.backend.content.book.domain.Author;
import yeobaek.backend.content.book.domain.AuthorBook;
import yeobaek.backend.content.book.persistence.Book;
import yeobaek.backend.content.book.domain.BookStatus;
import yeobaek.backend.content.book.domain.Chapter;
import yeobaek.backend.content.book.domain.Passage;
import yeobaek.backend.content.api.value.AuthorName;
import yeobaek.backend.content.api.value.BookTitle;
import yeobaek.backend.content.api.value.ChapterTitle;
import yeobaek.backend.content.api.value.Isni;
import yeobaek.backend.content.api.value.Publisher;
import yeobaek.backend.content.api.value.SentenceContent;
import yeobaek.backend.content.book.repository.AuthorBookRepository;
import yeobaek.backend.content.book.repository.AuthorRepository;
import yeobaek.backend.content.book.repository.BookManagementRepository;
import yeobaek.backend.content.book.repository.ChapterRepository;
import yeobaek.backend.content.book.repository.PassageRepository;
import yeobaek.backend.space.club.persistence.Club;
import yeobaek.backend.space.club.domain.ClubMember;
import yeobaek.backend.space.api.club.ClubName;
import yeobaek.backend.space.api.club.JoinCode;
import yeobaek.backend.space.club.repository.ClubMemberRepository;
import yeobaek.backend.space.club.repository.ClubRepository;
import yeobaek.backend.appreciation.comment.persistence.Comment;
import yeobaek.backend.appreciation.api.comment.CommentContent;
import yeobaek.backend.appreciation.comment.repository.CommentRepository;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.collaboration.persistence.AppreciationContextRepository;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.shared.exception.BadRequestException;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.support.IntegrationTest;
import yeobaek.backend.shared.exception.NotFoundException;

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

    @Autowired
    private AppreciationContextRepository appreciationContextRepository;

    @Autowired
    private SpaceContentBindingApi bindingApi;

    @Test
    @DisplayName("삭제 상태와 공동 작가 및 nullable 정보를 포함해 모든 도서를 ID 순으로 조회한다")
    void findBooks() {
        Book first = bookRepository.save(newBook(new BookTitle("표지 없는 책"), null, null, 1, null));
        Book second = bookRepository.save(newBook(new BookTitle("함께 쓴 책"), new Publisher("여백 출판"),
                2026,
                42,
                COVER_KEY));
        Author firstAuthor = authorRepository.save(new Author(new AuthorName("첫 작가"), new Isni("000000012345964X")));
        Author secondAuthor = authorRepository.save(new Author(new AuthorName("둘째 작가")));
        authorBookRepository.save(new AuthorBook(firstAuthor, second));
        authorBookRepository.save(new AuthorBook(secondAuthor, second));
        bookRepository.delete(second.getId());

        AdminBooksResponse response = adminBookService.findBooks();

        assertThat(response.books())
                .extracting(
                        AdminBookResponse::bookId,
                        AdminBookResponse::title,
                        AdminBookResponse::publisher,
                        AdminBookResponse::publishedYear,
                        AdminBookResponse::passageCount,
                        AdminBookResponse::coverImageUrl,
                        AdminBookResponse::status)
                .containsExactly(
                        tuple(first.getId(), "표지 없는 책", null, null, 1, null, "ACTIVE"),
                        tuple(second.getId(), "함께 쓴 책", "여백 출판", 2026, 42,
                                "https://yeobaek-local-book-covers.s3.ap-northeast-2.amazonaws.com/" + COVER_KEY,
                                "DELETED"));
        assertThat(response.books().getFirst().authors()).isEmpty();
        AdminBookResponse secondResponse = response.books().getLast();
        assertThat(secondResponse.authors())
                .extracting(AdminBookAuthorResponse::authorId, AdminBookAuthorResponse::name,
                        AdminBookAuthorResponse::isni)
                .containsExactlyInAnyOrder(
                        tuple(firstAuthor.getId(), "첫 작가", "000000012345964X"),
                        tuple(secondAuthor.getId(), "둘째 작가", null));
    }

    @Test
    @DisplayName("도서가 없으면 빈 목록을 반환한다")
    void findBooksWhenEmpty() {
        assertThat(adminBookService.findBooks().books()).isEmpty();
    }

    @Test
    @DisplayName("도서를 삭제해도 기존 모임과 독서 활동 기록의 연결은 보존된다")
    void preservesExistingClubAndActivityRecords() {
        Book book = bookRepository.save(newBook(new BookTitle("운수 좋은 날"), null, 1924, 1, null));
        Author author = authorRepository.save(new Author(new AuthorName("현진건")));
        AuthorBook authorBook = authorBookRepository.save(new AuthorBook(author, book));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage passage = passageRepository.save(newPassage(chapter, 1, Collections.singletonList(new SentenceContent("본문"))));
        Member member = memberRepository.save(new Member(new Nickname("민서")));
        Club club = clubRepository.save(newClub(new ClubName("1기"), new JoinCode("CODE01")));
        bindingApi.bind(new SpaceId(club.getSpaceId()), new ContentId(book.getContentId()));
        ClubMember membership = clubMemberRepository.save(new ClubMember(new yeobaek.backend.shared.identity.MemberId(member.getId()), club));
        Comment comment = newClubComment(membership, passage.getSentences().getFirst(), new CommentContent("댓글"));
        commentRepository.save(comment);
        appreciationContextRepository.save(CommentFixtures.contextOf(comment));

        adminBookService.delete(book.getId());

        assertThat(List.of(
                bookRepository.findById(book.getId()).orElseThrow().getStatus(),
                clubRepository.findById(club.getId()).isPresent(),
                bindingApi.isBound(new SpaceId(club.getSpaceId()), new ContentId(book.getContentId())),
                passageRepository.findById(passage.getId()).isPresent(),
                commentRepository.findById(comment.getId()).isPresent(),
                authorBookRepository.findById(authorBook.getId()).isPresent()))
                .containsExactly(BookStatus.DELETED, true, true, true, true, true);
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
        Book book = bookRepository.save(newBook(new BookTitle("제목"), null, null, 1, null));
        adminBookService.delete(book.getId());

        assertThatThrownBy(() -> adminBookService.delete(book.getId()))
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("동시에 같은 도서를 삭제하면 한 요청만 성공한다")
    void allowOnlyOneConcurrentDeletion() throws Exception {
        Book book = bookRepository.save(newBook(new BookTitle("동시 삭제"), null, null, 1, null));
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
        Book book = bookRepository.save(newBook(new BookTitle("표지 도서"), null, null, 1, null));

        adminBookService.replaceCoverImage(book.getId(), COVER_KEY);
        assertThat(bookRepository.getById(book.getId()).getCoverImageKey()).isEqualTo(COVER_KEY);

        adminBookService.removeCoverImage(book.getId());
        assertThat(bookRepository.getById(book.getId()).getCoverImageKey()).isNull();
    }

    @Test
    @DisplayName("삭제된 도서의 표지는 교체할 수 없다")
    void cannotReplaceCoverOfDeletedBook() {
        Book book = bookRepository.save(newBook(new BookTitle("삭제 표지 도서"), null, null, 1, null));
        adminBookService.delete(book.getId());

        assertThatThrownBy(() -> adminBookService.replaceCoverImage(book.getId(), COVER_KEY))
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE);
    }
}
