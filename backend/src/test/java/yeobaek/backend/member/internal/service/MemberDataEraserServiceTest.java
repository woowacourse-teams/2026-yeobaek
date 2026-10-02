package yeobaek.backend.member.internal.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class MemberDataEraserServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberDataEraserService memberDataEraserService;

    @Test
    @DisplayName("회원 데이터 삭제 API는 회원 저장소만 삭제한다")
    void erase() {
        memberDataEraserService.erase(new MemberId(7L));

        verify(memberRepository).deleteById(7L);
    }
}
