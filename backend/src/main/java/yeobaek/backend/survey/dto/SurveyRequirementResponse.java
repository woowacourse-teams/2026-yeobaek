package yeobaek.backend.survey.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SurveyRequirementResponse(
        @Schema(description = "본인 댓글을 포함한 댓글 조회 기록에 따른 설문 필요 여부") boolean surveyRequired
) {
}
