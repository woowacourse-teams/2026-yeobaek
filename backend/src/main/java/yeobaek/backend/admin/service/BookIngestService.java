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
import yeobaek.backend.content.api.book.BookIngestApi;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.space.api.publicroom.PublicRoomApi;

/**
 * 인제스트 규격 JSON(API.md 6장) 업로드. 단일 트랜잭션이므로 실패 시 아무것도 저장되지 않는다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookIngestService {

    private final BookIngestApi bookIngestApi;
    private final PublicRoomApi publicRoomApi;
    private final SpaceContentBindingApi bindingService;

    @Transactional
    public BookUploadResponse upload(BookUploadRequest request) {
        int chapterCount = request.chapters() == null ? 0 : request.chapters().size();
        log.atInfo().addKeyValue(OPERATION, "admin.book.upload")
                .addKeyValue("chapterCount", chapterCount).log("도서 업로드를 시작합니다.");
        BookIngestApi.Result result = bookIngestApi.ingest(toCommand(request));

        var publicRoom = publicRoomApi.create();
        bindingService.bind(publicRoom.id(), result.contentId());
        BookUploadResponse response = new BookUploadResponse(result.bookId(), result.title(),
                result.coverImageUrl(), result.passageCount());
        log.atInfo().addKeyValue(OPERATION, "admin.book.upload").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(BOOK_ID, result.bookId()).addKeyValue("chapterCount", chapterCount)
                .addKeyValue("passageCount", result.passageCount()).log("도서 업로드를 완료했습니다.");
        return response;
    }

    private BookIngestApi.Command toCommand(BookUploadRequest request) {
        return new BookIngestApi.Command(request.title(), request.publisher(), request.publishedYear(),
                request.coverImageKey(), request.authors().stream().map(this::toAuthorReference).toList(),
                request.chapters().stream().map(this::toChapterInput).toList());
    }

    private BookIngestApi.AuthorReference toAuthorReference(AuthorEntryRequest request) {
        return new BookIngestApi.AuthorReference(request.authorId(),
                request.name(), request.isni());
    }

    private BookIngestApi.ChapterInput toChapterInput(ChapterUploadRequest request) {
        return new BookIngestApi.ChapterInput(request.title(),
                request.passages().stream().map(this::toPassageInput).toList());
    }

    private BookIngestApi.PassageInput toPassageInput(PassageUploadRequest request) {
        return new BookIngestApi.PassageInput(request.sentences().stream().map(this::toSentenceInput).toList());
    }

    private BookIngestApi.SentenceInput toSentenceInput(SentenceUploadRequest request) {
        return new BookIngestApi.SentenceInput(request.content());
    }

}
