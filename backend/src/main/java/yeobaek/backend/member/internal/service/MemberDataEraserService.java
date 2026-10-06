package yeobaek.backend.member.internal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.member.api.MemberDataEraser;
import yeobaek.backend.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class MemberDataEraserService implements MemberDataEraser {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public void erase(MemberId memberId) {
        memberRepository.deleteById(memberId.value());
    }
}
