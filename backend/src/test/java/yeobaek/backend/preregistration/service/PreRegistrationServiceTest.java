package yeobaek.backend.preregistration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import yeobaek.backend.preregistration.domain.PreRegistration;
import yeobaek.backend.preregistration.repository.PreRegistrationRepository;
import yeobaek.backend.support.ConflictException;
import yeobaek.backend.support.ErrorCode;

@ExtendWith(MockitoExtension.class)
class PreRegistrationServiceTest {

    @Mock
    private PreRegistrationRepository preRegistrationRepository;

    @InjectMocks
    private PreRegistrationService preRegistrationService;

    @Test
    @DisplayName("정규화한 이메일로 중복을 확인하고 사전신청을 저장한다")
    void create() {
        given(preRegistrationRepository.existsByEmail("reader@example.com")).willReturn(false);

        preRegistrationService.create("  Reader@Example.COM  ");

        ArgumentCaptor<PreRegistration> captor = ArgumentCaptor.forClass(PreRegistration.class);
        verify(preRegistrationRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("reader@example.com");
    }

    @Test
    @DisplayName("이미 등록된 정규화 이메일이면 저장하지 않고 중복 예외를 반환한다")
    void rejectExistingEmail() {
        given(preRegistrationRepository.existsByEmail("reader@example.com")).willReturn(true);

        assertThatThrownBy(() -> preRegistrationService.create("READER@example.com"))
                .isInstanceOf(ConflictException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.PRE_REGISTRATION_ALREADY_EXISTS);

        verify(preRegistrationRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("동시 요청의 DB unique 위반도 같은 중복 예외로 변환한다")
    void mapUniqueConstraintRaceToConflict() {
        given(preRegistrationRepository.existsByEmail("reader@example.com")).willReturn(false);
        var uniqueViolation = new ConstraintViolationException(
                "duplicate email",
                new SQLException("duplicate email"),
                ConstraintKind.UNIQUE,
                "pre_registrations.uk_pre_registrations_email");
        given(preRegistrationRepository.saveAndFlush(any(PreRegistration.class)))
                .willThrow(new DataIntegrityViolationException("duplicate email", uniqueViolation));

        assertThatThrownBy(() -> preRegistrationService.create("reader@example.com"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("이미 사전신청한 이메일입니다.")
                .extracting("code")
                .isEqualTo(ErrorCode.PRE_REGISTRATION_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("같은 제약 조건명이더라도 unique 위반이 아니면 중복 신청으로 숨기지 않는다")
    void propagateNonUniqueConstraintViolation() {
        given(preRegistrationRepository.existsByEmail("reader@example.com")).willReturn(false);
        var constraintViolation = new ConstraintViolationException(
                "foreign key violation",
                new SQLException("foreign key violation"),
                ConstraintKind.FOREIGN_KEY,
                "pre_registrations.uk_pre_registrations_email");
        var unexpectedViolation = new DataIntegrityViolationException("unexpected constraint", constraintViolation);
        given(preRegistrationRepository.saveAndFlush(any(PreRegistration.class))).willThrow(unexpectedViolation);

        assertThatThrownBy(() -> preRegistrationService.create("reader@example.com"))
                .isSameAs(unexpectedViolation);
    }

    @Test
    @DisplayName("이메일 unique 외의 DB 무결성 오류는 중복 신청으로 숨기지 않는다")
    void propagateUnexpectedDataIntegrityViolation() {
        given(preRegistrationRepository.existsByEmail("reader@example.com")).willReturn(false);
        var unexpectedViolation = new DataIntegrityViolationException("unexpected constraint");
        given(preRegistrationRepository.saveAndFlush(any(PreRegistration.class)))
                .willThrow(unexpectedViolation);

        assertThatThrownBy(() -> preRegistrationService.create("reader@example.com"))
                .isSameAs(unexpectedViolation);
    }

    @Test
    @DisplayName("이메일이 유효하지 않으면 저장소를 호출하지 않는다")
    void rejectInvalidEmail() {
        assertThatThrownBy(() -> preRegistrationService.create("not-an-email"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(preRegistrationRepository, never()).existsByEmail(any());
        verify(preRegistrationRepository, never()).saveAndFlush(any());
    }
}
