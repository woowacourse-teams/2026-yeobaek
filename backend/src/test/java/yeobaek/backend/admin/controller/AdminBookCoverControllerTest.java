package yeobaek.backend.admin.controller;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
        var request = new BookCoverUploadUrlRequest("image/webp", 1024L, 256L);
        var expiration = Instant.parse("2026-08-26T12:10:00Z");
        var original = new BookCoverUploadUrlResponse.UploadTarget("https://s3.example/original", expiration,
                Map.of("Content-Type", "image/webp", "Cache-Control", BookCoverUploadService.CACHE_CONTROL));
        var low = new BookCoverUploadUrlResponse.UploadTarget("https://s3.example/low", expiration,
                Map.of("Content-Type", "image/jpeg", "Cache-Control", BookCoverUploadService.CACHE_CONTROL));
        var legacy = new BookCoverUploadUrlResponse.UploadTarget("https://s3.example/legacy", expiration,
                Map.of("Content-Type", "image/jpeg", "Cache-Control", BookCoverUploadService.CACHE_CONTROL));
        var response = new BookCoverUploadUrlResponse("123e4567-e89b-12d3-a456-426614174000", original, low, legacy);
        given(bookCoverUploadService.issueUploadUrl(request)).willReturn(response);

        mockMvc.perform(post("/api/admin/book-covers/upload-url")
                        .header("X-Admin-Token", "controller-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType":"image/webp","contentLength":1024,"lowContentLength":256}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverImageKey").value(response.coverImageKey()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.original").isMap())
                .andExpect(jsonPath("$.original.uploadUrl").value(original.uploadUrl()))
                .andExpect(jsonPath("$.original.expiresAt").value("2026-08-26T12:10:00Z"))
                .andExpect(jsonPath("$.original.requiredHeaders['Content-Type']").value("image/webp"))
                .andExpect(jsonPath("$.original.requiredHeaders['Cache-Control']").value(BookCoverUploadService.CACHE_CONTROL))
                .andExpect(jsonPath("$.low").isMap())
                .andExpect(jsonPath("$.low.uploadUrl").value(low.uploadUrl()))
                .andExpect(jsonPath("$.low.expiresAt").value("2026-08-26T12:10:00Z"))
                .andExpect(jsonPath("$.low.requiredHeaders['Content-Type']").value("image/jpeg"))
                .andExpect(jsonPath("$.low.requiredHeaders['Cache-Control']").value(BookCoverUploadService.CACHE_CONTROL))
                .andExpect(jsonPath("$.legacy").isMap())
                .andExpect(jsonPath("$.legacy.uploadUrl").value(legacy.uploadUrl()))
                .andExpect(jsonPath("$.legacy.expiresAt").value("2026-08-26T12:10:00Z"))
                .andExpect(jsonPath("$.legacy.requiredHeaders['Content-Type']").value("image/jpeg"))
                .andExpect(jsonPath("$.legacy.requiredHeaders['Cache-Control']").value(BookCoverUploadService.CACHE_CONTROL));

        verify(bookCoverUploadService, times(1)).issueUploadUrl(request);
    }

    @Test
    @DisplayName("표지 서명 서비스 예외를 동일 인스턴스로 전파한다")
    void propagateServiceException() throws Exception {
        var request = new BookCoverUploadUrlRequest("image/webp", 1024L, 256L);
        var failure = new IllegalStateException("서명 실패");
        given(bookCoverUploadService.issueUploadUrl(request)).willThrow(failure);
        mockMvc.perform(post("/api/admin/book-covers/upload-url")
                        .header("X-Admin-Token", "controller-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\":\"image/webp\",\"contentLength\":1024,\"lowContentLength\":256}"))
                .andExpect(result -> assertThat(result.getResolvedException()).isSameAs(failure));
        verify(bookCoverUploadService, times(1)).issueUploadUrl(request);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"contentLength\":1024}",
            "{\"contentType\":null,\"contentLength\":1024}",
            "{\"contentType\":\"image/webp\"}",
            "{\"contentType\":\"image/webp\",\"contentLength\":null,\"lowContentLength\":256}",
            "{\"contentType\":\"image/webp\",\"contentLength\":1024}",
            "{\"contentType\":\"image/webp\",\"contentLength\":1024,\"lowContentLength\":null}"
    })
    @DisplayName("표지 업로드 필수 필드가 누락되거나 null이면 서비스를 호출하지 않는다")
    void rejectMissingOrNullUploadField(String content) throws Exception {
        mockMvc.perform(post("/api/admin/book-covers/upload-url")
                        .header("X-Admin-Token", "controller-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException()));

        verifyNoInteractions(bookCoverUploadService);
    }
}
