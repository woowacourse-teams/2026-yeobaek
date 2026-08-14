package yeobaek.backend.club.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    @DisplayName("홈에서 마지막으로 읽던 책을 조회한다")
    void findLastReading() throws Exception {
        givenValidMember(1L);
        given(progressService.findLastReading(1L)).willReturn(Optional.of(new LastReadingResponse(
                1L, "교환독서 1기", new ClubBookResponse(1L, "운수 좋은 날", List.of("현진건"), 312),
                42, 13, LocalDateTime.of(2026, 8, 5, 14, 30))));

        mockMvc.perform(get("/api/members/me/last-reading").header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubId").value(1))
                .andExpect(jsonPath("$.progressRate").value(13));
    }

    @Test
    @DisplayName("읽기 기록이 없으면 204를 응답한다")
    void findLastReadingEmpty() throws Exception {
        givenValidMember(1L);
        given(progressService.findLastReading(1L)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/members/me/last-reading").header("X-Member-Id", "1"))
                .andExpect(status().isNoContent());
    }
}
