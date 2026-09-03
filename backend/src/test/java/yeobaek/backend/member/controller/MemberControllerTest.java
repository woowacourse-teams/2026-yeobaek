package yeobaek.backend.member.controller;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.member.dto.BlockedMemberResponse;
import yeobaek.backend.member.dto.BlockedMembersResponse;
import yeobaek.backend.member.dto.MemberCreateResponse;
import yeobaek.backend.member.service.MemberBlockService;
import yeobaek.backend.member.service.MemberService;
import yeobaek.backend.support.ControllerTest;
import yeobaek.backend.support.analytics.AnalyticsEvent;
import yeobaek.backend.support.analytics.AnalyticsTracker;

@WebMvcTest(MemberController.class)
class MemberControllerTest extends ControllerTest {

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private MemberBlockService memberBlockService;

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
    @DisplayName("차단 목록 전체 응답 계약을 반환한다")
    void findBlockedMembers() throws Exception {
        givenValidMember(1L);
        given(memberBlockService.findBlockedMembers(1L)).willReturn(new BlockedMembersResponse(List.of(
                new BlockedMemberResponse(2L, "가람"),
                new BlockedMemberResponse(3L, "지수"))));

        mockMvc.perform(get("/api/members/me/blocks")
                        .header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.blockedMembers").isArray())
                .andExpect(jsonPath("$.blockedMembers.length()").value(2))
                .andExpect(jsonPath("$.blockedMembers[0].memberId").value(2))
                .andExpect(jsonPath("$.blockedMembers[0].nickname").value("가람"))
                .andExpect(jsonPath("$.blockedMembers[1].memberId").value(3))
                .andExpect(jsonPath("$.blockedMembers[1].nickname").value("지수"));

        verify(memberBlockService).findBlockedMembers(1L);
    }

    @Test
    @DisplayName("차단 요청을 서비스에 전달하고 본문 없이 응답한다")
    void block() throws Exception {
        givenValidMember(1L);

        mockMvc.perform(put("/api/members/me/blocks/{memberId}", 2L)
                        .header("X-Member-Id", "1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(memberBlockService).block(1L, 2L);
    }

    @Test
    @DisplayName("차단 해제 요청을 서비스에 전달하고 본문 없이 응답한다")
    void unblock() throws Exception {
        givenValidMember(1L);

        mockMvc.perform(delete("/api/members/me/blocks/{memberId}", 2L)
                        .header("X-Member-Id", "1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(memberBlockService).unblock(1L, 2L);
    }

    @Test
    @DisplayName("계정 삭제 요청을 서비스에 전달하고 빈 204 응답을 반환한다")
    void deleteMember() throws Exception {
        givenValidMember(7L);

        mockMvc.perform(delete("/api/members/me")
                        .header("X-Member-Id", "7"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(memberService, times(1)).delete(7L);
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
