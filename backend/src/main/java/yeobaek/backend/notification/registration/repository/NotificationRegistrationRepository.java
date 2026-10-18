package yeobaek.backend.notification.registration.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yeobaek.backend.notification.registration.domain.NotificationRegistration;

@Repository
public interface NotificationRegistrationRepository extends JpaRepository<NotificationRegistration, Long> {
    List<NotificationRegistration> findAllByMember_IdIn(List<Long> memberIds);
}
