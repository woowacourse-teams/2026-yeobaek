package yeobaek.backend.preregistration.service;

import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.preregistration.domain.PreRegistration;
import yeobaek.backend.preregistration.repository.PreRegistrationRepository;
import yeobaek.backend.support.ConflictException;
import yeobaek.backend.support.ErrorCode;

@Service
@RequiredArgsConstructor
public class PreRegistrationService {

    private static final String EMAIL_UNIQUE_CONSTRAINT = "uk_pre_registrations_email";

    private final PreRegistrationRepository preRegistrationRepository;

    @Transactional
    public void create(String email) {
        PreRegistration preRegistration = new PreRegistration(email);
        if (preRegistrationRepository.existsByEmail(preRegistration.getEmail())) {
            throw duplicateEmail();
        }

        try {
            preRegistrationRepository.saveAndFlush(preRegistration);
        } catch (DataIntegrityViolationException exception) {
            if (isEmailUniqueConstraintViolation(exception)) {
                throw new ConflictException(ErrorCode.PRE_REGISTRATION_ALREADY_EXISTS, exception);
            }
            throw exception;
        }
    }

    private boolean isEmailUniqueConstraintViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                return isEmailUniqueConstraint(constraintViolation);
            }
            cause = cause.getCause();
        }
        return false;
    }

    private boolean isEmailUniqueConstraint(ConstraintViolationException constraintViolation) {
        String constraintName = constraintViolation.getConstraintName();
        if (constraintViolation.getKind() != ConstraintKind.UNIQUE || constraintName == null) {
            return false;
        }

        if (EMAIL_UNIQUE_CONSTRAINT.equalsIgnoreCase(constraintName)) {
            return true;
        }

        int qualifierLength = constraintName.length() - EMAIL_UNIQUE_CONSTRAINT.length();
        return 0 < qualifierLength
                && '.' == constraintName.charAt(qualifierLength - 1)
                && constraintName.regionMatches(
                        true, qualifierLength, EMAIL_UNIQUE_CONSTRAINT, 0, EMAIL_UNIQUE_CONSTRAINT.length());
    }

    private ConflictException duplicateEmail() {
        return new ConflictException(ErrorCode.PRE_REGISTRATION_ALREADY_EXISTS);
    }
}
