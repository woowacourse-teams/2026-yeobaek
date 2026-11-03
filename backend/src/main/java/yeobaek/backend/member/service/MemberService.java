package yeobaek.backend.member.service;

import static yeobaek.backend.support.LogField.CREATED_MEMBER_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.REASON;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.dto.MemberCreateResponse;
import yeobaek.backend.member.dto.MemberNicknameUpdateResponse;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.publicroom.repository.PublicRoomMemberRepository;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.InvalidRequestException;
import yeobaek.backend.support.NotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final CommentRepository commentRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final PublicRoomMemberRepository publicRoomMemberRepository;

    @Transactional
    public MemberCreateResponse create(Nickname nickname) {
        log.atInfo().addKeyValue(OPERATION, "member.create")
                .log("회원을 생성합니다.");
        Member member = new Member(nickname);
        Member savedMember;
        try {
            savedMember = memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException e) {
            throw translateNicknameConflict(e);
        }
        var response = new MemberCreateResponse(savedMember.getId(), savedMember.getNickname());
        log.atInfo().addKeyValue(OPERATION, "member.create").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(CREATED_MEMBER_ID, savedMember.getId()).log("회원을 생성했습니다.");
        return response;
    }

    @Transactional
    public MemberNicknameUpdateResponse updateNickname(Long memberId, Nickname nickname) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND, "회원이 존재하지 않습니다."));
        member.changeNickname(nickname);
        try {
            memberRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw translateNicknameConflict(e);
        }
        return new MemberNicknameUpdateResponse(member.getId(), member.getNickname());
    }

    @Transactional
    public void delete(Long memberId) {
        log.atInfo().addKeyValue(OPERATION, "member.delete").log("회원을 삭제합니다.");
        deleteComments(memberId);
        deletePublicRoomMemberships(memberId);
        deleteClubMemberships(memberId);
        deleteMember(memberId);
        log.atInfo().addKeyValue(OPERATION, "member.delete").addKeyValue(RESULT, SUCCESS)
                .log("회원을 삭제했습니다.");
    }

    private RuntimeException translateNicknameConflict(DataIntegrityViolationException exception) {
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && ("uk_members_nickname".equals(violation.getConstraintName())
                    || "members.uk_members_nickname".equals(violation.getConstraintName()))) {
                return new InvalidRequestException(
                        "이미 사용 중인 닉네임입니다.", exception, Map.of(REASON, "duplicate_nickname"));
            }
            cause = cause.getCause();
        }
        return exception;
    }

    private void deleteComments(Long memberId) {
        logPersistenceAttempt("member.deleteComments");
        commentRepository.deleteAllByMemberId(memberId);
        logPersistenceSuccess("member.deleteComments");
    }

    private void deletePublicRoomMemberships(Long memberId) {
        logPersistenceAttempt("member.deletePublicRoomActivities");
        publicRoomMemberRepository.deleteAllByMemberId(memberId);
        logPersistenceSuccess("member.deletePublicRoomActivities");
    }

    private void deleteClubMemberships(Long memberId) {
        logPersistenceAttempt("member.deleteClubMemberships");
        clubMemberRepository.deleteAllByMemberId(memberId);
        logPersistenceSuccess("member.deleteClubMemberships");
    }

    private void deleteMember(Long memberId) {
        logPersistenceAttempt("member.deleteMember");
        memberRepository.deleteById(memberId);
        logPersistenceSuccess("member.deleteMember");
    }

    private void logPersistenceAttempt(String operation) {
        log.atInfo().addKeyValue(OPERATION, operation).log("회원 삭제 영속성 작업을 시작합니다.");
    }

    private void logPersistenceSuccess(String operation) {
        log.atInfo().addKeyValue(OPERATION, operation).addKeyValue(RESULT, SUCCESS)
                .log("회원 삭제 영속성 작업을 완료했습니다.");
    }
}
