package yeobaek.backend.member.internal.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.member.api.MemberBlockApi;
import yeobaek.backend.member.api.MemberNotFoundFailure;
import yeobaek.backend.member.api.SelfBlockFailure;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.MemberBlock;
import yeobaek.backend.member.domain.MemberProfile;
import yeobaek.backend.member.repository.MemberBlockRepository;
import yeobaek.backend.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class MemberBlockApiService implements MemberBlockApi {

    private final MemberRepository memberRepository;
    private final MemberBlockRepository memberBlockRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<MemberId, MemberProfile> findBlockedProfiles(MemberId blockerId) {
        Map<MemberId, MemberProfile> profiles = new LinkedHashMap<>();
        memberBlockRepository.findAllWithBlockedByBlockerId(blockerId.value()).stream()
                .map(MemberBlock::getBlocked)
                .map(this::toProfile)
                .forEach(profile -> profiles.put(profile.id(), profile));
        return Collections.unmodifiableMap(profiles);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<MemberId> findBlockedMemberIds(MemberId blockerId, Set<MemberId> memberIds) {
        if (memberIds.isEmpty()) {
            return Set.of();
        }
        return memberBlockRepository.findBlockedMemberIds(
                        blockerId.value(), memberIds.stream().map(MemberId::value).sorted().toList())
                .stream()
                .map(MemberId::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    @Transactional
    public void block(MemberId blockerId, MemberId blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new SelfBlockFailure(blockerId);
        }
        Member blocked = findMember(blockedId);
        if (!memberBlockRepository.existsByBlockerIdAndBlockedId(blockerId.value(), blockedId.value())) {
            Member blocker = memberRepository.getReferenceById(blockerId.value());
            memberBlockRepository.save(new MemberBlock(blocker, blocked));
        }
    }

    @Override
    @Transactional
    public void unblock(MemberId blockerId, MemberId blockedId) {
        if (!memberRepository.existsById(blockedId.value())) {
            throw new MemberNotFoundFailure(blockedId);
        }
        memberBlockRepository.deleteByBlockerIdAndBlockedId(blockerId.value(), blockedId.value());
    }

    @Override
    @Transactional
    public void eraseAllInvolving(MemberId memberId) {
        memberBlockRepository.deleteAllInvolving(memberId.value());
    }

    private Member findMember(MemberId memberId) {
        return memberRepository.findById(memberId.value())
                .orElseThrow(() -> new MemberNotFoundFailure(memberId));
    }

    private MemberProfile toProfile(Member member) {
        return new MemberProfile(new MemberId(member.getId()), member.getNickname());
    }
}
