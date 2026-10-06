package yeobaek.backend.application.account;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import java.util.List;
import yeobaek.backend.appreciation.api.lifecycle.AppreciationRootApi;
import org.mockito.InOrder;
import yeobaek.backend.appreciation.api.erasure.AppreciationDataEraser;
import yeobaek.backend.space.api.club.ClubMembershipApi;
import yeobaek.backend.collaboration.api.context.AppreciationContextApi;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.member.api.block.MemberBlockApi;
import yeobaek.backend.member.api.MemberDataEraser;
import yeobaek.backend.space.api.publicroom.PublicRoomVisitApi;
import yeobaek.backend.reading.api.ReadingProgressApi;

class AccountDeletionWorkflowTest {

    @Test
    void erasesOwnedModuleDataBeforeMemberRoot() {
        var contexts = mock(AppreciationContextApi.class);
        var appreciations = mock(AppreciationDataEraser.class);
        var roots = mock(AppreciationRootApi.class);
        var progress = mock(ReadingProgressApi.class);
        var visits = mock(PublicRoomVisitApi.class);
        var memberships = mock(ClubMembershipApi.class);
        var blocks = mock(MemberBlockApi.class);
        var members = mock(MemberDataEraser.class);
        var workflow = new AccountDeletionWorkflow(contexts, roots, appreciations, progress, visits,
                memberships, blocks, members);
        var memberId = new MemberId(7L);

        workflow.delete(memberId);

        verifyDeletionOrder(contexts, appreciations, progress, visits,
                memberships, blocks, members, memberId);
    }

    private void verifyDeletionOrder(AppreciationContextApi contexts, AppreciationDataEraser appreciations,
                                     ReadingProgressApi progress, PublicRoomVisitApi visits,
                                     ClubMembershipApi memberships,
                                     MemberBlockApi blocks, MemberDataEraser members, MemberId memberId) {
        InOrder ordered = inOrder(contexts, appreciations, progress, visits, memberships, blocks, members);
        ordered.verify(contexts).detachAll(List.of());
        ordered.verify(appreciations).eraseAuthoredBy(memberId);
        ordered.verify(appreciations).eraseViewsBy(memberId);
        ordered.verify(appreciations).eraseReportsBy(memberId);
        ordered.verify(progress).erase(memberId);
        ordered.verify(visits).erase(memberId);
        ordered.verify(memberships).erase(memberId);
        ordered.verify(blocks).eraseAllInvolving(memberId);
        ordered.verify(members).erase(memberId);
    }
}
