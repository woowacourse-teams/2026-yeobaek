-- Run after 2026-10-02-core-modules.sql and before starting the new application.
-- Any non-zero assertion fails through the CHECK constraint. Keep writers stopped
-- until this file and application smoke tests both pass.

CREATE TEMPORARY TABLE core_module_verification_guard (
    assertion_name VARCHAR(128) NOT NULL,
    violation_count BIGINT NOT NULL,
    CONSTRAINT ck_core_module_verification_guard CHECK (violation_count = 0)
);

INSERT INTO core_module_verification_guard VALUES
('club backup count',
 (SELECT COUNT(*) FROM clubs) - (SELECT COUNT(*) FROM core_migration_backup_20261002_clubs)),
('public room backup count',
 (SELECT COUNT(*) FROM public_rooms) - (SELECT COUNT(*) FROM core_migration_backup_20261002_public_rooms)),
('book backup count',
 (SELECT COUNT(*) FROM books) - (SELECT COUNT(*) FROM core_migration_backup_20261002_books)),
('passage backup count',
 (SELECT COUNT(*) FROM passages) - (SELECT COUNT(*) FROM core_migration_backup_20261002_passages)),
('sentence backup count',
 (SELECT COUNT(*) FROM sentences) - (SELECT COUNT(*) FROM core_migration_backup_20261002_sentences)),
('comment backup count',
 (SELECT COUNT(*) FROM comments) - (SELECT COUNT(*) FROM core_migration_backup_20261002_comments)),
('club member backup count',
 (SELECT COUNT(*) FROM club_members) - (SELECT COUNT(*) FROM core_migration_backup_20261002_club_members)),
('public room activity backup count',
 (SELECT COUNT(*) FROM public_room_activities)
 - (SELECT COUNT(*) FROM core_migration_backup_20261002_public_room_activities)),
('view backup count',
 (SELECT COUNT(*) FROM comment_views) - (SELECT COUNT(*) FROM core_migration_backup_20261002_comment_views)),
('report backup count',
 (SELECT COUNT(*) FROM comment_reports) - (SELECT COUNT(*) FROM core_migration_backup_20261002_comment_reports));

INSERT INTO core_module_verification_guard VALUES
('known space root count',
 (SELECT COUNT(*) FROM spaces WHERE kind IN ('CLUB', 'PUBLIC_ROOM'))
 - ((SELECT COUNT(*) FROM clubs) + (SELECT COUNT(*) FROM public_rooms))),
('book content root count',
 (SELECT COUNT(*) FROM contents WHERE kind = 'BOOK') - (SELECT COUNT(*) FROM books)),
('known location root count',
 (SELECT COUNT(*) FROM content_locations WHERE kind IN ('PASSAGE', 'SENTENCE'))
 - ((SELECT COUNT(*) FROM passages) + (SELECT COUNT(*) FROM sentences))),
('comment appreciation root count',
 (SELECT COUNT(*) FROM appreciations WHERE kind = 'COMMENT') - (SELECT COUNT(*) FROM comments)),
('club binding count',
 (SELECT COUNT(*) FROM club_content_bindings) - (SELECT COUNT(*) FROM clubs)),
('public room binding count',
 (SELECT COUNT(*) FROM public_room_content_bindings) - (SELECT COUNT(*) FROM public_rooms)),
('appreciation context count',
 (SELECT COUNT(*) FROM appreciation_contexts) - (SELECT COUNT(*) FROM comments)),
('public room visit count',
 (SELECT COUNT(*) FROM public_room_visits) - (SELECT COUNT(*) FROM public_room_activities)),
('appreciation view count',
 (SELECT COUNT(*) FROM appreciation_views) - (SELECT COUNT(*) FROM comment_views)),
('appreciation report count',
 (SELECT COUNT(*) FROM appreciation_reports) - (SELECT COUNT(*) FROM comment_reports));

