package yeobaek.backend.notification.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.ErrorCode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.Sentence;
import yeobaek.backend.book.domain.vo.ContentSequence;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.book.repository.SentenceRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.event.ClubSentenceCommentCreatedEvent;
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
import yeobaek.backend.notification.FcmPushSender;
import yeobaek.backend.notification.FcmFailureHandler;
import yeobaek.backend.notification.FcmRetryProcessor;
import yeobaek.backend.notification.domain.FcmRetryTask;
import yeobaek.backend.notification.domain.FcmSendFailure;
import yeobaek.backend.notification.domain.FcmSendResult;
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

    @MockitoBean
    private FcmRetryProcessor retryProcessor;

    @Autowired
    private QueuedTaskExecutor executor;

    private Connection commentConnection;
    private Connection recipientConnection;

    @BeforeEach
    void setUp() throws SQLException, FirebaseMessagingException {
        executor.reset();
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
        given(clubMemberRepository.findAllJoinedWithMemberByClubIdExcludingBlockers(5L, 7L))
                .willReturn(List.of(readerMembership));
        String fid = "c" + "a".repeat(21);
        given(registrationService.findByMembers(any())).willReturn(new NotificationRegistrations(List.of(
                new NotificationRegistration(reader, new FirebaseInstallationId(fid)))));

        var room = mock(PublicRoom.class);
        given(room.isReading(sentence)).willReturn(true);
        given(publicRoomRepository.findWithBookById(8L)).willReturn(Optional.of(room));
        given(recipientQueryRepository.findFidsForPublicRoomComment(8L, 13L, 7L)).willReturn(List.of(fid));
        given(registrationService.snapshotForSend(List.of(fid))).willReturn(new NotificationRegistrations(List.of()));
        given(pushSender.send(any(), any(), any(), any())).willReturn(new FcmSendResult(1, List.of()));
    }

    @Test
    @DisplayName("모임 알림은 댓글 커밋과 별도 수신자 조회 커밋이 끝난 뒤 발송한다")
    void sendClubNotificationAfterBothCommits() throws Exception {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            commentService.create(7L, 5L, 13L, new CommentContent("댓글"));

            verifyNoInteractions(pushSender, registrationService);
        });

        executor.runNextOnWorker();

        var order = inOrder(commentConnection, recipientConnection, pushSender);
        order.verify(commentConnection).commit();
        order.verify(recipientConnection).commit();
        order.verify(pushSender).send(any(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("저장 트랜잭션에서 확보한 문단 순서와 공간 정보를 이벤트를 거쳐 워커에 전달한다")
    void preserveNavigationDataThroughEvent(boolean club) throws Exception {
        Passage passage = sentenceRepository.findById(13L).orElseThrow().getPassage();
        given(passage.getSequence()).willReturn(new ContentSequence(12));

        if (club) {
            commentService.create(7L, 5L, 13L, new CommentContent("댓글"));
        } else {
            commentService.createInPublicRoom(7L, 8L, 13L, new CommentContent("댓글"));
        }
        given(passage.getSequence()).willReturn(new ContentSequence(1));
        executor.runNextOnWorker();

        Map<String, String> expected = Map.of(
                "type", club ? "CLUB_COMMENT" : "PUBLIC_ROOM_COMMENT",
                club ? "clubId" : "publicRoomId", club ? "5" : "8",
                "passageSequence", "12", "sentenceId", "13");
        verify(pushSender).send(any(), any(), any(), eq(expected));
    }

    @Test
    @DisplayName("공개방 알림은 응답 후 다른 스레드에서 조회하고 DB 트랜잭션 밖에서 발송한다")
    void sendPublicRoomNotificationOnWorkerThread() throws Exception {
        Thread requestThread = Thread.currentThread();
        doAnswer(invocation -> {
            assertThat(Thread.currentThread()).isNotSameAs(requestThread);
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return new FcmSendResult(1, List.of());
        }).when(pushSender).send(any(), any(), any(), any());

        var response = commentService.createInPublicRoom(7L, 8L, 13L, new CommentContent("댓글"));

        assertThat(response.commentId()).isEqualTo(11L);
        verifyNoInteractions(recipientQueryRepository, pushSender);
        executor.runNextOnWorker();
        verify(pushSender).send(any(), any(), any(), any());
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
        assertThat(executor.tasks).isEmpty();
    }

    @Test
    @DisplayName("댓글 커밋 자체가 실패하면 알림을 발송하지 않는다")
    void skipNotificationWhenCommitFails() throws SQLException {
        doThrow(new SQLException("commit failed")).when(commentConnection).commit();

        assertThatThrownBy(() -> commentService.createInPublicRoom(7L, 8L, 13L, new CommentContent("댓글")))
                .isInstanceOf(TransactionSystemException.class);

        verifyNoInteractions(recipientQueryRepository, pushSender);
        assertThat(executor.tasks).isEmpty();
    }

    @Test
    @DisplayName("FCM 예외는 실패 로그를 남기고 커밋된 댓글의 응답을 유지한다")
    void keepResponseWhenFcmFails() throws Exception {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        given(exception.getErrorCode()).willReturn(ErrorCode.UNKNOWN);
        doThrow(exception).when(pushSender).send(any(), any(), any(), any());

        try (var logs = new LogCapture(FcmFailureHandler.class.getName())) {
            var response = commentService.create(7L, 5L, 13L, new CommentContent("댓글"));
            executor.runNextOnWorker();

            assertThat(response.commentId()).isEqualTo(11L);
            assertThat(logs.field(logs.event("notification.fcm.logFailure", "failure"), "commentId"))
                    .isEqualTo(11L);
        }
        verify(commentConnection).commit();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("SDK 요청값 검증에 실패해도 ERROR를 남기고 커밋한 댓글 응답을 유지한다")
    void keepResponseWhenRequestIsInvalid(boolean club) throws Exception {
        doThrow(new IllegalArgumentException("민감한 요청값")).when(pushSender).send(any(), any(), any(), any());

        try (var logs = new LogCapture(FcmFailureHandler.class.getName())) {
            var response = club
                    ? commentService.create(7L, 5L, 13L, new CommentContent("댓글"))
                    : commentService.createInPublicRoom(7L, 8L, 13L, new CommentContent("댓글"));
            executor.runNextOnWorker();

            assertThat(response.commentId()).isEqualTo(11L);
            assertThat(logs.field(logs.event("notification.fcm.handleInvalidRequest", "failure"), "handlingCode"))
                    .isEqualTo("INVALID_ARGUMENT");
            assertThat(logs.structuredText()).doesNotContain("민감한 요청값");
        }
        verify(commentConnection).commit();
    }

    @Test
    @DisplayName("수신자 DB 조회 실패는 댓글 식별자와 함께 기록하고 댓글 응답을 유지한다")
    void logRecipientDatabaseFailure() throws Exception {
        given(recipientQueryRepository.findFidsForPublicRoomComment(8L, 13L, 7L))
                .willThrow(new DataAccessResourceFailureException("database unavailable"));

        try (var logs = new LogCapture(CommentNotificationWorker.class.getName())) {
            var response = commentService.createInPublicRoom(7L, 8L, 13L, new CommentContent("댓글"));
            executor.runNextOnWorker();

            assertThat(response.commentId()).isEqualTo(11L);
            assertThat(logs.field(logs.event("notification.comment.logFailure", "failure"), "commentId"))
                    .isEqualTo(11L);
        }
        verifyNoInteractions(pushSender);
    }

    @Test
    @DisplayName("커밋 후 조회 예외도 댓글 응답을 바꾸지 않고 조회 트랜잭션만 롤백한다")
    void keepResponseWhenRecipientQueryFails() throws Exception {
        given(recipientQueryRepository.findFidsForPublicRoomComment(8L, 13L, 7L))
                .willThrow(new IllegalStateException("query failed"));

        var response = commentService.createInPublicRoom(7L, 8L, 13L, new CommentContent("댓글"));
        executor.runNextOnWorker();

        assertThat(response.commentId()).isEqualTo(11L);
        verify(commentConnection).commit();
        verify(recipientConnection).rollback();
        verifyNoInteractions(pushSender);
    }

    @Test
    @DisplayName("트랜잭션 없이 발행한 이벤트는 알림을 보내지 않는다")
    void skipNotificationWithoutTransaction() {
        eventPublisher.publishEvent(new ClubSentenceCommentCreatedEvent(11L, 5L, 13L, 7L, 3, Instant.now()));

        verifyNoInteractions(registrationService, pushSender, dataSource);
        assertThat(executor.tasks).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("작업 제출이 거절돼도 댓글은 커밋하고 WARN으로 알림 누락을 기록한다")
    void keepCommentWhenSubmissionIsRejected(boolean club) throws SQLException {
        executor.reject = true;

        try (var logs = new LogCapture(CommentNotificationListener.class.getName())) {
            var response = club
                    ? commentService.create(7L, 5L, 13L, new CommentContent("댓글"))
                    : commentService.createInPublicRoom(7L, 8L, 13L, new CommentContent("댓글"));

            assertThat(response.commentId()).isEqualTo(11L);
            assertThat(logs.field(logs.event("notification.comment.submit", "rejected"), "commentId"))
                    .isEqualTo(11L);
        }
        verify(commentConnection).commit();
        verifyNoInteractions(registrationService, pushSender, recipientQueryRepository);
        assertThat(executor.tasks).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("모임·공개방 모두 저장된 댓글 생성 시각이 워커까지 전달되어 30분 만료를 판단한다")
    void retainCommentCreationTimeThroughEvent(boolean club) throws Exception {
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(31);
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 11L);
            ReflectionTestUtils.setField(comment, "createdAt", createdAt);
            return comment;
        });
        String fid = "c" + "a".repeat(21);
        given(registrationService.snapshotForSend(List.of(fid))).willReturn(new NotificationRegistrations(List.of(
                new NotificationRegistration(1L, mock(Member.class), new FirebaseInstallationId(fid)))));
        given(pushSender.send(any(), any(), any(), any())).willReturn(new FcmSendResult(0, List.of(
                new FcmSendFailure(fid, "UNAVAILABLE", "UNAVAILABLE", 503, null))));

        if (club) {
            commentService.create(7L, 5L, 13L, new CommentContent("댓글"));
        } else {
            commentService.createInPublicRoom(7L, 8L, 13L, new CommentContent("댓글"));
        }
        executor.runNextOnWorker();

        var task = ArgumentCaptor.forClass(FcmRetryTask.class);
        verify(retryProcessor).schedule(task.capture());
        assertThat(task.getValue().commentCreatedAt()).isEqualTo(createdAt.atZone(ZoneId.systemDefault()).toInstant());
        assertThat(task.getValue().stopReason(0)).contains("EXPIRED");
        verify(pushSender, times(1)).send(any(), any(), any(), any());
    }

    static class QueuedTaskExecutor implements TaskExecutor {

        private final Deque<Runnable> tasks = new ArrayDeque<>();
        private boolean reject;

        @Override
        public void execute(Runnable task) {
            if (reject) {
                throw new TaskRejectedException("queue full");
            }
            tasks.addLast(task);
        }

        void runNextOnWorker() throws Exception {
            Runnable task = tasks.removeFirst();
            try (var worker = Executors.newSingleThreadExecutor()) {
                worker.submit(task).get(5, TimeUnit.SECONDS);
            }
        }

        void reset() {
            tasks.clear();
            reject = false;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @Import({CommentService.class, CommentNotificationListener.class, CommentNotificationService.class,
            CommentNotificationRecipientReader.class, CommentNotificationWorker.class, FcmFailureHandler.class})
    static class TestConfig {

        @Bean
        QueuedTaskExecutor notificationTaskExecutor() {
            return new QueuedTaskExecutor();
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }
}
