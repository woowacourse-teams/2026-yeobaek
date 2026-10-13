"""Integration checks against a disposable MySQL 8.4 container."""

import importlib.util
import json
from pathlib import Path
import subprocess
import time
import unittest
import uuid


HERE = Path(__file__).resolve().parent
BACKEND = HERE.parents[1]
SPEC = importlib.util.spec_from_file_location("duplicate_books", HERE / "duplicate-books.py")
SCRIPT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SCRIPT)


class DuplicateBooksTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.container = "yeobaek-book-copy-test-" + uuid.uuid4().hex[:12]
        subprocess.run([
            "docker", "run", "-d", "--name", cls.container,
            "-e", "MYSQL_ROOT_PASSWORD=temporary-test-password",
            "-e", "MYSQL_DATABASE=yeobaek", "mysql:8.4",
        ], check=True, capture_output=True)
        cls.addClassCleanup(subprocess.run, ["docker", "rm", "-fv", cls.container], capture_output=True)
        cls.command = SCRIPT.mysql_command(SCRIPT.parse_args(["--total", "1", "--container", cls.container]))
        deadline = time.monotonic() + 90
        while time.monotonic() < deadline:
            result = subprocess.run(cls.command, input="SELECT 1;", text=True, capture_output=True)
            if result.returncode == 0:
                return
            if "unknown option" in result.stderr:
                raise RuntimeError(result.stderr)
            time.sleep(1)
        raise RuntimeError("Disposable MySQL did not become ready: " + result.stderr)

    def sql(self, sql):
        db = SCRIPT.Mysql(self.command)
        try:
            return db.query(sql)
        finally:
            db.close()

    def setUp(self):
        baseline = (BACKEND / "src/test/resources/migration/2026-10-12-reading-schema-baseline.sql").read_text()
        migration = (BACKEND / "docs/migrations/2026-10-12-reading-schema.sql").read_text()
        seed = (BACKEND / "src/main/resources/local/data.sql").read_text()
        self.sql("DROP DATABASE yeobaek; CREATE DATABASE yeobaek; USE yeobaek;" + baseline + migration + seed)
        self.sql("""
            INSERT INTO books (id, title, publisher, published_year, cover_image_key, passage_count, status)
            VALUES (7, '두 번째 책', NULL, NULL, NULL, 2, 'ACTIVE');
            INSERT INTO author_books (author_id, book_id) VALUES (1, 7);
            INSERT INTO chapters (id, book_id, title, `sequence`)
            VALUES (10, 7, '첫 장', 1), (11, 7, '동일 순서의 다른 장', 1);
            INSERT INTO passages (id, chapter_id, `sequence`) VALUES (101, 10, 1), (102, 11, 1);
            INSERT INTO sentences (id, passage_id, `sequence`, content)
            VALUES (1001, 101, 1, '한글 😀와 따옴표 ''도 그대로'), (1002, 102, 1, '다른 장의 문장');
        """)

    def run_copy(self, total, *extra):
        return subprocess.run([
            "python3", str(HERE / "duplicate-books.py"), "--container", self.container,
            "--total", str(total), *extra,
        ], text=True, capture_output=True)

    def book_tree(self, book_id):
        return self.sql(f"""
            SELECT HEX(b.title), IFNULL(HEX(b.publisher), 'NULL'),
                   IFNULL(b.published_year, -1), IFNULL(b.cover_image_key, 'NULL'),
                   b.status, b.passage_count, ch.`sequence`, HEX(ch.title),
                   p.`sequence`, s.`sequence`, HEX(s.content)
            FROM books b JOIN chapters ch ON ch.book_id = b.id
            JOIN passages p ON p.chapter_id = ch.id
            JOIN sentences s ON s.passage_id = p.id
            WHERE b.id = {book_id}
            ORDER BY ch.`sequence`, ch.id, p.`sequence`, p.id, s.`sequence`, s.id
        """)

    def unrelated_counts(self):
        tables = ("authors", "members", "clubs", "club_books", "club_members", "comments",
                  "club_comments", "public_rooms", "public_room_members")
        return {table: self.sql(f"SELECT COUNT(*) FROM {table}") for table in tables}

    def test_round_robin_preserves_trees_and_original_data(self):
        original = {book_id: self.book_tree(book_id) for book_id in (1, 7)}
        unrelated = self.unrelated_counts()
        result = self.run_copy(7)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(json.loads(result.stdout)["after"], 7)
        copies = self.sql("SELECT id FROM books WHERE id NOT IN (1, 7) ORDER BY id")
        for index, row in enumerate(copies):
            copy_id = int(row[0])
            self.assertEqual(self.book_tree(copy_id), original[(1, 7)[index % 2]])
            self.assertEqual(self.sql(f"SELECT author_id FROM author_books WHERE book_id={copy_id}"), [["1"]])
        self.assertEqual(self.unrelated_counts(), unrelated)
        for book_id in (1, 7):
            self.assertEqual(self.book_tree(book_id), original[book_id])

    def test_repeated_target_is_noop_and_smaller_target_never_deletes(self):
        self.assertEqual(self.run_copy(5).returncode, 0)
        before = self.sql("SELECT id FROM books ORDER BY id")
        again = self.run_copy(5)
        self.assertEqual(again.returncode, 0, again.stderr)
        self.assertEqual(json.loads(again.stdout)["added"], 0)
        smaller = self.run_copy(3)
        self.assertNotEqual(smaller.returncode, 0)
        self.assertEqual(self.sql("SELECT id FROM books ORDER BY id"), before)

    def test_dry_run_and_empty_source(self):
        result = self.run_copy(100, "--dry-run")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(json.loads(result.stdout)["added"], 98)
        self.assertEqual(self.sql("SELECT COUNT(*) FROM books"), [["2"]])
        self.sql("SET FOREIGN_KEY_CHECKS=0; DELETE FROM books")
        self.assertNotEqual(self.run_copy(100).returncode, 0)
        self.assertEqual(self.sql("SELECT COUNT(*) FROM books"), [["0"]])

    def test_invalid_source_and_mid_copy_error_leave_no_partial_data(self):
        self.sql("UPDATE books SET passage_count=3 WHERE id=7")
        self.assertNotEqual(self.run_copy(5).returncode, 0)
        self.assertEqual(self.sql("SELECT COUNT(*) FROM books"), [["2"]])
        self.sql("UPDATE books SET passage_count=2 WHERE id=7")
        db = SCRIPT.Mysql(self.command)
        original_query = db.query
        copies = 0

        def fail_second_copy(sql):
            nonlocal copies
            if sql.startswith("INSERT INTO books"):
                copies += 1
                if copies == 2:
                    return original_query("INSERT INTO author_books (author_id, book_id) VALUES (-1, -1)")
            return original_query(sql)

        db.query = fail_second_copy
        try:
            with self.assertRaises(RuntimeError):
                SCRIPT.duplicate(db, 5)
        finally:
            db.close()
        self.assertEqual(self.sql("SELECT COUNT(*) FROM books"), [["2"]])
        self.assertEqual(self.sql("SELECT COUNT(*) FROM chapters"), [["5"]])

    def test_invalid_total_is_rejected_before_connecting(self):
        result = self.run_copy(0)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("positive integer", result.stderr)


if __name__ == "__main__":
    unittest.main()
