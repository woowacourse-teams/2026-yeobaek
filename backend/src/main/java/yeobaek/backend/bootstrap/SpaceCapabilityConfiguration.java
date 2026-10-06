package yeobaek.backend.bootstrap;

import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.space.internal.access.SpaceAccessCapability;

@Configuration
public class SpaceCapabilityConfiguration {

    @Bean
    Map<SpaceKind, SpaceAccessCapability> spaceAccessCapabilities(
            List<SpaceAccessCapability> capabilities) {
        return CapabilityMap.from(capabilities, SpaceAccessCapability::supportedKind);
    }
}
