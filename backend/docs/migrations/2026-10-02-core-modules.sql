-- Core module schema expand/backfill for the approved M1 cutover.
-- Preconditions: stop the old application and every writer before running this file.
-- MySQL DDL auto-commits. This file therefore rejects a partial/repeated run before
-- touching source tables and creates durable source backups before the first ALTER.

CREATE TEMPORARY TABLE core_module_migration_guard (
    violation_count BIGINT NOT NULL,
    CONSTRAINT ck_core_module_migration_guard CHECK (violation_count = 0)
);

-- Reject every partial/repeated state. Durable backups are intentionally part of the
-- guard: the migration never overwrites or drops a previous recovery copy.
INSERT INTO core_module_migration_guard (violation_count)
SELECT COUNT(*)
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
      'spaces', 'contents', 'content_locations', 'appreciations',
      'space_content_bindings', 'club_content_bindings',
      'public_room_content_bindings', 'appreciation_contexts',
      'reading_progresses', 'public_room_visits',
      'appreciation_views', 'appreciation_reports',
      'core_migration_backup_20261002_clubs',
      'core_migration_backup_20261002_public_rooms',
      'core_migration_backup_20261002_books',
      'core_migration_backup_20261002_passages',
      'core_migration_backup_20261002_sentences',
      'core_migration_backup_20261002_comments',
      'core_migration_backup_20261002_club_members',
      'core_migration_backup_20261002_public_room_activities',
      'core_migration_backup_20261002_comment_views',
      'core_migration_backup_20261002_comment_reports'
  );

INSERT INTO core_module_migration_guard (violation_count)
SELECT COUNT(*)
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND (
      (table_name = 'clubs' AND column_name IN ('space_id', 'space_kind'))
      OR (table_name = 'public_rooms' AND column_name IN ('space_id', 'space_kind'))
      OR (table_name = 'books' AND column_name IN ('content_id', 'content_kind'))
      OR (table_name = 'passages' AND column_name IN ('location_id', 'location_kind'))
      OR (table_name = 'sentences' AND column_name IN ('location_id', 'location_kind'))
      OR (table_name = 'comments' AND column_name = 'appreciation_kind')
  );

-- Reject contradictory source rows before any persistent change.
INSERT INTO core_module_migration_guard (violation_count)
SELECT
    (SELECT COUNT(*) FROM comments c
      WHERE NOT (
          (c.club_member_id IS NOT NULL AND c.public_room_id IS NULL AND c.writer_id IS NULL)
          OR (c.club_member_id IS NULL AND c.public_room_id IS NOT NULL AND c.writer_id IS NOT NULL)
      ))
  + (SELECT COUNT(*) FROM club_members cm
      WHERE (cm.last_read_passage_id IS NULL) <> (cm.last_read_at IS NULL))
  + (SELECT COUNT(*) FROM public_room_activities pra
      WHERE (pra.last_read_passage_id IS NULL) <> (pra.last_read_at IS NULL))
  + (SELECT COUNT(*) FROM club_members cm
      JOIN clubs c ON c.id = cm.club_id
      JOIN passages p ON p.id = cm.last_read_passage_id
      JOIN chapters ch ON ch.id = p.chapter_id
      WHERE ch.book_id <> c.book_id)
  + (SELECT COUNT(*) FROM public_room_activities pra
      JOIN public_rooms pr ON pr.id = pra.public_room_id
      JOIN passages p ON p.id = pra.last_read_passage_id
      JOIN chapters ch ON ch.id = p.chapter_id
      WHERE ch.book_id <> pr.book_id)
  + (SELECT COUNT(*) FROM comments c
      JOIN sentences s ON s.id = c.sentence_id
      JOIN passages p ON p.id = s.passage_id
      JOIN chapters ch ON ch.id = p.chapter_id
      LEFT JOIN club_members cm ON cm.id = c.club_member_id
      LEFT JOIN clubs cl ON cl.id = cm.club_id
      LEFT JOIN public_rooms pr ON pr.id = c.public_room_id
      WHERE ch.book_id <> COALESCE(cl.book_id, pr.book_id))
  + (SELECT COUNT(*) FROM comments c
      LEFT JOIN club_members cm ON cm.id = c.club_member_id
      WHERE c.club_member_id IS NOT NULL AND cm.id IS NULL)
  + (SELECT COUNT(*) FROM comments c
      LEFT JOIN public_rooms pr ON pr.id = c.public_room_id
      LEFT JOIN members m ON m.id = c.writer_id
      WHERE c.public_room_id IS NOT NULL AND (pr.id IS NULL OR m.id IS NULL))
  + (SELECT COUNT(*) FROM comment_views cv
      LEFT JOIN members m ON m.id = cv.member_id
      LEFT JOIN comments c ON c.id = cv.comment_id
      WHERE m.id IS NULL OR c.id IS NULL)
  + (SELECT COUNT(*) FROM comment_reports cr
      LEFT JOIN members m ON m.id = cr.reporter_id
      LEFT JOIN comments c ON c.id = cr.comment_id
      WHERE m.id IS NULL OR c.id IS NULL);

