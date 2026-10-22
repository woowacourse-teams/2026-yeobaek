SELECT installed_rank, version, description, type, success
FROM flyway_schema_history ORDER BY installed_rank;

-- Both rows must exist, be nullable, and have NULL defaults.
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'books'
  AND column_name IN ('description', 'body_character_count')
ORDER BY column_name;

-- Readiness gate: active_uncomputed and nonpositive must both be zero after V5 succeeds.
SELECT COUNT(*) AS active_uncomputed FROM books
WHERE status = 'ACTIVE' AND body_character_count IS NULL;
SELECT COUNT(*) AS nonpositive FROM books WHERE body_character_count <= 0;
SELECT id AS uncomputed_book_id FROM books WHERE body_character_count IS NULL ORDER BY id;
