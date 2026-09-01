package yeobaek.backend.preregistration.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PreRegistrationCreateRequest(
        @Schema(description = "사전신청 이메일 (최대 254자)", example = "reader@example.com") String email
) {
}
