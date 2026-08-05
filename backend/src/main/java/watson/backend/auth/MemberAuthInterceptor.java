package watson.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.HandlerInterceptor;
import watson.backend.member.MemberRepository;

@RequiredArgsConstructor
public class MemberAuthInterceptor implements HandlerInterceptor {

    public static final String MEMBER_ID_ATTRIBUTE = "authMemberId";
    private static final String MEMBER_ID_HEADER = "X-Member-Id";

    private final MemberRepository memberRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader(MEMBER_ID_HEADER);
        if (header == null || header.isBlank()) {
            throw new IllegalArgumentException("X-Member-Id 헤더가 필요합니다.");
        }
        long memberId = parseMemberId(header);
        if (!memberRepository.existsById(memberId)) {
            throw new IllegalArgumentException("존재하지 않는 회원입니다.");
        }
        request.setAttribute(MEMBER_ID_ATTRIBUTE, memberId);
        return true;
    }

    private long parseMemberId(String header) {
        try {
            return Long.parseLong(header);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("X-Member-Id 헤더가 올바르지 않습니다.");
        }
    }
}
