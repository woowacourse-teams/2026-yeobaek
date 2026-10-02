package yeobaek.backend.space.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.space.api.SpaceRootLifecycleApi;
import yeobaek.backend.space.persistence.SpaceRootRepository;
import yeobaek.backend.support.IntegrationTest;

class SpaceRootLifecycleApiTest extends IntegrationTest {

    @Autowired
    private SpaceRootLifecycleApi lifecycleApi;

    @Autowired
    private SpaceRootRepository repository;

    @Test
    void createsCanonicalRootForExtension() {
        var id = lifecycleApi.create("THIRD_SPACE");

        assertThat(repository.findById(id.value())).get()
                .extracting("kind").isEqualTo("THIRD_SPACE");
    }
}
