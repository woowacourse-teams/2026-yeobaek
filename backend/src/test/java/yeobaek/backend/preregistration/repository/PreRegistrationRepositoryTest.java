package yeobaek.backend.preregistration.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import yeobaek.backend.preregistration.domain.PreRegistration;
import yeobaek.backend.support.IntegrationTest;

class PreRegistrationRepositoryTest extends IntegrationTest {

    @Autowired
    private PreRegistrationRepository preRegistrationRepository;

    @Test
    @DisplayName("사전신청을 저장하고 정규화 이메일의 존재 여부를 확인한다")
    void saveAndExistsByEmail() {
        PreRegistration saved = preRegistrationRepository.saveAndFlush(
                new PreRegistration("Reader@Example.com"));

        assertThat(saved.getId()).isNotNull();
        assertThat(preRegistrationRepository.existsByEmail("reader@example.com")).isTrue();
    }

    @Test
    @DisplayName("같은 정규화 이메일은 DB unique 제약으로 중복 저장할 수 없다")
    void rejectDuplicateEmail() {
        preRegistrationRepository.saveAndFlush(new PreRegistration("reader@example.com"));

        assertThatThrownBy(() -> preRegistrationRepository.saveAndFlush(
                new PreRegistration(" READER@example.com ")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
