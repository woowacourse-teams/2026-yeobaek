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

    @Query("""
            select preference from NotificationPreference preference
            join fetch preference.registration registration
            where registration.member.id in :memberIds and registration.enabled = true
              and exists (
                  select membership.id from ClubMember membership
                  where membership.member.id = registration.member.id
                    and membership.club.id = :clubId
                    and membership.status = yeobaek.backend.club.domain.ClubMemberStatus.JOINED
              )
              and not exists (
                  select setting.id from NotificationClubPreference setting
                  where setting.registrationId = registration.id
                    and setting.clubId = :clubId
                    and setting.enabled = false
              )
            """)
    List<NotificationPreference> findEnabledClubRegistrationsByMemberIds(
            @Param("memberIds") List<Long> memberIds, @Param("clubId") Long clubId);

    @Query("""
            select preference from NotificationPreference preference
            join fetch preference.registration registration
            where registration.id in :registrationIds
              and registration.firebaseInstallationId = :fid
              and registration.enabled = true
              and exists (
                  select membership.id from ClubMember membership
                  where membership.member.id = registration.member.id
                    and membership.club.id = :clubId
                    and membership.status = yeobaek.backend.club.domain.ClubMemberStatus.JOINED
              )
              and not exists (
                  select setting.id from NotificationClubPreference setting
                  where setting.registrationId = registration.id
                    and setting.clubId = :clubId
                    and setting.enabled = false
              )
            """)
    List<NotificationPreference> findEnabledClubRegistrationsByRegistrationIdsAndFid(
            @Param("registrationIds") List<Long> registrationIds, @Param("fid") FirebaseInstallationId fid,
            @Param("clubId") Long clubId);

}
