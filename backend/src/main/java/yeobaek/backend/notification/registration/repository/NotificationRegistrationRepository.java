package yeobaek.backend.notification.registration.repository;

import java.util.List;
import java.util.Optional;
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

    Optional<NotificationRegistration> findByMember_IdAndFirebaseInstallationId(
            Long memberId, FirebaseInstallationId fid);

    // 회원·FID 유니크 제약으로 동시 등록을 하나로 처리해요.
    // 중복이면 기존 ID를 그대로 대입해 기존 등록과 설정을 유지해요.
    @Modifying
    @Query(value = """
            insert into notification_registrations (member_id, fid, enabled)
            select id, :fid, true from members where id = :memberId
            on duplicate key update id = notification_registrations.id
            """, nativeQuery = true)
    int insertIfAbsent(@Param("memberId") Long memberId, @Param("fid") String fid);

    List<NotificationRegistration> findAllByIdInAndFirebaseInstallationId(
            List<Long> ids, FirebaseInstallationId fid);

    @Modifying
    @Query("""
            delete from NotificationRegistration registration
            where registration.id in :ids
              and registration.firebaseInstallationId = :fid
            """)
    int deleteUnregistered(@Param("ids") List<Long> ids, @Param("fid") FirebaseInstallationId fid);
}
