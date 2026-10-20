-- 모든 행의 failure_count가 0이어야 한다. 기존 테이블에도 읽기 전용으로 실행할 수 있다.
SELECT 'missing_innodb_table' AS check_name, 1 - COUNT(*) AS failure_count
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'notification_registrations' AND engine = 'InnoDB'
UNION ALL
SELECT 'invalid_required_columns', 3 - COUNT(*)
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'notification_registrations'
  AND is_nullable = 'NO'
  AND ((column_name = 'id' AND column_type = 'bigint' AND extra LIKE '%auto_increment%')
    OR (column_name = 'member_id' AND column_type = 'bigint')
    OR (column_name = 'fid' AND column_type = 'varchar(255)' AND collation_name = 'ascii_bin'))
UNION ALL
SELECT 'unexpected_columns', COUNT(*)
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'notification_registrations'
  AND column_name NOT IN ('id', 'member_id', 'fid')
UNION ALL
SELECT 'invalid_required_indexes', 3 - COUNT(*)
FROM (
    SELECT index_name, MAX(non_unique) AS non_unique,
           GROUP_CONCAT(column_name ORDER BY seq_in_index) AS indexed_columns
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'notification_registrations'
    GROUP BY index_name
) indexes_to_check
WHERE (index_name = 'PRIMARY' AND non_unique = 0 AND indexed_columns = 'id')
   OR (index_name = 'ix_notification_registrations_member_id' AND non_unique = 1 AND indexed_columns = 'member_id')
   OR (index_name = 'ix_notification_registrations_fid' AND non_unique = 1 AND indexed_columns = 'fid')
UNION ALL
SELECT 'unexpected_unique_indexes', COUNT(DISTINCT index_name)
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'notification_registrations'
  AND non_unique = 0 AND index_name <> 'PRIMARY'
UNION ALL
SELECT 'invalid_member_foreign_key', 1 - COUNT(*)
FROM information_schema.key_column_usage kcu
JOIN information_schema.referential_constraints rc
  ON rc.constraint_schema = kcu.constraint_schema
 AND rc.constraint_name = kcu.constraint_name AND rc.table_name = kcu.table_name
WHERE kcu.table_schema = DATABASE() AND kcu.table_name = 'notification_registrations'
  AND kcu.column_name = 'member_id' AND kcu.referenced_table_schema = DATABASE()
  AND kcu.referenced_table_name = 'members' AND kcu.referenced_column_name = 'id'
  AND rc.delete_rule IN ('NO ACTION', 'RESTRICT')
UNION ALL
SELECT 'orphan_registrations', COUNT(*)
FROM notification_registrations registration
LEFT JOIN members member ON member.id = registration.member_id
WHERE member.id IS NULL;
