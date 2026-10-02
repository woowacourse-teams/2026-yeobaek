package yeobaek.backend.bootstrap;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yeobaek.backend.space.api.SpaceAccessCapability;
import yeobaek.backend.space.api.SpaceAccessRegistry;

@Configuration
public class SpaceCapabilityConfiguration {

    @Bean
    SpaceAccessRegistry spaceAccessCapabilities(
            List<SpaceAccessCapability> capabilities) {
        CapabilityRegistry<String, SpaceAccessCapability> registry =
                new CapabilityRegistry<>(capabilities, SpaceAccessCapability::supportedKind);
        return registry::get;
    }
}