INSERT INTO core_module_verification_guard
SELECT 'club identity mismatch', COUNT(*)
FROM clubs c
LEFT JOIN spaces s ON s.id = c.space_id AND s.kind = 'CLUB'
WHERE s.id IS NULL OR c.space_kind <> 'CLUB';

INSERT INTO core_module_verification_guard
SELECT 'public room identity mismatch', COUNT(*)
FROM public_rooms pr
LEFT JOIN spaces s ON s.id = pr.space_id AND s.kind = 'PUBLIC_ROOM'
WHERE s.id IS NULL OR pr.space_kind <> 'PUBLIC_ROOM';

INSERT INTO core_module_verification_guard
SELECT 'book identity mismatch', COUNT(*)
FROM books b
LEFT JOIN contents c ON c.id = b.content_id AND c.kind = 'BOOK'
WHERE c.id IS NULL OR b.content_kind <> 'BOOK';

INSERT INTO core_module_verification_guard
SELECT 'passage location mismatch', COUNT(*)
FROM passages p
JOIN chapters ch ON ch.id = p.chapter_id
LEFT JOIN content_locations cl
  ON cl.id = p.location_id AND cl.kind = 'PASSAGE' AND cl.content_id = ch.book_id
WHERE cl.id IS NULL OR p.location_kind <> 'PASSAGE';

INSERT INTO core_module_verification_guard
SELECT 'sentence location mismatch', COUNT(*)
FROM sentences s
JOIN passages p ON p.id = s.passage_id
JOIN chapters ch ON ch.id = p.chapter_id
LEFT JOIN content_locations cl
  ON cl.id = s.location_id AND cl.kind = 'SENTENCE' AND cl.content_id = ch.book_id
WHERE cl.id IS NULL OR s.location_kind <> 'SENTENCE';

INSERT INTO core_module_verification_guard
SELECT 'comment root mismatch', COUNT(*)
FROM comments c
LEFT JOIN club_members cm ON cm.id = c.club_member_id
LEFT JOIN appreciations a
  ON a.id = c.id
 AND a.kind = 'COMMENT'
 AND a.author_id = COALESCE(c.writer_id, cm.member_id)
 AND a.created_at = c.created_at
 AND (a.updated_at <=> c.updated_at)
WHERE a.id IS NULL OR c.appreciation_kind <> 'COMMENT';

INSERT INTO core_module_verification_guard
SELECT 'club content binding mismatch', COUNT(*)
FROM clubs c
LEFT JOIN club_content_bindings cb
  ON cb.space_id = c.space_id AND cb.content_id = c.book_id
WHERE cb.space_id IS NULL;

INSERT INTO core_module_verification_guard
SELECT 'public room content binding mismatch', COUNT(*)
FROM public_rooms pr
LEFT JOIN public_room_content_bindings rb
  ON rb.space_id = pr.space_id AND rb.content_id = pr.book_id
WHERE rb.space_id IS NULL;

INSERT INTO core_module_verification_guard
SELECT 'comment context mismatch', COUNT(*)
FROM comments c
LEFT JOIN club_members cm ON cm.id = c.club_member_id
LEFT JOIN clubs club ON club.id = cm.club_id
LEFT JOIN public_rooms room ON room.id = c.public_room_id
LEFT JOIN sentences sentence ON sentence.id = c.sentence_id
LEFT JOIN appreciation_contexts ac
  ON ac.appreciation_id = c.id
 AND ac.space_id = COALESCE(club.space_id, room.space_id)
 AND ac.content_id = COALESCE(club.book_id, room.book_id)
 AND ac.location_id = sentence.location_id
WHERE ac.appreciation_id IS NULL;

INSERT INTO core_module_verification_guard
SELECT 'club progress mismatch', COUNT(*)
FROM club_members cm
JOIN clubs c ON c.id = cm.club_id
JOIN passages p ON p.id = cm.last_read_passage_id
LEFT JOIN reading_progresses rp
  ON rp.actor_id = cm.member_id
 AND rp.space_id = c.space_id
 AND rp.content_id = c.book_id
 AND rp.location_id = p.location_id
 AND rp.last_read_at = cm.last_read_at
