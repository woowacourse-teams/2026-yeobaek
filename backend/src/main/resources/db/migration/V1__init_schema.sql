-- MySQL 8.4 initial schema from the production snapshot after constraint/index name normalization.

CREATE TABLE `authors` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `isni` varchar(16) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_authors_isni` (`isni`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `books` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cover_image_key` varchar(80) DEFAULT NULL,
  `passage_count` int NOT NULL,
  `published_year` int DEFAULT NULL,
  `publisher` varchar(100) DEFAULT NULL,
  `status` enum('ACTIVE','DELETED') NOT NULL DEFAULT 'ACTIVE',
  `title` varchar(100) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `author_books` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `author_id` bigint NOT NULL,
  `book_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_author_books_author` (`author_id`),
  KEY `fk_author_books_book` (`book_id`),
  CONSTRAINT `fk_author_books_author` FOREIGN KEY (`author_id`) REFERENCES `authors` (`id`),
  CONSTRAINT `fk_author_books_book` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `chapters` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sequence` int NOT NULL,
  `title` varchar(100) NOT NULL,
  `book_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_chapters_book` (`book_id`),
  CONSTRAINT `fk_chapters_book` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `passages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sequence` int NOT NULL,
  `chapter_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_passages_chapter` (`chapter_id`),
  CONSTRAINT `fk_passages_chapter` FOREIGN KEY (`chapter_id`) REFERENCES `chapters` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sentences` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `sequence` int NOT NULL,
  `passage_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sentences_passage_sequence` (`passage_id`,`sequence`),
  CONSTRAINT `fk_sentences_passage` FOREIGN KEY (`passage_id`) REFERENCES `passages` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `members` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nickname` varchar(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `member_blocks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `blocked_id` bigint NOT NULL,
  `blocker_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_blocks_blocker_blocked` (`blocker_id`,`blocked_id`),
  KEY `fk_member_blocks_blocked` (`blocked_id`),
  CONSTRAINT `fk_member_blocks_blocker` FOREIGN KEY (`blocker_id`) REFERENCES `members` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_member_blocks_blocked` FOREIGN KEY (`blocked_id`) REFERENCES `members` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `clubs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `join_code` varchar(10) NOT NULL,
  `name` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_clubs_join_code` (`join_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `club_books` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `club_id` bigint NOT NULL,
  `book_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_club_books_club` (`club_id`),
  KEY `fk_club_books_book` (`book_id`),
  CONSTRAINT `fk_club_books_book` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`),
  CONSTRAINT `fk_club_books_club` FOREIGN KEY (`club_id`) REFERENCES `clubs` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `club_members` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `status` varchar(20) NOT NULL DEFAULT 'JOINED',
  `club_id` bigint NOT NULL,
  `member_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_club_members_member_club` (`member_id`,`club_id`),
  KEY `fk_club_members_club` (`club_id`),
  CONSTRAINT `fk_club_members_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`),
  CONSTRAINT `fk_club_members_club` FOREIGN KEY (`club_id`) REFERENCES `clubs` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `club_member_last_reads` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `club_member_id` bigint NOT NULL,
  `last_read_passage_id` bigint DEFAULT NULL,
  `last_read_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_club_member_last_reads_member` (`club_member_id`),
  KEY `fk_club_member_last_reads_passage` (`last_read_passage_id`),
  CONSTRAINT `fk_club_member_last_reads_member` FOREIGN KEY (`club_member_id`) REFERENCES `club_members` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_club_member_last_reads_passage` FOREIGN KEY (`last_read_passage_id`) REFERENCES `passages` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `public_rooms` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `book_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_public_rooms_book` (`book_id`),
  CONSTRAINT `fk_public_rooms_book` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `public_room_members` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `public_room_id` bigint NOT NULL,
  `last_visited_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_public_room_members_member_room` (`member_id`,`public_room_id`),
  KEY `fk_public_room_members_room` (`public_room_id`),
  CONSTRAINT `fk_public_room_members_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`),
  CONSTRAINT `fk_public_room_members_room` FOREIGN KEY (`public_room_id`) REFERENCES `public_rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `public_room_member_last_reads` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `public_room_member_id` bigint NOT NULL,
  `last_read_passage_id` bigint DEFAULT NULL,
  `last_read_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_public_room_member_last_reads_member` (`public_room_member_id`),
  KEY `fk_public_room_member_last_reads_passage` (`last_read_passage_id`),
  CONSTRAINT `fk_public_room_member_last_reads_member` FOREIGN KEY (`public_room_member_id`) REFERENCES `public_room_members` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_public_room_member_last_reads_passage` FOREIGN KEY (`last_read_passage_id`) REFERENCES `passages` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `comments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(1000) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `sentence_id` bigint NOT NULL,
  `writer_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_comments_sentence` (`sentence_id`),
  KEY `fk_comments_writer` (`writer_id`),
  CONSTRAINT `fk_comments_writer` FOREIGN KEY (`writer_id`) REFERENCES `members` (`id`),
  CONSTRAINT `fk_comments_sentence` FOREIGN KEY (`sentence_id`) REFERENCES `sentences` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `club_comments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment_id` bigint NOT NULL,
  `club_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_club_comments_comment` (`comment_id`),
  KEY `fk_club_comments_club` (`club_id`),
  CONSTRAINT `fk_club_comments_club` FOREIGN KEY (`club_id`) REFERENCES `clubs` (`id`),
  CONSTRAINT `fk_club_comments_comment` FOREIGN KEY (`comment_id`) REFERENCES `comments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `public_room_comments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment_id` bigint NOT NULL,
  `public_room_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_public_room_comments_comment` (`comment_id`),
  KEY `fk_public_room_comments_room` (`public_room_id`),
  CONSTRAINT `fk_public_room_comments_comment` FOREIGN KEY (`comment_id`) REFERENCES `comments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_public_room_comments_room` FOREIGN KEY (`public_room_id`) REFERENCES `public_rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `comment_views` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment_id` bigint NOT NULL,
  `member_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_comment_views_member_comment` (`member_id`,`comment_id`),
  KEY `fk_comment_views_comment` (`comment_id`),
  CONSTRAINT `fk_comment_views_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comment_views_comment` FOREIGN KEY (`comment_id`) REFERENCES `comments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reports` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment_id` bigint NOT NULL,
  `reporter_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reports_reporter_comment` (`reporter_id`,`comment_id`),
  KEY `fk_reports_comment` (`comment_id`),
  CONSTRAINT `fk_reports_comment` FOREIGN KEY (`comment_id`) REFERENCES `comments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_reports_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `members` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
