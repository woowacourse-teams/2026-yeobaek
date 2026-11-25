package yeobaek.backend.admin.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.admin.dto.AuthorEntryRequest;
import yeobaek.backend.admin.dto.BookUploadRequest;
import yeobaek.backend.admin.dto.ChapterUploadRequest;
import yeobaek.backend.admin.dto.PassageUploadRequest;
import yeobaek.backend.admin.dto.SentenceUploadRequest;
import yeobaek.backend.book.domain.Author;
import yeobaek.backend.book.domain.vo.AuthorName;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.Isni;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.book.repository.ActiveBookRepository;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.repository.AuthorRepository;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.book.service.BookCoverUrlResolver;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.InvalidRequestException;
import yeobaek.backend.support.NotFoundException;

@ExtendWith(MockitoExtension.class)
class BookIngestValidationTest {

    @Mock
    private ActiveBookRepository activeBookRepository;
    @Mock
    private BookManagementRepository bookManagementRepository;
    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private AuthorBookRepository authorBookRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private PassageRepository passageRepository;
    @Mock
    private BookCoverUrlResolver bookCoverUrlResolver;
    @Mock
    private PublicRoomRepository publicRoomRepository;
    @InjectMocks
    private BookIngestService service;

    @ParameterizedTest
    @MethodSource("emptyStructures")
    @DisplayName("작가·목차·문단·문장의 필수 구조가 비어 있으면 업로드를 거부한다")
    void rejectEmptyStructure(List<AuthorEntryRequest> authors, List<ChapterUploadRequest> chapters) {
        assertThatThrownBy(() -> service.upload(request(authors, chapters)))
                .isInstanceOf(InvalidRequestException.class);
    }

    static Stream<Arguments> emptyStructures() {
        var author = new AuthorEntryRequest(null, new AuthorName("작가"), null);
        var chapter = new ChapterUploadRequest(new ChapterTitle("목차"), List.of(new PassageUploadRequest(
                List.of(new SentenceUploadRequest(new SentenceContent("본문"))))));
        return Stream.of(
                Arguments.of(List.of(), List.of(chapter)),
                Arguments.of(List.of(author), List.of()),
                Arguments.of(List.of(author), List.of(new ChapterUploadRequest(new ChapterTitle("목차"), List.of()))),
                Arguments.of(List.of(author), List.of(new ChapterUploadRequest(new ChapterTitle("목차"),
                        List.of(new PassageUploadRequest(List.of()))))));
    }

    @Test
    @DisplayName("문자 수가 짧아도 UTF-8 본문이 MySQL TEXT 한도를 넘으면 거부한다")
    void rejectOversizedUtf8Sentence() {
        var request = request(List.of(new AuthorEntryRequest(null, new AuthorName("작가"), null)),
                List.of(chapterWithContent("가".repeat(21_846))));

        assertThatThrownBy(() -> service.upload(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("문장 하나는 65535바이트를 넘을 수 없습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 작가 참조는 AUTHOR_NOT_FOUND로 거부한다")
    void rejectUnknownAuthorReference() {
        when(authorRepository.findById(13L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upload(requestWithAuthors(new AuthorEntryRequest(13L, null, null))))
                .isInstanceOf(NotFoundException.class)
                .extracting("code").isEqualTo(ErrorCode.AUTHOR_NOT_FOUND);
    }

    @Test
    @DisplayName("같은 ISNI가 다른 이름으로 등록되어 있으면 이름 불일치를 안내한다")
    void rejectAuthorNameMismatch() {
        Isni isni = new Isni("000000012345964X");
        when(authorRepository.findByIsni(isni.value()))
                .thenReturn(Optional.of(new Author(new AuthorName("등록 작가"), isni)));

        assertThatThrownBy(() -> service.upload(requestWithAuthors(
                new AuthorEntryRequest(null, new AuthorName("다른 작가"), isni))))
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.AUTHOR_NAME_MISMATCH);
    }

    @Test
    @DisplayName("ID 참조와 ISNI 참조가 같은 작가를 가리키면 중복 작가로 거부한다")
    void rejectDuplicateAuthorAcrossReferenceForms() {
        Isni isni = new Isni("000000012345964X");
        Author author = new Author(new AuthorName("등록 작가"), isni);
        ReflectionTestUtils.setField(author, "id", 13L);
        when(authorRepository.findById(13L)).thenReturn(Optional.of(author));
        when(authorRepository.findByIsni(isni.value())).thenReturn(Optional.of(author));

        assertThatThrownBy(() -> service.upload(requestWithAuthors(
                new AuthorEntryRequest(13L, null, null),
                new AuthorEntryRequest(null, new AuthorName("등록 작가"), isni))))
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.DUPLICATE_AUTHOR);
    }

    @Test
    @DisplayName("기존 작가 ID와 신규 작가 이름을 동시에 지정하면 거부한다")
    void rejectMixedAuthorReference() {
        assertThatThrownBy(() -> service.upload(requestWithAuthors(
                new AuthorEntryRequest(13L, new AuthorName("작가"), null))))
                .isInstanceOf(InvalidRequestException.class);
    }

    private BookUploadRequest requestWithAuthors(AuthorEntryRequest... authors) {
        return request(List.of(authors), List.of(chapterWithContent("본문")));
    }

    private BookUploadRequest request(List<AuthorEntryRequest> authors, List<ChapterUploadRequest> chapters) {
        return new BookUploadRequest(new BookTitle("테스트 도서"), null, null, null,
                authors, chapters, null, null, null);
    }

    private ChapterUploadRequest chapterWithContent(String content) {
        return new ChapterUploadRequest(new ChapterTitle("목차"), List.of(new PassageUploadRequest(
                List.of(new SentenceUploadRequest(new SentenceContent(content))))));
    }
}
