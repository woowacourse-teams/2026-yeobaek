package yeobaek.backend.application.reading;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.location.ContentLocationQueryApi;
import yeobaek.backend.content.api.location.LocationKind;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.member.domain.MemberProfile;
import yeobaek.backend.reading.api.ReadingProgressApi;
import yeobaek.backend.space.api.access.SpaceAccessApi;

class ReadingProgressWorkflowTest {

    private static final MemberId ACTOR = new MemberId(1L);
    private static final SpaceId SPACE = new SpaceId(2L);
    private static final ContentId CONTENT = new ContentId(3L);
    private static final ContentLocationId LOCATION = new ContentLocationId(4L);
    private static final LocalDateTime READ_AT = LocalDateTime.of(2026, 10, 2, 20, 0);

    @Test
    void validatesCanonicalCollaborationBeforeWritingProgress() {
        var members = mock(MemberQuery.class);
        var spaces = mock(SpaceAccessApi.class);
        var bindings = mock(SpaceContentBindingApi.class);
        var contents = mock(ContentApi.class);
        var progress = mock(ReadingProgressApi.class);
        var locations = mock(ContentLocationQueryApi.class);
        given(members.getProfile(ACTOR)).willReturn(new MemberProfile(ACTOR, "독자"));
        given(spaces.canAccess(ACTOR, SPACE)).willReturn(true);
        given(bindings.isBound(SPACE, CONTENT)).willReturn(true);
        given(locations.get(LOCATION)).willReturn(new ContentLocationQueryApi.Location(
                LOCATION, CONTENT, LocationKind.PASSAGE, LOCATION, 1));
        var workflow = new ReadingProgressWorkflow(members, spaces, bindings, contents, progress, locations);

        workflow.update(ACTOR, SPACE, CONTENT, LOCATION, READ_AT);

        verifyValidationOrder(members, spaces, bindings, contents, progress, locations);
    }

    @Test
    void rejectsUnboundContentBeforeLocationOrWrite() {
        var members = mock(MemberQuery.class);
        var spaces = mock(SpaceAccessApi.class);
        var bindings = mock(SpaceContentBindingApi.class);
        var contents = mock(ContentApi.class);
        var progress = mock(ReadingProgressApi.class);
        var locations = mock(ContentLocationQueryApi.class);
        given(members.getProfile(ACTOR)).willReturn(new MemberProfile(ACTOR, "독자"));
        given(spaces.canAccess(ACTOR, SPACE)).willReturn(true);
        given(bindings.isBound(SPACE, CONTENT)).willReturn(false);
        var workflow = new ReadingProgressWorkflow(members, spaces, bindings, contents, progress, locations);

        assertThatThrownBy(() -> workflow.update(ACTOR, SPACE, CONTENT, LOCATION, READ_AT))
                .isInstanceOf(ReadingProgressPolicyException.class);

        verify(locations, never()).get(LOCATION);
        verify(progress, never()).update(ACTOR, SPACE, CONTENT, LOCATION, READ_AT);
    }

    private void verifyValidationOrder(MemberQuery members, SpaceAccessApi spaces,
                                       SpaceContentBindingApi bindings, ContentApi contents,
                                       ReadingProgressApi progress, ContentLocationQueryApi locations) {
        var ordered = inOrder(members, spaces, bindings, contents, progress, locations);
        ordered.verify(members).getProfile(ACTOR);
        ordered.verify(spaces).canAccess(ACTOR, SPACE);
        ordered.verify(bindings).isBound(SPACE, CONTENT);
        ordered.verify(locations).get(LOCATION);
        ordered.verify(contents).requireAvailable(CONTENT);
        ordered.verify(progress).update(ACTOR, SPACE, CONTENT, LOCATION, READ_AT);
    }
}
