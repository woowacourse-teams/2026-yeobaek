package yeobaek.backend.bootstrap;

import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yeobaek.backend.collaboration.internal.binding.SpaceContentBindingCapability;
import yeobaek.backend.space.api.SpaceKind;

@Configuration
public class SpaceContentBindingConfiguration {

    @Bean
    Map<SpaceKind, SpaceContentBindingCapability> spaceContentBindings(
            List<SpaceContentBindingCapability> capabilities
    ) {
        return CapabilityMap.from(capabilities, SpaceContentBindingCapability::supportedKind);
    }
}
