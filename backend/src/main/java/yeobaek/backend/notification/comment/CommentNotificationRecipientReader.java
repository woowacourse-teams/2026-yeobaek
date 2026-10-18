package yeobaek.backend.notification.comment;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.repository.SentenceRepository;
import yeobaek.backend.club.domain.ClubMembers;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.member.domain.Members;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.notification.registration.service.NotificationRegistrationService;

@Service
@RequiredArgsConstructor
public class CommentNotificationRecipientReader {

    private final NotificationRegistrationService registrationService;
    private final CommentNotificationRecipientQueryRepository recipientQueryRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final SentenceRepository sentenceRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Set<String> findReadersInClub(Long clubId, Long sentenceId, Long writerId) {
        ClubMembers targets = new ClubMembers(clubMemberRepository.findAllJoinedWithMemberByClubId(clubId));
        Members recipients = new Members(targets.otherMembersAheadOf(
                sentenceRepository.getReferenceById(sentenceId), memberRepository.getReferenceById(writerId)));
        return registrationService.findByMembers(recipients).firebaseInstallationIds();
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Set<String> findPreviousCommentersInPublicRoom(Long publicRoomId, Long sentenceId, Long writerId) {
        return recipientQueryRepository.findFidsForPublicRoomComment(publicRoomId, sentenceId, writerId);
    }
}
