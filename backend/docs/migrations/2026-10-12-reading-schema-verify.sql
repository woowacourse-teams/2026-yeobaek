-- 모든 결과가 0이어야 한다. 마지막 table_count는 20이어야 한다.

SELECT 'unexpected_old_tables' AS check_name, COUNT(*) AS failure_count
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('comment_reports', 'public_room_activities', 'sentence_comments',
                     'public_room_visits', 'last_reads', 'public_room_books')
UNION ALL
SELECT 'missing_target_tables', 20 - COUNT(*)
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('authors', 'author_books', 'books', 'chapters', 'passages', 'sentences',
                     'members', 'member_blocks', 'public_rooms', 'public_room_members',
                     'public_room_member_last_reads', 'clubs', 'club_books', 'club_members',
                     'club_member_last_reads', 'comments', 'public_room_comments',
                     'club_comments', 'comment_views', 'reports')
UNION ALL
SELECT 'invalid_target_unique_indexes', 7 - COUNT(*)
FROM (
    SELECT table_name, index_name,
           GROUP_CONCAT(column_name ORDER BY seq_in_index) AS indexed_columns,
           MAX(non_unique) AS non_unique
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND CONCAT(table_name, '.', index_name) IN (
          'club_books.uk_club_books_club',
          'club_member_last_reads.uk_club_member_last_reads_member',
          'public_room_members.uk_public_room_members_member_room',
          'public_room_member_last_reads.uk_public_room_member_last_reads_member',
          'club_comments.uk_club_comments_comment',
          'public_room_comments.uk_public_room_comments_comment',
          'reports.uk_reports_reporter_comment')
    GROUP BY table_name, index_name
) target_indexes
WHERE non_unique = 0
  AND CONCAT(table_name, '.', index_name, ':', indexed_columns) IN (
      'club_books.uk_club_books_club:club_id',
      'club_member_last_reads.uk_club_member_last_reads_member:club_member_id',
      'public_room_members.uk_public_room_members_member_room:member_id,public_room_id',
      'public_room_member_last_reads.uk_public_room_member_last_reads_member:public_room_member_id',
      'club_comments.uk_club_comments_comment:comment_id',
      'public_room_comments.uk_public_room_comments_comment:comment_id',
      'reports.uk_reports_reporter_comment:reporter_id,comment_id')
UNION ALL
SELECT 'missing_required_cascade_foreign_keys', 11 - COUNT(*)
FROM information_schema.key_column_usage kcu
JOIN information_schema.referential_constraints rc
  ON rc.constraint_schema = kcu.constraint_schema
 AND rc.constraint_name = kcu.constraint_name
 AND rc.table_name = kcu.table_name
WHERE kcu.table_schema = DATABASE()
  AND rc.delete_rule = 'CASCADE'
  AND CONCAT(kcu.table_name, '.', kcu.column_name, ':', kcu.referenced_table_name) IN (
      'club_books.club_id:clubs',
      'club_member_last_reads.club_member_id:club_members',
      'public_room_member_last_reads.public_room_member_id:public_room_members',
      'club_comments.comment_id:comments',
      'public_room_comments.comment_id:comments',
      'reports.comment_id:comments',
      'reports.reporter_id:members',
      'comment_views.comment_id:comments',
      'comment_views.member_id:members',
      'member_blocks.blocker_id:members',
      'member_blocks.blocked_id:members')
UNION ALL
SELECT 'nullable_comment_writers', COUNT(*) FROM comments WHERE writer_id IS NULL
UNION ALL
SELECT 'clubs_without_book', COUNT(*)
FROM clubs c LEFT JOIN club_books cb ON cb.club_id = c.id WHERE cb.id IS NULL
UNION ALL
SELECT 'orphan_club_reads', COUNT(*)
FROM club_member_last_reads lr LEFT JOIN club_members cm ON cm.id = lr.club_member_id
WHERE cm.id IS NULL
UNION ALL
SELECT 'orphan_public_reads', COUNT(*)
FROM public_room_member_last_reads lr
LEFT JOIN public_room_members prm ON prm.id = lr.public_room_member_id
WHERE prm.id IS NULL
UNION ALL
SELECT 'comments_without_exactly_one_space', COUNT(*)
FROM comments c
LEFT JOIN club_comments cc ON cc.comment_id = c.id
LEFT JOIN public_room_comments prc ON prc.comment_id = c.id
WHERE (cc.id IS NULL) = (prc.id IS NULL)
UNION ALL
SELECT 'orphan_reports', COUNT(*)
FROM reports r
LEFT JOIN comments c ON c.id = r.comment_id
LEFT JOIN members m ON m.id = r.reporter_id
WHERE c.id IS NULL OR m.id IS NULL
UNION ALL
SELECT 'orphan_comment_views', COUNT(*)
FROM comment_views cv
LEFT JOIN comments c ON c.id = cv.comment_id
LEFT JOIN members m ON m.id = cv.member_id
WHERE c.id IS NULL OR m.id IS NULL
UNION ALL
SELECT 'orphan_member_blocks', COUNT(*)
FROM member_blocks mb
LEFT JOIN members blocker ON blocker.id = mb.blocker_id
LEFT JOIN members blocked ON blocked.id = mb.blocked_id
WHERE blocker.id IS NULL OR blocked.id IS NULL;

SELECT COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE';

SELECT table_name, column_name, is_nullable, column_type
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('clubs', 'club_members', 'comments', 'public_room_members',
                     'club_books', 'club_member_last_reads',
                     'public_room_member_last_reads', 'club_comments',
                     'public_room_comments', 'reports')
ORDER BY table_name, ordinal_position;
