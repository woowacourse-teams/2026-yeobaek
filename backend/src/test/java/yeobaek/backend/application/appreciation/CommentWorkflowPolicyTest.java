package yeobaek.backend.application.appreciation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import yeobaek.backend.appreciation.api.comment.CommentApi;
import yeobaek.backend.appreciation.api.comment.CommentException;
import yeobaek.backend.appreciation.api.comment.CommentViewApi;
import yeobaek.backend.appreciation.comment.internal.CommentSnapshot;
import yeobaek.backend.collaboration.api.context.AppreciationContextApi;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.location.ContentLocationQueryApi;
import yeobaek.backend.content.api.location.LocationKind;
import yeobaek.backend.appreciation.api.comment.CommentContent;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.member.api.block.MemberBlockApi;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.member.domain.MemberProfile;
import yeobaek.backend.space.api.access.SpaceAccessApi;

class CommentWorkflowPolicyTest {

    private static final MemberId ACTOR = new MemberId(1L);
    private static final MemberId AUTHOR = new MemberId(2L);
    private static final AppreciationId COMMENT_ID = new AppreciationId(10L);
    private static final SpaceId SPACE_ID = new SpaceId(20L);
    private static final ContentId CONTENT_ID = new ContentId(30L);
    private static final ContentLocationId LOCATION_ID = new ContentLocationId(40L);

    @Test
    void sharingChecksActorAccessBindingLocationAndAvailabilityBeforeCreating() {
        var comments = mock(CommentApi.class);
        var contexts = mock(AppreciationContextApi.class);
        var views = mock(CommentViewApi.class);
        var contents = mock(ContentApi.class);
        var members = mock(MemberQuery.class);
        var spaces = mock(SpaceAccessApi.class);
        var bindings = mock(SpaceContentBindingApi.class);
        var locations = mock(ContentLocationQueryApi.class);
        var body = new CommentContent("감상");
        var comment = comment(ACTOR);
        given(members.getProfile(ACTOR)).willReturn(new MemberProfile(ACTOR, "작성자"));
        given(spaces.canAccess(ACTOR, SPACE_ID)).willReturn(true);
        given(bindings.isBound(SPACE_ID, CONTENT_ID)).willReturn(true);
        given(locations.get(LOCATION_ID)).willReturn(new ContentLocationQueryApi.Location(
                LOCATION_ID, CONTENT_ID, LocationKind.SENTENCE, new ContentLocationId(41L), 1));
        given(comments.create(ACTOR, body)).willReturn(comment);
        var workflow = new CommentSharingWorkflow(
                comments, views, contexts, contents, members, spaces, bindings, locations);

        workflow.share(ACTOR, SPACE_ID, CONTENT_ID, LOCATION_ID, body);

        verifySharingOrder(members, spaces, bindings, contents, comments, views, contexts, locations, body);
    }

    @Test
    void sharingWritesClientMessageAndStructuredContextAtDecisionPoint() {
        var comments = mock(CommentApi.class);
        var contexts = mock(AppreciationContextApi.class);
        var views = mock(CommentViewApi.class);
        var contents = mock(ContentApi.class);
        var members = mock(MemberQuery.class);
        var spaces = mock(SpaceAccessApi.class);
        var bindings = mock(SpaceContentBindingApi.class);
        var locations = mock(ContentLocationQueryApi.class);
        given(members.getProfile(ACTOR)).willReturn(new MemberProfile(ACTOR, "작성자"));
        given(spaces.canAccess(ACTOR, SPACE_ID)).willReturn(false);
        var workflow = new CommentSharingWorkflow(
                comments, views, contexts, contents, members, spaces, bindings, locations);

        assertThatThrownBy(() -> workflow.share(
                ACTOR, SPACE_ID, CONTENT_ID, LOCATION_ID, new CommentContent("감상")))
                .isInstanceOfSatisfying(CommentPolicyException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(ErrorCode.SPACE_ACCESS_DENIED);
                    assertThat(exception.getMessage())
                            .isEqualTo("감상을 공유할 공간에 접근할 수 없습니다: spaceId=20");
                    assertThat(exception.getLogContext()).containsAllEntriesOf(java.util.Map.of(
                            "reason", "SPACE_ACCESS_DENIED",
                            "actorId", "1",
                            "spaceId", "20",
                            "contentId", "30",
                            "locationId", "40"));
                });

