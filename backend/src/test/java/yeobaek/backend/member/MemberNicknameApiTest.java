package yeobaek.backend.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.IntegrationTest;

class MemberNicknameApiTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private MemberRepository memberRepository;

    @Test
    @DisplayName("본인의 닉네임만 변경하며 현재 닉네임과 같은 값으로 요청해도 성공한다")
    void updateNicknameIdempotently() throws Exception {
        Member member = memberRepository.save(new Member(new Nickname("기존닉네임")));
        Member other = memberRepository.save(new Member(new Nickname("다른회원")));

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(put("/api/members/me/nickname")
                            .header("X-Member-Id", member.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nickname":"새닉네임"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.memberId").value(member.getId()))
                    .andExpect(jsonPath("$.nickname").value("새닉네임"));
        }

        assertThat(memberRepository.findById(member.getId()).orElseThrow().getNickname()).isEqualTo("새닉네임");
        assertThat(memberRepository.findById(other.getId()).orElseThrow().getNickname()).isEqualTo("다른회원");
    }

    @Test
    @DisplayName("다른 회원이 사용 중인 닉네임으로 변경을 요청하면 오류 응답을 반환하고 기존 닉네임을 유지한다")
    void rejectDuplicateNickname() throws Exception {
        Member member = memberRepository.save(new Member(new Nickname("기존닉네임")));
        memberRepository.save(new Member(new Nickname("사용중")));

        mockMvc.perform(put("/api/members/me/nickname")
                        .header("X-Member-Id", member.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"사용중"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 닉네임입니다."));

        assertThat(memberRepository.findById(member.getId()).orElseThrow().getNickname()).isEqualTo("기존닉네임");
    }

    @Test
    @DisplayName("닉네임 변경 중 DB에서 중복이 감지되면 오류 응답을 반환하고 기존 닉네임을 유지한다")
    void rejectDatabaseConflictOnUpdate() throws Exception {
        Member member = memberRepository.save(new Member(new Nickname("기존닉네임")));
        memberRepository.save(new Member(new Nickname("Reader")));
        doReturn(false).when(memberRepository).existsByNicknameExcludingMember(anyString(), anyLong());

        mockMvc.perform(put("/api/members/me/nickname")
                        .header("X-Member-Id", member.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"reader"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 닉네임입니다."));

        assertThat(memberRepository.findById(member.getId()).orElseThrow().getNickname()).isEqualTo("기존닉네임");
    }

    @Test
    @DisplayName("회원 생성 중 DB에서 중복이 감지되면 오류 응답을 반환하고 회원을 추가하지 않는다")
    void rejectDatabaseConflictOnCreate() throws Exception {
        memberRepository.save(new Member(new Nickname("사용중")));
        doReturn(false).when(memberRepository).existsByNickname(anyString());

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"사용중"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 닉네임입니다."));

        assertThat(memberRepository.count()).isOne();
    }

    @Test
    @DisplayName("두 회원이 동시에 같은 닉네임으로 변경하면 한 요청만 성공한다")
    void concurrentNicknameUpdates() throws Exception {
        Member first = memberRepository.save(new Member(new Nickname("첫회원")));
        Member second = memberRepository.save(new Member(new Nickname("둘째회원")));
        doReturn(false).when(memberRepository).existsByNicknameExcludingMember(anyString(), anyLong());
        var start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstResponse = executor.submit(() -> updateNicknameAfterStart(first.getId(), start));
            var secondResponse = executor.submit(() -> updateNicknameAfterStart(second.getId(), start));
            start.countDown();

            assertThat(java.util.List.of(firstResponse.get(15, TimeUnit.SECONDS),
                    secondResponse.get(15, TimeUnit.SECONDS))).containsExactlyInAnyOrder(200, 400);
        }

        assertThat(memberRepository.findAll()).extracting(Member::getNickname)
                .filteredOn("동시닉네임"::equals).hasSize(1);
    }

    private int updateNicknameAfterStart(Long memberId, CountDownLatch start) throws Exception {
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("동시 요청 시작을 기다리는 시간이 초과됐습니다.");
        }
        return mockMvc.perform(put("/api/members/me/nickname")
                        .header("X-Member-Id", memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"동시닉네임"}
                                """))
                .andReturn().getResponse().getStatus();
    }
}
