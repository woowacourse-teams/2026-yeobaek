package yeobaek.backend.web.v2.space;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import yeobaek.backend.application.club.ClubCreationWorkflow;
import yeobaek.backend.application.club.ClubMembershipWorkflow;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.NotFoundException;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.space.api.club.ClubApi;
import yeobaek.backend.space.api.club.JoinCode;
import yeobaek.backend.web.v2.content.ContentWebAdapterRegistry;

@ExtendWith(MockitoExtension.class)
class ClubSpaceWebAdapterTest {

    @Mock private ClubCreationWorkflow creations;
    @Mock private ClubMembershipWorkflow memberships;
    @Mock private ClubApi clubs;
    @Mock private ContentWebAdapterRegistry contentAdapters;
    @InjectMocks private ClubSpaceWebAdapter adapter;

    @Test
    void missingJoinCodePreservesErrorCodeAndLookupContext() {
        var code = new JoinCode("ABC123");
        given(clubs.findByJoinCode(code)).willReturn(Optional.empty());

        var exception = assertThrows(NotFoundException.class,
                () -> adapter.join(new MemberId(7L), new SpaceRequests.JoinClubData(code)),
                "없는 참여 코드는 대상 미존재 예외로 처리해야 한다");

        assertThat(exception).extracting(NotFoundException::getCode, NotFoundException::getMessage,
                        NotFoundException::getLogContext)
                .containsExactly(ErrorCode.JOIN_CODE_NOT_FOUND,
                        "참여 코드에 해당하는 모임을 찾을 수 없습니다.", Map.of("actorId", "7", "lookup", "joinCode"));
        verify(clubs).findByJoinCode(code);
        verifyNoInteractions(memberships, creations, contentAdapters);
    }
}
