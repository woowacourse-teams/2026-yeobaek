package yeobaek.backend.notification.registration.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationRegistration;

@Repository
public interface NotificationRegistrationRepository extends JpaRepository<NotificationRegistration, Long> {
    List<NotificationRegistration> findAllByMember_IdIn(List<Long> memberIds);

    List<NotificationRegistration> findAllByFirebaseInstallationId_ValueIn(List<String> fids);

    @Modifying
    @Query("""
            delete from NotificationRegistration registration
            where registration.id in :ids
              and registration.firebaseInstallationId = :fid
            """)
    int deleteUnregistered(@Param("ids") List<Long> ids, @Param("fid") FirebaseInstallationId fid);
}
