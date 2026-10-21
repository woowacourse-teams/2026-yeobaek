package yeobaek.backend.notification.comment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.Sentence;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.domain.Comment;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.MemberBlock;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberBlockRepository;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationRegistration;
import yeobaek.backend.notification.registration.repository.NotificationRegistrationRepository;
import yeobaek.backend.publicroom.domain.PublicRoom;
import yeobaek.backend.publicroom.domain.PublicRoomMember;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.publicroom.repository.PublicRoomMemberRepository;
import yeobaek.backend.support.IntegrationTest;

class CommentNotificationRecipientServiceTest extends IntegrationTest {

    @Autowired
    private CommentNotificationRecipientService recipientService;

    @Autowired
    private BookManagementRepository bookRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private PassageRepository passageRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberBlockRepository memberBlockRepository;

    @Autowired
    private NotificationRegistrationRepository registrationRepository;

    @Autowired
    private PublicRoomRepository publicRoomRepository;

    @Autowired
    private PublicRoomMemberRepository publicRoomMemberRepository;

    @Autowired
    private CommentRepository commentRepository;

    private Club club;
    private Sentence sentence;
    private Passage nextPassage;
    private Member writer;

    @BeforeEach
    void setUp() {
        var book = bookRepository.save(new Book(new BookTitle("알림 도서"), null, null, 2, null));
        var chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        var passage = passageRepository.save(new Passage(chapter, 1, List.of(new SentenceContent("댓글 문장"))));
        nextPassage = passageRepository.save(new Passage(chapter, 2, List.of(new SentenceContent("다음 문장"))));
        sentence = passage.getSentences().getFirst();
        club = clubRepository.save(new Club(new ClubName("알림 모임"), book, new JoinCode("PUSH01")));
        writer = saveReader("작성자", 'c');
    }

    @Test
    @DisplayName("작성자를 차단한 모임 수신자의 모든 기기를 제외하고 다른 수신자는 유지한다")
    void excludeRecipientsBlockingWriter() {
        Member blocker = saveReader("차단한 독자", 'd');
        saveRegistration(blocker, 'e');
        saveReader("일반 독자", 'f');
        memberBlockRepository.save(new MemberBlock(blocker, writer));

        var fids = recipientService.findReadersInClub(club.getId(), sentence.getId(), writer.getId());

        assertThat(fids.firebaseInstallationIds()).containsExactly("f" + "a".repeat(21));
    }

    @Test
    @DisplayName("작성자가 수신자만 차단한 경우에는 모임 알림 대상에서 제외하지 않는다")
    void retainRecipientBlockedByWriter() {
        Member reader = saveReader("독자", 'd');
        memberBlockRepository.save(new MemberBlock(writer, reader));

        var fids = recipientService.findReadersInClub(club.getId(), sentence.getId(), writer.getId());

        assertThat(fids.firebaseInstallationIds()).containsExactly("d" + "a".repeat(21));
    }

    @Test
    @DisplayName("수신자가 작성자의 차단을 해제하면 다음 조회부터 모임 알림 대상에 포함한다")
    void includeRecipientAfterUnblockingWriter() {
        Member reader = saveReader("독자", 'd');
        var block = memberBlockRepository.save(new MemberBlock(reader, writer));
        assertThat(recipientService.findReadersInClub(club.getId(), sentence.getId(), writer.getId()).firebaseInstallationIds()).isEmpty();

        memberBlockRepository.deleteById(block.getId());

        assertThat(recipientService.findReadersInClub(club.getId(), sentence.getId(), writer.getId()).firebaseInstallationIds())
                .containsExactly("d" + "a".repeat(21));
    }

    private Member saveReader(String nickname, char fidPrefix) {
        var member = memberRepository.save(new Member(new Nickname(nickname)));
        var membership = new ClubMember(member, club);
        membership.updateProgress(nextPassage, LocalDateTime.of(2026, 10, 19, 10, 0));
        clubMemberRepository.save(membership);
        saveRegistration(member, fidPrefix);
        return member;
    }

    @Test
    @DisplayName("모임 수신자의 중복 FID 등록 ID를 모두 보관하고 같은 FID의 차단자 등록은 포함하지 않는다")
    void retainOnlySelectedClubRegistrationIds() {
        Member reader = saveReader("독자", 'd');
        var duplicate = saveRegistration(reader, 'd');
        Member blocker = saveReader("차단자", 'd');
        memberBlockRepository.save(new MemberBlock(blocker, writer));
        var fid = duplicate.getFirebaseInstallationId();
        var selectedIds = registrationRepository.findAllByMember_IdIn(List.of(reader.getId())).stream()
                .map(NotificationRegistration::getId).toList();

        var snapshot = recipientService.findReadersInClub(club.getId(), sentence.getId(), writer.getId());
        var later = saveRegistration(reader, 'd');

        assertThat(snapshot.idsFor(fid)).containsExactlyInAnyOrderElementsOf(selectedIds)
                .doesNotContain(later.getId());
        assertThat(snapshot.firebaseInstallationIds()).containsExactly(fid.value());
    }

    @Test
    @DisplayName("공개방은 이전 댓글 작성자의 등록 ID를 보관하고 같은 FID의 비참여자 등록은 제외한다")
    void retainOnlySelectedPublicRoomRegistrationIds() {
        var room = publicRoomRepository.save(new PublicRoom(club.getBook()));
        Member reader = saveReader("댓글 독자", 'd');
        publicRoomMemberRepository.save(new PublicRoomMember(reader, room));
        commentRepository.save(new Comment(room, reader, sentence, new CommentContent("이전 댓글")));
        var duplicate = saveRegistration(reader, 'd');
        saveReader("비참여자", 'd');
        var fid = duplicate.getFirebaseInstallationId();
        var selectedIds = registrationRepository.findAllByMember_IdIn(List.of(reader.getId())).stream()
                .map(NotificationRegistration::getId).toList();

        var snapshot = recipientService.findPreviousCommentersInPublicRoom(
                room.getId(), sentence.getId(), writer.getId());

        assertThat(snapshot.idsFor(fid)).containsExactlyInAnyOrderElementsOf(selectedIds);
        assertThat(snapshot.firebaseInstallationIds()).containsExactly(fid.value());
    }

    private NotificationRegistration saveRegistration(Member member, char prefix) {
        return registrationRepository.save(new NotificationRegistration(
                member, new FirebaseInstallationId(prefix + "a".repeat(21))));
    }
}
