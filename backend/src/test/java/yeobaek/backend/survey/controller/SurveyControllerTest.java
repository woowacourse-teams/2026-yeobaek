package yeobaek.backend.survey.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.support.ControllerTest;
import yeobaek.backend.survey.dto.SurveyRequirementResponse;
import yeobaek.backend.survey.service.SurveyService;

@WebMvcTest(SurveyController.class)
class SurveyControllerTest extends ControllerTest {

    @MockitoBean
    private SurveyService surveyService;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("요청 회원의 설문 필요 여부를 JSON으로 반환한다")
    void getSurveyRequirement(boolean surveyRequired) throws Exception {
        givenValidMember(7L);
        given(surveyService.getSurveyRequirement(7L)).willReturn(new SurveyRequirementResponse(surveyRequired));

        mockMvc.perform(get("/api/members/me/survey-requirement")
                        .header("X-Member-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.surveyRequired").value(surveyRequired));

        verify(surveyService, times(1)).getSurveyRequirement(7L);
    }

    @Test
    @DisplayName("서비스 예외를 변경하지 않고 전파한다")
    void propagateServiceException() throws Exception {
        givenValidMember(7L);
        var serviceException = new IllegalStateException("설문 대상 조회 실패");
        given(surveyService.getSurveyRequirement(7L)).willThrow(serviceException);

        var result = mockMvc.perform(get("/api/members/me/survey-requirement")
                        .header("X-Member-Id", "7"))
                .andReturn();

        assertSame(serviceException, result.getResolvedException(),
                "컨트롤러는 서비스 예외 인스턴스를 변경하지 않아야 한다");
        verify(surveyService, times(1)).getSurveyRequirement(7L);
    }
}
