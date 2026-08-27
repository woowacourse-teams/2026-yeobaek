package yeobaek.backend.admin.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.admin.dto.BookCoverUploadUrlRequest;
import yeobaek.backend.admin.dto.BookCoverUploadUrlResponse;
import yeobaek.backend.admin.service.BookCoverUploadService;
import yeobaek.backend.support.ControllerTest;

@WebMvcTest(AdminBookCoverController.class)
@TestPropertySource(properties = "admin.token=controller-test-token")
class AdminBookCoverControllerTest extends ControllerTest {

    @MockitoBean
    private BookCoverUploadService bookCoverUploadService;

    @Test
    @DisplayName("표지 업로드 URL 발급 계약을 반환한다")
    void issueUploadUrl() throws Exception {
        var request = new BookCoverUploadUrlRequest("image/webp", 1024);
        var response = new BookCoverUploadUrlResponse(
                "yeobaek/book-covers/123e4567-e89b-12d3-a456-426614174000.webp",
                "https://s3.example/upload",
                Instant.parse("2026-08-26T12:10:00Z"),
                Map.of("Content-Type", "image/webp", "Cache-Control", BookCoverUploadService.CACHE_CONTROL));
        given(bookCoverUploadService.issueUploadUrl(request)).willReturn(response);

        mockMvc.perform(post("/api/admin/book-covers/upload-url")
                        .header("X-Admin-Token", "controller-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType":"image/webp","contentLength":1024}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverImageKey").value(response.coverImageKey()))
                .andExpect(jsonPath("$.uploadUrl").value(response.uploadUrl()))
                .andExpect(jsonPath("$.expiresAt").value("2026-08-26T12:10:00Z"))
                .andExpect(jsonPath("$.requiredHeaders['Content-Type']").value("image/webp"))
                .andExpect(jsonPath("$.requiredHeaders['Cache-Control']")
                        .value(BookCoverUploadService.CACHE_CONTROL));

        verify(bookCoverUploadService).issueUploadUrl(request);
    }
}