-- The offset formula used below must stay inside signed BIGINT.
INSERT INTO core_module_migration_guard (violation_count)
SELECT
    (COALESCE((SELECT MAX(id) FROM clubs), 0)
        > 9223372036854775807 - COALESCE((SELECT MAX(id) FROM public_rooms), 0))
  + (COALESCE((SELECT MAX(id) FROM passages), 0)
        > 9223372036854775807 - COALESCE((SELECT MAX(id) FROM sentences), 0));

DROP TEMPORARY TABLE core_module_migration_guard;

CREATE TABLE core_migration_backup_20261002_clubs LIKE clubs;
INSERT INTO core_migration_backup_20261002_clubs SELECT * FROM clubs;
CREATE TABLE core_migration_backup_20261002_public_rooms LIKE public_rooms;
INSERT INTO core_migration_backup_20261002_public_rooms SELECT * FROM public_rooms;
CREATE TABLE core_migration_backup_20261002_books LIKE books;
INSERT INTO core_migration_backup_20261002_books SELECT * FROM books;
CREATE TABLE core_migration_backup_20261002_passages LIKE passages;
INSERT INTO core_migration_backup_20261002_passages SELECT * FROM passages;
CREATE TABLE core_migration_backup_20261002_sentences LIKE sentences;
INSERT INTO core_migration_backup_20261002_sentences SELECT * FROM sentences;
CREATE TABLE core_migration_backup_20261002_comments LIKE comments;
INSERT INTO core_migration_backup_20261002_comments SELECT * FROM comments;
CREATE TABLE core_migration_backup_20261002_club_members LIKE club_members;
INSERT INTO core_migration_backup_20261002_club_members SELECT * FROM club_members;
CREATE TABLE core_migration_backup_20261002_public_room_activities LIKE public_room_activities;
INSERT INTO core_migration_backup_20261002_public_room_activities SELECT * FROM public_room_activities;
CREATE TABLE core_migration_backup_20261002_comment_views LIKE comment_views;
INSERT INTO core_migration_backup_20261002_comment_views SELECT * FROM comment_views;
CREATE TABLE core_migration_backup_20261002_comment_reports LIKE comment_reports;
INSERT INTO core_migration_backup_20261002_comment_reports SELECT * FROM comment_reports;

-- These tables become dormant after cutover. Their legacy cross-domain foreign keys
-- would otherwise block canonical appreciation/member erasure even though the new app
-- neither writes nor cleans the legacy copies. Resolve actual constraint names from
-- metadata because Hibernate-generated names differ between environments.
SET @legacy_fk_clauses = (
    SELECT GROUP_CONCAT(CONCAT('DROP FOREIGN KEY `', constraint_name, '`') SEPARATOR ', ')
    FROM information_schema.referential_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'comments'
      AND referenced_table_name IN ('club_members', 'public_rooms', 'members', 'sentences')
);
SET @legacy_fk_sql = IF(@legacy_fk_clauses IS NULL, 'DO 0',
    CONCAT('ALTER TABLE comments ', @legacy_fk_clauses));
PREPARE core_drop_legacy_fks FROM @legacy_fk_sql;
EXECUTE core_drop_legacy_fks;
DEALLOCATE PREPARE core_drop_legacy_fks;

SET @legacy_fk_clauses = (
    SELECT GROUP_CONCAT(CONCAT('DROP FOREIGN KEY `', constraint_name, '`') SEPARATOR ', ')
    FROM information_schema.referential_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'comment_views'
);
SET @legacy_fk_sql = IF(@legacy_fk_clauses IS NULL, 'DO 0',
    CONCAT('ALTER TABLE comment_views ', @legacy_fk_clauses));
