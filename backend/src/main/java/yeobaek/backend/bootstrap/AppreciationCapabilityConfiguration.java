package yeobaek.backend.bootstrap;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yeobaek.backend.appreciation.api.AppreciationKind;
import yeobaek.backend.appreciation.spi.erasure.AppreciationSubtypeEraser;
import yeobaek.backend.appreciation.spi.erasure.AppreciationSubtypeEraserRegistry;
import yeobaek.backend.appreciation.spi.erasure.UnsupportedAppreciationKindException;

@Configuration
public class AppreciationCapabilityConfiguration {

    @Bean
    AppreciationSubtypeEraserRegistry appreciationSubtypeErasers(List<AppreciationSubtypeEraser> erasers) {
        CapabilityRegistry<AppreciationKind, AppreciationSubtypeEraser> registry =
                new CapabilityRegistry<>(erasers, AppreciationSubtypeEraser::supportedKind);
        List<AppreciationSubtypeEraser> registered = List.copyOf(erasers);
        return new AppreciationSubtypeEraserRegistry() {
            @Override
            public AppreciationSubtypeEraser get(AppreciationKind kind) {
                try {
                    return registry.get(kind);
                } catch (IllegalArgumentException failure) {
                    throw new UnsupportedAppreciationKindException(kind,
                            "저장된 감상 타입을 삭제할 구현을 찾을 수 없습니다: kind=" + kind.value(), failure);
                }
            }

            @Override
            public List<AppreciationSubtypeEraser> all() {
                return new java.util.ArrayList<>(registered);
            }
        };
    }
}
