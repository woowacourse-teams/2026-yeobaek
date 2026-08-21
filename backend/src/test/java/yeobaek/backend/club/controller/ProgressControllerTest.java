package yeobaek.backend.club.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.club.dto.ClubBookResponse;
import yeobaek.backend.club.dto.LastReadingResponse;
import yeobaek.backend.club.dto.ProgressResponse;
import yeobaek.backend.club.service.ProgressService;
import yeobaek.backend.support.ControllerTest;

@WebMvcTest(ProgressController.class)
class ProgressControllerTest extends ControllerTest {

    @MockitoBean
    private ProgressService progressService;

    @Test
    @DisplayName("진도를 갱신하면 최근 열람 순서와 진도율을 응답한다")
    void updateProgress() throws Exception {
        givenValidMember(1L);
        given(progressService.updateProgress(anyLong(), anyLong(), anyLong()))
                .willReturn(new ProgressResponse(42, 13, LocalDateTime.of(2026, 8, 5, 14, 30)));

        mockMvc.perform(put("/api/clubs/{clubId}/progress", 1L)
                        .header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passageId\": 1042}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastReadPassageSequence").value(42))
                .andExpect(jsonPath("$.progressRate").value(13));
    }

    @Test
    @DisplayName("마지막 읽기 기록이 있으면 전체 응답 계약을 반환한다")
    void findLastReading() throws Exception {
        givenValidMember(1L);
        var lastReadAt = LocalDateTime.of(2026, 8, 5, 14, 30);
        var response = new LastReadingResponse(
                1L, "교환독서 1기", new ClubBookResponse(1L, "운수 좋은 날", List.of("현진건"), 312),
                42, 13, lastReadAt);
        given(progressService.findLastReading(1L)).willReturn(Optional.of(response));

        mockMvc.perform(get("/api/members/me/last-reading").header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.clubId").value(1))
                .andExpect(jsonPath("$.clubName").value("교환독서 1기"))
                .andExpect(jsonPath("$.book").isMap())
                .andExpect(jsonPath("$.book.bookId").value(1))
                .andExpect(jsonPath("$.book.title").value("운수 좋은 날"))
                .andExpect(jsonPath("$.book.authors").isArray())
                .andExpect(jsonPath("$.book.authors.length()").value(1))
                .andExpect(jsonPath("$.book.authors[0]").value("현진건"))
                .andExpect(jsonPath("$.book.passageCount").value(312))
                .andExpect(jsonPath("$.lastReadPassageSequence").value(42))
                .andExpect(jsonPath("$.progressRate").value(13))
                .andExpect(jsonPath("$.lastReadAt").value("2026-08-05T14:30:00"));

        verify(progressService, times(1)).findLastReading(1L);
    }

    @Test
    @DisplayName("마지막 읽기 기록이 없으면 빈 204 응답을 반환한다")
    void findLastReadingEmpty() throws Exception {
        givenValidMember(1L);
        given(progressService.findLastReading(1L)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/members/me/last-reading").header("X-Member-Id", "1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(progressService, times(1)).findLastReading(1L);
    }
}
