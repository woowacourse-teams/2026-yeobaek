package yeobaek.backend.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.LogCapture;

class MemberLoggingTest {

    @Test
    @DisplayName("회원 생성 로그는 인증 요청자가 아닌 새 회원 식별자로 기록한다")
    void logCreatedMemberId() {
        var members = mock(MemberRepository.class);
        var savedMember = mock(Member.class);
        given(savedMember.getId()).willReturn(7L);
        given(savedMember.getNickname()).willReturn("새회원");
        given(members.save(any(Member.class))).willReturn(savedMember);
        var service = new MemberService(members);

        try (var logs = new LogCapture(MemberService.class.getName())) {
            service.create(new Nickname("새회원"));

            var event = logs.event("member.create", "success");
            assertThat(logs.field(event, "createdMemberId")).isEqualTo(7L);
            assertThat(logs.hasField(event, "memberId")).isFalse();
        }
    }

}
