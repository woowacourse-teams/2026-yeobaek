-- 여백 개발 환경 전체 스키마 초기화 (MySQL 8.4)
--
-- 경고: 개발 환경 전용이다. 선택한 yeobaek 데이터베이스의 모든 기존 테이블과 데이터가 삭제된다.
-- 실행 전에 애플리케이션을 중지하고, 개선 스키마를 사용하는 새 이미지가 배포되도록 준비한다.
-- 데이터베이스 자체와 계정 권한은 삭제하지 않는다.
--
-- 실행 예시:
-- mysql --show-warnings --skip-force -uroot yeobaek < 2026-10-14-dev-schema-reset.sql
--
-- MySQL DDL은 암묵적으로 커밋되므로 이 파일은 트랜잭션 롤백을 제공하지 않는다.

CREATE TEMPORARY TABLE yeobaek_dev_schema_reset_guard (
    step_name VARCHAR(100) NOT NULL PRIMARY KEY,
    passed TINYINT NOT NULL,
    CONSTRAINT ck_yeobaek_dev_schema_reset_guard CHECK (passed = 1)
);

INSERT INTO yeobaek_dev_schema_reset_guard
VALUES ('yeobaek database is selected', IF(DATABASE() <=> 'yeobaek', 1, 0));

INSERT INTO yeobaek_dev_schema_reset_guard
SELECT 'no cross-schema foreign keys reference yeobaek', IF(COUNT(*) = 0, 1, 0)
FROM information_schema.key_column_usage
WHERE referenced_table_schema = DATABASE()
  AND table_schema <> DATABASE();

SET @reset_old_group_concat_max_len = @@SESSION.group_concat_max_len;
SET SESSION group_concat_max_len = 1048576;

SELECT COUNT(*),
       COALESCE(GROUP_CONCAT(
           CONCAT('`', REPLACE(table_name, '`', '``'), '`')
           ORDER BY table_name SEPARATOR ', '
       ), ''),
       COALESCE(SUM(CHAR_LENGTH(CONCAT('`', REPLACE(table_name, '`', '``'), '`'))), 0)
           + GREATEST(COUNT(*) - 1, 0) * 2
INTO @reset_table_count, @reset_table_list, @reset_expected_table_list_length
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE';

INSERT INTO yeobaek_dev_schema_reset_guard
VALUES (
    'drop table list is complete',
    IF(CHAR_LENGTH(@reset_table_list) = @reset_expected_table_list_length, 1, 0)
);

SET @reset_drop_sql = IF(
    @reset_table_count = 0,
    'DO 0',
    CONCAT('DROP TABLE ', @reset_table_list)
);

INSERT INTO yeobaek_dev_schema_reset_guard
VALUES (
    'drop statement fits max allowed packet',
    IF(OCTET_LENGTH(@reset_drop_sql) < @@max_allowed_packet, 1, 0)
);

SET SESSION group_concat_max_len = @reset_old_group_concat_max_len;

