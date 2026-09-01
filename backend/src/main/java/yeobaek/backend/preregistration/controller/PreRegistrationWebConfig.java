package yeobaek.backend.preregistration.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PreRegistrationWebConfig implements WebMvcConfigurer {

    private final String allowedOrigin;
    private final PreRegistrationRateLimitInterceptor rateLimitInterceptor;

    public PreRegistrationWebConfig(@Value("${landing.cors.allowed-origin}") String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
        this.rateLimitInterceptor = new PreRegistrationRateLimitInterceptor(new PreRegistrationRateLimiter());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/pre-registrations")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("POST")
                .allowedHeaders("Content-Type")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/pre-registrations");
    }
}
