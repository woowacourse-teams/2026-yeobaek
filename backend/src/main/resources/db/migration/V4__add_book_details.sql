-- Descriptions are optional; unmeasured body lengths remain NULL until V5 completes.
ALTER TABLE books
    ADD COLUMN description TEXT NULL,
    ADD COLUMN body_character_count BIGINT NULL;
