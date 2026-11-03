package yeobaek.backend.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.admin.dto.BookUploadRequest;
import yeobaek.backend.admin.dto.BookUploadResponse;
import yeobaek.backend.admin.dto.AdminBookResponse;
import yeobaek.backend.admin.dto.AdminBooksResponse;
import yeobaek.backend.admin.service.AdminBookService;
import yeobaek.backend.admin.service.BookIngestService;
import yeobaek.backend.book.domain.vo.BookHashtags;
import yeobaek.backend.book.domain.vo.BookGenres;
import yeobaek.backend.book.domain.BookStatus;
import yeobaek.backend.support.ControllerTest;

@WebMvcTest(AdminBookController.class)
@TestPropertySource(properties = "admin.token=controller-test-token")
class AdminBookMetadataControllerTest extends ControllerTest {

    @MockitoBean
    private BookIngestService bookIngestService;

    @MockitoBean
    private AdminBookService adminBookService;

    @Test
    @DisplayName("기존 관리자 응답에 장르와 해시태그 배열을 추가한다")
    void returnsMetadata() throws Exception {
        given(adminBookService.findBooks()).willReturn(new AdminBooksResponse(List.of(
                new AdminBookResponse(1L, "책", List.of(), null, null, 1, null, BookStatus.ACTIVE,
                        null, List.of("로맨스", "비극"), List.of("재회", "첫사랑")))));

        mockMvc.perform(get("/api/admin/books").header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.books[0].bookId").value(1))
                .andExpect(jsonPath("$.books[0].genres[0]").value("로맨스"))
                .andExpect(jsonPath("$.books[0].genres[1]").value("비극"))
                .andExpect(jsonPath("$.books[0].hashtags.length()").value(2))
                .andExpect(jsonPath("$.books[0].hashtags[0]").value("재회"))
                .andExpect(jsonPath("$.books[0].hashtags[1]").value("첫사랑"));

        verify(adminBookService, times(1)).findBooks();
    }

    @Test
    @DisplayName("장르와 해시태그를 단일 인제스트 요청으로 전달한다")
    void uploadsMetadata() throws Exception {
        given(bookIngestService.upload(org.mockito.ArgumentMatchers.any()))
                .willReturn(new BookUploadResponse(1L, "책", null, 0));

        mockMvc.perform(post("/api/admin/books").header("X-Admin-Token", "controller-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"책","authors":[],"chapters":[],"genres":["로맨스","비극"],
                                 "hashtags":["첫사랑","#파국예감"]}
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<BookUploadRequest> captor = ArgumentCaptor.forClass(BookUploadRequest.class);
        verify(bookIngestService, times(1)).upload(captor.capture());
        assertThat(captor.getValue().genres().value()).containsExactly("로맨스", "비극");
        assertThat(captor.getValue().hashtags().value()).containsExactly("첫사랑", "#파국예감");
    }

    @Test
    @DisplayName("복수 장르 교체 요청의 배열을 그대로 전달한다")
    void replacesGenres() throws Exception {
        mockMvc.perform(put("/api/admin/books/3/genres").header("X-Admin-Token", "controller-test-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"genres\":[\"로맨스\",\"비극\"]}"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));

        verify(adminBookService, times(1)).replaceGenres(3L, new BookGenres(List.of("로맨스", "비극")));
    }

    @Test
    @DisplayName("해시태그 전체 교체 요청을 VO로 바인딩하고 204를 반환한다")
    void replacesHashtags() throws Exception {
        mockMvc.perform(put("/api/admin/books/3/hashtags").header("X-Admin-Token", "controller-test-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"hashtags\":[\"재회\",\"첫사랑\"]}"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));

        verify(adminBookService, times(1)).replaceHashtags(3L, new BookHashtags(List.of("재회", "첫사랑")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"hashtags\":[]}", "{\"hashtags\":null}"})
    @DisplayName("빈 배열·null 저장 요청은 해시태그 제거 인자로 전달한다")
    void clearsHashtags(String json) throws Exception {
        mockMvc.perform(put("/api/admin/books/3/hashtags").header("X-Admin-Token", "controller-test-token")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNoContent()).andExpect(content().string(""));

        ArgumentCaptor<BookHashtags> captor = ArgumentCaptor.forClass(BookHashtags.class);
        verify(adminBookService, times(1)).replaceHashtags(org.mockito.ArgumentMatchers.eq(3L), captor.capture());
        assertThat(captor.getValue() == null || captor.getValue().value().isEmpty()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"genres\":[]}", "{\"genres\":null}"})
    @DisplayName("빈 장르 저장 요청은 제거 가능한 인자로 전달한다")
    void clearsGenres(String json) throws Exception {
        mockMvc.perform(put("/api/admin/books/3/genres").header("X-Admin-Token", "controller-test-token")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNoContent()).andExpect(content().string(""));

        ArgumentCaptor<BookGenres> captor = ArgumentCaptor.forClass(BookGenres.class);
        verify(adminBookService, times(1)).replaceGenres(org.mockito.ArgumentMatchers.eq(3L), captor.capture());
        assertThat(captor.getValue() == null || captor.getValue().value().isEmpty()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"hashtags\":[\"가\",\"나\",\"다\",\"라\",\"마\",\"바\"]}",
            "{\"hashtags\":[\"가나다라마바사아자차카\"]}"})
    @DisplayName("해시태그 개수·길이 초과는 서비스 호출 전에 400으로 거절한다")
    void rejectsOverflow(String json) throws Exception {
        mockMvc.perform(put("/api/admin/books/3/hashtags").header("X-Admin-Token", "controller-test-token")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(adminBookService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"genres", "hashtags"})
    @DisplayName("수정할 필드가 없는 요청은 명시적인 비우기 요청으로 처리하지 않는다")
    void rejectsMissingUpdateField(String field) throws Exception {
        mockMvc.perform(put("/api/admin/books/3/" + field).header("X-Admin-Token", "controller-test-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(adminBookService);
    }
}
