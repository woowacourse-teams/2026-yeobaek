package yeobaek.backend.survey.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.comment.repository.CommentViewRepository;
import yeobaek.backend.survey.dto.SurveyRequirementResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurveyService {

    private final CommentViewRepository commentViewRepository;

    public SurveyRequirementResponse getSurveyRequirement(Long memberId) {
        boolean surveyRequired = commentViewRepository.existsByMemberId(memberId);
        return new SurveyRequirementResponse(surveyRequired);
    }
}
