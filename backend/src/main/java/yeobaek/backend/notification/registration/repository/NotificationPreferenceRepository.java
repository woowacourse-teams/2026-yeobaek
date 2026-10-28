package yeobaek.backend.notification.registration.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationPreference;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    List<NotificationPreference> findAllByRegistration_Id(Long registrationId);

    @Query("""
            select preference from NotificationPreference preference
            join fetch preference.registration registration
            where registration.member.id in :memberIds and registration.enabled = true
            """)
    List<NotificationPreference> findEnabledWithRegistrationsByMemberIds(@Param("memberIds") List<Long> memberIds);

    @Query("""
            select preference from NotificationPreference preference
            join fetch preference.registration registration
            where registration.id in :registrationIds
              and registration.firebaseInstallationId = :fid
              and registration.enabled = true
            """)
    List<NotificationPreference> findEnabledWithRegistrationsByRegistrationIdsAndFid(
            @Param("registrationIds") List<Long> registrationIds, @Param("fid") FirebaseInstallationId fid);

}
