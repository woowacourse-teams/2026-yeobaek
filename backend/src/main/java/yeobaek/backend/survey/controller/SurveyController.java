package yeobaek.backend.survey.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.auth.AuthMember;
import yeobaek.backend.survey.dto.SurveyRequirementResponse;
import yeobaek.backend.survey.service.SurveyService;

@Tag(name = "설문")
@SecurityRequirement(name = "memberId")
@RestController
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;

    @Operation(summary = "설문 필요 여부 조회",
            description = "기간 제한 없이 모임 또는 공개방의 댓글 조회 기록이 있으면 true를 반환한다. 댓글 작성 시 생성되는 본인 조회 기록도 포함한다.")
    @GetMapping("/api/members/me/survey-requirement")
    public SurveyRequirementResponse getSurveyRequirement(@AuthMember Long memberId) {
        return surveyService.getSurveyRequirement(memberId);
    }
}
