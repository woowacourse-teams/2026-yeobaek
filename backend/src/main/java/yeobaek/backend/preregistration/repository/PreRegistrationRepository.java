package yeobaek.backend.preregistration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yeobaek.backend.preregistration.domain.PreRegistration;

public interface PreRegistrationRepository extends JpaRepository<PreRegistration, Long> {

    boolean existsByEmail(String email);
}
