package yeobaek.backend.club.controller;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.book.domain.BookStatus;
import yeobaek.backend.club.dto.ClubBookResponse;
import yeobaek.backend.club.dto.LastReadingResponse;
import yeobaek.backend.club.dto.ProgressResponse;
import yeobaek.backend.club.service.ProgressService;
import yeobaek.backend.support.ControllerTest;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.NotFoundException;

@WebMvcTest(ProgressController.class)
class ProgressControllerTest extends ControllerTest {

    @MockitoBean
    private ProgressService progressService;

    @Test
    @DisplayName("진도 갱신 요청을 서비스에 전달하고 전체 응답 계약을 반환한다")
    void updateProgress() throws Exception {
        givenValidMember(1L);
        var lastReadAt = LocalDateTime.of(2026, 8, 5, 14, 30);
        var response = new ProgressResponse(42, 13, lastReadAt);
        given(progressService.updateProgress(1L, 7L, 1042L)).willReturn(response);

        mockMvc.perform(put("/api/clubs/{clubId}/progress", 7L)
                        .header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"passageId":1042}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.lastReadPassageSequence").value(42))
                .andExpect(jsonPath("$.progressRate").value(13))
                .andExpect(jsonPath("$.lastReadAt").value("2026-08-05T14:30:00"));

        verify(progressService, times(1)).updateProgress(1L, 7L, 1042L);
    }

    @Test
    @DisplayName("마지막 읽기 기록이 있으면 전체 응답 계약을 반환한다")
    void findLastReading() throws Exception {
        givenValidMember(2L);
        var lastReadAt = LocalDateTime.of(2026, 8, 6, 9, 15);
        var response = new LastReadingResponse(
                7L,
                "교환독서 1기",
                new ClubBookResponse(5L, "운수 좋은 날", List.of("현진건"), null, 312, BookStatus.DELETED),
                42,
                13,
                lastReadAt);
        given(progressService.findLastReading(2L)).willReturn(Optional.of(response));

        mockMvc.perform(get("/api/members/me/last-reading")
                        .header("X-Member-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.clubId").value(7))
                .andExpect(jsonPath("$.clubName").value("교환독서 1기"))
                .andExpect(jsonPath("$.book").isMap())
                .andExpect(jsonPath("$.book.bookId").value(5))
                .andExpect(jsonPath("$.book.title").value("운수 좋은 날"))
                .andExpect(jsonPath("$.book.authors").isArray())
                .andExpect(jsonPath("$.book.authors.length()").value(1))
                .andExpect(jsonPath("$.book.authors[0]").value("현진건"))
                .andExpect(jsonPath("$.book.coverImageUrl").value((Object) null))
                .andExpect(jsonPath("$.book.passageCount").value(312))
                .andExpect(jsonPath("$.book.status").value("DELETED"))
                .andExpect(jsonPath("$.lastReadPassageSequence").value(42))
                .andExpect(jsonPath("$.progressRate").value(13))
                .andExpect(jsonPath("$.lastReadAt").value("2026-08-06T09:15:00"));

        verify(progressService, times(1)).findLastReading(2L);
    }

    @Test
    @DisplayName("마지막 읽기 기록이 없으면 빈 204 응답을 반환한다")
    void findLastReadingEmpty() throws Exception {
        givenValidMember(3L);
        given(progressService.findLastReading(3L)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/members/me/last-reading")
                        .header("X-Member-Id", "3"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(progressService, times(1)).findLastReading(3L);
    }

    @Test
    @DisplayName("진도 갱신 본문이 없으면 서비스를 호출하지 않는다")
    void rejectMissingUpdateBody() throws Exception {
        givenValidMember(4L);

        mockMvc.perform(put("/api/clubs/{clubId}/progress", 7L)
                        .header("X-Member-Id", "4")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertInstanceOf(
                        HttpMessageNotReadableException.class,
                        result.getResolvedException()));

        verifyNoInteractions(progressService);
    }

    @Test
    @DisplayName("서비스 예외를 변경하지 않고 전파한다")
    void propagateServiceException() throws Exception {
        givenValidMember(5L);
        var serviceException = new NotFoundException(ErrorCode.PASSAGE_NOT_FOUND);
        given(progressService.updateProgress(5L, 7L, 999L)).willThrow(serviceException);

        var result = mockMvc.perform(put("/api/clubs/{clubId}/progress", 7L)
                        .header("X-Member-Id", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"passageId":999}
                                """))
                .andReturn();

        assertSame(serviceException, result.getResolvedException(),
                "컨트롤러는 서비스 예외 인스턴스를 변경하지 않아야 한다");
        verify(progressService, times(1)).updateProgress(5L, 7L, 999L);
    }
}
