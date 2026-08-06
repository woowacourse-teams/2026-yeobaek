package watson.backend.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import watson.backend.member.domain.Member;
import watson.backend.member.dto.MemberCreateResponse;
import watson.backend.member.repository.MemberRepository;

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
