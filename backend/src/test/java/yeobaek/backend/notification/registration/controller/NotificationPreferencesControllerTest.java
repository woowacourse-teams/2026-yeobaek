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
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.dto.NotificationPreferencesResponse;
import yeobaek.backend.notification.registration.dto.NotificationPreferencesUpdateRequest;
import yeobaek.backend.notification.registration.service.NotificationPreferencesService;
import yeobaek.backend.support.ControllerTest;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.NotFoundException;

@WebMvcTest(NotificationPreferencesController.class)
class NotificationPreferencesControllerTest extends ControllerTest {
    private static final String FID = "c" + "a".repeat(21);

    @MockitoBean
    private NotificationPreferencesService preferencesService;

    @Test
    @DisplayName("회원과 FID로 조회한 전체·유형별 설정을 반환한다")
    void findPreferences() throws Exception {
        givenValidMember(7L);
        given(preferencesService.find(7L, new FirebaseInstallationId(FID)))
                .willReturn(new NotificationPreferencesResponse(false, true, false, true));

        mockMvc.perform(get("/api/notifications/preferences").header("X-Member-Id", 7).param("fid", FID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.clubJoin").value(true))
                .andExpect(jsonPath("$.clubComment").value(false))
                .andExpect(jsonPath("$.publicRoomComment").value(true));

        verify(preferencesService, times(1)).find(7L, new FirebaseInstallationId(FID));
    }

    @Test
    @DisplayName("전달한 false와 생략한 항목을 구분해 서비스에 전달하고 204를 반환한다")
    void updateOnlyProvidedSettings() throws Exception {
        givenValidMember(7L);

        mockMvc.perform(patch("/api/notifications/preferences").header("X-Member-Id", 7).param("fid", FID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":false,"clubComment":false}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(preferencesService, times(1)).update(7L, new FirebaseInstallationId(FID),
                new NotificationPreferencesUpdateRequest(false, null, false, null));
    }

    @Test
    @DisplayName("모든 항목을 전달한 변경 요청을 그대로 전달한다")
    void updateAllSettings() throws Exception {
        givenValidMember(7L);

        mockMvc.perform(patch("/api/notifications/preferences").header("X-Member-Id", 7).param("fid", FID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":true,"clubJoin":false,"clubComment":true,"publicRoomComment":false}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(preferencesService, times(1)).update(7L, new FirebaseInstallationId(FID),
                new NotificationPreferencesUpdateRequest(true, false, true, false));
    }

    @Test
    @DisplayName("FID 쿼리 파라미터가 없으면 서비스를 호출하지 않는다")
    void rejectMissingFid() throws Exception {
        givenValidMember(7L);

        mockMvc.perform(get("/api/notifications/preferences").header("X-Member-Id", 7))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(preferencesService);
    }

    @Test
    @DisplayName("FID 형식이 잘못된 변경 요청은 서비스를 호출하지 않는다")
    void rejectInvalidFid() throws Exception {
        givenValidMember(7L);

        mockMvc.perform(patch("/api/notifications/preferences").header("X-Member-Id", 7).param("fid", "invalid")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(preferencesService);
    }

    @Test
    @DisplayName("등록 정보 조회의 서비스 예외를 같은 인스턴스로 전파한다")
    void propagateServiceException() throws Exception {
        givenValidMember(7L);
        var exception = new NotFoundException(ErrorCode.NOTIFICATION_REGISTRATION_NOT_FOUND, "등록 정보가 없어요.");
        given(preferencesService.find(7L, new FirebaseInstallationId(FID))).willThrow(exception);

        var result = mockMvc.perform(get("/api/notifications/preferences").header("X-Member-Id", 7).param("fid", FID))
                .andReturn();

        assertSame(exception, result.getResolvedException(), "서비스 예외를 그대로 전파해야 해요.");
        verify(preferencesService, times(1)).find(7L, new FirebaseInstallationId(FID));
    }
}
