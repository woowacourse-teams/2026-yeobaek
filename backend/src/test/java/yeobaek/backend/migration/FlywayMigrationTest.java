package yeobaek.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import yeobaek.backend.BackendApplication;

@Testcontainers
class FlywayMigrationTest {

    private static final String MAIN_CONFIG_LOCATION = Path.of(
            "src", "main", "resources", "application.properties").toAbsolutePath().toUri().toString();

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @BeforeEach
    void resetSchema() {
        Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .cleanDisabled(false)
                .load()
                .clean();
    }

    @Test
    @DisplayName("빈 DB는 JPA 검증 전에 마이그레이션하고 앱을 재기동해도 데이터를 보존한다")
    void emptyDatabaseMigratesBeforeJpaValidationAndRestartPreservesData() {
        try (ConfigurableApplicationContext context = startApplication("classpath:db/migration")) {
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
            assertThat(context.getBean(Flyway.class).validateWithResult().validationSuccessful).isTrue();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class)).isEqualTo(1);
            jdbcTemplate.update("INSERT INTO members (nickname) VALUES (?)", "재기동 회원");
        }

        try (ConfigurableApplicationContext context = startApplication("classpath:db/migration")) {
            assertThat(context.getBean(JdbcTemplate.class).queryForList(
                    "SELECT nickname FROM members", String.class)).containsExactly("재기동 회원");
            assertThat(context.getBean(Flyway.class).migrate().migrationsExecuted).isZero();
            assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        }
    }

    @Test
    @DisplayName("기존 DB를 baseline하면 데이터를 보존하고 이후 버전의 마이그레이션만 적용한다")
    void explicitBaselinePreservesExistingDataAndAppliesOnlyLaterChanges() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V1__init_schema.sql"))
                .execute(dataSource);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("INSERT INTO members (nickname) VALUES (?)", "기존 회원");
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration", "classpath:migration/flyway-upgrade")
                .baselineVersion("1")
                .load();

        flyway.baseline();

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList("SELECT nickname FROM members ORDER BY id", String.class))
                .containsExactly("기존 회원", "마이그레이션 회원");
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        try (ConfigurableApplicationContext context = startApplication(
                "classpath:db/migration,classpath:migration/flyway-upgrade")) {
            assertThat(context.isActive()).isTrue();
            assertThat(context.getBean(Flyway.class).migrate().migrationsExecuted).isZero();
        }
    }

    @Test
    @DisplayName("이력이 없는 기존 DB는 명시적인 baseline 없이 마이그레이션할 수 없다")
    void nonemptyDatabaseWithoutHistoryRequiresExplicitBaseline() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("CREATE TABLE existing_data (id BIGINT PRIMARY KEY)");
        jdbcTemplate.update("INSERT INTO existing_data (id) VALUES (1)");
        Flyway flyway = Flyway.configure().dataSource(dataSource).load();

        assertThatThrownBy(flyway::migrate).isInstanceOf(FlywayException.class)
                .hasMessageContaining("non-empty schema");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM existing_data", Integer.class)).isEqualTo(1);
    }

    private ConfigurableApplicationContext startApplication(String locations) {
        SpringApplication application = new SpringApplication(BackendApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        return application.run(
                "--spring.config.location=" + MAIN_CONFIG_LOCATION,
                "--spring.datasource.url=" + MYSQL.getJdbcUrl(),
                "--spring.datasource.username=" + MYSQL.getUsername(),
                "--spring.datasource.password=" + MYSQL.getPassword(),
                "--spring.flyway.locations=" + locations,
                "--posthog.enabled=false",
                "--spring.main.banner-mode=off",
                "--logging.level.root=ERROR");
    }
}
