package yeobaek.backend.preregistration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.preregistration.domain.PreRegistration;

public interface PreRegistrationRepository extends JpaRepository<PreRegistration, Long> {

    @Query("select (count(p) > 0) from PreRegistration p where p.email.value = :email")
    boolean existsByEmail(@Param("email") String email);
}
