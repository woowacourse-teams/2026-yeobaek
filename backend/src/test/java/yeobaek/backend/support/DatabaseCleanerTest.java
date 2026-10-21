package yeobaek.backend.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;

class DatabaseCleanerTest extends IntegrationTest {

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Test
    @DisplayName("데이터베이스를 정리하면 저장된 데이터와 자동 증가 값이 초기화된다")
    void cleanResetsTablesAndAutoIncrement() {
        Member first = memberRepository.save(new Member(new Nickname("민서")));

        databaseCleaner.clean();

        Member afterClean = memberRepository.save(new Member(new Nickname("지수")));
        assertThat(memberRepository.count()).isEqualTo(1);
        assertThat(afterClean.getId()).isEqualTo(first.getId());
        assertThat(jdbcTemplate.queryForObject("SELECT @@FOREIGN_KEY_CHECKS", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("데이터베이스를 정리해도 Flyway 이력과 검증 결과는 유지된다")
    void cleanPreservesMigrationHistory() {
        var history = jdbcTemplate.queryForList("SELECT * FROM flyway_schema_history ORDER BY installed_rank");
        assertThat(history).isNotEmpty();

        databaseCleaner.clean();

        assertThat(jdbcTemplate.queryForList("SELECT * FROM flyway_schema_history ORDER BY installed_rank"))
                .isEqualTo(history);
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }
}
