package yeobaek.backend.bootstrap;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yeobaek.backend.appreciation.api.AppreciationSubtypeEraser;
import yeobaek.backend.appreciation.api.AppreciationSubtypeEraserRegistry;
import yeobaek.backend.appreciation.api.UnsupportedAppreciationKindFailure;

@Configuration
public class AppreciationCapabilityConfiguration {

    @Bean
    AppreciationSubtypeEraserRegistry appreciationSubtypeErasers(List<AppreciationSubtypeEraser> erasers) {
        CapabilityRegistry<String, AppreciationSubtypeEraser> registry =
                new CapabilityRegistry<>(erasers, AppreciationSubtypeEraser::supportedKind);
        List<AppreciationSubtypeEraser> registered = List.copyOf(erasers);
        return new AppreciationSubtypeEraserRegistry() {
            @Override
            public AppreciationSubtypeEraser get(String kind) {
                try {
                    return registry.get(kind);
                } catch (IllegalArgumentException failure) {
                    throw new UnsupportedAppreciationKindFailure(kind, failure);
                }
            }

            @Override
            public List<AppreciationSubtypeEraser> all() {
                return new java.util.ArrayList<>(registered);
            }
        };
    }
}
