package yeobaek.backend.notification.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.google.firebase.messaging.FirebaseMessagingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.Sentence;
import yeobaek.backend.book.domain.vo.ContentSequence;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.book.repository.SentenceRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.domain.ClubCommentCreated;
import yeobaek.backend.comment.domain.Comment;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.comment.repository.CommentReportRepository;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.comment.repository.CommentViewRepository;
import yeobaek.backend.comment.service.CommentService;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberBlockRepository;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.notification.FcmMessageFactory;
import yeobaek.backend.notification.FcmPushSender;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationRegistration;
import yeobaek.backend.notification.registration.domain.NotificationRegistrations;
import yeobaek.backend.notification.registration.service.NotificationRegistrationService;
import yeobaek.backend.publicroom.domain.PublicRoom;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.support.LogCapture;

@SpringJUnitConfig(CommentNotificationTransactionTest.TestConfig.class)
class CommentNotificationTransactionTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private CommentRepository commentRepository;

    @MockitoBean
    private CommentReportRepository commentReportRepository;

    @MockitoBean
    private CommentViewRepository commentViewRepository;

    @MockitoBean
    private ClubRepository clubRepository;

    @MockitoBean
    private ClubMemberRepository clubMemberRepository;

    @MockitoBean
    private SentenceRepository sentenceRepository;

    @MockitoBean
    private PassageRepository passageRepository;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private MemberBlockRepository memberBlockRepository;

    @MockitoBean
    private PublicRoomRepository publicRoomRepository;

    @MockitoBean
    private NotificationRegistrationService registrationService;

    @MockitoBean
    private CommentNotificationRecipientQueryRepository recipientQueryRepository;

    @MockitoBean
    private FcmPushSender pushSender;

    private Connection commentConnection;
    private Connection recipientConnection;

    @BeforeEach
    void setUp() throws SQLException {
        commentConnection = mock(Connection.class);
        recipientConnection = mock(Connection.class);
        given(dataSource.getConnection()).willReturn(commentConnection, recipientConnection);
        given(commentConnection.getAutoCommit()).willReturn(true);
        given(recipientConnection.getAutoCommit()).willReturn(true);

        var writer = new Member(new Nickname("작성자"));
        ReflectionTestUtils.setField(writer, "id", 7L);
        var club = mock(Club.class);
        var sentence = mock(Sentence.class);
        var passage = mock(Passage.class);
        var writerMembership = mock(ClubMember.class);
        given(passage.getSequence()).willReturn(new ContentSequence(1));
        given(sentence.getPassage()).willReturn(passage);
        given(club.isReading(sentence)).willReturn(true);
        given(writerMembership.getClub()).willReturn(club);
        given(writerMembership.getMember()).willReturn(writer);
        given(clubRepository.findById(5L)).willReturn(Optional.of(club));
        given(clubMemberRepository.findJoinedByMemberIdAndClubId(7L, 5L))
                .willReturn(Optional.of(writerMembership));
        given(sentenceRepository.findById(13L)).willReturn(Optional.of(sentence));
        given(sentenceRepository.getReferenceById(13L)).willReturn(sentence);
        given(memberRepository.getReferenceById(7L)).willReturn(writer);
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 11L);
            return comment;
        });

        var reader = new Member(new Nickname("독자"));
        ReflectionTestUtils.setField(reader, "id", 9L);
        var readerMembership = mock(ClubMember.class);
        var lastRead = mock(Passage.class);
        given(lastRead.getSequence()).willReturn(new ContentSequence(2));
        given(readerMembership.getLastReadPassage()).willReturn(lastRead);
        given(readerMembership.getMember()).willReturn(reader);
        given(clubMemberRepository.findAllJoinedWithMemberByClubId(5L)).willReturn(List.of(readerMembership));
        String fid = "c" + "a".repeat(21);
        given(registrationService.findByMembers(any())).willReturn(new NotificationRegistrations(List.of(
                new NotificationRegistration(reader, new FirebaseInstallationId(fid)))));

        var room = mock(PublicRoom.class);
        given(room.isReading(sentence)).willReturn(true);
        given(publicRoomRepository.findWithBookById(8L)).willReturn(Optional.of(room));
        given(recipientQueryRepository.findFidsForPublicRoomComment(8L, 13L, 7L)).willReturn(Set.of(fid));
    }

    @Test
    @DisplayName("모임 알림은 댓글 커밋과 별도 수신자 조회 커밋이 끝난 뒤 발송한다")
    void sendClubNotificationAfterBothCommits() throws Exception {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var response = commentService.create(7L, 5L, 13L, new CommentContent("댓글"));

            assertThat(response.commentId()).isEqualTo(11L);
            verifyNoInteractions(pushSender, registrationService);
        });

        var order = inOrder(commentConnection, recipientConnection, pushSender);
        order.verify(commentConnection).commit();
        order.verify(recipientConnection).commit();
        order.verify(pushSender).send(any());
    }

    @Test
    @DisplayName("공개방 알림은 별도 조회를 마친 뒤 요청 스레드에서 동기로 발송한다")
    void sendPublicRoomNotificationOnRequestThread() throws Exception {
        Thread requestThread = Thread.currentThread();
        doAnswer(invocation -> {
            assertThat(Thread.currentThread()).isSameAs(requestThread);
            return null;
        }).when(pushSender).send(any());

        var response = commentService.createInPublicRoom(7L, 8L, 13L, new CommentContent("댓글"));

        assertThat(response.commentId()).isEqualTo(11L);
        var order = inOrder(commentConnection, recipientQueryRepository, recipientConnection, pushSender);
        order.verify(commentConnection).commit();
        order.verify(recipientQueryRepository).findFidsForPublicRoomComment(8L, 13L, 7L);
        order.verify(recipientConnection).commit();
        order.verify(pushSender).send(any());
    }

    @Test
    @DisplayName("댓글 트랜잭션을 롤백하면 수신자 조회와 발송을 하지 않는다")
    void skipNotificationOnRollback() throws SQLException {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            commentService.create(7L, 5L, 13L, new CommentContent("댓글"));
            status.setRollbackOnly();
        });

        verify(commentConnection).rollback();
        verifyNoInteractions(registrationService, pushSender);
    }

    @Test
    @DisplayName("댓글 커밋 자체가 실패하면 알림을 발송하지 않는다")
    void skipNotificationWhenCommitFails() throws SQLException {
        doThrow(new SQLException("commit failed")).when(commentConnection).commit();

        assertThatThrownBy(() -> commentService.createInPublicRoom(7L, 8L, 13L, new CommentContent("댓글")))
                .isInstanceOf(TransactionSystemException.class);

        verifyNoInteractions(recipientQueryRepository, pushSender);
    }

    @Test
    @DisplayName("FCM 예외는 실패 로그를 남기고 커밋된 댓글의 응답을 유지한다")
    void keepResponseWhenFcmFails() throws Exception {
        doThrow(mock(FirebaseMessagingException.class)).when(pushSender).send(any());

        try (var logs = new LogCapture(CommentNotificationListener.class.getName())) {
            var response = commentService.create(7L, 5L, 13L, new CommentContent("댓글"));

            assertThat(response.commentId()).isEqualTo(11L);
            assertThat(logs.field(logs.event("notification.comment.send", "failure"), "commentId"))
                    .isEqualTo(11L);
        }
        verify(commentConnection).commit();
    }

    @Test
    @DisplayName("수신자 DB 조회 실패는 댓글 식별자와 함께 기록하고 댓글 응답을 유지한다")
    void logRecipientDatabaseFailure() {
        given(recipientQueryRepository.findFidsForPublicRoomComment(8L, 13L, 7L))
                .willThrow(new DataAccessResourceFailureException("database unavailable"));

        try (var logs = new LogCapture(CommentNotificationListener.class.getName())) {
            var response = commentService.createInPublicRoom(7L, 8L, 13L, new CommentContent("댓글"));

            assertThat(response.commentId()).isEqualTo(11L);
            assertThat(logs.field(logs.event("notification.comment.send", "failure"), "commentId"))
                    .isEqualTo(11L);
        }
        verifyNoInteractions(pushSender);
    }

    @Test
    @DisplayName("커밋 후 조회 예외도 댓글 응답을 바꾸지 않고 조회 트랜잭션만 롤백한다")
    void keepResponseWhenRecipientQueryFails() throws SQLException {
        given(recipientQueryRepository.findFidsForPublicRoomComment(8L, 13L, 7L))
                .willThrow(new IllegalStateException("query failed"));

        var response = commentService.createInPublicRoom(7L, 8L, 13L, new CommentContent("댓글"));

        assertThat(response.commentId()).isEqualTo(11L);
        verify(commentConnection).commit();
        verify(recipientConnection).rollback();
        verifyNoInteractions(pushSender);
    }

    @Test
    @DisplayName("트랜잭션 없이 발행한 이벤트는 알림을 보내지 않는다")
    void skipNotificationWithoutTransaction() {
        eventPublisher.publishEvent(new ClubCommentCreated(11L, 5L, 13L, 7L));

        verifyNoInteractions(registrationService, pushSender, dataSource);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @Import({CommentService.class, CommentNotificationListener.class, CommentNotificationService.class,
            CommentNotificationRecipientReader.class, FcmMessageFactory.class})
    static class TestConfig {

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }
}
