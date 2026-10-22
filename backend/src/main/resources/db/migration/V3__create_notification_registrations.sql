-- FID 중복 및 회원 탈퇴 정책은 기존 동작을 유지한다.
CREATE TABLE notification_registrations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    fid VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (id),
    INDEX ix_notification_registrations_member_id (member_id),
    INDEX ix_notification_registrations_fid (fid),
    CONSTRAINT fk_notification_registrations_member
        FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE = InnoDB;