        verify(bindings, never()).isBound(SPACE_ID, CONTENT_ID);
        verify(comments, never()).create(ACTOR, new CommentContent("감상"));
    }

    private void verifySharingOrder(MemberQuery members, SpaceAccessApi spaces,
                                    SpaceContentBindingApi bindings, ContentApi contents,
                                    CommentApi comments, CommentViewApi views, AppreciationContextApi contexts,
                                    ContentLocationQueryApi locations, CommentContent body) {
        var ordered = inOrder(members, spaces, bindings, contents, comments, views, contexts, locations);
        ordered.verify(members).getProfile(ACTOR);
        ordered.verify(spaces).canAccess(ACTOR, SPACE_ID);
        ordered.verify(bindings).isBound(SPACE_ID, CONTENT_ID);
        ordered.verify(locations).get(LOCATION_ID);
        ordered.verify(contents).requireAvailable(CONTENT_ID);
        ordered.verify(comments).create(ACTOR, body);
        ordered.verify(contexts).attach(COMMENT_ID, SPACE_ID, CONTENT_ID, LOCATION_ID);
        ordered.verify(views).markViewed(ACTOR, java.util.List.of(COMMENT_ID));
    }

    @Test
    void modificationRejectsNonOwnerInsideReusableWorkflow() {
        var comments = mock(CommentApi.class);
        given(comments.get(COMMENT_ID)).willReturn(comment(AUTHOR));
        var contexts = mock(AppreciationContextApi.class);
        var workflow = new CommentModificationWorkflow(comments, contexts, mock(ContentApi.class),
                mock(MemberQuery.class), mock(SpaceAccessApi.class));

        CommentContent changed = new CommentContent("변경");
        assertThatThrownBy(() -> workflow.update(ACTOR, COMMENT_ID, changed))
                .isInstanceOfSatisfying(CommentException.class,
                        failure -> {
                            assertThat(failure.getCode()).isEqualTo(ErrorCode.NOT_COMMENT_OWNER);
                            assertThat(failure.getLogContext()).containsEntry("actorId", "1");
                        });

        verify(contexts, never()).get(COMMENT_ID);
        verify(comments, never()).update(ACTOR, COMMENT_ID, changed);
    }

    @Test
    void reportingLocksThenRejectsBlockedAuthorBeforeCreatingReaction() {
        var comments = mock(CommentApi.class);
        var contexts = mock(AppreciationContextApi.class);
        var contents = mock(ContentApi.class);
        var members = mock(MemberQuery.class);
        var blocks = mock(MemberBlockApi.class);
        var spaces = mock(SpaceAccessApi.class);
        given(members.getProfile(ACTOR)).willReturn(new MemberProfile(ACTOR, "신고자"));
        given(comments.getForUpdate(COMMENT_ID)).willReturn(comment(AUTHOR));
        given(blocks.findBlockedMemberIds(ACTOR, Set.of(AUTHOR))).willReturn(Set.of(AUTHOR));
        var workflow = new CommentReportWorkflow(comments, contexts, contents, members, blocks, spaces);

        assertThatThrownBy(() -> workflow.report(ACTOR, COMMENT_ID))
                .isInstanceOfSatisfying(CommentException.class,
                        failure -> {
                            assertThat(failure.getCode()).isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
                            assertThat(failure.getLogContext())
                                    .containsEntry("actorId", "1")
                                    .containsEntry("reason", "NOT_VISIBLE")
                                    .containsEntry("authorId", "2");
                        });

        verify(comments).getForUpdate(COMMENT_ID);
        verify(contexts, never()).get(COMMENT_ID);
        verify(comments, never()).report(ACTOR, COMMENT_ID);
    }

    @Test
    void reportingKeepsIdempotenceInsideLockedReusableWorkflow() {
        var comments = mock(CommentApi.class);
        var contexts = mock(AppreciationContextApi.class);
        var contents = mock(ContentApi.class);
        var members = mock(MemberQuery.class);
        var blocks = mock(MemberBlockApi.class);
        var spaces = mock(SpaceAccessApi.class);
        given(members.getProfile(ACTOR)).willReturn(new MemberProfile(ACTOR, "신고자"));
        given(comments.getForUpdate(COMMENT_ID)).willReturn(comment(AUTHOR));
        given(blocks.findBlockedMemberIds(ACTOR, Set.of(AUTHOR))).willReturn(Set.of());
        given(contexts.get(COMMENT_ID)).willReturn(
                new AppreciationContextApi.Context(SPACE_ID, CONTENT_ID, LOCATION_ID));
        given(spaces.canAccess(ACTOR, SPACE_ID)).willReturn(true);
        given(comments.report(ACTOR, COMMENT_ID)).willReturn(false);
        var workflow = new CommentReportWorkflow(comments, contexts, contents, members, blocks, spaces);

        assertThat(workflow.report(ACTOR, COMMENT_ID)).isFalse();

        verifyReportOrder(members, comments, blocks, contexts, spaces, contents);
    }

    private void verifyReportOrder(MemberQuery members, CommentApi comments, MemberBlockApi blocks,
                                   AppreciationContextApi contexts, SpaceAccessApi spaces, ContentApi contents) {
        var ordered = inOrder(members, comments, blocks, contexts, spaces, contents);
        ordered.verify(members).getProfile(ACTOR);
        ordered.verify(comments).getForUpdate(COMMENT_ID);
        ordered.verify(blocks).findBlockedMemberIds(ACTOR, Set.of(AUTHOR));
        ordered.verify(contexts).get(COMMENT_ID);
        ordered.verify(spaces).canAccess(ACTOR, SPACE_ID);
        ordered.verify(contents).requireAvailable(CONTENT_ID);
        ordered.verify(comments).report(ACTOR, COMMENT_ID);
    }

    private CommentSnapshot comment(MemberId authorId) {
        return new CommentSnapshot(COMMENT_ID, authorId, "감상", LocalDateTime.of(2026, 1, 1, 0, 0), null);
    }
}
