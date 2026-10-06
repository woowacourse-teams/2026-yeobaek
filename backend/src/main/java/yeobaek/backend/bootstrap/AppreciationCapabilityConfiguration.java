package yeobaek.backend.bootstrap;

import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yeobaek.backend.appreciation.api.AppreciationKind;
import yeobaek.backend.appreciation.internal.erasure.AppreciationSubtypeEraser;

@Configuration
public class AppreciationCapabilityConfiguration {

    @Bean
    Map<AppreciationKind, AppreciationSubtypeEraser> appreciationSubtypeErasers(
            List<AppreciationSubtypeEraser> erasers
    ) {
        return CapabilityMap.from(erasers, AppreciationSubtypeEraser::supportedKind);
    }
}