PREPARE core_drop_legacy_fks FROM @legacy_fk_sql;
EXECUTE core_drop_legacy_fks;
DEALLOCATE PREPARE core_drop_legacy_fks;

SET @legacy_fk_clauses = (
    SELECT GROUP_CONCAT(CONCAT('DROP FOREIGN KEY `', constraint_name, '`') SEPARATOR ', ')
    FROM information_schema.referential_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'comment_reports'
);
SET @legacy_fk_sql = IF(@legacy_fk_clauses IS NULL, 'DO 0',
    CONCAT('ALTER TABLE comment_reports ', @legacy_fk_clauses));
PREPARE core_drop_legacy_fks FROM @legacy_fk_sql;
EXECUTE core_drop_legacy_fks;
DEALLOCATE PREPARE core_drop_legacy_fks;

SET @legacy_fk_clauses = (
    SELECT GROUP_CONCAT(CONCAT('DROP FOREIGN KEY `', constraint_name, '`') SEPARATOR ', ')
    FROM information_schema.referential_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'public_room_activities'
);
SET @legacy_fk_sql = IF(@legacy_fk_clauses IS NULL, 'DO 0',
    CONCAT('ALTER TABLE public_room_activities ', @legacy_fk_clauses));
PREPARE core_drop_legacy_fks FROM @legacy_fk_sql;
EXECUTE core_drop_legacy_fks;
DEALLOCATE PREPARE core_drop_legacy_fks;

ALTER TABLE comment_views
    ADD CONSTRAINT fk_legacy_comment_views_actor_cleanup FOREIGN KEY (member_id)
        REFERENCES members (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_legacy_comment_views_target_cleanup FOREIGN KEY (comment_id)
        REFERENCES comments (id) ON DELETE CASCADE;

ALTER TABLE comment_reports
    ADD CONSTRAINT fk_legacy_comment_reports_actor_cleanup FOREIGN KEY (reporter_id)
        REFERENCES members (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_legacy_comment_reports_target_cleanup FOREIGN KEY (comment_id)
        REFERENCES comments (id) ON DELETE CASCADE;

ALTER TABLE public_room_activities
    ADD CONSTRAINT fk_legacy_room_activities_actor_cleanup FOREIGN KEY (member_id)
        REFERENCES members (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_legacy_room_activities_room_cleanup FOREIGN KEY (public_room_id)
        REFERENCES public_rooms (id) ON DELETE CASCADE;

CREATE TABLE spaces (
    id BIGINT NOT NULL AUTO_INCREMENT,
    kind VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_spaces_id_kind UNIQUE (id, kind)
);

CREATE TABLE contents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    kind VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_contents_id_kind UNIQUE (id, kind)
);

CREATE TABLE content_locations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    content_id BIGINT NOT NULL,
    kind VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_content_locations_id_kind UNIQUE (id, kind),
    CONSTRAINT uk_content_locations_id_content UNIQUE (id, content_id),
    CONSTRAINT fk_content_locations_content FOREIGN KEY (content_id) REFERENCES contents (id)
);

CREATE TABLE appreciations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    kind VARCHAR(64) NOT NULL,
    author_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_appreciations_id_kind UNIQUE (id, kind),
    CONSTRAINT fk_appreciations_author FOREIGN KEY (author_id) REFERENCES members (id)
);

SET @space_public_room_offset = COALESCE((SELECT MAX(id) FROM clubs), 0);
SET @location_sentence_offset = COALESCE((SELECT MAX(id) FROM passages), 0);

INSERT INTO spaces (id, kind)
SELECT id, 'CLUB' FROM clubs ORDER BY id;
INSERT INTO spaces (id, kind)
SELECT @space_public_room_offset + id, 'PUBLIC_ROOM' FROM public_rooms ORDER BY id;

INSERT INTO contents (id, kind)
SELECT id, 'BOOK' FROM books ORDER BY id;

INSERT INTO content_locations (id, content_id, kind)
SELECT p.id, ch.book_id, 'PASSAGE'
FROM passages p
JOIN chapters ch ON ch.id = p.chapter_id
ORDER BY p.id;
INSERT INTO content_locations (id, content_id, kind)
SELECT @location_sentence_offset + s.id, ch.book_id, 'SENTENCE'
FROM sentences s
JOIN passages p ON p.id = s.passage_id
JOIN chapters ch ON ch.id = p.chapter_id
ORDER BY s.id;

