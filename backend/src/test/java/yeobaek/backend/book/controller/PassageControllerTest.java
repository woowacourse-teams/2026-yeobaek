package yeobaek.backend.book.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.book.dto.PassageResponse;
import yeobaek.backend.book.dto.PassagesResponse;
import yeobaek.backend.book.service.PassageService;
import yeobaek.backend.support.ControllerTest;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;

@WebMvcTest(PassageController.class)
class PassageControllerTest extends ControllerTest {

    @MockitoBean
    private PassageService passageService;

    @Test
    @DisplayName("본문 범위를 조회한다")
    void findPassages() throws Exception {
        givenValidMember(1L);
        given(passageService.findPassages(1L, 1L, 42, 43)).willReturn(new PassagesResponse(List.of(
                new PassageResponse(1042L, 42, 2L, "새침하게 흐린 품이 눈이 올 듯하더니...", 3),
                new PassageResponse(1043L, 43, 2L, "얼다가 만 비가 추적추적 내리었다.", 0))));

        mockMvc.perform(get("/api/clubs/{clubId}/passages?from=42&to=43", 1L).header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passages[0].sequence").value(42))
                .andExpect(jsonPath("$.passages[0].commentCount").value(3));
    }

    @Test
    @DisplayName("모임 미소속 회원의 조회는 403을 응답한다")
    void rejectOutsider() throws Exception {
        givenValidMember(1L);
        given(passageService.findPassages(anyLong(), anyLong(), anyInt(), anyInt()))
                .willThrow(new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER));

        mockMvc.perform(get("/api/clubs/1/passages?from=1&to=3").header("X-Member-Id", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("모임에 참여하지 않은 회원입니다."));
    }

    @Test
    @DisplayName("허용 범위를 넘는 요청은 400을 응답한다")
    void rejectTooWideRange() throws Exception {
        givenValidMember(1L);
        given(passageService.findPassages(anyLong(), anyLong(), anyInt(), anyInt()))
                .willThrow(new IllegalArgumentException("본문은 한 번에 최대 100개까지 조회할 수 있습니다."));

        mockMvc.perform(get("/api/clubs/1/passages?from=1&to=101").header("X-Member-Id", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("범위 파라미터가 없으면 400을 응답한다")
    void missingRangeParameter() throws Exception {
        givenValidMember(1L);

        mockMvc.perform(get("/api/clubs/1/passages?from=1").header("X-Member-Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("필수 파라미터가 없습니다: to"));
    }
}
