package yeobaek.backend.application.account;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.erasure.AppreciationDataEraser;
import yeobaek.backend.appreciation.api.lifecycle.AppreciationRootApi;
import yeobaek.backend.space.api.club.ClubMembershipApi;
import yeobaek.backend.collaboration.api.context.AppreciationContextApi;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.member.api.block.MemberBlockApi;
import yeobaek.backend.member.api.MemberDataEraser;
import yeobaek.backend.space.api.publicroom.PublicRoomVisitApi;
import yeobaek.backend.reading.api.ReadingProgressApi;

@Service
@RequiredArgsConstructor
public class AccountDeletionWorkflow {

    private final AppreciationContextApi contextApi;
    private final AppreciationRootApi appreciationRoots;
    private final AppreciationDataEraser appreciationEraser;
    private final ReadingProgressApi readingProgressApi;
    private final PublicRoomVisitApi visitApi;
    private final ClubMembershipApi clubMembershipApi;
    private final MemberBlockApi blockApi;
    private final MemberDataEraser memberDataEraser;

    @Transactional
    public void delete(MemberId memberId) {
        contextApi.detachAll(appreciationRoots.findAuthoredBy(memberId).stream()
                .map(AppreciationRootApi.Root::id).toList());
        appreciationEraser.eraseAuthoredBy(memberId);
        appreciationEraser.eraseViewsBy(memberId);
        appreciationEraser.eraseReportsBy(memberId);
        readingProgressApi.erase(memberId);
        visitApi.erase(memberId);
        clubMembershipApi.erase(memberId);
        blockApi.eraseAllInvolving(memberId);
        memberDataEraser.erase(memberId);
    }
}
