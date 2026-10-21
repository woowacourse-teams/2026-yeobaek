package yeobaek.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import yeobaek.backend.BackendApplication;

@Testcontainers
class FlywayMigrationTest {

    // 동명 테스트 설정이 메인 설정을 가리므로 실제 앱 기동 설정 파일을 직접 지정해 검증한다.
    private static final String MAIN_CONFIG_LOCATION = Path.of(
            "src", "main", "resources", "application.properties").toAbsolutePath().toUri().toString();

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Test
    @DisplayName("메인 설정은 ddl-auto=validate를 사용하며 앱 재기동 시 기존 데이터를 보존한다")
    void mainConfigurationUsesHibernateValidationAndPreservesDataOnRestart() {
        try (ConfigurableApplicationContext context = startApplicationUsingMainConfiguration()) {
            // Hibernate가 Flyway의 스키마를 재생성하거나 삭제하지 않도록 메인 설정을 검증한다.
            assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
            assertThat(context.getBean(Flyway.class).validateWithResult().validationSuccessful).isTrue();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class)).isPositive();
            jdbcTemplate.update("INSERT INTO members (nickname) VALUES (?)", "재기동 회원");
        }

        try (ConfigurableApplicationContext context = startApplicationUsingMainConfiguration()) {
            assertThat(context.getBean(JdbcTemplate.class).queryForList(
                    "SELECT nickname FROM members", String.class)).containsExactly("재기동 회원");
            assertThat(context.getBean(Flyway.class).migrate().migrationsExecuted).isZero();
        }
    }

    private ConfigurableApplicationContext startApplicationUsingMainConfiguration() {
        SpringApplication application = new SpringApplication(BackendApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        return application.run(
                "--spring.config.location=" + MAIN_CONFIG_LOCATION,
                "--spring.datasource.url=" + MYSQL.getJdbcUrl(),
                "--spring.datasource.username=" + MYSQL.getUsername(),
                "--spring.datasource.password=" + MYSQL.getPassword(),
                "--posthog.enabled=false",
                "--spring.main.banner-mode=off",
                "--logging.level.root=ERROR");
    }
}
