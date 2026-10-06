package yeobaek.backend.bootstrap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.jupiter.api.Test;
import yeobaek.backend.appreciation.spi.erasure.AppreciationSubtypeEraser;
import yeobaek.backend.appreciation.api.AppreciationKind;

class AppreciationCapabilityConfigurationTest {

    @Test
    void rejectsDuplicateSubtypeRegistration() {
        AppreciationSubtypeEraser first = mock(AppreciationSubtypeEraser.class);
        AppreciationSubtypeEraser second = mock(AppreciationSubtypeEraser.class);
        given(first.supportedKind()).willReturn(AppreciationKind.COMMENT);
        given(second.supportedKind()).willReturn(AppreciationKind.COMMENT);

        assertThatThrownBy(() -> new AppreciationCapabilityConfiguration()
                .appreciationSubtypeErasers(List.of(first, second)))
                .isInstanceOf(IllegalStateException.class);
    }
}
