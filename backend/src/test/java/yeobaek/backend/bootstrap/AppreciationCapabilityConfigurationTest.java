package yeobaek.backend.bootstrap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.jupiter.api.Test;
import yeobaek.backend.appreciation.api.AppreciationSubtypeEraser;

class AppreciationCapabilityConfigurationTest {

    @Test
    void rejectsDuplicateSubtypeRegistration() {
        AppreciationSubtypeEraser first = mock(AppreciationSubtypeEraser.class);
        AppreciationSubtypeEraser second = mock(AppreciationSubtypeEraser.class);
        given(first.supportedKind()).willReturn("COMMENT");
        given(second.supportedKind()).willReturn("COMMENT");

        assertThatThrownBy(() -> new AppreciationCapabilityConfiguration()
                .appreciationSubtypeErasers(List.of(first, second)))
                .isInstanceOf(IllegalStateException.class);
    }
}
