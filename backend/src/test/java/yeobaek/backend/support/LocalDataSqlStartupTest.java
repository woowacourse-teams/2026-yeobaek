package yeobaek.backend.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;

class LocalDataSqlStartupTest {

    private static final String MAIN_CONFIG_LOCATION = Path.of(
            "src", "main", "resources", "application.properties")
            .toAbsolutePath()
            .toUri()
            .toString();

    @Test
    void localProfileSeedsDataAfterSchemaCreationAndOtherProfilesDoNot() {
        try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")) {
            mysql.start();

            try (ConfigurableApplicationContext localContext = startApplication(mysql, "local", false)) {
                verifyLocalSeed(localContext.getBean(JdbcTemplate.class));
            }

            try (ConfigurableApplicationContext defaultContext = startApplication(mysql, "default", true)) {
                assertThat(defaultContext.getEnvironment().getActiveProfiles()).isEmpty();
                verifySeedIsAbsent(defaultContext);
            }

            try (ConfigurableApplicationContext prodContext = startApplication(mysql, "prod", true)) {
                verifySeedIsAbsent(prodContext);
            }
        }
    }

    private ConfigurableApplicationContext startApplication(
            MySQLContainer<?> mysql, String profile, boolean recreateSchema) {
        SpringApplication application = new SpringApplication(yeobaek.backend.BackendApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);

        List<String> arguments = new ArrayList<>(List.of(
                "--spring.config.location=" + MAIN_CONFIG_LOCATION,
                "--spring.datasource.url=" + mysql.getJdbcUrl(),
                "--spring.datasource.username=" + mysql.getUsername(),
                "--spring.datasource.password=" + mysql.getPassword(),
                "--storage.s3.bucket=yeobaek-seed-test",
                "--storage.s3.region=ap-northeast-2",
                "--storage.s3.public-base-url=https://example.com",
                "--posthog.enabled=false",
                "--spring.main.banner-mode=off",
                "--logging.level.root=ERROR"));
        if (!"default".equals(profile)) {
            arguments.add("--spring.profiles.active=" + profile);
        }
        if (recreateSchema) {
            arguments.add("--spring.jpa.hibernate.ddl-auto=create");
        }

        return application.run(arguments.toArray(String[]::new));
    }

    private void verifyLocalSeed(JdbcTemplate jdbcTemplate) {
        assertThat(jdbcTemplate.queryForObject("select count(*) from books", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from authors", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from author_books", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from chapters", Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("select count(*) from passages", Integer.class)).isEqualTo(30);
        assertThat(jdbcTemplate.queryForObject("select count(*) from sentences", Integer.class)).isEqualTo(59);

        assertThat(jdbcTemplate.queryForList(
                "select `sequence` from passages order by `sequence`", Integer.class))
                .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, 30).boxed().toList());
        assertThat(jdbcTemplate.queryForList(
                "select concat(id, ':', nickname) from members order by id", String.class))
                .containsExactly("1:민서", "2:지수");
        assertThat(jdbcTemplate.queryForObject(
                "select concat(id, ':', name, ':', join_code) from clubs", String.class))
                .isEqualTo("1:교환독서 1기:A3F9KQ");
        assertThat(jdbcTemplate.queryForList("""
                select concat(`sequence`, ':', content)
                from sentences
                where passage_id = 2
                order by `sequence`
                """, String.class))
                .containsExactly(
                        "1:(개발용 시드 문단 2) 실제 본문은 인제스트 파이프라인으로 투입된다. ",
                        "2:이 문단은 읽기 화면·진도·댓글 개발을 위한 자리 채움 텍스트다.");
        assertThat(jdbcTemplate.query("""
                select m.nickname, p.`sequence`, cm.last_read_at, cm.status
                from club_members cm
                join members m on m.id = cm.member_id
                join passages p on p.id = cm.last_read_passage_id
                order by cm.id
                """, (row, rowNumber) -> tuple(row.getString("nickname"), row.getInt("sequence"),
                        row.getTimestamp("last_read_at").toLocalDateTime(), row.getString("status"))))
                .containsExactly(
                        tuple("민서", 10, LocalDateTime.of(2026, 8, 5, 14, 30), "JOINED"),
                        tuple("지수", 20, LocalDateTime.of(2026, 8, 5, 15, 0), "JOINED"));
        assertThat(jdbcTemplate.queryForList("""
                select concat(m.nickname, ':', c.content)
                from comments c
                join club_members cm on cm.id = c.club_member_id
                join members m on m.id = cm.member_id
                where c.sentence_id = 2
                order by c.id
                """, String.class))
                .containsExactly(
                        "민서:이 문장에서 멈칫했어요.",
                        "지수:저도 이 대목의 분위기가 오래 남았어요.");
        assertThat(jdbcTemplate.queryForList(
                "select created_at from comments order by id", LocalDateTime.class))
                .allSatisfy(createdAt -> assertThat(createdAt).isNotNull());

        jdbcTemplate.update("insert into members (nickname) values (?)", "다음 회원");
        assertThat(jdbcTemplate.queryForObject(
                "select id from members where nickname = '다음 회원'", Long.class)).isEqualTo(3L);
    }

    private void verifySeedIsAbsent(ConfigurableApplicationContext context) {
        assertThat(context.getEnvironment().getProperty("spring.application.name")).isEqualTo("backend");
        assertThat(context.getEnvironment().getProperty("spring.sql.init.mode")).isEqualTo("never");
        assertThat(context.getBean(JdbcTemplate.class)
                .queryForObject("select count(*) from books", Integer.class)).isZero();
    }
}
