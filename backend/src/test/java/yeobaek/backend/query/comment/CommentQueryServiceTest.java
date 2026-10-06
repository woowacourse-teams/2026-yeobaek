package yeobaek.backend.query.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import yeobaek.backend.appreciation.api.comment.CommentApi;
import yeobaek.backend.appreciation.api.comment.CommentViewApi;
import yeobaek.backend.appreciation.comment.internal.CommentSnapshot;
import yeobaek.backend.collaboration.api.context.AppreciationContextApi;
import yeobaek.backend.collaboration.api.context.AppreciationContextApi.LocatedAppreciation;
import yeobaek.backend.content.api.location.ContentLegacyLocationQueryApi;
import yeobaek.backend.member.api.block.MemberBlockApi;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;

class CommentQueryServiceTest {

    private static final MemberId REQUESTER = new MemberId(1L);
    private static final MemberId AUTHOR = new MemberId(2L);
    private static final SpaceId SPACE = new SpaceId(3L);
    private static final ContentId TARGET_CONTENT = new ContentId(4L);
    private static final ContentId OTHER_CONTENT = new ContentId(5L);
    private static final ContentLocationId TARGET_LOCATION = new ContentLocationId(6L);
    private static final ContentLocationId OTHER_LOCATION = new ContentLocationId(7L);
    private static final AppreciationId TARGET_COMMENT = new AppreciationId(8L);
    private static final AppreciationId OTHER_COMMENT = new AppreciationId(9L);

    @Test
    void countsUnreadCommentsOnlyWithinRequestedContent() {
        Fixture fixture = fixture();
        given(fixture.locations.findSentenceInfo(List.of(TARGET_LOCATION))).willReturn(List.of(sentence()));

        long count = fixture.queryService.countNewVisible(REQUESTER, SPACE, TARGET_CONTENT, 1);

        assertThat(count).isEqualTo(1);
        verify(fixture.comments).findByIds(List.of(TARGET_COMMENT));
    }

    @Test
    void discoversCommentsOnlyWithinRequestedContent() {
        Fixture fixture = fixture();
        given(fixture.locations.findSentenceInfo(Set.of(TARGET_LOCATION))).willReturn(List.of(sentence()));
        given(fixture.locations.findPassage(10L)).willReturn(Optional.of(
                new ContentLegacyLocationQueryApi.LocationReference(
                        new ContentLocationId(11L), TARGET_CONTENT, 10L, 1)));

        var found = fixture.queryService.findDiscovery(REQUESTER, SPACE, TARGET_CONTENT);

        assertThat(found).singleElement()
                .extracting(CommentQueryService.DiscoverySnapshot::locationId)
                .isEqualTo(TARGET_LOCATION);
        verify(fixture.comments).findByIds(List.of(TARGET_COMMENT));
    }

    private Fixture fixture() {
        var locations = mock(ContentLegacyLocationQueryApi.class);
        var contexts = mock(AppreciationContextApi.class);
        var comments = mock(CommentApi.class);
        var views = mock(CommentViewApi.class);
        var blocks = mock(MemberBlockApi.class);
        given(contexts.findInSpace(SPACE)).willReturn(List.of(
                new LocatedAppreciation(TARGET_COMMENT, TARGET_CONTENT, TARGET_LOCATION),
                new LocatedAppreciation(OTHER_COMMENT, OTHER_CONTENT, OTHER_LOCATION)));
        var target = new CommentSnapshot(TARGET_COMMENT, AUTHOR, "대상 댓글",
                LocalDateTime.of(2026, 10, 3, 12, 0), null);
        given(comments.findByIds(List.of(TARGET_COMMENT))).willReturn(List.of(target));
        given(views.findViewedIds(REQUESTER, List.of(TARGET_COMMENT))).willReturn(Set.of());
        given(blocks.findBlockedMemberIds(REQUESTER, Set.of(AUTHOR))).willReturn(Set.of());
        return new Fixture(new CommentQueryService(locations, contexts, comments, views, blocks), locations, comments);
    }

    private ContentLegacyLocationQueryApi.SentenceInfo sentence() {
        return new ContentLegacyLocationQueryApi.SentenceInfo(
                TARGET_LOCATION, 12L, "문장", 10L, 1, 1);
    }

    private record Fixture(CommentQueryService queryService, ContentLegacyLocationQueryApi locations,
                           CommentApi comments) {
    }
}
