-- MySQL 8.4. Existing databases must be verified before an explicit baseline at version 1.
CREATE TABLE authors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    isni VARCHAR(16),
    PRIMARY KEY (id),
    CONSTRAINT uk_authors_isni UNIQUE (isni)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE books (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    publisher VARCHAR(100),
    published_year INT,
    cover_image_key VARCHAR(80),
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
    `sequence` INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_chapters_book FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE passages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    chapter_id BIGINT NOT NULL,
    `sequence` INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_passages_chapter FOREIGN KEY (chapter_id) REFERENCES chapters (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sentences (
    id BIGINT NOT NULL AUTO_INCREMENT,
    passage_id BIGINT NOT NULL,
    `sequence` INT NOT NULL,
    content TEXT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_sentences_passage_sequence UNIQUE (passage_id, `sequence`),
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
    CONSTRAINT fk_member_blocks_blocker FOREIGN KEY (blocker_id) REFERENCES members (id) ON DELETE CASCADE,
    CONSTRAINT fk_member_blocks_blocked FOREIGN KEY (blocked_id) REFERENCES members (id) ON DELETE CASCADE
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
    CONSTRAINT fk_club_books_club FOREIGN KEY (club_id) REFERENCES clubs (id) ON DELETE CASCADE,
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
    last_read_passage_id BIGINT,
    last_read_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_club_member_last_reads_member UNIQUE (club_member_id),
    CONSTRAINT fk_club_member_last_reads_member FOREIGN KEY (club_member_id) REFERENCES club_members (id) ON DELETE CASCADE,
    CONSTRAINT fk_club_member_last_reads_passage FOREIGN KEY (last_read_passage_id) REFERENCES passages (id)
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
    last_visited_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_public_room_members_member_room UNIQUE (member_id, public_room_id),
    CONSTRAINT fk_public_room_members_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_public_room_members_room FOREIGN KEY (public_room_id) REFERENCES public_rooms (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE public_room_member_last_reads (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_room_member_id BIGINT NOT NULL,
    last_read_passage_id BIGINT,
    last_read_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_public_room_member_last_reads_member UNIQUE (public_room_member_id),
    CONSTRAINT fk_public_room_member_last_reads_member FOREIGN KEY (public_room_member_id) REFERENCES public_room_members (id) ON DELETE CASCADE,
    CONSTRAINT fk_public_room_member_last_reads_passage FOREIGN KEY (last_read_passage_id) REFERENCES passages (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    writer_id BIGINT NOT NULL,
    sentence_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
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
    CONSTRAINT fk_club_comments_comment FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE,
    CONSTRAINT fk_club_comments_club FOREIGN KEY (club_id) REFERENCES clubs (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE public_room_comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    public_room_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_public_room_comments_comment UNIQUE (comment_id),
    CONSTRAINT fk_public_room_comments_comment FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE,
    CONSTRAINT fk_public_room_comments_room FOREIGN KEY (public_room_id) REFERENCES public_rooms (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE comment_views (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    comment_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_comment_views_member_comment (member_id, comment_id),
    CONSTRAINT fk_comment_views_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_views_comment FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporter_id BIGINT NOT NULL,
    comment_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_reports_reporter_comment UNIQUE (reporter_id, comment_id),
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES members (id) ON DELETE CASCADE,
    CONSTRAINT fk_reports_comment FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
