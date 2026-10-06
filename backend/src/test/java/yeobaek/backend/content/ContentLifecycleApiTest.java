package yeobaek.backend.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.location.LocationKind;
import yeobaek.backend.content.api.lifecycle.ContentLifecycleApi;
import yeobaek.backend.content.api.ContentNotFoundException;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.support.IntegrationTest;

class ContentLifecycleApiTest extends IntegrationTest {

    @Autowired
    private ContentLifecycleApi lifecycleApi;

    @Autowired
    private ContentApi contentApi;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsExtensionContentAndLocationWithoutAddingCoreKinds() {
        ContentKind futureContent = new ContentKind("FUTURE_CONTENT");
        LocationKind futureLocation = new LocationKind("FUTURE_LOCATION");
        var content = lifecycleApi.createContent(futureContent);
        var otherContent = lifecycleApi.createContent(futureContent);
        var location = lifecycleApi.createLocation(content, futureLocation);

        assertThat(contentApi.ownsLocation(content, location)).isTrue();
        assertThat(contentApi.ownsLocation(otherContent, location)).isFalse();
        assertThat(jdbcTemplate.queryForObject("select kind from contents where id = ?", String.class,
                content.value())).isEqualTo("FUTURE_CONTENT");
        assertThat(jdbcTemplate.queryForObject("select kind from content_locations where id = ?", String.class,
                location.value())).isEqualTo("FUTURE_LOCATION");
    }

    @Test
    void rejectsAnUnknownContentBeforePersistingItsLocation() {
        assertThatThrownBy(() -> lifecycleApi.createLocation(
                new ContentId(9999L), new LocationKind("FUTURE_LOCATION")))
                .isInstanceOf(ContentNotFoundException.class);
        assertThat(jdbcTemplate.queryForObject("select count(*) from content_locations", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from contents", Long.class)).isZero();
    }
}
