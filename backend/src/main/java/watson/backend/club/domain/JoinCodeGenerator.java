package watson.backend.club.domain;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * 참여 코드 발급기 — 6자 대문자·숫자 (M4 결정: 사전 존재 확인 5회 + DB unique 최후 방어).
 */
@Component
public class JoinCodeGenerator {

    public static final int LENGTH = 6;
    private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
        }
        return code.toString();
    }
}
