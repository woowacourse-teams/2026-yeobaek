package yeobaek.backend.book.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.MissingServletRequestParameterException;
import yeobaek.backend.book.dto.PassageResponse;
import yeobaek.backend.book.dto.PassagesResponse;
import yeobaek.backend.book.dto.SentenceResponse;
import yeobaek.backend.book.service.PassageService;
import yeobaek.backend.support.ControllerTest;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;

@WebMvcTest(PassageController.class)
class PassageControllerTest extends ControllerTest {

    @MockitoBean
    private PassageService passageService;

    @Test
    @DisplayName("본문 범위 요청을 서비스에 전달하고 목록 전체 계약을 반환한다")
    void findPassages() throws Exception {
        givenValidMember(1L);
        var response = new PassagesResponse(List.of(
                new PassageResponse(1042L, 42, 2L, List.of(
                        new SentenceResponse(5012L, 1, "첫 번째 문장. ", 3),
                        new SentenceResponse(5013L, 2, "두 번째 문장.", 0))),
                new PassageResponse(1043L, 43, 2L, List.of(
                        new SentenceResponse(5014L, 1, "세 번째 문장.", 0)))));
        given(passageService.findPassages(1L, 7L, 42, 43)).willReturn(response);

        mockMvc.perform(get("/api/clubs/{clubId}/passages", 7L)
                        .header("X-Member-Id", "1")
                        .param("from", "42")
                        .param("to", "43"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.passages").isArray())
                .andExpect(jsonPath("$.passages.length()").value(2))
                .andExpect(jsonPath("$.passages[0].passageId").value(1042))
                .andExpect(jsonPath("$.passages[0].sequence").value(42))
                .andExpect(jsonPath("$.passages[0].chapterId").value(2))
                .andExpect(jsonPath("$.passages[0].sentences").isArray())
                .andExpect(jsonPath("$.passages[0].sentences.length()").value(2))
                .andExpect(jsonPath("$.passages[0].sentences[0].sentenceId").value(5012))
                .andExpect(jsonPath("$.passages[0].sentences[0].sequence").value(1))
                .andExpect(jsonPath("$.passages[0].sentences[0].content").value("첫 번째 문장. "))
                .andExpect(jsonPath("$.passages[0].sentences[0].commentCount").value(3))
                .andExpect(jsonPath("$.passages[0].sentences[1].sentenceId").value(5013))
                .andExpect(jsonPath("$.passages[0].sentences[1].sequence").value(2))
                .andExpect(jsonPath("$.passages[0].sentences[1].content").value("두 번째 문장."))
                .andExpect(jsonPath("$.passages[0].sentences[1].commentCount").value(0))
                .andExpect(jsonPath("$.passages[1].passageId").value(1043))
                .andExpect(jsonPath("$.passages[1].sequence").value(43))
                .andExpect(jsonPath("$.passages[1].chapterId").value(2))
                .andExpect(jsonPath("$.passages[1].sentences").isArray())
                .andExpect(jsonPath("$.passages[1].sentences.length()").value(1))
                .andExpect(jsonPath("$.passages[1].sentences[0].sentenceId").value(5014))
                .andExpect(jsonPath("$.passages[1].sentences[0].sequence").value(1))
                .andExpect(jsonPath("$.passages[1].sentences[0].content").value("세 번째 문장."))
                .andExpect(jsonPath("$.passages[1].sentences[0].commentCount").value(0));

        verify(passageService, times(1)).findPassages(1L, 7L, 42, 43);
    }

    @Test
    @DisplayName("필수 to 파라미터가 없으면 서비스를 호출하지 않는다")
    void rejectMissingToParameter() throws Exception {
        givenValidMember(2L);

        mockMvc.perform(get("/api/clubs/{clubId}/passages", 7L)
                        .header("X-Member-Id", "2")
                        .param("from", "42"))
                .andExpect(result -> {
                    var exception = assertInstanceOf(
                            MissingServletRequestParameterException.class,
                            result.getResolvedException());
                    assertEquals("to", exception.getParameterName(),
                            "누락된 필수 파라미터를 식별해야 한다");
                });

        verifyNoInteractions(passageService);
    }

    @Test
    @DisplayName("서비스 예외를 변경하지 않고 전파한다")
    void propagateServiceException() throws Exception {
        givenValidMember(3L);
        var serviceException = new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER);
        given(passageService.findPassages(3L, 7L, 1, 20)).willThrow(serviceException);

        var result = mockMvc.perform(get("/api/clubs/{clubId}/passages", 7L)
                        .header("X-Member-Id", "3")
                        .param("from", "1")
                        .param("to", "20"))
                .andReturn();

        assertSame(serviceException, result.getResolvedException(),
                "컨트롤러는 서비스 예외 인스턴스를 변경하지 않아야 한다");
        verify(passageService, times(1)).findPassages(3L, 7L, 1, 20);
    }
}
