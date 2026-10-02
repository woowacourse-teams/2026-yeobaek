package yeobaek.backend.bootstrap;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yeobaek.backend.collaboration.api.SpaceContentBindingCapability;
import yeobaek.backend.collaboration.api.SpaceContentBindingRegistry;

@Configuration
public class SpaceContentBindingConfiguration {

    @Bean
    SpaceContentBindingRegistry spaceContentBindings(List<SpaceContentBindingCapability> capabilities) {
        CapabilityRegistry<String, SpaceContentBindingCapability> registry =
                new CapabilityRegistry<>(capabilities, SpaceContentBindingCapability::supportedKind);
        return registry::get;
    }
}
