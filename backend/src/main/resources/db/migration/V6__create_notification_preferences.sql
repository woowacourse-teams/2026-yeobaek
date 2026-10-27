-- 유형별 설정이 없던 기존 중복 등록은 같은 회원·FID의 가장 오래된 행만 유지한다.
DELETE duplicate_registration
FROM notification_registrations duplicate_registration
JOIN notification_registrations original_registration
  ON duplicate_registration.member_id = original_registration.member_id
 AND duplicate_registration.fid = original_registration.fid
 AND duplicate_registration.id > original_registration.id;

ALTER TABLE notification_registrations
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD CONSTRAINT uk_notification_registrations_member_fid UNIQUE (member_id, fid),
    DROP FOREIGN KEY fk_notification_registrations_member;

ALTER TABLE notification_registrations
    ADD CONSTRAINT fk_notification_registrations_member
        FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE;

CREATE TABLE notification_preferences (
    id BIGINT NOT NULL AUTO_INCREMENT,
    registration_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_preferences_registration_type UNIQUE (registration_id, notification_type),
    CONSTRAINT fk_notification_preferences_registration
        FOREIGN KEY (registration_id) REFERENCES notification_registrations (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- 기존 등록도 현재 지원하는 모든 유형을 ON으로 시작한다.
INSERT INTO notification_preferences (registration_id, notification_type, enabled, updated_at)
SELECT registration.id, types.notification_type, TRUE, CURRENT_TIMESTAMP(6)
FROM notification_registrations registration
CROSS JOIN (
    SELECT 'CLUB_JOIN' AS notification_type
    UNION ALL SELECT 'CLUB_COMMENT'
    UNION ALL SELECT 'PUBLIC_ROOM_COMMENT'
) types;
