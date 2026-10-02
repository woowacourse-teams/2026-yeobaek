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
import yeobaek.backend.appreciation.api.CommentApi;
import yeobaek.backend.appreciation.api.CommentFailure;
import yeobaek.backend.appreciation.domain.Comment;
import yeobaek.backend.collaboration.api.AppreciationContextApi;
import yeobaek.backend.collaboration.api.SpaceContentBindingApi;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.member.api.MemberBlockApi;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.member.domain.MemberProfile;
import yeobaek.backend.space.api.SpaceAccessApi;

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
        var contents = mock(ContentApi.class);
        var members = mock(MemberQuery.class);
        var spaces = mock(SpaceAccessApi.class);
        var bindings = mock(SpaceContentBindingApi.class);
        var comment = comment(ACTOR);
        given(members.getProfile(ACTOR)).willReturn(new MemberProfile(ACTOR, "작성자"));
        given(spaces.canAccess(ACTOR, SPACE_ID)).willReturn(true);
        given(bindings.isBound(SPACE_ID, CONTENT_ID)).willReturn(true);
        given(contents.ownsLocation(CONTENT_ID, LOCATION_ID)).willReturn(true);
        given(comments.create(ACTOR, "감상")).willReturn(comment);
        var workflow = new CommentSharingWorkflow(comments, contexts, contents, members, spaces, bindings);

        workflow.share(ACTOR, SPACE_ID, CONTENT_ID, LOCATION_ID, "감상");

        verifySharingOrder(members, spaces, bindings, contents, comments, contexts);
    }

    private void verifySharingOrder(MemberQuery members, SpaceAccessApi spaces,
                                    SpaceContentBindingApi bindings, ContentApi contents,
                                    CommentApi comments, AppreciationContextApi contexts) {
        var ordered = inOrder(members, spaces, bindings, contents, comments, contexts);
        ordered.verify(members).getProfile(ACTOR);
        ordered.verify(spaces).canAccess(ACTOR, SPACE_ID);
        ordered.verify(bindings).isBound(SPACE_ID, CONTENT_ID);
        ordered.verify(contents).ownsLocation(CONTENT_ID, LOCATION_ID);
        ordered.verify(contents).requireAvailable(CONTENT_ID);
        ordered.verify(comments).create(ACTOR, "감상");
        ordered.verify(contexts).attach(COMMENT_ID, SPACE_ID, CONTENT_ID, LOCATION_ID);
        ordered.verify(comments).markViewed(ACTOR, java.util.List.of(COMMENT_ID));
    }

    @Test
    void modificationRejectsNonOwnerInsideReusableWorkflow() {
        var comments = mock(CommentApi.class);
        given(comments.get(COMMENT_ID)).willReturn(comment(AUTHOR));
        var contexts = mock(AppreciationContextApi.class);
        var workflow = new CommentModificationWorkflow(comments, contexts, mock(ContentApi.class),
                mock(MemberQuery.class), mock(SpaceAccessApi.class));

        assertThatThrownBy(() -> workflow.update(ACTOR, COMMENT_ID, "변경"))
                .isInstanceOfSatisfying(CommentFailure.class,
                        failure -> assertThat(failure.reason()).isEqualTo(CommentFailure.Reason.NOT_OWNER));

        verify(contexts, never()).get(COMMENT_ID);
        verify(comments, never()).update(ACTOR, COMMENT_ID, "변경");
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
                .isInstanceOfSatisfying(CommentFailure.class,
                        failure -> assertThat(failure.reason()).isEqualTo(CommentFailure.Reason.NOT_VISIBLE));

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

    private Comment comment(MemberId authorId) {
        return new Comment(COMMENT_ID, authorId, "감상", LocalDateTime.of(2026, 1, 1, 0, 0), null);
    }
}
