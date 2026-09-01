package yeobaek.backend.member.controller;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.member.dto.MemberCreateResponse;
import yeobaek.backend.member.service.MemberService;
import yeobaek.backend.support.ControllerTest;
import yeobaek.backend.support.analytics.AnalyticsEvent;
import yeobaek.backend.support.analytics.AnalyticsTracker;

@WebMvcTest(MemberController.class)
class MemberControllerTest extends ControllerTest {

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private AnalyticsTracker analyticsTracker;

    @Test
    @DisplayName("회원 생성 요청을 서비스에 전달하고 전체 응답 계약을 반환한다")
    void createMember() throws Exception {
        var response = new MemberCreateResponse(7L, "민서");
        given(memberService.create("민서")).willReturn(response);

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"민서"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.memberId").value(7))
                .andExpect(jsonPath("$.nickname").value("민서"));

        verify(memberService, times(1)).create("민서");
        verify(analyticsTracker, times(1)).track(7L, AnalyticsEvent.memberCreated());
    }

    @Test
    @DisplayName("요청 본문이 없으면 서비스를 호출하지 않는다")
    void rejectMissingBody() throws Exception {
        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertInstanceOf(
                        HttpMessageNotReadableException.class,
                        result.getResolvedException()));

        verifyNoInteractions(memberService);
    }

    @Test
    @DisplayName("서비스 예외를 변경하지 않고 전파한다")
    void propagateServiceException() throws Exception {
        var serviceException = new IllegalArgumentException("회원 생성 실패");
        given(memberService.create("중복 닉네임")).willThrow(serviceException);

        var result = mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"중복 닉네임"}
                                """))
                .andReturn();

        assertSame(serviceException, result.getResolvedException(),
                "컨트롤러는 서비스 예외 인스턴스를 변경하지 않아야 한다");
        verify(memberService, times(1)).create("중복 닉네임");
        verifyNoInteractions(analyticsTracker);
    }
}
