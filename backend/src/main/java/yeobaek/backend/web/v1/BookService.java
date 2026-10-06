package yeobaek.backend.web.v1;

import static yeobaek.backend.support.LogField.BOOK_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.application.content.ContentMetadataQueryService;
import yeobaek.backend.application.content.ContentMetadataQueryService.Detail;
import yeobaek.backend.application.content.ContentMetadataQueryService.Summary;
import yeobaek.backend.content.api.idmapping.ContentIdMappingApi;
import yeobaek.backend.content.api.idmapping.ContentIdMappingNotFoundException;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.NotFoundException;
import yeobaek.backend.web.book.dto.BookDetailResponse;
import yeobaek.backend.web.book.dto.BookSummaryResponse;
import yeobaek.backend.web.book.dto.BooksResponse;
import yeobaek.backend.web.book.dto.ChapterResponse;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BookService {

    private static final String DELETED_STATUS = "DELETED";

    private final ContentMetadataQueryService queries;
    private final ContentIdMappingApi idMappings;

    public BooksResponse findBooks(String keyword) {
        boolean searchUsed = keyword != null && !keyword.isBlank();
        log.atInfo().addKeyValue(OPERATION, "book.findBooks")
                .addKeyValue("keyword", keyword)
                .addKeyValue("searchUsed", searchUsed).log("도서 목록을 조회합니다.");
        List<Summary> metadata = queries.search(ContentKind.BOOK, keyword);
        Map<ContentId, Long> bookIds = idMappings.toImplementationIds(ContentKind.BOOK,
                metadata.stream().map(Summary::contentId).toList());
        var response = new BooksResponse(metadata.stream()
                .map(content -> summary(content, requireBookId(bookIds, content.contentId())))
                .toList());
        log.atInfo().addKeyValue(OPERATION, "book.findBooks").addKeyValue(RESULT, SUCCESS)
                .addKeyValue("keyword", keyword).addKeyValue("searchUsed", searchUsed)
                .addKeyValue("resultCount", response.books().size())
                .log("도서 목록을 조회했습니다.");
        return response;
    }

    public BookDetailResponse findBook(Long bookId) {
        log.atInfo().addKeyValue(OPERATION, "book.findBook")
                .addKeyValue(BOOK_ID, bookId).log("도서 상세를 조회합니다.");
        ContentId contentId = resolve(bookId);
        Detail metadata = queries.getDetail(contentId);
        var response = detail(metadata, bookId);
        log.atInfo().addKeyValue(OPERATION, "book.findBook").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(BOOK_ID, bookId).addKeyValue("chapterCount", response.chapters().size())
                .log("도서 상세를 조회했습니다.");
        return response;
    }

    private ContentId resolve(Long bookId) {
        try {
            return idMappings.toContentId(ContentKind.BOOK, bookId);
        } catch (ContentIdMappingNotFoundException failure) {
            throw new NotFoundException(ErrorCode.BOOK_NOT_FOUND,
                    "이용할 도서가 존재하지 않습니다: bookId=" + bookId,
                    Map.of(BOOK_ID, bookId.toString()), failure);
        }
    }

    private BookSummaryResponse summary(Summary metadata, long bookId) {
        return new BookSummaryResponse(bookId, metadata.title(), metadata.creators(),
                metadata.publisher(), metadata.publishedYear(),
                metadata.coverImageUrl(), metadata.unitCount());
    }

    private BookDetailResponse detail(Detail metadata, long bookId) {
        return new BookDetailResponse(bookId, metadata.title(), metadata.creators(),
                metadata.publisher(), metadata.publishedYear(),
                metadata.coverImageUrl(), metadata.unitCount(), metadata.sections().stream()
                .map(section -> new ChapterResponse(section.sectionId(), section.title(), section.sequence(),
                        section.startUnitSequence(), section.endUnitSequence()))
                .toList());
    }

    private long requireBookId(Map<ContentId, Long> bookIds, ContentId contentId) {
        Long bookId = bookIds.get(contentId);
        if (bookId == null) {
            throw new IllegalStateException("도서의 레거시 식별자를 찾을 수 없습니다: contentId=" + contentId.value());
        }
        return bookId;
    }
}
