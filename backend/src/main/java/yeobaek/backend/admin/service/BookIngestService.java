package yeobaek.backend.admin.service;

import static yeobaek.backend.support.LogField.BOOK_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SUCCESS;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.admin.dto.AuthorEntryRequest;
import yeobaek.backend.admin.dto.BookUploadRequest;
import yeobaek.backend.admin.dto.BookUploadResponse;
import yeobaek.backend.admin.dto.ChapterUploadRequest;
import yeobaek.backend.admin.dto.PassageUploadRequest;
import yeobaek.backend.admin.dto.SentenceUploadRequest;
import yeobaek.backend.book.api.BookCoverApi;
import yeobaek.backend.book.api.BookIngestApi;
import yeobaek.backend.book.api.BookIngestFailure;
import yeobaek.backend.collaboration.api.SpaceContentBindingApi;
import yeobaek.backend.publicroom.api.PublicRoomProvisioningApi;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.InvalidRequestException;
import yeobaek.backend.support.NotFoundException;

/**
 * 인제스트 규격 JSON(API.md 6장) 업로드. 단일 트랜잭션이므로 실패 시 아무것도 저장되지 않는다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookIngestService {

    private final BookIngestApi bookIngestApi;
    private final BookCoverApi bookCoverApi;
    private final PublicRoomProvisioningApi publicRoomProvisioningApi;
    private final SpaceContentBindingApi bindingService;

    @Transactional
    public BookUploadResponse upload(BookUploadRequest request) {
        int chapterCount = request.chapters() == null ? 0 : request.chapters().size();
        log.atInfo().addKeyValue(OPERATION, "admin.book.upload")
                .addKeyValue("chapterCount", chapterCount).log("도서 업로드를 시작합니다.");
        BookIngestApi.Result result;
        try {
            result = bookIngestApi.ingest(toCommand(request));
        } catch (BookIngestFailure failure) {
            throw compatibleFailure(failure);
        }

        var publicRoom = publicRoomProvisioningApi.create();
        bindingService.bind(publicRoom.id(), result.contentId());
        BookUploadResponse response = new BookUploadResponse(result.bookId(), result.title(),
                bookCoverApi.resolve(result.coverImageKey()), result.passageCount());
        log.atInfo().addKeyValue(OPERATION, "admin.book.upload").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(BOOK_ID, result.bookId()).addKeyValue("chapterCount", chapterCount)
                .addKeyValue("passageCount", result.passageCount()).log("도서 업로드를 완료했습니다.");
        return response;
    }

    private BookIngestApi.Command toCommand(BookUploadRequest request) {
        return new BookIngestApi.Command(request.title().value(),
                request.publisher() == null ? null : request.publisher().value(), request.publishedYear(),
                request.coverImageKey(), request.authors().stream().map(this::toAuthorReference).toList(),
                request.chapters().stream().map(this::toChapterInput).toList());
    }

    private BookIngestApi.AuthorReference toAuthorReference(AuthorEntryRequest request) {
        return new BookIngestApi.AuthorReference(request.authorId(),
                request.name() == null ? null : request.name().value(),
                request.isni() == null ? null : request.isni().value());
    }

    private BookIngestApi.ChapterInput toChapterInput(ChapterUploadRequest request) {
        return new BookIngestApi.ChapterInput(request.title().value(),
                request.passages().stream().map(this::toPassageInput).toList());
    }

    private BookIngestApi.PassageInput toPassageInput(PassageUploadRequest request) {
        return new BookIngestApi.PassageInput(request.sentences().stream().map(this::toSentenceInput).toList());
    }

    private BookIngestApi.SentenceInput toSentenceInput(SentenceUploadRequest request) {
        return new BookIngestApi.SentenceInput(request.content().value());
    }

    private RuntimeException compatibleFailure(BookIngestFailure failure) {
        return switch (failure.reason()) {
            case AUTHORS_EMPTY, CHAPTERS_EMPTY, PASSAGES_EMPTY, SENTENCES_EMPTY, SENTENCE_TOO_LARGE,
                    MIXED_AUTHOR_REFERENCE -> new InvalidRequestException(failure.getMessage(), failure.context());
            case AUTHOR_NOT_FOUND -> new NotFoundException(
                    ErrorCode.AUTHOR_NOT_FOUND, failure.getMessage(), failure.context());
            case AUTHOR_NAME_MISMATCH -> new BadRequestException(
                    ErrorCode.AUTHOR_NAME_MISMATCH, failure.getMessage(), failure.context());
            case DUPLICATE_AUTHOR, DUPLICATE_AUTHOR_ISNI -> new BadRequestException(
                    ErrorCode.DUPLICATE_AUTHOR, failure.getMessage(), failure.context());
            case DUPLICATE_BOOK -> new BadRequestException(
                    ErrorCode.DUPLICATE_BOOK, failure.getMessage(), failure.context());
        };
    }
}
