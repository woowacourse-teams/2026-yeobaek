package yeobaek.backend.application.appreciation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import yeobaek.backend.appreciation.api.comment.CommentViewApi;
import yeobaek.backend.appreciation.comment.internal.CommentSnapshot;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.content.api.Content;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.location.ContentLocationQueryApi;
import yeobaek.backend.content.api.location.LocationKind;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.member.domain.MemberProfile;
import yeobaek.backend.query.comment.CommentQueryService;
import yeobaek.backend.space.api.access.SpaceAccessApi;

class CommentQueryWorkflowTest {

    private static final MemberId REQUESTER = new MemberId(1L);
    private static final MemberId AUTHOR = new MemberId(2L);
    private static final SpaceId SPACE = new SpaceId(3L);
    private static final ContentId CONTENT = new ContentId(4L);
    private static final ContentLocationId LOCATION = new ContentLocationId(5L);

    @Test
    void returnsCommentsAndMarksThemViewedThroughCanonicalTarget() {
        Fixture fixture = fixture(LocationKind.SENTENCE, 2);
        var comment = new CommentSnapshot(new AppreciationId(6L), AUTHOR, "감상",
                LocalDateTime.of(2026, 10, 2, 12, 0), null);
        given(fixture.comments.findVisible(REQUESTER, SPACE, LOCATION)).willReturn(List.of(comment));
        given(fixture.members.findProfiles(Set.of(AUTHOR)))
                .willReturn(Map.of(AUTHOR, new MemberProfile(AUTHOR, "작성자")));

        var result = fixture.workflow.findComments(REQUESTER, SPACE, CONTENT, LOCATION);

        assertThat(result).singleElement().satisfies(found -> assertThat(found.comment()).isSameAs(comment));
        verify(fixture.views).markViewed(REQUESTER, List.of(comment.id()));
    }

    @Test
    void sortsDiscoveryUsingLegacySentenceIdOnlyAsInternalTieBreaker() {
        Fixture fixture = fixture(LocationKind.PASSAGE, 2);
        var first = new ContentLocationId(11L);
        var second = new ContentLocationId(12L);
        given(fixture.comments.findDiscovery(REQUESTER, SPACE, CONTENT)).willReturn(List.of(
                new CommentQueryService.DiscoverySnapshot(first, LOCATION, 21L, "첫째", 2, 1, 1, 1,
                        LocalDateTime.of(2026, 1, 1, 0, 0)),
                new CommentQueryService.DiscoverySnapshot(second, LOCATION, 22L, "둘째", 2, 1, 1, 1,
                        LocalDateTime.of(2026, 1, 1, 0, 0))));

        var result = fixture.workflow.findDiscovery(REQUESTER, SPACE, CONTENT, LOCATION);

        assertThat(result).extracting(CommentQueryWorkflow.DiscoveryResult::locationId)
                .containsExactly(second, first);
    }

    @Test
    void passesRequestedContentToUnreadCountQuery() {
        Fixture fixture = fixture(LocationKind.PASSAGE, 2);
        given(fixture.comments.countNewVisible(REQUESTER, SPACE, CONTENT, 2)).willReturn(3L);

        long count = fixture.workflow.countNewComments(REQUESTER, SPACE, CONTENT, LOCATION);

        assertThat(count).isEqualTo(3L);
        verify(fixture.comments).countNewVisible(REQUESTER, SPACE, CONTENT, 2);
    }

    private Fixture fixture(LocationKind kind, int passageSequence) {
        var comments = mock(CommentQueryService.class);
        var views = mock(CommentViewApi.class);
        var members = mock(MemberQuery.class);
        var spaces = mock(SpaceAccessApi.class);
        var bindings = mock(SpaceContentBindingApi.class);
        var contents = mock(ContentApi.class);
        var locations = mock(ContentLocationQueryApi.class);
        var content = mock(Content.class);
        given(members.getProfile(REQUESTER)).willReturn(new MemberProfile(REQUESTER, "독자"));
        given(spaces.canAccess(REQUESTER, SPACE)).willReturn(true);
        given(bindings.isBound(SPACE, CONTENT)).willReturn(true);
        given(locations.get(LOCATION)).willReturn(new ContentLocationQueryApi.Location(
                LOCATION, CONTENT, kind, LOCATION, passageSequence));
        given(contents.getContent(CONTENT)).willReturn(content);
        given(content.kind()).willReturn(ContentKind.BOOK);
        given(content.available()).willReturn(true);
        return new Fixture(new CommentQueryWorkflow(comments, views, members, spaces, bindings, contents, locations),
                comments, views, members);
    }

    private record Fixture(CommentQueryWorkflow workflow, CommentQueryService comments, CommentViewApi views,
                           MemberQuery members) {
    }
}