SET FOREIGN_KEY_CHECKS = 0;
PREPARE reset_drop_stmt FROM @reset_drop_sql;
EXECUTE reset_drop_stmt;
DEALLOCATE PREPARE reset_drop_stmt;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE authors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    isni VARCHAR(16) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_authors_isni UNIQUE (isni)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE books (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    publisher VARCHAR(100) NULL,
    published_year INT NULL,
    cover_image_key VARCHAR(80) NULL,
    passage_count INT NOT NULL,
    status ENUM('ACTIVE', 'DELETED') NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE author_books (
    id BIGINT NOT NULL AUTO_INCREMENT,
    author_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_author_books_author FOREIGN KEY (author_id) REFERENCES authors (id),
    CONSTRAINT fk_author_books_book FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE chapters (
    id BIGINT NOT NULL AUTO_INCREMENT,
    book_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    sequence INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_chapters_book FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE passages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    chapter_id BIGINT NOT NULL,
    sequence INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_passages_chapter FOREIGN KEY (chapter_id) REFERENCES chapters (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sentences (
    id BIGINT NOT NULL AUTO_INCREMENT,
    passage_id BIGINT NOT NULL,
    sequence INT NOT NULL,
    content TEXT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_sentences_passage_sequence UNIQUE (passage_id, sequence),
    CONSTRAINT fk_sentences_passage FOREIGN KEY (passage_id) REFERENCES passages (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nickname VARCHAR(20) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE member_blocks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    blocker_id BIGINT NOT NULL,
    blocked_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_member_blocks_blocker_blocked UNIQUE (blocker_id, blocked_id),
    CONSTRAINT fk_member_blocks_blocker FOREIGN KEY (blocker_id)
        REFERENCES members (id) ON DELETE CASCADE,
    CONSTRAINT fk_member_blocks_blocked FOREIGN KEY (blocked_id)
        REFERENCES members (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE public_rooms (
    id BIGINT NOT NULL AUTO_INCREMENT,
    book_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_public_rooms_book UNIQUE (book_id),
    CONSTRAINT fk_public_rooms_book FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE public_room_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    public_room_id BIGINT NOT NULL,
    last_visited_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_public_room_members_member_room UNIQUE (member_id, public_room_id),
    CONSTRAINT fk_public_room_members_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_public_room_members_room FOREIGN KEY (public_room_id) REFERENCES public_rooms (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE clubs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(20) NOT NULL,
    join_code VARCHAR(10) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_clubs_join_code UNIQUE (join_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE club_books (
    id BIGINT NOT NULL AUTO_INCREMENT,
    club_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_club_books_club UNIQUE (club_id),
    CONSTRAINT fk_club_books_club FOREIGN KEY (club_id)
        REFERENCES clubs (id) ON DELETE CASCADE,
    CONSTRAINT fk_club_books_book FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE club_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    club_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'JOINED',
    PRIMARY KEY (id),
    CONSTRAINT uk_club_members_member_club UNIQUE (member_id, club_id),
    CONSTRAINT fk_club_members_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_club_members_club FOREIGN KEY (club_id) REFERENCES clubs (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    writer_id BIGINT NOT NULL,
    sentence_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comments_writer FOREIGN KEY (writer_id) REFERENCES members (id),
    CONSTRAINT fk_comments_sentence FOREIGN KEY (sentence_id) REFERENCES sentences (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE club_comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    club_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_club_comments_comment UNIQUE (comment_id),
    CONSTRAINT fk_club_comments_comment FOREIGN KEY (comment_id)
        REFERENCES comments (id) ON DELETE CASCADE,
    CONSTRAINT fk_club_comments_club FOREIGN KEY (club_id) REFERENCES clubs (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE public_room_comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    public_room_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_public_room_comments_comment UNIQUE (comment_id),
    CONSTRAINT fk_public_room_comments_comment FOREIGN KEY (comment_id)
        REFERENCES comments (id) ON DELETE CASCADE,
    CONSTRAINT fk_public_room_comments_room FOREIGN KEY (public_room_id) REFERENCES public_rooms (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE comment_views (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    comment_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_comment_views_member_comment (member_id, comment_id),
    CONSTRAINT fk_comment_views_member FOREIGN KEY (member_id)
        REFERENCES members (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_views_comment FOREIGN KEY (comment_id)
        REFERENCES comments (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporter_id BIGINT NOT NULL,
    comment_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_reports_reporter_comment UNIQUE (reporter_id, comment_id),
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id)
        REFERENCES members (id) ON DELETE CASCADE,
    CONSTRAINT fk_reports_comment FOREIGN KEY (comment_id)
        REFERENCES comments (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO yeobaek_dev_schema_reset_guard
SELECT 'exactly the 20 expected tables exist', IF(
    COUNT(*) = 20
    AND SUM(table_name IN (
        'authors', 'author_books', 'books', 'chapters', 'passages', 'sentences',
        'members', 'member_blocks', 'public_rooms', 'public_room_members',
        'public_room_member_last_reads', 'clubs', 'club_books', 'club_members',
        'club_member_last_reads', 'comments', 'public_room_comments',
        'club_comments', 'comment_views', 'reports'
    )) = 20,
    1, 0
)
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE';

INSERT INTO yeobaek_dev_schema_reset_guard
SELECT 'legacy columns are absent', IF(COUNT(*) = 0, 1, 0)
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND CONCAT(table_name, '.', column_name) IN (
      'clubs.book_id',
      'club_members.last_read_passage_id',
      'club_members.last_read_at',
      'comments.club_member_id',
      'comments.public_room_id',
      'public_room_members.last_read_passage_id',
      'public_room_members.last_read_at'
  );

INSERT INTO yeobaek_dev_schema_reset_guard
VALUES ('foreign key checks are enabled', IF(@@FOREIGN_KEY_CHECKS = 1, 1, 0));

SELECT 'SUCCESS' AS reset_status,
       DATABASE() AS database_name,
       COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE';

DROP TEMPORARY TABLE yeobaek_dev_schema_reset_guard;
