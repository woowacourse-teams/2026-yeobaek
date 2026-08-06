package watson.backend.club.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import watson.backend.club.dto.ClubBookResponse;
import watson.backend.club.dto.LastReadingResponse;
import watson.backend.club.dto.ProgressResponse;
import watson.backend.club.service.ProgressService;
import watson.backend.support.ControllerTest;

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

        mockMvc.perform(put("/api/clubs/1/progress")
                        .header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passageId\": 1042}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastReadPassageSequence").value(42))
                .andExpect(jsonPath("$.progressRate").value(13))
                .andDo(document("progress-update", resource(ResourceSnippetParameters.builder()
                        .tag("읽기")
                        .summary("진도 갱신 (최근 열람 보고)")
                        .description("항상 마지막 열람 본문으로 덮어쓴다. 앞부분 재열람 시 진도율은 후퇴한다 (PRD 3.4).")
                        .requestHeaders(headerWithName("X-Member-Id").description("회원 ID"))
                        .requestFields(PayloadDocumentation.fieldWithPath("passageId").description("열람한 본문 ID"))
                        .responseFields(
                                PayloadDocumentation.fieldWithPath("lastReadPassageSequence").description("최근 열람 본문의 전체 순서"),
                                PayloadDocumentation.fieldWithPath("progressRate").description("진도율 (0~100 정수, 반올림)"),
                                PayloadDocumentation.fieldWithPath("lastReadAt").description("마지막으로 읽은 시간"))
                        .build())));
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
                .andExpect(jsonPath("$.progressRate").value(13))
                .andDo(document("last-reading", resource(ResourceSnippetParameters.builder()
                        .tag("읽기")
                        .summary("홈 — 마지막으로 읽던 책 조회")
                        .description("전 모임 중 마지막으로 읽은 시간이 가장 최근인 모임. 읽기 기록이 없으면 204.")
                        .requestHeaders(headerWithName("X-Member-Id").description("회원 ID"))
                        .responseFields(
                                PayloadDocumentation.fieldWithPath("clubId").description("모임 ID"),
                                PayloadDocumentation.fieldWithPath("clubName").description("모임 이름"),
                                PayloadDocumentation.fieldWithPath("book.bookId").description("도서 ID"),
                                PayloadDocumentation.fieldWithPath("book.title").description("도서 제목"),
                                PayloadDocumentation.fieldWithPath("book.authors[]").description("작가 이름 목록"),
                                PayloadDocumentation.fieldWithPath("book.passageCount").description("본문 개수"),
                                PayloadDocumentation.fieldWithPath("lastReadPassageSequence").description("최근 열람 본문의 전체 순서"),
                                PayloadDocumentation.fieldWithPath("progressRate").description("진도율 (0~100 정수, 반올림)"),
                                PayloadDocumentation.fieldWithPath("lastReadAt").description("마지막으로 읽은 시간"))
                        .build())));
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
