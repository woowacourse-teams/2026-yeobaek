package yeobaek.backend.application.account;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.AppreciationDataEraser;
import yeobaek.backend.club.api.ClubMembershipApi;
import yeobaek.backend.collaboration.api.AppreciationContextApi;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.member.api.MemberBlockApi;
import yeobaek.backend.member.api.MemberDataEraser;
import yeobaek.backend.reading.api.PublicRoomVisitApi;
import yeobaek.backend.reading.api.ReadingProgressApi;

@Service
@RequiredArgsConstructor
public class AccountDeletionWorkflow {

    private final AppreciationContextApi contextApi;
    private final AppreciationDataEraser appreciationEraser;
    private final ReadingProgressApi readingProgressApi;
    private final PublicRoomVisitApi visitApi;
    private final ClubMembershipApi clubMembershipApi;
    private final MemberBlockApi blockApi;
    private final MemberDataEraser memberDataEraser;

    @Transactional
    public void delete(MemberId memberId) {
        contextApi.eraseAuthoredBy(memberId);
        appreciationEraser.eraseAuthoredBy(memberId);
        appreciationEraser.eraseReactionsBy(memberId);
        readingProgressApi.erase(memberId);
        visitApi.erase(memberId);
        clubMembershipApi.erase(memberId);
        blockApi.eraseAllInvolving(memberId);
        memberDataEraser.erase(memberId);
    }
}
