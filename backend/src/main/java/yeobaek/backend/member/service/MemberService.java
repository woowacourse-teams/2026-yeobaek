package yeobaek.backend.member.service;

import static yeobaek.backend.support.LogField.ATTEMPT;
import static yeobaek.backend.support.LogField.MEMBER_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.PHASE;
import static yeobaek.backend.support.LogField.REASON;
import static yeobaek.backend.support.LogField.SUCCESS;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.dto.MemberCreateResponse;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.InvalidRequestException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final CommentRepository commentRepository;
    private final ClubMemberRepository clubMemberRepository;

    @Transactional
    public MemberCreateResponse create(Nickname nickname) {
        log.atInfo().addKeyValue(OPERATION, "member.create").addKeyValue(PHASE, ATTEMPT)
                .log("회원을 생성합니다.");
        Member member = new Member(nickname);
        if (memberRepository.existsByNickname(member.getNickname())) {
            throw new InvalidRequestException(
                    "이미 사용 중인 닉네임입니다.", Map.of(REASON, "duplicate_nickname"));
        }

        Member savedMember = memberRepository.save(member);
        var response = new MemberCreateResponse(savedMember.getId(), savedMember.getNickname());
        log.atInfo().addKeyValue(OPERATION, "member.create").addKeyValue(PHASE, SUCCESS)
                .addKeyValue(MEMBER_ID, savedMember.getId()).log("회원을 생성했습니다.");
        return response;
    }

    @Transactional
    public void delete(Long memberId) {
        log.atInfo().addKeyValue(OPERATION, "member.delete").addKeyValue(PHASE, ATTEMPT)
                .addKeyValue(MEMBER_ID, memberId).log("회원을 삭제합니다.");
        deleteComments(memberId);
        deleteClubMemberships(memberId);
        deleteMember(memberId);
        log.atInfo().addKeyValue(OPERATION, "member.delete").addKeyValue(PHASE, SUCCESS)
                .addKeyValue(MEMBER_ID, memberId).log("회원을 삭제했습니다.");
    }

    private void deleteComments(Long memberId) {
        logPersistenceAttempt("member.delete.comments", memberId);
        commentRepository.deleteAllByMemberId(memberId);
        logPersistenceSuccess("member.delete.comments", memberId);
    }

    private void deleteClubMemberships(Long memberId) {
        logPersistenceAttempt("member.delete.clubMemberships", memberId);
        clubMemberRepository.deleteAllByMemberId(memberId);
        logPersistenceSuccess("member.delete.clubMemberships", memberId);
    }

    private void deleteMember(Long memberId) {
        logPersistenceAttempt("member.delete.member", memberId);
        memberRepository.deleteById(memberId);
        logPersistenceSuccess("member.delete.member", memberId);
    }

    private void logPersistenceAttempt(String operation, Long memberId) {
        log.atInfo().addKeyValue(OPERATION, operation).addKeyValue(PHASE, ATTEMPT)
                .addKeyValue(MEMBER_ID, memberId).log("회원 삭제 영속성 작업을 시작합니다.");
    }

    private void logPersistenceSuccess(String operation, Long memberId) {
        log.atInfo().addKeyValue(OPERATION, operation).addKeyValue(PHASE, SUCCESS)
                .addKeyValue(MEMBER_ID, memberId).log("회원 삭제 영속성 작업을 완료했습니다.");
    }
}
