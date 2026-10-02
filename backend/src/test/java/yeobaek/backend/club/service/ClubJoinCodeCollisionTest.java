package yeobaek.backend.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.club.internal.ClubCommandService;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.support.LogCapture;
import yeobaek.backend.space.api.SpaceRootLifecycleApi;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.web.compatibility.ClubService;

@ExtendWith(MockitoExtension.class)
class ClubJoinCodeCollisionTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMemberRepository memberRepository;

    @Mock
    private SpaceRootLifecycleApi spaceRoots;

    @BeforeEach
    void setUp() {
        given(spaceRoots.create("CLUB")).willReturn(new SpaceId(30L));
    }

    @InjectMocks
    private ClubCommandService commandService;

    @Test
    @DisplayName("발급된 코드가 이미 존재하면 재생성해 유일한 코드를 발급한다")
    void regenerateOnCollision() {
        given(clubRepository.existsByJoinCode("TAKEN1")).willReturn(true);
        given(clubRepository.existsByJoinCode("FRESH1")).willReturn(false);
        given(clubRepository.save(any(Club.class))).willAnswer(invocation -> {
            Club club = invocation.getArgument(0);
            ReflectionTestUtils.setField(club, "id", 3L);
            return club;
        });

        try (var logs = new LogCapture(ClubService.class.getName());
                MockedStatic<JoinCode> generator = mockStatic(JoinCode.class)) {
            generator.when(JoinCode::generate)
                    .thenReturn(new JoinCode("TAKEN1"), new JoinCode("TAKEN1"), new JoinCode("FRESH1"));
            var club = commandService.create(new ClubName("새 모임"));

            assertThat(club.joinCode()).isEqualTo("FRESH1");
            var recovered = logs.event("club.generateUniqueJoinCode", "recovered");
            assertThat(logs.field(recovered, "retryCount")).isEqualTo(2);
            assertThat(logs.structuredText()).doesNotContain("TAKEN1", "FRESH1");
        }
    }

    @Test
    @DisplayName("5회 연속 충돌하면 서버 에러로 처리한다")
    void failAfterFiveCollisions() {
        given(clubRepository.existsByJoinCode("TAKEN1")).willReturn(true);
        try (MockedStatic<JoinCode> generator = mockStatic(JoinCode.class)) {
            generator.when(JoinCode::generate).thenReturn(new JoinCode("TAKEN1"));
            assertThatThrownBy(() -> commandService.create(new ClubName("새 모임")))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
