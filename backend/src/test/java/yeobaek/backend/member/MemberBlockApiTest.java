package yeobaek.backend.member;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.IntegrationTest;

class MemberBlockApiTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("차단과 해제를 중복 요청해도 API가 멱등하게 동작한다")
    void blockAndUnblockIdempotently() throws Exception {
        Member blocker = memberRepository.save(new Member(new Nickname("민서")));
        Member blocked = memberRepository.save(new Member(new Nickname("지수")));

        mockMvc.perform(put("/api/members/me/blocks/{memberId}", blocked.getId())
                        .header("X-Member-Id", blocker.getId()))
                .andExpect(status().isNoContent());
        mockMvc.perform(put("/api/members/me/blocks/{memberId}", blocked.getId())
                        .header("X-Member-Id", blocker.getId()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/members/me/blocks")
                        .header("X-Member-Id", blocker.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blockedMembers.length()").value(1))
                .andExpect(jsonPath("$.blockedMembers[0].memberId").value(blocked.getId()))
                .andExpect(jsonPath("$.blockedMembers[0].nickname").value("지수"));

        mockMvc.perform(delete("/api/members/me/blocks/{memberId}", blocked.getId())
                        .header("X-Member-Id", blocker.getId()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/members/me/blocks/{memberId}", blocked.getId())
                        .header("X-Member-Id", blocker.getId()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/members/me/blocks")
                        .header("X-Member-Id", blocker.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blockedMembers").isEmpty());
    }

}
