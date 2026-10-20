-- MySQL 8.4, 대상 DB를 선택한 연결에서 한 번 적용한다.
-- members.id는 현재 엔티티와 같은 signed BIGINT, members 테이블은 InnoDB여야 한다.
-- 기존 테이블이 있으면 중단한다. IF NOT EXISTS로 불일치를 숨기지 않는다.
-- FID 중복 정책과 회원 탈퇴 정리는 별도 변경 사항이다. UNIQUE나 ON DELETE CASCADE는 추가하지 않는다.
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
