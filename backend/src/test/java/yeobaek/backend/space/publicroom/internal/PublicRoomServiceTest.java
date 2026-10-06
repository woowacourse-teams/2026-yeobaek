package yeobaek.backend.space.publicroom.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.lifecycle.SpaceRootLifecycleApi;
import yeobaek.backend.space.publicroom.persistence.PublicRoom;
import yeobaek.backend.space.publicroom.repository.PublicRoomRepository;

class PublicRoomServiceTest {

    @Test
    void resolvesCanonicalSpaceThroughTheLockingRepositoryEntryPoint() {
        PublicRoomRepository repository = mock(PublicRoomRepository.class);
        PublicRoom room = mock(PublicRoom.class);
        given(room.getSpaceId()).willReturn(12L);
        given(room.getId()).willReturn(34L);
        given(repository.findBySpaceRootIdForUpdate(12L)).willReturn(Optional.of(room));

        var service = new PublicRoomService(repository, mock(SpaceRootLifecycleApi.class));
        var result = service.findBySpaceIdForUpdate(new SpaceId(12L));

        assertThat(result).get().satisfies(found -> {
            assertThat(found.id()).isEqualTo(new SpaceId(12L));
            assertThat(found.publicRoomId()).isEqualTo(34L);
        });
        verify(repository).findBySpaceRootIdForUpdate(12L);
    }
}
