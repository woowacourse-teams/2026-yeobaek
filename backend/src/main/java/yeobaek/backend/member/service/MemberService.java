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
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.dto.MemberCreateResponse;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.InvalidRequestException;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;

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

}
