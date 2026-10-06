package yeobaek.backend.bootstrap;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.space.spi.access.SpaceAccessCapability;
import yeobaek.backend.space.spi.access.SpaceAccessRegistry;

@Configuration
public class SpaceCapabilityConfiguration {

    @Bean
    SpaceAccessRegistry spaceAccessCapabilities(
            List<SpaceAccessCapability> capabilities) {
        CapabilityRegistry<SpaceKind, SpaceAccessCapability> registry =
                new CapabilityRegistry<>(capabilities, SpaceAccessCapability::supportedKind);
        return registry::get;
    }
}
