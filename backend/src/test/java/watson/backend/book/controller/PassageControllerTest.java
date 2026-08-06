package watson.backend.book.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import watson.backend.book.dto.PassageResponse;
import watson.backend.book.dto.PassagesResponse;
import watson.backend.book.service.PassageService;
import watson.backend.support.ControllerTest;
import watson.backend.support.ForbiddenException;

@WebMvcTest(PassageController.class)
class PassageControllerTest extends ControllerTest {

    @MockitoBean
    private PassageService passageService;

    @Test
    @DisplayName("본문 범위를 조회한다")
    void findPassages() throws Exception {
        givenValidMember(1L);
        given(passageService.findPassages(1L, 1L, 42, 43)).willReturn(new PassagesResponse(List.of(
                new PassageResponse(1042L, 42, 2L, "새침하게 흐린 품이 눈이 올 듯하더니...", null, 3),
                new PassageResponse(1043L, 43, 2L, null, "https://example.com/1.png", 0))));

        mockMvc.perform(get("/api/clubs/1/passages?from=42&to=43").header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passages[0].sequence").value(42))
                .andExpect(jsonPath("$.passages[0].commentCount").value(3))
                .andDo(document("passage-range", resource(ResourceSnippetParameters.builder()
                        .tag("읽기")
                        .summary("본문 범위 조회")
                        .description("전체 순서 기준 범위(양 끝 포함)의 본문을 조회한다. 범위는 최대 100개, 모임 미소속은 403.")
                        .requestHeaders(headerWithName("X-Member-Id").description("회원 ID"))
                        .queryParameters(
                                parameterWithName("from").description("시작 본문의 전체 순서 (1 이상)"),
                                parameterWithName("to").description("끝 본문의 전체 순서 (양 끝 포함, to-from+1 ≤ 100)"))
                        .responseFields(
                                PayloadDocumentation.fieldWithPath("passages[].passageId").description("본문 ID"),
                                PayloadDocumentation.fieldWithPath("passages[].sequence").description("책 전체 기준 순서"),
                                PayloadDocumentation.fieldWithPath("passages[].chapterId").description("소속 목차 ID"),
                                PayloadDocumentation.fieldWithPath("passages[].content").description("본문 텍스트 (이미지 문단이면 null)").optional(),
                                PayloadDocumentation.fieldWithPath("passages[].imageUrl").description("이미지 URL (텍스트 문단이면 null)").optional(),
                                PayloadDocumentation.fieldWithPath("passages[].commentCount").description("이 모임의 댓글 수"))
                        .build())));
    }

    @Test
    @DisplayName("모임 미소속 회원의 조회는 403을 응답한다")
    void rejectOutsider() throws Exception {
        givenValidMember(1L);
        given(passageService.findPassages(anyLong(), anyLong(), anyInt(), anyInt()))
                .willThrow(new ForbiddenException("모임에 참여하지 않은 회원입니다."));

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
