package yeobaek.backend.admin.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.admin.dto.AdminAuthorBookResponse;
import yeobaek.backend.admin.dto.AdminAuthorResponse;
import yeobaek.backend.admin.dto.AdminAuthorsResponse;
import yeobaek.backend.admin.service.AdminAuthorService;
import yeobaek.backend.support.ControllerTest;

@WebMvcTest(AdminAuthorController.class)
@TestPropertySource(properties = "admin.token=test-admin-token")
class AdminAuthorControllerTest extends ControllerTest {

    @MockitoBean
    private AdminAuthorService adminAuthorService;

    @Test
    @DisplayName("관리자 토큰으로 작가 목록을 조회한다")
    void findAuthors() throws Exception {
        given(adminAuthorService.findAuthors()).willReturn(new AdminAuthorsResponse(List.of(
                new AdminAuthorResponse(12L, "현진건", "000000012345964X",
                        List.of(new AdminAuthorBookResponse(3L, "운수 좋은 날"))))));

        mockMvc.perform(get("/api/admin/authors").header("X-Admin-Token", "test-admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authors[0].authorId").value(12))
                .andExpect(jsonPath("$.authors[0].books[0].title").value("운수 좋은 날"));
    }

    @Test
    @DisplayName("관리자 토큰이 없으면 401을 응답한다")
    void rejectMissingToken() throws Exception {
        mockMvc.perform(get("/api/admin/authors"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
