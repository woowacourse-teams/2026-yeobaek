package yeobaek.backend.appreciation.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import yeobaek.backend.appreciation.api.AppreciationKind;
import yeobaek.backend.appreciation.api.lifecycle.AppreciationRootApi;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.support.IntegrationTest;

class AppreciationRootKindRoundTripTest extends IntegrationTest {

    @Autowired
    private AppreciationRootApi roots;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void persistsAndReadsAnExtensionKindWithoutChangingTheCoreType() {
        AppreciationKind note = new AppreciationKind("NOTE");

        var created = roots.create(note, new MemberId(7L), LocalDateTime.of(2026, 10, 3, 12, 0));

        assertThat(roots.get(created.id()).kind()).isEqualTo(note);
        assertThat(jdbc.queryForObject("select kind from appreciations where id = ?", String.class,
                created.id().value())).isEqualTo("NOTE");
    }
}
