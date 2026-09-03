package yeobaek.backend.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.MemberBlock;
import yeobaek.backend.member.dto.BlockedMemberResponse;
import yeobaek.backend.member.dto.BlockedMembersResponse;
import yeobaek.backend.member.repository.MemberBlockRepository;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.NotFoundException;

@Service
@RequiredArgsConstructor
public class MemberBlockService {

    private final MemberRepository memberRepository;
    private final MemberBlockRepository memberBlockRepository;

    @Transactional(readOnly = true)
    public BlockedMembersResponse findBlockedMembers(Long blockerId) {
        return new BlockedMembersResponse(memberBlockRepository.findAllWithBlockedByBlockerId(blockerId).stream()
                .map(block -> new BlockedMemberResponse(block.getBlocked().getId(), block.getBlocked().getNickname()))
                .toList());
    }

    @Transactional
    public void block(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new BadRequestException(ErrorCode.CANNOT_BLOCK_SELF);
        }
        Member blocked = memberRepository.findById(blockedId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));
        if (!memberBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            memberBlockRepository.save(new MemberBlock(memberRepository.getReferenceById(blockerId), blocked));
        }
    }

    @Transactional
    public void unblock(Long blockerId, Long blockedId) {
        if (!memberRepository.existsById(blockedId)) {
            throw new NotFoundException(ErrorCode.MEMBER_NOT_FOUND);
        }
        memberBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
    }
}
