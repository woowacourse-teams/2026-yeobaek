package yeobaek.backend.web.v2.content;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import yeobaek.backend.application.content.ContentMetadataQueryService;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.support.ControllerTest;

@WebMvcTest(V2ContentController.class)
class V2ContentControllerTest extends ControllerTest {

    @MockitoBean
    private ContentMetadataQueryService contents;

    @MockitoBean
    private ContentWebAdapterRegistry adapters;

    @Test
    void findContents() throws Exception {
        givenValidMember(1L);
        var result = new ContentMetadataQueryService.Summary(new ContentId(2L), ContentKind.BOOK, "책",
                List.of("작가"), "출판사", 2026, null, 10);
        var response = new ContentResponses.SummaryResponse(2L, ContentKind.BOOK,
                new ContentResponses.BookSummaryData("책", List.of("작가"), "출판사", 2026, null, 10));
        given(contents.search(ContentKind.BOOK, "검색")).willReturn(List.of(result));
        given(adapters.summary(result)).willReturn(response);

        mockMvc.perform(get("/api/v2/contents").header("X-Member-Id", "1")
                        .param("kind", "BOOK").param("keyword", "검색"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.contents").isArray())
                .andExpect(jsonPath("$.contents.length()").value(1))
                .andExpect(jsonPath("$.contents[0].contentId").value(2))
                .andExpect(jsonPath("$.contents[0].kind").value("BOOK"))
                .andExpect(jsonPath("$.contents[0].data.title").value("책"))
                .andExpect(jsonPath("$.contents[0].data.authors[0]").value("작가"))
                .andExpect(jsonPath("$.contents[0].data.publisher").value("출판사"))
                .andExpect(jsonPath("$.contents[0].data.publishedYear").value(2026))
                .andExpect(jsonPath("$.contents[0].data.coverImageUrl").value((Object) null))
                .andExpect(jsonPath("$.contents[0].data.passageCount").value(10));

        verify(contents).search(ContentKind.BOOK, "검색");
        verify(adapters).summary(result);
    }

    @Test
    void findEmptyContentsWithoutKeyword() throws Exception {
        givenValidMember(1L);
        given(contents.search(ContentKind.BOOK, null)).willReturn(List.of());
        mockMvc.perform(get("/api/v2/contents").header("X-Member-Id", "1").param("kind", "BOOK"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.contents").isArray()).andExpect(jsonPath("$.contents.length()").value(0));
        verify(contents).search(ContentKind.BOOK, null);
    }

    @Test
    void findContentDetail() throws Exception {
        givenValidMember(1L);
        var result = new ContentMetadataQueryService.Detail(new ContentId(2L), ContentKind.BOOK, "책",
                List.of(), null, null, null, 10,
                List.of(new ContentMetadataQueryService.Section(3L, "1장", 1, 1, 10)));
        var response = new ContentResponses.DetailResponse(2L, ContentKind.BOOK,
                new ContentResponses.BookDetailData("책", List.of(), null, null, null, 10,
                        List.of(new ContentResponses.Chapter(3L, "1장", 1, 1, 10))));
        given(contents.getDetail(new ContentId(2L))).willReturn(result);
        given(adapters.detail(result)).willReturn(response);

        mockMvc.perform(get("/api/v2/contents/2").header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.contentId").value(2)).andExpect(jsonPath("$.kind").value("BOOK"))
                .andExpect(jsonPath("$.data.title").value("책"))
                .andExpect(jsonPath("$.data.authors").isArray()).andExpect(jsonPath("$.data.authors.length()").value(0))
                .andExpect(jsonPath("$.data.publisher").value((Object) null))
                .andExpect(jsonPath("$.data.publishedYear").value((Object) null))
                .andExpect(jsonPath("$.data.coverImageUrl").value((Object) null))
                .andExpect(jsonPath("$.data.passageCount").value(10))
                .andExpect(jsonPath("$.data.chapters").isArray()).andExpect(jsonPath("$.data.chapters.length()").value(1))
                .andExpect(jsonPath("$.data.chapters[0].chapterId").value(3))
                .andExpect(jsonPath("$.data.chapters[0].title").value("1장"))
                .andExpect(jsonPath("$.data.chapters[0].sequence").value(1))
                .andExpect(jsonPath("$.data.chapters[0].startPassageSequence").value(1))
                .andExpect(jsonPath("$.data.chapters[0].endPassageSequence").value(10));
        verify(contents).getDetail(new ContentId(2L));
        verify(adapters).detail(result);
    }

    @Test
    void rejectInvalidContentIdBeforeServiceCall() throws Exception {
        givenValidMember(1L);

        mockMvc.perform(get("/api/v2/contents/0").header("X-Member-Id", "1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contents, adapters);
    }

    @Test
    void propagateServiceException() throws Exception {
        givenValidMember(1L);
        var failure = new IllegalArgumentException("실패");
        given(contents.getDetail(new ContentId(99L))).willThrow(failure);
        var result = mockMvc.perform(get("/api/v2/contents/99").header("X-Member-Id", "1")).andReturn();
        assertSame(failure, result.getResolvedException(), "서비스 예외를 그대로 전파해야 한다");
        verify(contents).getDetail(new ContentId(99L));
    }
}