INSERT INTO appreciations (id, kind, author_id, created_at, updated_at)
SELECT c.id, 'COMMENT', COALESCE(c.writer_id, cm.member_id), c.created_at, c.updated_at
FROM comments c
LEFT JOIN club_members cm ON cm.id = c.club_member_id
ORDER BY c.id;

ALTER TABLE clubs
    ADD COLUMN space_id BIGINT NULL,
    ADD COLUMN space_kind VARCHAR(64) NULL;
UPDATE clubs SET space_id = id, space_kind = 'CLUB';
ALTER TABLE clubs
    MODIFY space_id BIGINT NOT NULL,
    MODIFY space_kind VARCHAR(64) NOT NULL,
    MODIFY book_id BIGINT NULL,
    ADD CONSTRAINT uk_clubs_space_id UNIQUE (space_id),
    ADD CONSTRAINT uk_clubs_space_identity UNIQUE (space_id, space_kind),
    ADD CONSTRAINT ck_clubs_space_kind CHECK (space_kind = 'CLUB'),
    ADD CONSTRAINT fk_clubs_space_identity FOREIGN KEY (space_id, space_kind)
        REFERENCES spaces (id, kind);

ALTER TABLE public_rooms
    ADD COLUMN space_id BIGINT NULL,
    ADD COLUMN space_kind VARCHAR(64) NULL;
UPDATE public_rooms
SET space_id = @space_public_room_offset + id, space_kind = 'PUBLIC_ROOM';
ALTER TABLE public_rooms
    MODIFY space_id BIGINT NOT NULL,
    MODIFY space_kind VARCHAR(64) NOT NULL,
    MODIFY book_id BIGINT NULL,
    ADD CONSTRAINT uk_public_rooms_space_id UNIQUE (space_id),
    ADD CONSTRAINT uk_public_rooms_space_identity UNIQUE (space_id, space_kind),
    ADD CONSTRAINT ck_public_rooms_space_kind CHECK (space_kind = 'PUBLIC_ROOM'),
    ADD CONSTRAINT fk_public_rooms_space_identity FOREIGN KEY (space_id, space_kind)
        REFERENCES spaces (id, kind);

ALTER TABLE books
    ADD COLUMN content_id BIGINT NULL,
    ADD COLUMN content_kind VARCHAR(64) NULL;
UPDATE books SET content_id = id, content_kind = 'BOOK';
ALTER TABLE books
    MODIFY content_id BIGINT NOT NULL,
    MODIFY content_kind VARCHAR(64) NOT NULL,
    ADD CONSTRAINT uk_books_content_id UNIQUE (content_id),
    ADD CONSTRAINT uk_books_content_identity UNIQUE (content_id, content_kind),
    ADD CONSTRAINT ck_books_content_kind CHECK (content_kind = 'BOOK'),
    ADD CONSTRAINT fk_books_content_identity FOREIGN KEY (content_id, content_kind)
        REFERENCES contents (id, kind);

ALTER TABLE passages
    ADD COLUMN location_id BIGINT NULL,
    ADD COLUMN location_kind VARCHAR(64) NULL;
UPDATE passages SET location_id = id, location_kind = 'PASSAGE';
ALTER TABLE passages
    MODIFY location_id BIGINT NOT NULL,
    MODIFY location_kind VARCHAR(64) NOT NULL,
    ADD CONSTRAINT uk_passages_location_id UNIQUE (location_id),
    ADD CONSTRAINT uk_passages_location_identity UNIQUE (location_id, location_kind),
    ADD CONSTRAINT ck_passages_location_kind CHECK (location_kind = 'PASSAGE'),
    ADD CONSTRAINT fk_passages_location_identity FOREIGN KEY (location_id, location_kind)
        REFERENCES content_locations (id, kind);

ALTER TABLE sentences
    ADD COLUMN location_id BIGINT NULL,
    ADD COLUMN location_kind VARCHAR(64) NULL;
UPDATE sentences
SET location_id = @location_sentence_offset + id, location_kind = 'SENTENCE';
ALTER TABLE sentences
    MODIFY location_id BIGINT NOT NULL,
    MODIFY location_kind VARCHAR(64) NOT NULL,
    ADD CONSTRAINT uk_sentences_location_id UNIQUE (location_id),
    ADD CONSTRAINT uk_sentences_location_identity UNIQUE (location_id, location_kind),
    ADD CONSTRAINT ck_sentences_location_kind CHECK (location_kind = 'SENTENCE'),
    ADD CONSTRAINT fk_sentences_location_identity FOREIGN KEY (location_id, location_kind)
        REFERENCES content_locations (id, kind);

