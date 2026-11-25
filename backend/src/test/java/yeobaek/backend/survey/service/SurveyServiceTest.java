package yeobaek.backend.survey.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import yeobaek.backend.comment.repository.CommentViewRepository;

@ExtendWith(MockitoExtension.class)
class SurveyServiceTest {

    @Mock
    private CommentViewRepository commentViewRepository;

    @InjectMocks
    private SurveyService service;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("요청 회원의 댓글 조회 기록 유무로 설문 필요 여부를 반환한다")
    void surveyRequirementFollowsMembersViewHistory(boolean hasViews) {
        when(commentViewRepository.existsByMemberId(17L)).thenReturn(hasViews);

        assertThat(service.getSurveyRequirement(17L).surveyRequired()).isEqualTo(hasViews);
    }
}
