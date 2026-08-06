package watson.backend.admin.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AdminWebConfig implements WebMvcConfigurer {

    private final String adminToken;

    public AdminWebConfig(@Value("${admin.token:}") String adminToken) {
        this.adminToken = adminToken;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AdminAuthInterceptor(adminToken))
                .addPathPatterns("/api/admin/**");
    }
}
