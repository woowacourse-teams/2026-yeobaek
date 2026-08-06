package watson.backend.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import watson.backend.member.domain.Member;

/**
 * 인터셉터가 검증한 X-Member-Id 헤더의 회원 ID를 컨트롤러 파라미터로 주입한다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthMember {
}