WHERE cm.last_read_at IS NOT NULL AND rp.id IS NULL;

INSERT INTO core_module_verification_guard
SELECT 'public room progress mismatch', COUNT(*)
FROM public_room_activities pra
JOIN public_rooms pr ON pr.id = pra.public_room_id
JOIN passages p ON p.id = pra.last_read_passage_id
LEFT JOIN reading_progresses rp
  ON rp.actor_id = pra.member_id
 AND rp.space_id = pr.space_id
 AND rp.content_id = pr.book_id
 AND rp.location_id = p.location_id
 AND rp.last_read_at = pra.last_read_at
WHERE pra.last_read_at IS NOT NULL AND rp.id IS NULL;

INSERT INTO core_module_verification_guard
SELECT 'reading progress count',
       COUNT(*) - (SELECT COUNT(*) FROM reading_progresses)
FROM (
    SELECT cm.id FROM club_members cm WHERE cm.last_read_at IS NOT NULL
    UNION ALL
    SELECT pra.id FROM public_room_activities pra WHERE pra.last_read_at IS NOT NULL
) expected_progresses;

INSERT INTO core_module_verification_guard
SELECT 'public room visit mismatch', COUNT(*)
FROM public_room_activities pra
JOIN public_rooms pr ON pr.id = pra.public_room_id
LEFT JOIN public_room_visits prv
  ON prv.actor_id = pra.member_id
 AND prv.space_id = pr.space_id
 AND (prv.last_visited_at <=> pra.last_visited_at)
WHERE prv.id IS NULL;

INSERT INTO core_module_verification_guard
SELECT 'view row mismatch', COUNT(*)
FROM comment_views cv
LEFT JOIN appreciation_views av
  ON av.id = cv.id AND av.actor_id = cv.member_id AND av.appreciation_id = cv.comment_id
WHERE av.id IS NULL;

INSERT INTO core_module_verification_guard
SELECT 'report row mismatch', COUNT(*)
FROM comment_reports cr
LEFT JOIN appreciation_reports ar
  ON ar.id = cr.id AND ar.reporter_id = cr.reporter_id AND ar.appreciation_id = cr.comment_id
WHERE ar.id IS NULL;

-- The migration creates every root of the known initial kinds with exactly one subtype.
-- Unknown future kinds are deliberately not enumerated or rejected.
INSERT INTO core_module_verification_guard
SELECT 'orphan club root', COUNT(*)
FROM spaces s LEFT JOIN clubs c ON c.space_id = s.id
WHERE s.kind = 'CLUB' AND c.id IS NULL;
INSERT INTO core_module_verification_guard
SELECT 'orphan public room root', COUNT(*)
FROM spaces s LEFT JOIN public_rooms pr ON pr.space_id = s.id
WHERE s.kind = 'PUBLIC_ROOM' AND pr.id IS NULL;
INSERT INTO core_module_verification_guard
SELECT 'orphan book root', COUNT(*)
FROM contents c LEFT JOIN books b ON b.content_id = c.id
WHERE c.kind = 'BOOK' AND b.id IS NULL;
INSERT INTO core_module_verification_guard
SELECT 'orphan passage root', COUNT(*)
FROM content_locations cl LEFT JOIN passages p ON p.location_id = cl.id
WHERE cl.kind = 'PASSAGE' AND p.id IS NULL;
INSERT INTO core_module_verification_guard
SELECT 'orphan sentence root', COUNT(*)
FROM content_locations cl LEFT JOIN sentences s ON s.location_id = cl.id
WHERE cl.kind = 'SENTENCE' AND s.id IS NULL;
INSERT INTO core_module_verification_guard
SELECT 'orphan comment root', COUNT(*)
FROM appreciations a LEFT JOIN comments c ON c.id = a.id
WHERE a.kind = 'COMMENT' AND c.id IS NULL;

DROP TEMPORARY TABLE core_module_verification_guard;
