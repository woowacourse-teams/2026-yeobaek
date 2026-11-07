CREATE TABLE notification_club_preferences (
    id BIGINT NOT NULL AUTO_INCREMENT,
    registration_id BIGINT NOT NULL,
    club_id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_club_preferences_registration_club UNIQUE (registration_id, club_id),
    CONSTRAINT fk_notification_club_preferences_registration
        FOREIGN KEY (registration_id) REFERENCES notification_registrations (id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_club_preferences_club
        FOREIGN KEY (club_id) REFERENCES clubs (id) ON DELETE CASCADE
) ENGINE = InnoDB;
