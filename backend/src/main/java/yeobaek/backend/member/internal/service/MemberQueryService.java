package yeobaek.backend.member.internal.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.member.api.MemberNotFoundException;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.MemberProfile;
import yeobaek.backend.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryService implements MemberQuery {

    private final MemberRepository memberRepository;

    @Override
    public MemberProfile getProfile(MemberId memberId) {
        return memberRepository.findById(memberId.value())
                .map(this::toProfile)
                .orElseThrow(() -> new MemberNotFoundException(memberId,
                        "조회할 회원이 존재하지 않습니다: memberId=" + memberId.value()));
    }

    @Override
    public Map<MemberId, MemberProfile> findProfiles(Set<MemberId> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        Map<MemberId, MemberProfile> profiles = new LinkedHashMap<>();
        memberRepository.findAllById(memberIds.stream().map(MemberId::value).sorted().toList())
                .stream()
                .map(this::toProfile)
                .forEach(profile -> profiles.put(profile.id(), profile));
        return Collections.unmodifiableMap(profiles);
    }

    private MemberProfile toProfile(Member member) {
        return new MemberProfile(new MemberId(member.getId()), member.getNickname());
    }
}
