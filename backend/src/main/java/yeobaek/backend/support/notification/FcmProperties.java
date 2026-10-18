package yeobaek.backend.support.notification;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fcm")
public record FcmProperties(
        @NotBlank String projectId
) {
}