SET @legacy_comment_check_exists = (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'comments'
      AND constraint_type = 'CHECK'
      AND constraint_name = 'ck_comments_single_space'
);
SET @legacy_check_sql = IF(@legacy_comment_check_exists = 0, 'DO 0',
    'ALTER TABLE comments DROP CHECK ck_comments_single_space');
PREPARE core_drop_legacy_check FROM @legacy_check_sql;
EXECUTE core_drop_legacy_check;
DEALLOCATE PREPARE core_drop_legacy_check;
ALTER TABLE comments ADD COLUMN appreciation_kind VARCHAR(64) NULL;
UPDATE comments SET appreciation_kind = 'COMMENT';
ALTER TABLE comments
    MODIFY appreciation_kind VARCHAR(64) NOT NULL,
    MODIFY sentence_id BIGINT NULL,
    MODIFY created_at DATETIME(6) NULL,
    ADD CONSTRAINT uk_comments_appreciation_identity UNIQUE (id, appreciation_kind),
    ADD CONSTRAINT ck_comments_appreciation_kind CHECK (appreciation_kind = 'COMMENT'),
    ADD CONSTRAINT fk_comments_appreciation_identity FOREIGN KEY (id, appreciation_kind)
        REFERENCES appreciations (id, kind) ON DELETE CASCADE;

CREATE TABLE space_content_bindings (
    space_id BIGINT NOT NULL,
    content_id BIGINT NOT NULL,
    PRIMARY KEY (space_id, content_id),
    CONSTRAINT fk_space_content_bindings_space FOREIGN KEY (space_id) REFERENCES spaces (id),
    CONSTRAINT fk_space_content_bindings_content FOREIGN KEY (content_id) REFERENCES contents (id)
);

INSERT INTO space_content_bindings (space_id, content_id)
SELECT space_id, book_id FROM clubs WHERE book_id IS NOT NULL
UNION
SELECT space_id, book_id FROM public_rooms WHERE book_id IS NOT NULL;

CREATE TABLE club_content_bindings (
    space_id BIGINT NOT NULL,
    content_id BIGINT NOT NULL,
    PRIMARY KEY (space_id),
    CONSTRAINT fk_club_content_bindings_pair FOREIGN KEY (space_id, content_id)
        REFERENCES space_content_bindings (space_id, content_id),
    CONSTRAINT fk_club_content_bindings_club FOREIGN KEY (space_id) REFERENCES clubs (space_id)
);
INSERT INTO club_content_bindings (space_id, content_id)
SELECT space_id, book_id FROM clubs WHERE book_id IS NOT NULL;

CREATE TABLE public_room_content_bindings (
    space_id BIGINT NOT NULL,
    content_id BIGINT NOT NULL,
    PRIMARY KEY (space_id),
    CONSTRAINT uk_public_room_content_bindings_content UNIQUE (content_id),
    CONSTRAINT fk_public_room_content_bindings_pair FOREIGN KEY (space_id, content_id)
        REFERENCES space_content_bindings (space_id, content_id),
    CONSTRAINT fk_public_room_content_bindings_room FOREIGN KEY (space_id) REFERENCES public_rooms (space_id)
);
INSERT INTO public_room_content_bindings (space_id, content_id)
SELECT space_id, book_id FROM public_rooms WHERE book_id IS NOT NULL;

CREATE TABLE appreciation_contexts (
    appreciation_id BIGINT NOT NULL,
    space_id BIGINT NOT NULL,
    content_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    PRIMARY KEY (appreciation_id),
    CONSTRAINT fk_appreciation_contexts_appreciation FOREIGN KEY (appreciation_id)
        REFERENCES appreciations (id),
    CONSTRAINT fk_appreciation_contexts_space_content FOREIGN KEY (space_id, content_id)
        REFERENCES space_content_bindings (space_id, content_id),
    CONSTRAINT fk_appreciation_contexts_location_content FOREIGN KEY (location_id, content_id)
        REFERENCES content_locations (id, content_id)
);

