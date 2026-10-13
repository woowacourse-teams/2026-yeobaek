-- 여백 읽기 스키마 수동 전환 (MySQL 8.4)
-- 애플리케이션을 중지하고 전체 백업을 검증한 뒤 한 번만 실행한다.
-- MySQL DDL은 암묵적으로 커밋되므로 이 파일은 트랜잭션 롤백을 제공하지 않는다.

CREATE TEMPORARY TABLE yeobaek_reading_schema_guard (
    step_name VARCHAR(100) NOT NULL PRIMARY KEY,
    passed TINYINT NOT NULL,
    CONSTRAINT ck_yeobaek_reading_schema_guard CHECK (passed = 1)
);

INSERT INTO yeobaek_reading_schema_guard
SELECT 'required source tables exist', IF(COUNT(*) = 15, 1, 0)
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('authors', 'author_books', 'books', 'chapters', 'passages', 'sentences',
                     'members', 'member_blocks', 'public_rooms', 'public_room_activities',
                     'clubs', 'club_members', 'comments', 'comment_views', 'comment_reports');

INSERT INTO yeobaek_reading_schema_guard
SELECT 'target tables must not exist', IF(COUNT(*) = 0, 1, 0)
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('club_books', 'club_member_last_reads', 'public_room_members',
                     'public_room_member_last_reads', 'club_comments',
                     'public_room_comments', 'reports');

INSERT INTO yeobaek_reading_schema_guard
SELECT 'required source columns exist', IF(COUNT(*) = 18, 1, 0)
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND CONCAT(table_name, '.', column_name) IN (
      'clubs.book_id',
      'club_members.id', 'club_members.last_read_passage_id', 'club_members.last_read_at',
      'comments.id', 'comments.club_member_id', 'comments.public_room_id',
      'comments.writer_id', 'comments.sentence_id',
      'comment_reports.id', 'comment_reports.comment_id', 'comment_reports.reporter_id',
      'public_room_activities.id', 'public_room_activities.member_id',
      'public_room_activities.public_room_id', 'public_room_activities.last_read_passage_id',
      'public_room_activities.last_read_at', 'public_room_activities.last_visited_at'
  );

INSERT INTO yeobaek_reading_schema_guard
SELECT 'comment report unique index has expected shape', IF(
    COUNT(*) = 2
    AND MAX(non_unique) = 0
    AND GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'reporter_id,comment_id',
    1, 0)
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'comment_reports'
  AND index_name = 'uk_comment_reports_reporter_comment';

INSERT INTO yeobaek_reading_schema_guard
SELECT 'public activity unique index has expected shape', IF(
    COUNT(*) = 2
    AND MAX(non_unique) = 0
    AND GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'member_id,public_room_id',
    1, 0)
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'public_room_activities'
  AND index_name = 'uk_public_room_activities_member_room';

INSERT INTO yeobaek_reading_schema_guard
SELECT 'clubs book foreign key is discoverable', IF(COUNT(*) = 1, 1, 0)
FROM information_schema.key_column_usage
WHERE table_schema = DATABASE() AND table_name = 'clubs' AND column_name = 'book_id'
  AND referenced_table_name = 'books';

INSERT INTO yeobaek_reading_schema_guard
SELECT 'club progress foreign key is discoverable', IF(COUNT(*) = 1, 1, 0)
FROM information_schema.key_column_usage
WHERE table_schema = DATABASE() AND table_name = 'club_members'
  AND column_name = 'last_read_passage_id' AND referenced_table_name = 'passages';

INSERT INTO yeobaek_reading_schema_guard
SELECT 'club comment foreign key is discoverable', IF(COUNT(*) = 1, 1, 0)
FROM information_schema.key_column_usage
WHERE table_schema = DATABASE() AND table_name = 'comments'
  AND column_name = 'club_member_id' AND referenced_table_name = 'club_members';

INSERT INTO yeobaek_reading_schema_guard
SELECT 'public comment foreign key is discoverable', IF(COUNT(*) = 1, 1, 0)
FROM information_schema.key_column_usage
WHERE table_schema = DATABASE() AND table_name = 'comments'
  AND column_name = 'public_room_id' AND referenced_table_name = 'public_rooms';

INSERT INTO yeobaek_reading_schema_guard
SELECT 'public progress foreign key is discoverable', IF(COUNT(*) = 1, 1, 0)
FROM information_schema.key_column_usage
WHERE table_schema = DATABASE() AND table_name = 'public_room_activities'
  AND column_name = 'last_read_passage_id' AND referenced_table_name = 'passages';

INSERT INTO yeobaek_reading_schema_guard
SELECT 'comment scope and writers are valid', IF(COUNT(*) = 0, 1, 0)
FROM comments c
LEFT JOIN club_members cm ON cm.id = c.club_member_id
WHERE (c.club_member_id IS NULL) = (c.public_room_id IS NULL)
   OR (c.club_member_id IS NOT NULL AND cm.id IS NULL)
   OR (c.club_member_id IS NOT NULL AND c.writer_id IS NOT NULL AND c.writer_id <> cm.member_id)
   OR (c.public_room_id IS NOT NULL AND c.writer_id IS NULL);

