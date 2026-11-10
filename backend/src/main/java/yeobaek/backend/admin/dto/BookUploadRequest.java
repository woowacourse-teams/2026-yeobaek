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
import tools.jackson.databind.annotation.JsonDeserialize;
import yeobaek.backend.admin.controller.StrictStringDeserializer;
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
        @JsonDeserialize(contentUsing = StrictStringDeserializer.class)
        @Schema(description = "자유 입력 장르 목록. 장르당 최대 255자(Unicode 코드 포인트 기준). 생략하거나 null 또는 []를 보내면 장르 없이 등록한다.", types = {"array", "null"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED) List<String> genres,
        @JsonDeserialize(contentUsing = StrictStringDeserializer.class)
        @Schema(description = "해시태그 목록(최대 5개, 각 10자). 생략하거나 null 또는 []를 보내면 해시태그 없이 등록한다.", types = {"array", "null"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED) List<String> hashtags
) {

    public BookUploadRequest {
        if (authors != null) {
            authors = Collections.unmodifiableList(new ArrayList<>(authors));
        }
        if (chapters != null) {
            chapters = Collections.unmodifiableList(new ArrayList<>(chapters));
        }
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
