-- 데이터를 변경하지 않는 사전 확인이다. 실제 변경은 Flyway V3가 앱 기동 시 수행한다.
-- notification_registrations가 이미 있으면 V3와 중복되므로 자동 진행하지 않고 적용 이력과 구조를 대조한다.
SELECT DATABASE() AS selected_database, @@hostname AS mysql_host, VERSION() AS mysql_version;

SELECT table_name, engine
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('members', 'notification_registrations');

SELECT table_name, column_name, column_type, is_nullable, extra, collation_name
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND (table_name = 'notification_registrations' OR (table_name = 'members' AND column_name = 'id'))
ORDER BY table_name, ordinal_position;

SELECT table_name, index_name, non_unique,
       GROUP_CONCAT(column_name ORDER BY seq_in_index) AS indexed_columns
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'notification_registrations'
GROUP BY table_name, index_name, non_unique;
