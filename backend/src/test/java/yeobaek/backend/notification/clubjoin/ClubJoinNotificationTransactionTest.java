package yeobaek.backend.notification.clubjoin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import yeobaek.backend.club.event.ClubMemberJoinedEvent;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.notification.FcmFailureHandler;
import yeobaek.backend.notification.FcmNotificationService;
import yeobaek.backend.notification.FcmPushSender;
import yeobaek.backend.notification.FcmRetryService;
import yeobaek.backend.notification.NotificationListener;
import yeobaek.backend.notification.comment.CommentNotificationService;
import yeobaek.backend.notification.domain.FcmSendResult;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationRegistration;
import yeobaek.backend.notification.registration.domain.NotificationRegistrations;
import yeobaek.backend.notification.registration.service.NotificationRegistrationService;
import yeobaek.backend.support.LogCapture;

@SpringJUnitConfig(ClubJoinNotificationTransactionTest.TestConfig.class)
class ClubJoinNotificationTransactionTest {
    @Autowired
    private PlatformTransactionManager transactions;
    @Autowired
    private ApplicationEventPublisher events;
    @Autowired
    private QueuedExecutor executor;
    @MockitoBean
    private DataSource dataSource;
    @MockitoBean
    private ClubJoinNotificationRecipientQueryRepository recipients;
    @MockitoBean
    private NotificationRegistrationService registrations;
    @MockitoBean
    private CommentNotificationService commentNotifications;
    @MockitoBean
    private FcmPushSender sender;
    @MockitoBean
    private FcmFailureHandler failures;
    @MockitoBean
    private FcmRetryService retries;

    private Connection joinConnection;
    private Connection recipientConnection;

    @BeforeEach
    void setUp() throws Exception {
        executor.tasks.clear();
        executor.reject = false;
        joinConnection = mock(Connection.class);
        recipientConnection = mock(Connection.class);
        given(dataSource.getConnection()).willReturn(joinConnection, recipientConnection);
        given(joinConnection.getAutoCommit()).willReturn(true);
        given(recipientConnection.getAutoCommit()).willReturn(true);
        given(recipients.findRecipientMemberIds(5L, 7L)).willReturn(List.of(9L));
        given(registrations.findByMemberIds(any(), any())).willReturn(new NotificationRegistrations(List.of(
                new NotificationRegistration(1L, mock(Member.class), new FirebaseInstallationId("c" + "a".repeat(21))))));
        given(sender.send(any(), any(), any(), any())).willReturn(new FcmSendResult(1, List.of()));
    }

    @Test
    @DisplayName("가입과 별도 수신자 조회가 커밋된 뒤 워커에서 발송해요")
    void sendAfterBothCommits() throws Exception {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            events.publishEvent(event());
            verifyNoInteractions(sender, recipients);
        });
        assertThat(executor.tasks).hasSize(1);

        executor.runNext();

        var order = inOrder(joinConnection, recipientConnection, sender);
        order.verify(joinConnection).commit();
        order.verify(recipientConnection).commit();
        order.verify(sender).send(any(), any(), any(), any());
    }

    @Test
    @DisplayName("가입 롤백 시 수신자 조회나 발송을 제출하지 않아요")
    void skipRolledBackJoin() throws Exception {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            events.publishEvent(event());
            status.setRollbackOnly();
        });

        assertThat(executor.tasks).isEmpty();
        verifyNoInteractions(sender, recipients);
        verify(joinConnection).rollback();
    }

    @Test
    @DisplayName("트랜잭션 없이 발행한 입장 이벤트는 발송하지 않아요")
    void skipEventWithoutTransaction() {
        events.publishEvent(event());

        assertThat(executor.tasks).isEmpty();
        verifyNoInteractions(sender, recipients, dataSource);
    }

    @Test
    @DisplayName("실행기 제출이 거절되어도 가입 커밋을 유지하고 모임 ID로 기록해요")
    void retainCommitWhenExecutorRejects() throws Exception {
        executor.reject = true;
        try (var logs = new LogCapture(NotificationListener.class.getName())) {
            new TransactionTemplate(transactions).executeWithoutResult(status -> events.publishEvent(event()));

            assertThat(logs.field(logs.event("notification.clubJoin.submit", "rejected"), "clubId"))
                    .isEqualTo(5L);
        }
        verify(joinConnection).commit();
        verifyNoInteractions(sender, recipients);
    }

    @Test
    @DisplayName("수신자 DB 실패는 조회만 롤백하고 모임 ID로 오류를 기록해요")
    void retainCommitWhenRecipientLookupFails() throws Exception {
        given(recipients.findRecipientMemberIds(5L, 7L))
                .willThrow(new DataAccessResourceFailureException("database unavailable"));
        new TransactionTemplate(transactions).executeWithoutResult(status -> events.publishEvent(event()));

        try (var logs = new LogCapture(FcmNotificationService.class.getName())) {
            executor.runNext();

            assertThat(logs.field(logs.event("notification.clubJoin.logFailure", "failure"), "clubId"))
                    .isEqualTo(5L);
        }
        verify(joinConnection).commit();
        verify(recipientConnection).rollback();
        verifyNoInteractions(sender);
    }

    private ClubMemberJoinedEvent event() {
        return new ClubMemberJoinedEvent(5L, 7L, Instant.now());
    }

    static class QueuedExecutor implements TaskExecutor {
        private final Deque<Runnable> tasks = new ArrayDeque<>();
        private boolean reject;

        @Override
        public void execute(Runnable task) {
            if (reject) {
                throw new TaskRejectedException("queue full");
            }
            tasks.addLast(task);
        }

        void runNext() throws Exception {
            Runnable task = tasks.removeFirst();
            try (var worker = Executors.newSingleThreadExecutor()) {
                worker.submit(task).get(5, TimeUnit.SECONDS);
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @Import({NotificationListener.class, ClubJoinNotificationService.class, FcmNotificationService.class})
    static class TestConfig {
        @Bean
        QueuedExecutor notificationTaskExecutor() {
            return new QueuedExecutor();
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }
}
