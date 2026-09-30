package yeobaek.backend.auth;

import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.REQUESTED_MEMBER_ID;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.REASON;
import static yeobaek.backend.support.LogField.SUCCESS;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.NotFoundException;
import yeobaek.backend.support.RequestLoggingFilter;
import yeobaek.backend.support.InvalidRequestException;

@RequiredArgsConstructor
@Slf4j
public class MemberAuthInterceptor implements HandlerInterceptor {

    public static final String MEMBER_ID_ATTRIBUTE = "authMemberId";
    private static final String MEMBER_ID_HEADER = "X-Member-Id";

    private final MemberRepository memberRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.atInfo().addKeyValue(OPERATION, "auth.member.preHandle")
                .log("회원 인증을 시작합니다.");
        String header = request.getHeader(MEMBER_ID_HEADER);
        if (header == null || header.isBlank()) {
            throw new InvalidRequestException(
                    "X-Member-Id 헤더가 필요합니다.", Map.of(REASON, "missing_member_id_header"));
        }
        long memberId = parseMemberId(header);
        if (!memberRepository.existsById(memberId)) {
            throw new NotFoundException(
                    ErrorCode.MEMBER_NOT_FOUND,
                    "X-Member-Id가 가리키는 회원이 존재하지 않습니다: memberId=" + memberId,
                    Map.of(REQUESTED_MEMBER_ID, Long.toString(memberId)));
        }
        request.setAttribute(MEMBER_ID_ATTRIBUTE, memberId);
        MDC.put(RequestLoggingFilter.MEMBER_ID, Long.toString(memberId));
        log.atInfo()
                .addKeyValue(OPERATION, "auth.member.preHandle")
                .addKeyValue(RESULT, SUCCESS)
                .log("회원 인증에 성공했습니다.");
        return true;
    }

    private long parseMemberId(String header) {
        try {
            return Long.parseLong(header);
        } catch (NumberFormatException e) {
            throw new InvalidRequestException(
                    "X-Member-Id 헤더가 올바르지 않습니다.", e,
                    Map.of(REASON, "invalid_member_id_header"));
        }
    }
}
