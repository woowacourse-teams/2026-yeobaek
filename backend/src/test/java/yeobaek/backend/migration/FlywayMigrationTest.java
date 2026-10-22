package yeobaek.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import yeobaek.backend.BackendApplication;
import yeobaek.backend.support.FirebaseTestConfiguration;

@Testcontainers
class FlywayMigrationTest {

    // 동명 테스트 설정이 메인 설정을 가리므로 실제 앱 기동 설정 파일을 직접 지정해 검증한다.
    private static final String MAIN_CONFIG_LOCATION = Path.of(
            "src", "main", "resources", "application.properties").toAbsolutePath().toUri().toString();

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    private static final String FID = "c" + "a".repeat(21);

    @BeforeEach
    void resetTestDatabase() {
        Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .cleanDisabled(false).load().clean();
    }

    @Test
    @DisplayName("메인 설정은 ddl-auto=validate를 사용하며 앱 재기동 시 기존 데이터를 보존한다")
    void mainConfigurationUsesHibernateValidationAndPreservesDataOnRestart() {
        try (ConfigurableApplicationContext context = startApplicationUsingMainConfiguration()) {
            // Hibernate가 Flyway의 스키마를 재생성하거나 삭제하지 않도록 메인 설정을 검증한다.
            assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
            assertThat(context.getBean(Flyway.class).validateWithResult().validationSuccessful).isTrue();
            assertThat(jdbcTemplate.queryForList(
                    "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank", String.class))
                    .containsExactly("1", "2", "3");
            jdbcTemplate.update("INSERT INTO members (nickname) VALUES (?)", "재기동 회원");
            jdbcTemplate.update("INSERT INTO notification_registrations (member_id, fid) "
                    + "SELECT id, ? FROM members WHERE nickname = ?", FID, "재기동 회원");
        }

        try (ConfigurableApplicationContext context = startApplicationUsingMainConfiguration()) {
            assertThat(context.getBean(JdbcTemplate.class).queryForList(
                    "SELECT CONCAT(m.nickname, ':', r.fid) FROM members m "
                            + "JOIN notification_registrations r ON r.member_id = m.id", String.class))
                    .containsExactly("재기동 회원:" + FID);
            assertThat(context.getBean(Flyway.class).migrate().migrationsExecuted).isZero();
        }
    }

    @Test
    @DisplayName("V2가 적용된 DB는 기존 데이터를 보존하면서 알림 등록 V3만 추가한다")
    void upgradeExistingV2Database() {
        Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .target("2").load().migrate();
        JdbcTemplate jdbc = jdbcTemplate();
        jdbc.update("INSERT INTO members (nickname) VALUES (?)", "기존 회원");

        Flyway flyway = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);

        try (ConfigurableApplicationContext context = startApplicationUsingMainConfiguration()) {
            assertThat(context.getBean(Flyway.class).validateWithResult().validationSuccessful).isTrue();
            assertThat(jdbc.queryForList("SELECT nickname FROM members", String.class)).containsExactly("기존 회원");
            assertThat(jdbc.queryForList(
                    "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank", String.class))
                    .containsExactly("1", "2", "3");
            assertThat(context.getBean(Flyway.class).migrate().migrationsExecuted).isZero();
        }
    }

    @Test
    @DisplayName("같은 FID를 여러 등록 정보로 저장할 수 있다")
    void allowDuplicateFids() {
        JdbcTemplate jdbc = migrateWithMember();

        jdbc.update("INSERT INTO notification_registrations (member_id, fid) VALUES (40, ?), (40, ?)", FID, FID);

        assertThat(jdbc.queryForList("SELECT id FROM notification_registrations WHERE fid = ?", Long.class, FID))
                .hasSize(2).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("FID의 대소문자가 다르면 서로 다른 등록 정보로 조회한다")
    void distinguishFidCase() {
        JdbcTemplate jdbc = migrateWithMember();
        String uppercaseFid = "cA" + "a".repeat(20);
        jdbc.update("INSERT INTO notification_registrations (member_id, fid) VALUES (40, ?), (40, ?)", FID, uppercaseFid);

        assertThat(jdbc.queryForList("SELECT fid FROM notification_registrations WHERE fid = ?", String.class, FID))
                .containsExactly(FID);
        assertThat(jdbc.queryForList(
                "SELECT fid FROM notification_registrations WHERE fid = ?", String.class, uppercaseFid))
                .containsExactly(uppercaseFid);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {99999L})
    @DisplayName("회원이 없거나 존재하지 않는 회원이면 알림 등록을 거절한다")
    void rejectRegistrationWithoutMember(Long memberId) {
        JdbcTemplate jdbc = migrateWithMember();

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO notification_registrations (member_id, fid) VALUES (?, ?)", memberId, FID))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FID가 null이면 알림 등록을 거절한다")
    void rejectNullFid() {
        JdbcTemplate jdbc = migrateWithMember();

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO notification_registrations (member_id, fid) VALUES (40, NULL)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("알림 등록이 남은 회원은 삭제를 거절하고 기존 등록 정보를 보존한다")
    void preventMemberDeletionWithRegistration() {
        JdbcTemplate jdbc = migrateWithMember();
        jdbc.update("INSERT INTO notification_registrations (member_id, fid) VALUES (40, ?)", FID);

        assertThatThrownBy(() -> jdbc.update("DELETE FROM members WHERE id = 40"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT nickname FROM members WHERE id = 40", String.class))
                .isEqualTo("등록 회원");
        assertThat(jdbc.queryForList(
                "SELECT fid FROM notification_registrations WHERE member_id = 40", String.class))
                .containsExactly(FID);
    }

    private JdbcTemplate migrateWithMember() {
        Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()).load().migrate();
        JdbcTemplate jdbc = jdbcTemplate();
        jdbc.update("INSERT INTO members (id, nickname) VALUES (40, '등록 회원')");
        return jdbc;
    }

    private JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()));
    }

    private ConfigurableApplicationContext startApplicationUsingMainConfiguration() {
        SpringApplication application = new SpringApplication(BackendApplication.class, FirebaseTestConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        return application.run(
                "--spring.config.location=" + MAIN_CONFIG_LOCATION,
                "--spring.datasource.url=" + MYSQL.getJdbcUrl(),
                "--spring.datasource.username=" + MYSQL.getUsername(),
                "--spring.datasource.password=" + MYSQL.getPassword(),
                "--posthog.enabled=false",
                "--fcm.project-id=yeobaek-test",
                "--spring.main.banner-mode=off",
                "--logging.level.root=ERROR");
    }
}
