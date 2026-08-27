package yeobaek.backend.admin.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.admin.dto.AdminAuthorBookResponse;
import yeobaek.backend.admin.dto.AdminAuthorResponse;
import yeobaek.backend.admin.dto.AdminAuthorsResponse;
import yeobaek.backend.admin.service.AdminAuthorService;
import yeobaek.backend.book.domain.BookStatus;
import yeobaek.backend.support.ControllerTest;

@WebMvcTest(AdminAuthorController.class)
@TestPropertySource(properties = "admin.token=controller-test-token")
class AdminAuthorControllerTest extends ControllerTest {

    @MockitoBean
    private AdminAuthorService adminAuthorService;

    @Test
    @DisplayName("작가와 작품 목록의 nullable 필드와 빈 컬렉션을 포함한 전체 계약을 반환한다")
    void findAuthors() throws Exception {
        var response = new AdminAuthorsResponse(List.of(
                new AdminAuthorResponse(
                        12L,
                        "현진건",
                        "000000012345964X",
                        List.of(
                                new AdminAuthorBookResponse(3L, "운수 좋은 날",
                                        "https://covers.example/cover.jpg", BookStatus.DELETED),
                                new AdminAuthorBookResponse(4L, "표지 없는 책", null, BookStatus.ACTIVE))),
                new AdminAuthorResponse(13L, "작자 미상", null, List.of())));
        given(adminAuthorService.findAuthors()).willReturn(response);

        mockMvc.perform(get("/api/admin/authors")
                        .header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.authors").isArray())
                .andExpect(jsonPath("$.authors.length()").value(2))
                .andExpect(jsonPath("$.authors[0].authorId").value(12))
                .andExpect(jsonPath("$.authors[0].name").value("현진건"))
                .andExpect(jsonPath("$.authors[0].isni").value("000000012345964X"))
                .andExpect(jsonPath("$.authors[0].books").isArray())
                .andExpect(jsonPath("$.authors[0].books.length()").value(2))
                .andExpect(jsonPath("$.authors[0].books[0].bookId").value(3))
                .andExpect(jsonPath("$.authors[0].books[0].title").value("운수 좋은 날"))
                .andExpect(jsonPath("$.authors[0].books[0].coverImageUrl")
                        .value("https://covers.example/cover.jpg"))
                .andExpect(jsonPath("$.authors[0].books[0].status").value("DELETED"))
                .andExpect(jsonPath("$.authors[0].books[1].coverImageUrl").value((Object) null))
                .andExpect(jsonPath("$.authors[1].authorId").value(13))
                .andExpect(jsonPath("$.authors[1].name").value("작자 미상"))
                .andExpect(jsonPath("$.authors[1].isni").value((Object) null))
                .andExpect(jsonPath("$.authors[1].books").isArray())
                .andExpect(jsonPath("$.authors[1].books.length()").value(0));

        verify(adminAuthorService, times(1)).findAuthors();
    }

    @Test
    @DisplayName("서비스 예외를 변경하지 않고 전파한다")
    void propagateServiceException() throws Exception {
        var serviceException = new IllegalStateException("작가 조회 실패");
        given(adminAuthorService.findAuthors()).willThrow(serviceException);

        var result = mockMvc.perform(get("/api/admin/authors")
                        .header("X-Admin-Token", "controller-test-token"))
                .andReturn();

        assertSame(serviceException, result.getResolvedException(),
                "컨트롤러는 서비스 예외 인스턴스를 변경하지 않아야 한다");
        verify(adminAuthorService, times(1)).findAuthors();
    }
}
