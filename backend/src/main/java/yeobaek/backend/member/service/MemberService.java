package yeobaek.backend.member.service;

import static yeobaek.backend.support.LogField.CREATED_MEMBER_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.REASON;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.util.Map;
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
import yeobaek.backend.publicroom.repository.PublicRoomActivityRepository;
import yeobaek.backend.support.InvalidRequestException;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final CommentRepository commentRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final PublicRoomActivityRepository publicRoomActivityRepository;

    @Transactional
    public MemberCreateResponse create(Nickname nickname) {
        log.atInfo().addKeyValue(OPERATION, "member.create")
                .log("회원을 생성합니다.");
        Member member = new Member(nickname);
        if (memberRepository.existsByNickname(member.getNickname())) {
            throw new InvalidRequestException(
                    "이미 사용 중인 닉네임입니다.", Map.of(REASON, "duplicate_nickname"));
        }

        Member savedMember = memberRepository.save(member);
        var response = new MemberCreateResponse(savedMember.getId(), savedMember.getNickname());
        log.atInfo().addKeyValue(OPERATION, "member.create").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(CREATED_MEMBER_ID, savedMember.getId()).log("회원을 생성했습니다.");
        return response;
    }

    @Transactional
    public void delete(Long memberId) {
        log.atInfo().addKeyValue(OPERATION, "member.delete").log("회원을 삭제합니다.");
        deleteComments(memberId);
        deletePublicRoomActivities(memberId);
        deleteClubMemberships(memberId);
        deleteMember(memberId);
        log.atInfo().addKeyValue(OPERATION, "member.delete").addKeyValue(RESULT, SUCCESS)
                .log("회원을 삭제했습니다.");
    }

    private void deleteComments(Long memberId) {
        logPersistenceAttempt("member.deleteComments");
        commentRepository.deleteAllByMemberId(memberId);
        logPersistenceSuccess("member.deleteComments");
    }

    private void deletePublicRoomActivities(Long memberId) {
        logPersistenceAttempt("member.deletePublicRoomActivities");
        publicRoomActivityRepository.deleteAllByMemberId(memberId);
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
