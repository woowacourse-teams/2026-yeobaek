package yeobaek.backend.web.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import yeobaek.backend.application.content.ContentMetadataQueryService;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.idmapping.ContentIdMappingApi;
import yeobaek.backend.support.LogCapture;

class BookLoggingTest {

    @Test
    @DisplayName("검색어가 없더라도 도서 조회 로그에 keyword 필드를 null로 기록한다")
    void logNullKeyword() {
        ContentMetadataQueryService metadata = mock(ContentMetadataQueryService.class);
        ContentIdMappingApi idMappings = mock(ContentIdMappingApi.class);
        when(metadata.search(ContentKind.BOOK, null)).thenReturn(List.of());
        when(idMappings.toImplementationIds(ContentKind.BOOK, List.of())).thenReturn(Map.of());
        var service = new BookService(metadata, idMappings);

        try (var logs = new LogCapture(BookService.class.getName())) {
            service.findBooks(null);

            var events = logs.events("book.findBooks");
            assertThat(events).isNotEmpty();
            assertThat(events).allSatisfy(event -> assertThat(logs.hasField(event, "keyword")).isTrue());
            assertThat(events).allSatisfy(event -> assertThat(logs.field(event, "keyword")).isNull());
            assertThat(events).extracting(event -> logs.field(event, "searchUsed"))
                    .containsOnly(false);
        }
    }

    @Test
    @DisplayName("검색어로 도서를 조회하면 모든 로그에 keyword와 searchUsed를 기록한다")
    void logSearchKeyword() {
        ContentMetadataQueryService metadata = mock(ContentMetadataQueryService.class);
        ContentIdMappingApi idMappings = mock(ContentIdMappingApi.class);
        when(metadata.search(ContentKind.BOOK, "여백")).thenReturn(List.of());
        when(idMappings.toImplementationIds(ContentKind.BOOK, List.of())).thenReturn(Map.of());
        var service = new BookService(metadata, idMappings);

        try (var logs = new LogCapture(BookService.class.getName())) {
            service.findBooks("여백");

            var events = logs.events("book.findBooks");
            assertThat(events).isNotEmpty();
            assertThat(events).extracting(event -> logs.field(event, "keyword"))
                    .containsOnly("여백");
            assertThat(events).extracting(event -> logs.field(event, "searchUsed"))
                    .containsOnly(true);
        }
    }
}
