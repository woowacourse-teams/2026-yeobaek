package yeobaek.backend.member.service;

import static yeobaek.backend.support.LogField.BLOCKED_MEMBER_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SUCCESS;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.MemberBlock;
import yeobaek.backend.member.dto.BlockedMemberResponse;
import yeobaek.backend.member.dto.BlockedMembersResponse;
import yeobaek.backend.member.repository.MemberBlockRepository;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.member.api.MemberNotFoundException;
import yeobaek.backend.member.api.block.SelfBlockException;
import yeobaek.backend.shared.identity.MemberId;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberBlockService {

    private final MemberRepository memberRepository;
    private final MemberBlockRepository memberBlockRepository;

    @Transactional(readOnly = true)
    public BlockedMembersResponse findBlockedMembers(Long blockerId) {
        logAttempt("memberBlock.findBlockedMembers", null);
        var response = new BlockedMembersResponse(memberBlockRepository.findAllWithBlockedByBlockerId(blockerId).stream()
                .map(block -> new BlockedMemberResponse(block.getBlocked().getId(), block.getBlocked().getNickname()))
                .toList());
        log.atInfo().addKeyValue(OPERATION, "memberBlock.findBlockedMembers").addKeyValue(RESULT, SUCCESS)
                .addKeyValue("resultCount", response.blockedMembers().size())
                .log("차단 목록을 조회했습니다.");
        return response;
    }

    @Transactional
    public void block(Long blockerId, Long blockedId) {
        logAttempt("memberBlock.block", blockedId);
        if (blockerId.equals(blockedId)) {
            throw new SelfBlockException(new MemberId(blockerId),
                    "자기 자신은 차단할 수 없습니다: memberId=" + blockerId);
        }
        Member blocked = memberRepository.findById(blockedId)
                .orElseThrow(() -> new MemberNotFoundException(new MemberId(blockedId),
                        "차단할 회원이 존재하지 않습니다: memberId=" + blockedId));
        if (!memberBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            memberBlockRepository.save(new MemberBlock(memberRepository.getReferenceById(blockerId), blocked));
        }
        logSuccess("memberBlock.block", blockedId);
    }

    @Transactional
    public void unblock(Long blockerId, Long blockedId) {
        logAttempt("memberBlock.unblock", blockedId);
        if (!memberRepository.existsById(blockedId)) {
            throw new MemberNotFoundException(new MemberId(blockedId),
                    "차단 해제할 회원이 존재하지 않습니다: memberId=" + blockedId);
        }
        memberBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
        logSuccess("memberBlock.unblock", blockedId);
    }

    private void logAttempt(String operation, Long blockedMemberId) {
        log.atInfo().addKeyValue(OPERATION, operation)
                .addKeyValue(BLOCKED_MEMBER_ID, blockedMemberId)
                .log("회원 차단 작업을 시작합니다.");
    }

    private void logSuccess(String operation, Long blockedMemberId) {
        log.atInfo().addKeyValue(OPERATION, operation).addKeyValue(RESULT, SUCCESS)
                .addKeyValue(BLOCKED_MEMBER_ID, blockedMemberId)
                .log("회원 차단 작업을 완료했습니다.");
    }
}
