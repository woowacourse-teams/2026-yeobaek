package yeobaek.backend.web.v2.space;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import yeobaek.backend.application.space.query.SpaceQueryService;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.web.v2.content.ContentWebAdapterRegistry;

@ExtendWith(MockitoExtension.class)
class PublicRoomSpaceWebAdapterTest {

    @Mock private SpaceQueryService spaces;
    @Mock private ContentWebAdapterRegistry contentAdapters;
    @InjectMocks private PublicRoomSpaceWebAdapter adapter;

    @Test
    void defaultsToMostVisitedWhenSortIsOmitted() {
        given(spaces.findPublic(new MemberId(1L), SpaceKind.PUBLIC_ROOM)).willReturn(List.of());
        adapter.findPublic(new MemberId(1L), null);
        verify(spaces).findPublic(new MemberId(1L), SpaceKind.PUBLIC_ROOM);
    }

    @Test
    void acceptsMostVisited() {
        given(spaces.findPublic(new MemberId(1L), SpaceKind.PUBLIC_ROOM)).willReturn(List.of());
        adapter.findPublic(new MemberId(1L), "MOST_VISITED");
        verify(spaces).findPublic(new MemberId(1L), SpaceKind.PUBLIC_ROOM);
    }

    @Test
    void rejectsBlankAndUnsupportedSort() {
        assertThrows(IllegalArgumentException.class, () -> adapter.findPublic(new MemberId(1L), ""));
        assertThrows(IllegalArgumentException.class, () -> adapter.findPublic(new MemberId(1L), "LATEST"));
    }
}
