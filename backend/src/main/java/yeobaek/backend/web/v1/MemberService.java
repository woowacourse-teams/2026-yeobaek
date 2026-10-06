package yeobaek.backend.web.v1;

import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SUCCESS;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.application.account.AccountDeletionWorkflow;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.dto.MemberCreateResponse;

@Service("memberCompatibilityService")
@RequiredArgsConstructor
@Slf4j(topic = "yeobaek.backend.member.service.MemberService")
public class MemberService {

    private final yeobaek.backend.member.service.MemberService memberCreator;
    private final AccountDeletionWorkflow accountDeletionWorkflow;

    public MemberCreateResponse create(Nickname nickname) {
        return memberCreator.create(nickname);
    }

    @Transactional
    public void delete(Long memberId) {
        log.atInfo().addKeyValue(OPERATION, "member.delete").log("회원을 삭제합니다.");
        log.atInfo().addKeyValue(OPERATION, "member.deleteData").log("회원 삭제 영속성 작업을 시작합니다.");
        accountDeletionWorkflow.delete(new MemberId(memberId));
        log.atInfo().addKeyValue(OPERATION, "member.deleteData").addKeyValue(RESULT, SUCCESS)
                .log("회원 삭제 영속성 작업을 완료했습니다.");
        log.atInfo().addKeyValue(OPERATION, "member.delete").addKeyValue(RESULT, SUCCESS)
                .log("회원을 삭제했습니다.");
    }
}
