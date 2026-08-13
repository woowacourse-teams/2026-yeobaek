package yeobaek.backend.member.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.member.dto.MemberCreateResponse;
import yeobaek.backend.member.service.MemberService;
import yeobaek.backend.support.ControllerTest;

@WebMvcTest(MemberController.class)
class MemberControllerTest extends ControllerTest {

    @MockitoBean
    private MemberService memberService;

    @Test
    @DisplayName("회원 생성에 성공하면 201과 발급된 ID를 응답한다")
    void createMember() throws Exception {
        given(memberService.create(anyString())).willReturn(new MemberCreateResponse(1L, "민서"));

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\": \"민서\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value(1))
                .andExpect(jsonPath("$.nickname").value("민서"))
                .andDo(document("member-create", resource(ResourceSnippetParameters.builder()
                        .tag("회원")
                        .summary("회원 생성")
                        .description("닉네임 입력만으로 회원을 생성하고 ID를 발급한다. 헤더 불필요(최초 진입).")
                        .requestFields(
                                PayloadDocumentation.fieldWithPath("nickname").description("닉네임 (1~20자, 공백만은 불가, 중복 불가)"))
                        .responseFields(
                                PayloadDocumentation.fieldWithPath("memberId").description("발급된 회원 ID"),
                                PayloadDocumentation.fieldWithPath("nickname").description("닉네임"))
                        .build())));
    }

    @Test
    @DisplayName("닉네임이 유효하지 않으면 400과 메시지를 응답한다")
    void createMemberWithInvalidNickname() throws Exception {
        given(memberService.create(anyString())).willThrow(new IllegalArgumentException("닉네임은 공백이 아닌 1~20자여야 합니다."));

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\": \" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("닉네임은 공백이 아닌 1~20자여야 합니다."));
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임이면 400과 메시지를 응답한다")
    void createMemberWithDuplicateNickname() throws Exception {
        given(memberService.create(anyString())).willThrow(new IllegalArgumentException("이미 사용 중인 닉네임입니다."));

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\": \"민서\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 닉네임입니다."));
    }
}
