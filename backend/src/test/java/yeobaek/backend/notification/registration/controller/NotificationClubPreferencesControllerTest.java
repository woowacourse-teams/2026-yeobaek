package yeobaek.backend.notification.registration.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.dto.NotificationClubPreferenceResponse;
import yeobaek.backend.notification.registration.dto.NotificationClubPreferenceUpdateRequest;
import yeobaek.backend.notification.registration.service.NotificationClubPreferencesService;
import yeobaek.backend.support.ControllerTest;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;

@WebMvcTest(NotificationClubPreferencesController.class)
class NotificationClubPreferencesControllerTest extends ControllerTest {
    private static final String FID = "c" + "a".repeat(21);
    private static final String URL = "/api/clubs/5/notifications/preferences";

    @MockitoBean
    private NotificationClubPreferencesService preferencesService;

    @Test
    @DisplayName("인증 회원과 FID, 모임 ID로 조회한 설정을 반환해요")
    void findPreference() throws Exception {
        givenValidMember(7L);
        given(preferencesService.find(7L, new FirebaseInstallationId(FID), 5L))
                .willReturn(new NotificationClubPreferenceResponse(false));

        mockMvc.perform(get(URL).header("X-Member-Id", 7).param("fid", FID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.enabled").value(false));

        verify(preferencesService, times(1)).find(7L, new FirebaseInstallationId(FID), 5L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("ON과 OFF 변경 요청을 전달하고 빈 204 응답을 반환해요")
    void updatePreference(boolean enabled) throws Exception {
        givenValidMember(7L);

        mockMvc.perform(patch(URL).header("X-Member-Id", 7).param("fid", FID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":" + enabled + "}"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(preferencesService, times(1)).update(7L, new FirebaseInstallationId(FID), 5L,
                new NotificationClubPreferenceUpdateRequest(enabled));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"enabled\":null}"})
    @DisplayName("enabled 누락과 null은 서비스 호출 전에 거부해요")
    void rejectMissingEnabled(String body) throws Exception {
        givenValidMember(7L);

        mockMvc.perform(patch(URL).header("X-Member-Id", 7).param("fid", FID)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(preferencesService);
    }

    @Test
    @DisplayName("FID가 없으면 서비스 호출 전에 거부해요")
    void rejectMissingFid() throws Exception {
        givenValidMember(7L);

        mockMvc.perform(get(URL).header("X-Member-Id", 7))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(preferencesService);
    }

    @Test
    @DisplayName("FID 형식이 잘못되면 서비스 호출 전에 거부해요")
    void rejectInvalidFid() throws Exception {
        givenValidMember(7L);

        mockMvc.perform(patch(URL).header("X-Member-Id", 7).param("fid", "invalid")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(preferencesService);
    }

    @Test
    @DisplayName("모임 참여 검증의 서비스 예외를 같은 인스턴스로 전달해요")
    void propagateServiceException() throws Exception {
        givenValidMember(7L);
        var exception = new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER, "모임에 참여 중인 회원이 아니에요.");
        given(preferencesService.find(7L, new FirebaseInstallationId(FID), 5L)).willThrow(exception);

        var result = mockMvc.perform(get(URL).header("X-Member-Id", 7).param("fid", FID)).andReturn();

        assertSame(exception, result.getResolvedException(), "서비스 예외를 그대로 전달해야 해요.");
        verify(preferencesService, times(1)).find(7L, new FirebaseInstallationId(FID), 5L);
    }
}