INSERT INTO appreciation_contexts (appreciation_id, space_id, content_id, location_id)
SELECT c.id,
       COALESCE(cl.space_id, pr.space_id),
       COALESCE(cl.book_id, pr.book_id),
       s.location_id
FROM comments c
LEFT JOIN club_members cm ON cm.id = c.club_member_id
LEFT JOIN clubs cl ON cl.id = cm.club_id
LEFT JOIN public_rooms pr ON pr.id = c.public_room_id
JOIN sentences s ON s.id = c.sentence_id;

CREATE TABLE reading_progresses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_id BIGINT NOT NULL,
    space_id BIGINT NOT NULL,
    content_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    last_read_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_reading_progresses_actor_space_content UNIQUE (actor_id, space_id, content_id),
    CONSTRAINT fk_reading_progresses_actor FOREIGN KEY (actor_id) REFERENCES members (id),
    CONSTRAINT fk_reading_progresses_space_content FOREIGN KEY (space_id, content_id)
        REFERENCES space_content_bindings (space_id, content_id),
    CONSTRAINT fk_reading_progresses_location_content FOREIGN KEY (location_id, content_id)
        REFERENCES content_locations (id, content_id)
);

INSERT INTO reading_progresses (actor_id, space_id, content_id, location_id, last_read_at)
SELECT cm.member_id, c.space_id, c.book_id, p.location_id, cm.last_read_at
FROM club_members cm
JOIN clubs c ON c.id = cm.club_id
JOIN passages p ON p.id = cm.last_read_passage_id
WHERE cm.last_read_at IS NOT NULL
ORDER BY cm.id;
INSERT INTO reading_progresses (actor_id, space_id, content_id, location_id, last_read_at)
SELECT pra.member_id, pr.space_id, pr.book_id, p.location_id, pra.last_read_at
FROM public_room_activities pra
JOIN public_rooms pr ON pr.id = pra.public_room_id
JOIN passages p ON p.id = pra.last_read_passage_id
WHERE pra.last_read_at IS NOT NULL
ORDER BY pra.id;

CREATE TABLE public_room_visits (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_id BIGINT NOT NULL,
    space_id BIGINT NOT NULL,
    last_visited_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_public_room_visits_actor_space UNIQUE (actor_id, space_id),
    CONSTRAINT fk_public_room_visits_actor FOREIGN KEY (actor_id) REFERENCES members (id),
    CONSTRAINT fk_public_room_visits_space FOREIGN KEY (space_id) REFERENCES spaces (id)
);
INSERT INTO public_room_visits (actor_id, space_id, last_visited_at)
SELECT pra.member_id, pr.space_id, pra.last_visited_at
FROM public_room_activities pra
JOIN public_rooms pr ON pr.id = pra.public_room_id
ORDER BY pra.id;

CREATE TABLE appreciation_views (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_id BIGINT NOT NULL,
    appreciation_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_appreciation_views_actor_appreciation (actor_id, appreciation_id),
    CONSTRAINT fk_appreciation_views_actor FOREIGN KEY (actor_id) REFERENCES members (id),
    CONSTRAINT fk_appreciation_views_appreciation FOREIGN KEY (appreciation_id)
        REFERENCES appreciations (id) ON DELETE CASCADE
);
INSERT INTO appreciation_views (id, actor_id, appreciation_id)
SELECT id, member_id, comment_id FROM comment_views ORDER BY id;

CREATE TABLE appreciation_reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporter_id BIGINT NOT NULL,
    appreciation_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_appreciation_reports_reporter_appreciation UNIQUE (reporter_id, appreciation_id),
    CONSTRAINT fk_appreciation_reports_reporter FOREIGN KEY (reporter_id) REFERENCES members (id),
    CONSTRAINT fk_appreciation_reports_appreciation FOREIGN KEY (appreciation_id)
        REFERENCES appreciations (id) ON DELETE CASCADE
);
INSERT INTO appreciation_reports (id, reporter_id, appreciation_id)
SELECT id, reporter_id, comment_id FROM comment_reports ORDER BY id;

-- Leave legacy progress/activity/context columns in place as nullable dormant aliases.
-- Their source values remain available for audit, while their cross-domain foreign
-- keys no longer block canonical deletion. Live legacy view/report/activity rows are
-- cleaned by the compatibility CASCADE keys above; durable backup rows are unchanged.
-- The new application writes only the module tables. Removal is a later contract.
