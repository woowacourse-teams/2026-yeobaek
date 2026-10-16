/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE TABLE `author_books` (
  `author_id` bigint NOT NULL, `book_id` bigint NOT NULL, `id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`), KEY `FKkudd737sm6cxhjgn948yk599b` (`author_id`), KEY `FKm8j8158lb50ea4juixpc78v13` (`book_id`),
  CONSTRAINT `FKkudd737sm6cxhjgn948yk599b` FOREIGN KEY (`author_id`) REFERENCES `authors` (`id`),
  CONSTRAINT `FKm8j8158lb50ea4juixpc78v13` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `authors` (
  `id` bigint NOT NULL AUTO_INCREMENT, `isni` varchar(16) DEFAULT NULL, `name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`), UNIQUE KEY `UK6tadnl6pw4c10oba4fedqdwrs` (`isni`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `books` (
  `passage_count` int NOT NULL, `published_year` int DEFAULT NULL, `id` bigint NOT NULL AUTO_INCREMENT,
  `cover_image_key` varchar(80) DEFAULT NULL, `publisher` varchar(100) DEFAULT NULL,
  `title` varchar(100) NOT NULL, `status` enum('ACTIVE','DELETED') NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `chapters` (
  `sequence` int NOT NULL, `book_id` bigint NOT NULL, `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL, PRIMARY KEY (`id`), KEY `FKed84iwj3kfagv35uls80txkj4` (`book_id`),
  CONSTRAINT `FKed84iwj3kfagv35uls80txkj4` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `club_members` (
  `club_id` bigint NOT NULL, `id` bigint NOT NULL AUTO_INCREMENT, `last_read_at` datetime(6) DEFAULT NULL,
  `last_read_passage_id` bigint DEFAULT NULL, `member_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'JOINED', PRIMARY KEY (`id`),
  UNIQUE KEY `uk_club_members_member_club` (`member_id`,`club_id`), KEY `FKbwqgge4dgaaxukg2hytd9enhp` (`club_id`),
  KEY `FKnjarfq46su8j4gt3fjjir3u0p` (`last_read_passage_id`),
  CONSTRAINT `FK9x1fl7cgarucbngq0d2ddiy2f` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`),
  CONSTRAINT `FKbwqgge4dgaaxukg2hytd9enhp` FOREIGN KEY (`club_id`) REFERENCES `clubs` (`id`),
  CONSTRAINT `FKnjarfq46su8j4gt3fjjir3u0p` FOREIGN KEY (`last_read_passage_id`) REFERENCES `passages` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `clubs` (
  `book_id` bigint NOT NULL, `id` bigint NOT NULL AUTO_INCREMENT, `join_code` varchar(10) NOT NULL,
  `name` varchar(20) NOT NULL, PRIMARY KEY (`id`), UNIQUE KEY `uk_clubs_join_code` (`join_code`),
  KEY `FK8gmvik0oj0v0511socbffwpbb` (`book_id`),
  CONSTRAINT `FK8gmvik0oj0v0511socbffwpbb` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `comment_reports` (
  `comment_id` bigint NOT NULL, `id` bigint NOT NULL AUTO_INCREMENT, `reporter_id` bigint NOT NULL,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_comment_reports_reporter_comment` (`reporter_id`,`comment_id`),
  KEY `FKj9so403o1bquehodqxpyijjma` (`comment_id`),
  CONSTRAINT `FKj9so403o1bquehodqxpyijjma` FOREIGN KEY (`comment_id`) REFERENCES `comments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FKmwwi8w5u0t91we1f8f9n988cc` FOREIGN KEY (`reporter_id`) REFERENCES `members` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `comment_views` (
  `comment_id` bigint NOT NULL, `id` bigint NOT NULL AUTO_INCREMENT, `member_id` bigint NOT NULL,
  PRIMARY KEY (`id`), KEY `idx_comment_views_member_comment` (`member_id`,`comment_id`),
  KEY `FKc4bn2uvas3ta2gwg896bdc2y7` (`comment_id`),
  CONSTRAINT `FK8rv6pli18ai6nll7o7ig76ph3` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FKc4bn2uvas3ta2gwg896bdc2y7` FOREIGN KEY (`comment_id`) REFERENCES `comments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `comments` (
  `club_member_id` bigint DEFAULT NULL, `created_at` datetime(6) NOT NULL, `id` bigint NOT NULL AUTO_INCREMENT,
  `public_room_id` bigint DEFAULT NULL, `sentence_id` bigint NOT NULL, `updated_at` datetime(6) DEFAULT NULL,
  `writer_id` bigint DEFAULT NULL, `content` varchar(1000) NOT NULL, PRIMARY KEY (`id`),
  KEY `FKjp8bssmj37lbwax28i4re7e13` (`club_member_id`), KEY `FK5wgei1a2p5pg6bt7pgkt0cms0` (`public_room_id`),
  KEY `FKoogioic30mm8co3rjta195go2` (`sentence_id`), KEY `FK5e2sgmwoy66tn58hgom68mbmq` (`writer_id`),
  CONSTRAINT `FK5e2sgmwoy66tn58hgom68mbmq` FOREIGN KEY (`writer_id`) REFERENCES `members` (`id`),
  CONSTRAINT `FK5wgei1a2p5pg6bt7pgkt0cms0` FOREIGN KEY (`public_room_id`) REFERENCES `public_rooms` (`id`),
  CONSTRAINT `FKjp8bssmj37lbwax28i4re7e13` FOREIGN KEY (`club_member_id`) REFERENCES `club_members` (`id`),
  CONSTRAINT `FKoogioic30mm8co3rjta195go2` FOREIGN KEY (`sentence_id`) REFERENCES `sentences` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `member_blocks` (
  `blocked_id` bigint NOT NULL, `blocker_id` bigint NOT NULL, `id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_member_blocks_blocker_blocked` (`blocker_id`,`blocked_id`),
  KEY `FK3exwi8406wwbr7sgj39aljgku` (`blocked_id`),
  CONSTRAINT `FK1gnm359fx4d66dyfsftmdsesv` FOREIGN KEY (`blocker_id`) REFERENCES `members` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FK3exwi8406wwbr7sgj39aljgku` FOREIGN KEY (`blocked_id`) REFERENCES `members` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `members` (
  `id` bigint NOT NULL AUTO_INCREMENT, `nickname` varchar(20) NOT NULL, PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `passages` (
  `sequence` int NOT NULL, `chapter_id` bigint NOT NULL, `id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`), KEY `FKfm8s7p450m10fo0uw0v978dhv` (`chapter_id`),
  CONSTRAINT `FKfm8s7p450m10fo0uw0v978dhv` FOREIGN KEY (`chapter_id`) REFERENCES `chapters` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `public_room_activities` (
  `id` bigint NOT NULL AUTO_INCREMENT, `last_read_at` datetime(6) DEFAULT NULL,
  `last_read_passage_id` bigint DEFAULT NULL, `last_visited_at` datetime(6) DEFAULT NULL,
  `member_id` bigint NOT NULL, `public_room_id` bigint NOT NULL, PRIMARY KEY (`id`),
  UNIQUE KEY `uk_public_room_activities_member_room` (`member_id`,`public_room_id`),
  KEY `FK44dak5imn4bya323yg4mdwwf6` (`last_read_passage_id`), KEY `FK5p33fv21ph102nxfbfm6ltgjb` (`public_room_id`),
  CONSTRAINT `FK2qq5xj2258qnd5i3wl75o4xsi` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`),
  CONSTRAINT `FK44dak5imn4bya323yg4mdwwf6` FOREIGN KEY (`last_read_passage_id`) REFERENCES `passages` (`id`),
  CONSTRAINT `FK5p33fv21ph102nxfbfm6ltgjb` FOREIGN KEY (`public_room_id`) REFERENCES `public_rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `public_rooms` (
  `book_id` bigint NOT NULL, `id` bigint NOT NULL AUTO_INCREMENT, PRIMARY KEY (`id`),
  UNIQUE KEY `uk_public_rooms_book` (`book_id`),
  CONSTRAINT `FK6080gyaqao2ckxyldub6dfihu` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `sentences` (
  `sequence` int NOT NULL, `id` bigint NOT NULL AUTO_INCREMENT, `passage_id` bigint NOT NULL,
  `content` text NOT NULL, PRIMARY KEY (`id`), UNIQUE KEY `uk_sentences_passage_sequence` (`passage_id`,`sequence`),
  CONSTRAINT `FKbcdqutqwweuthlmvgm641isd5` FOREIGN KEY (`passage_id`) REFERENCES `passages` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
