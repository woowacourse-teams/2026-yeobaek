package yeobaek.backend.bootstrap;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.collaboration.spi.binding.SpaceContentBindingCapability;
import yeobaek.backend.collaboration.spi.binding.SpaceContentBindingRegistry;

@Configuration
public class SpaceContentBindingConfiguration {

    @Bean
    SpaceContentBindingRegistry spaceContentBindings(List<SpaceContentBindingCapability> capabilities) {
        CapabilityRegistry<SpaceKind, SpaceContentBindingCapability> registry =
                new CapabilityRegistry<>(capabilities, SpaceContentBindingCapability::supportedKind);
        return registry::get;
    }
}
