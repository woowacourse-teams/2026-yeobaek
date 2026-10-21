package yeobaek.backend.notification.comment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.repository.SentenceRepository;
import yeobaek.backend.club.domain.ClubMembers;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.member.domain.Members;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.notification.registration.service.NotificationRegistrationService;
import yeobaek.backend.notification.registration.domain.NotificationRegistrations;

@Service
@RequiredArgsConstructor
public class CommentNotificationRecipientService {

    private final NotificationRegistrationService registrationService;
    private final CommentNotificationRecipientQueryRepository recipientQueryRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final SentenceRepository sentenceRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public NotificationRegistrations findReadersInClub(Long clubId, Long sentenceId, Long writerId) {
        ClubMembers targets = new ClubMembers(
                clubMemberRepository.findNotificationCandidatesWithReadingProgress(clubId, writerId));
        Members recipients = new Members(targets.otherMembersAheadInReadingProgress(
                sentenceRepository.getReferenceById(sentenceId), memberRepository.getReferenceById(writerId)));
        return registrationService.findByMembers(recipients);
    }

    @Transactional(readOnly = true)
    public NotificationRegistrations findPreviousCommentersInPublicRoom(Long publicRoomId, Long sentenceId, Long writerId) {
        return new NotificationRegistrations(
                recipientQueryRepository.findRegistrationsForPublicRoomComment(publicRoomId, sentenceId, writerId));
    }
}
