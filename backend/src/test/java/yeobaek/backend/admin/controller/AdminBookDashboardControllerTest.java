package yeobaek.backend.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import yeobaek.backend.admin.dto.AdminDashboardBookResponse;
import yeobaek.backend.admin.dto.AdminDashboardBooksResponse;
import yeobaek.backend.admin.service.AdminBookDashboardService;
import yeobaek.backend.book.domain.BookStatus;
import yeobaek.backend.support.ControllerTest;

@WebMvcTest(AdminBookDashboardController.class)
@TestPropertySource(properties = "admin.token=controller-test-token")
class AdminBookDashboardControllerTest extends ControllerTest {

    @MockitoBean
    private AdminBookDashboardService adminBookDashboardService;

    @Test
    @DisplayName("도서 대시보드의 전체 응답 계약을 반환한다")
    void findBooks() throws Exception {
        var response = new AdminDashboardBooksResponse(List.of(
                new AdminDashboardBookResponse(2L, "여백", BookStatus.ACTIVE, 3L)));
        given(adminBookDashboardService.findBooks()).willReturn(response);

        mockMvc.perform(get("/api/admin/dashboard/books")
                        .header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.books").isArray())
                .andExpect(jsonPath("$.books.length()").value(1))
                .andExpect(jsonPath("$.books[0].bookId").value(2))
                .andExpect(jsonPath("$.books[0].title").value("여백"))
                .andExpect(jsonPath("$.books[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.books[0].clubCount").value(3));

        verify(adminBookDashboardService, times(1)).findBooks();
    }

    @Test
    @DisplayName("도서가 없으면 빈 배열을 반환한다")
    void findBooksWhenEmpty() throws Exception {
        given(adminBookDashboardService.findBooks()).willReturn(new AdminDashboardBooksResponse(List.of()));

        mockMvc.perform(get("/api/admin/dashboard/books")
                        .header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books").isArray())
                .andExpect(jsonPath("$.books.length()").value(0));

        verify(adminBookDashboardService, times(1)).findBooks();
    }

    @Test
    @DisplayName("서비스 예외를 변경하지 않고 전파한다")
    void propagateServiceException() throws Exception {
        var serviceException = new IllegalStateException("도서 통계 조회 실패");
        given(adminBookDashboardService.findBooks()).willThrow(serviceException);

        var result = mockMvc.perform(get("/api/admin/dashboard/books")
                        .header("X-Admin-Token", "controller-test-token"))
                .andReturn();

        assertThat(result.getResolvedException()).isSameAs(serviceException);
        verify(adminBookDashboardService, times(1)).findBooks();
    }
}