SET @migration_report_count = (SELECT COUNT(*) FROM comment_reports);
SET @migration_activity_count = (SELECT COUNT(*) FROM public_room_activities);
SET @migration_comment_count = (SELECT COUNT(*) FROM comments);

CREATE TABLE club_books (
    id BIGINT NOT NULL AUTO_INCREMENT,
    club_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_club_books_club UNIQUE (club_id),
    CONSTRAINT fk_club_books_club FOREIGN KEY (club_id) REFERENCES clubs (id) ON DELETE CASCADE,
    CONSTRAINT fk_club_books_book FOREIGN KEY (book_id) REFERENCES books (id)
);

CREATE TABLE club_member_last_reads (
    id BIGINT NOT NULL AUTO_INCREMENT,
    club_member_id BIGINT NOT NULL,
    last_read_passage_id BIGINT NULL,
    last_read_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_club_member_last_reads_member UNIQUE (club_member_id),
    CONSTRAINT fk_club_member_last_reads_member FOREIGN KEY (club_member_id)
        REFERENCES club_members (id) ON DELETE CASCADE,
    CONSTRAINT fk_club_member_last_reads_passage FOREIGN KEY (last_read_passage_id)
        REFERENCES passages (id)
);

CREATE TABLE club_comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    club_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_club_comments_comment UNIQUE (comment_id),
    CONSTRAINT fk_club_comments_comment FOREIGN KEY (comment_id)
        REFERENCES comments (id) ON DELETE CASCADE,
    CONSTRAINT fk_club_comments_club FOREIGN KEY (club_id) REFERENCES clubs (id)
);

CREATE TABLE public_room_comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    public_room_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_public_room_comments_comment UNIQUE (comment_id),
    CONSTRAINT fk_public_room_comments_comment FOREIGN KEY (comment_id)
        REFERENCES comments (id) ON DELETE CASCADE,
    CONSTRAINT fk_public_room_comments_room FOREIGN KEY (public_room_id) REFERENCES public_rooms (id)
);

INSERT INTO club_books (id, club_id, book_id)
SELECT id, id, book_id FROM clubs;

INSERT INTO club_member_last_reads (id, club_member_id, last_read_passage_id, last_read_at)
SELECT id, id, last_read_passage_id, last_read_at
FROM club_members
WHERE last_read_passage_id IS NOT NULL OR last_read_at IS NOT NULL;

INSERT INTO club_comments (id, comment_id, club_id)
SELECT c.id, c.id, cm.club_id
FROM comments c
JOIN club_members cm ON cm.id = c.club_member_id
WHERE c.club_member_id IS NOT NULL;

INSERT INTO public_room_comments (id, comment_id, public_room_id)
SELECT id, id, public_room_id
FROM comments
WHERE public_room_id IS NOT NULL;

RENAME TABLE comment_reports TO reports,
             public_room_activities TO public_room_members;

ALTER TABLE reports
    RENAME INDEX uk_comment_reports_reporter_comment TO uk_reports_reporter_comment;
ALTER TABLE public_room_members
    RENAME INDEX uk_public_room_activities_member_room TO uk_public_room_members_member_room;

CREATE TABLE public_room_member_last_reads (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_room_member_id BIGINT NOT NULL,
    last_read_passage_id BIGINT NULL,
    last_read_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_public_room_member_last_reads_member UNIQUE (public_room_member_id),
    CONSTRAINT fk_public_room_member_last_reads_member FOREIGN KEY (public_room_member_id)
        REFERENCES public_room_members (id) ON DELETE CASCADE,
    CONSTRAINT fk_public_room_member_last_reads_passage FOREIGN KEY (last_read_passage_id)
        REFERENCES passages (id)
);

INSERT INTO public_room_member_last_reads
    (id, public_room_member_id, last_read_passage_id, last_read_at)
SELECT id, id, last_read_passage_id, last_read_at
FROM public_room_members
WHERE last_read_passage_id IS NOT NULL OR last_read_at IS NOT NULL;

