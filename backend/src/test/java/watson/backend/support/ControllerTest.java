package watson.backend.support;

import static org.mockito.BDDMockito.given;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import watson.backend.member.repository.MemberRepository;

/**
 * 컨트롤러 슬라이스 테스트 공통 베이스.
 * 회원 식별 인터셉터(AuthWebConfig)가 요구하는 MemberRepository를 mock으로 대체한다.
 */
@AutoConfigureRestDocs
public abstract class ControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected MemberRepository memberRepository;

    protected void givenValidMember(long memberId) {
        given(memberRepository.existsById(memberId)).willReturn(true);
    }
}
