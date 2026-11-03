package yeobaek.backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.BookDescription;
import yeobaek.backend.book.domain.vo.Publisher;

/**
 * 인제스트 규격 JSON (API.md 6장, 2026-08-06 확정). 본문 순서는 배열 등장 순서로 서버가 부여한다.
 */
public record BookUploadRequest(
        @JsonProperty(required = true)
        @JsonSetter(nulls = Nulls.FAIL)
        @Schema(type = "string", description = "도서 제목 (1~100자)") @NotNull BookTitle title,
        @Schema(type = "string", description = "출판사 (선택, 최대 100자)", nullable = true) Publisher publisher,
        @Schema(description = "출판연도 (선택, 정수)", nullable = true) Integer publishedYear,
        @Schema(description = "표지 UUID (선택, 기존 전체 객체 키도 허용)", nullable = true) String coverImageKey,
        @Schema(description = "작가 목록") @Valid @NotNull List<@NotNull AuthorEntryRequest> authors,
        @Schema(description = "목차 목록") @Valid @NotNull List<@NotNull ChapterUploadRequest> chapters,
        @Schema(type = "string", description = "검수한 책 소개 (선택)", nullable = true) BookDescription description,
        @Schema(description = "자유 입력 장르 목록 (선택)", types = {"array", "null"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED) List<String> genres,
        @Schema(description = "해시태그 목록 (선택, 최대 5개, 각 10자). null/빈 배열은 태그 없음.", types = {"array", "null"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED) List<String> hashtags
) {

    public BookUploadRequest(BookTitle title, Publisher publisher, Integer publishedYear, String coverImageKey,
                             List<AuthorEntryRequest> authors, List<ChapterUploadRequest> chapters,
                             BookDescription description) {
        this(title, publisher, publishedYear, coverImageKey, authors, chapters, description, null, null);
    }

    public BookUploadRequest {
        if (genres != null) {
            genres = Collections.unmodifiableList(new ArrayList<>(genres));
        }
        if (hashtags != null) {
            hashtags = Collections.unmodifiableList(new ArrayList<>(hashtags));
        }
        if (authors != null) {
            authors = Collections.unmodifiableList(new ArrayList<>(authors));
        }
        if (chapters != null) {
            chapters = Collections.unmodifiableList(new ArrayList<>(chapters));
        }
    }

    @Override
    public List<String> genres() {
        return genres == null ? null : Collections.unmodifiableList(genres);
    }

    @Override
    public List<String> hashtags() {
        return hashtags == null ? null : Collections.unmodifiableList(hashtags);
    }

    @Override
    public List<ChapterUploadRequest> chapters() {
        return chapters == null ? null : Collections.unmodifiableList(chapters);
    }

    @Override
    public List<AuthorEntryRequest> authors() {
        return authors == null ? null : Collections.unmodifiableList(authors);
    }
}