SET @migration_check_name = (
    SELECT constraint_name
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'comments'
      AND constraint_type = 'CHECK'
      AND constraint_name = 'ck_comments_single_space'
    LIMIT 1
);
SET @migration_sql = IF(
    @migration_check_name IS NULL,
    'SET @migration_noop = 1',
    CONCAT('ALTER TABLE `comments` DROP CHECK `',
           REPLACE(@migration_check_name, '`', '``'), '`')
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

UPDATE comments c
JOIN club_members cm ON cm.id = c.club_member_id
SET c.writer_id = cm.member_id
WHERE c.club_member_id IS NOT NULL;

INSERT INTO yeobaek_reading_schema_guard
SELECT 'renamed table ids are preserved', IF(
    (SELECT COUNT(*) FROM reports) = @migration_report_count
    AND (SELECT COUNT(*) FROM public_room_members) = @migration_activity_count,
    1, 0);

INSERT INTO yeobaek_reading_schema_guard
SELECT 'all comments and writers are preserved', IF(
    COUNT(*) = @migration_comment_count
    AND COUNT(writer_id) = @migration_comment_count,
    1, 0)
FROM comments;

INSERT INTO yeobaek_reading_schema_guard
SELECT 'club books copied exactly', IF(COUNT(*) = 0, 1, 0)
FROM clubs c
LEFT JOIN club_books cb ON cb.id = c.id AND cb.club_id = c.id AND cb.book_id = c.book_id
WHERE cb.id IS NULL;

INSERT INTO yeobaek_reading_schema_guard
SELECT 'club reads copied exactly', IF(COUNT(*) = 0, 1, 0)
FROM club_members cm
LEFT JOIN club_member_last_reads lr
  ON lr.id = cm.id AND lr.club_member_id = cm.id
 AND lr.last_read_passage_id <=> cm.last_read_passage_id
 AND lr.last_read_at <=> cm.last_read_at
WHERE (cm.last_read_passage_id IS NOT NULL OR cm.last_read_at IS NOT NULL)
  AND lr.id IS NULL;

INSERT INTO yeobaek_reading_schema_guard
SELECT 'public reads copied exactly', IF(COUNT(*) = 0, 1, 0)
FROM public_room_members prm
LEFT JOIN public_room_member_last_reads lr
  ON lr.id = prm.id AND lr.public_room_member_id = prm.id
 AND lr.last_read_passage_id <=> prm.last_read_passage_id
 AND lr.last_read_at <=> prm.last_read_at
WHERE (prm.last_read_passage_id IS NOT NULL OR prm.last_read_at IS NOT NULL)
  AND lr.id IS NULL;

INSERT INTO yeobaek_reading_schema_guard
SELECT 'comment spaces copied exactly', IF(COUNT(*) = 0, 1, 0)
FROM comments c
LEFT JOIN club_members cm ON cm.id = c.club_member_id
LEFT JOIN club_comments cc
  ON cc.id = c.id AND cc.comment_id = c.id AND cc.club_id = cm.club_id
LEFT JOIN public_room_comments prc
  ON prc.id = c.id AND prc.comment_id = c.id AND prc.public_room_id = c.public_room_id
WHERE (c.club_member_id IS NOT NULL AND cc.id IS NULL)
   OR (c.public_room_id IS NOT NULL AND prc.id IS NULL);

SET @migration_fk_name = (
    SELECT constraint_name FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 'clubs' AND column_name = 'book_id'
      AND referenced_table_name = 'books' LIMIT 1
);
SET @migration_sql = CONCAT('ALTER TABLE `clubs` DROP FOREIGN KEY `',
                            REPLACE(@migration_fk_name, '`', '``'), '`');
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @migration_fk_name = (
    SELECT constraint_name FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 'club_members'
      AND column_name = 'last_read_passage_id' AND referenced_table_name = 'passages' LIMIT 1
);
SET @migration_sql = CONCAT('ALTER TABLE `club_members` DROP FOREIGN KEY `',
                            REPLACE(@migration_fk_name, '`', '``'), '`');
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @migration_fk_name = (
    SELECT constraint_name FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 'comments'
      AND column_name = 'club_member_id' AND referenced_table_name = 'club_members' LIMIT 1
);
SET @migration_sql = CONCAT('ALTER TABLE `comments` DROP FOREIGN KEY `',
                            REPLACE(@migration_fk_name, '`', '``'), '`');
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @migration_fk_name = (
    SELECT constraint_name FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 'comments'
      AND column_name = 'public_room_id' AND referenced_table_name = 'public_rooms' LIMIT 1
);
SET @migration_sql = CONCAT('ALTER TABLE `comments` DROP FOREIGN KEY `',
                            REPLACE(@migration_fk_name, '`', '``'), '`');
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @migration_fk_name = (
    SELECT constraint_name FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 'public_room_members'
      AND column_name = 'last_read_passage_id' AND referenced_table_name = 'passages' LIMIT 1
);
SET @migration_sql = CONCAT('ALTER TABLE `public_room_members` DROP FOREIGN KEY `',
                            REPLACE(@migration_fk_name, '`', '``'), '`');
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

ALTER TABLE clubs DROP COLUMN book_id;
ALTER TABLE club_members DROP COLUMN last_read_passage_id, DROP COLUMN last_read_at;
ALTER TABLE comments DROP COLUMN club_member_id, DROP COLUMN public_room_id;
ALTER TABLE public_room_members DROP COLUMN last_read_passage_id, DROP COLUMN last_read_at;
ALTER TABLE comments MODIFY writer_id BIGINT NOT NULL;

DROP TEMPORARY TABLE yeobaek_reading_schema_guard;
