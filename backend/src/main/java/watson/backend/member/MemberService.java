package watson.backend.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public MemberCreateResponse create(String nickname) {
        Member member = memberRepository.save(new Member(nickname));
        return new MemberCreateResponse(member.getId(), member.getNickname());
    }
}
