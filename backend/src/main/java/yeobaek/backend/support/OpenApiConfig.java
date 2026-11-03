package yeobaek.backend.support;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;
import yeobaek.backend.auth.AuthMember;

@Configuration
@OpenAPIDefinition(info = @Info(title = "여백 API", description = "여백 백엔드 API 문서", version = "0.0.1-SNAPSHOT"),
        // 상대 URL: 프록시가 TLS를 종료해도 Try it out 요청이 스펙을 서빙한 원 주소(스킴 포함)로 나간다
        servers = @Server(url = "/"))
@SecurityScheme(name = "memberId", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER,
        paramName = "X-Member-Id", description = "회원 ID")
@SecurityScheme(name = "adminToken", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER,
        paramName = "X-Admin-Token", description = "고정 관리자 토큰")
public class OpenApiConfig {

    static {
        // @AuthMember 파라미터는 X-Member-Id 헤더에서 주입되므로 요청 파라미터 스펙에서 제외한다
        SpringDocUtils.getConfig().addAnnotationsToIgnore(AuthMember.class);
    }
}
