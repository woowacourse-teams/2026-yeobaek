"""Unit checks and an end-to-end check with a real Spring app and disposable MySQL."""

import importlib.util
import json
from pathlib import Path
import socket
import subprocess
import sys
import tempfile
import time
import unittest
from unittest.mock import Mock
import urllib.error
import urllib.request
import uuid


HERE = Path(__file__).resolve().parent
BACKEND = HERE.parents[1]
sys.path.insert(0, str(HERE))
SPEC = importlib.util.spec_from_file_location("load_test", HERE / "load-test.py")
SCRIPT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SCRIPT)
DATA = SCRIPT.dummy_data
DATA_ARGS = ["seed", "--books", "2", "--chapters-per-book", "2", "--passages-per-chapter", "5",
             "--sentences-per-passage", "2", "--members", "8", "--clubs", "3", "--members-per-club", "2"]


class ConfigurationTest(unittest.TestCase):
    def test_only_app_database_scalar_changes_and_switch_is_persisted(self):
        text = """services:
  app:
    image: example:commit
    environment:
      DB_URL: jdbc:mysql://mysql:3306/yeobaek
      ADMIN_TOKEN: ${ADMIN_TOKEN}
  mysql:
    environment:
      DB_URL: unchanged
"""
        patched = SCRIPT.compose_database_variable(text, "app", "jdbc:mysql://mysql:3306/yeobaek")
        self.assertIn('DB_URL: "${YEOBAEK_DATABASE_URL:-jdbc:mysql://mysql:3306/yeobaek}"', patched)
        self.assertIn("DB_URL: unchanged", patched)
        self.assertIn("ADMIN_TOKEN: ${ADMIN_TOKEN}", patched)
        env = "DB_PASSWORD=keep-me\n# comment\n"
        selected = SCRIPT.selected_env(env, "jdbc:mysql://mysql:3306/yeobaek_load_test")
        selected = SCRIPT.selected_env(selected, "jdbc:mysql://mysql:3306/yeobaek")
        self.assertEqual(selected.count("YEOBAEK_DATABASE_URL="), 1)
        self.assertIn("DB_PASSWORD=keep-me\n# comment\n", selected)

    def test_unsupported_yaml_and_ambiguous_dotenv_fail_before_mutation(self):
        with self.assertRaises(ValueError):
            SCRIPT.compose_database_variable("services: {app: {environment: {DB_URL: value}}}", "app", "value")
        with self.assertRaises(ValueError):
            SCRIPT.selected_env("YEOBAEK_DATABASE_URL=a\nYEOBAEK_DATABASE_URL=b\n", "url")

    def test_database_names_are_safe_and_jdbc_options_are_preserved(self):
        url = "jdbc:mysql://mysql:3306/yeobaek?useSSL=false&serverTimezone=Asia/Seoul"
        self.assertEqual(SCRIPT.replace_database(url, "yeobaek_load_test"),
                         "jdbc:mysql://mysql:3306/yeobaek_load_test?useSSL=false&serverTimezone=Asia/Seoul")
        for name in ("yeobaek;DROP DATABASE x", "mysql", "sys", "x`y", "a" * 65):
            with self.assertRaises(ValueError):
                SCRIPT.identifier(name)

    def test_comment_ratio_counts_and_parent_relationships(self):
        args = SCRIPT.parse_args(DATA_ARGS)
        plan = DATA.plan(args)
        self.assertEqual(plan["sentences"], 40)
        self.assertEqual(plan["comments"], 12)
        selected = list(DATA.comment_rows(args))
        self.assertEqual([row[2] for row in selected], [1, 4, 7, 11, 14, 17, 21, 24, 27, 31, 34, 37])
        self.assertEqual(len({row[2] for row in selected}), len(selected))
        self.assertEqual(plan["club_comments"] + plan["public_room_comments"], plan["comments"])

    def test_invalid_sizes_and_text_fail_before_database_access(self):
        args = SCRIPT.parse_args(DATA_ARGS)
        args.members_per_club = 9
        with self.assertRaises(ValueError):
            DATA.plan(args)
        args.members_per_club = 2
        args.comment_text = " "
        with self.assertRaises(ValueError):
            DATA.plan(args)

    def test_preview_needs_no_docker_or_configuration_file(self):
        result = subprocess.run([sys.executable, str(HERE / "load-test.py"), *DATA_ARGS, "--dry-run"],
                                text=True, capture_output=True, cwd="/tmp")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(json.loads(result.stdout)["rows"]["comments"], 12)

    def test_data_changes_require_stopped_app_or_running_dev_selection(self):
        args = SCRIPT.parse_args(DATA_ARGS)
        data = {"dev_url": "jdbc:mysql://mysql/dev", "load_url": "jdbc:mysql://mysql/load"}
        docker = Mock()
        for status in ("created", "exited"):
            for selected in ("dev_url", "load_url"):
                with self.subTest(status=status, selected=selected):
                    docker.container.return_value = {
                        "State": {"Running": False, "Restarting": False, "Status": status},
                        "Config": {"Env": ["DB_URL=" + data[selected]]}}
                    SCRIPT.require_idle_load_database(docker, args, data)
        for status in ("restarting", "removing", "dead"):
            with self.subTest(status=status):
                docker.container.return_value = {"State": {
                    "Running": False, "Restarting": status == "restarting", "Status": status}}
                with self.assertRaises(RuntimeError):
                    SCRIPT.require_idle_load_database(docker, args, data)
        docker.container.return_value = {"State": {"Running": False, "Status": "exited"}}
        for pending in ("configuration_pending", "pending"):
            with self.subTest(pending=pending), self.assertRaises(RuntimeError):
                SCRIPT.require_idle_load_database(docker, args, {**data, pending: True})
        for runtime, configured in (("dev_url", "dev_url"), ("dev_url", "load_url"), ("load_url", "dev_url")):
            with self.subTest(runtime=runtime, configured=configured):
                docker.container.return_value = {
                    "State": {"Running": True, "Restarting": False, "Status": "running"},
                    "Config": {"Env": ["DB_URL=" + data[runtime]]}}
                docker.config.return_value = {"services": {"app": {"environment": {"DB_URL": data[configured]}}}}
                if runtime == configured == "dev_url":
                    SCRIPT.require_idle_load_database(docker, args, data)
                else:
                    with self.assertRaises(RuntimeError):
                        SCRIPT.require_idle_load_database(docker, args, data)


class RealAppSwitchTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.workspace = tempfile.TemporaryDirectory(prefix="yeobaek-load-test-")
        cls.addClassCleanup(cls.workspace.cleanup)
        cls.root = Path(cls.workspace.name)
        cls.project = "yeobaek-load-check-" + uuid.uuid4().hex[:10]
        cls.image = cls.project + ":local"
        SCRIPT.run(["docker", "build", "-f", "infra/prod/Dockerfile", "-t", cls.image, "."], cwd=BACKEND)
        cls.addClassCleanup(SCRIPT.run, ["docker", "image", "rm", cls.image])
        with socket.socket() as reservation:
            reservation.bind(("127.0.0.1", 0))
            cls.port = reservation.getsockname()[1]
        cls.compose = cls.root / "docker-compose.yml"
        cls.compose.write_text(f"""services:
  mysql:
    image: mysql:8.4
    environment:
      MYSQL_DATABASE: yeobaek
      MYSQL_ROOT_PASSWORD: ${{DB_PASSWORD}}
      MYSQL_ROOT_HOST: "%"
  app:
    image: {cls.image}
    ports:
      - "127.0.0.1:{cls.port}:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_JPA_HIBERNATE_DDL_AUTO: validate
      DB_URL: jdbc:mysql://mysql:3306/yeobaek
      DB_USERNAME: root
      DB_PASSWORD: ${{DB_PASSWORD}}
      ADMIN_TOKEN: test-only
      BOOK_COVER_S3_BUCKET: test-only
      BOOK_COVER_S3_REGION: ap-northeast-2
      BOOK_COVER_PUBLIC_BASE_URL: https://example.invalid
      POSTHOG_ENABLED: "false"
""")
        (cls.root / ".env").write_text("DB_PASSWORD=disposable-test-password\n")
        cls.common = ["--compose-file", str(cls.compose), "--project", cls.project]
        cls.docker = SCRIPT.Docker(SCRIPT.parse_args([*cls.common, "status"]))
        cls.addClassCleanup(cls.docker.command, "down", "--volumes", "--remove-orphans")
        cls.docker.command("up", "-d", "mysql")
        ready = False
        deadline = time.monotonic() + 90
        while time.monotonic() < deadline:
            try:
                with cls.docker.database("yeobaek") as db:
                    db.query("SELECT 1")
                ready = True
                break
            except RuntimeError:
                time.sleep(1)
        if not ready:
            raise RuntimeError("Disposable MySQL was not ready")
        baseline = (BACKEND / "src/test/resources/migration/2026-10-12-reading-schema-baseline.sql").read_text()
        migration = (BACKEND / "docs/migrations/2026-10-12-reading-schema.sql").read_text()
        local = (BACKEND / "src/main/resources/local/data.sql").read_text()
        with cls.docker.database("yeobaek") as db:
            db.query(baseline + migration + local)
            db.query("UPDATE books SET title='개발 DB 보존 책' WHERE id=1")
        cls.docker.command("up", "-d", "app")
        cls.url = f"http://127.0.0.1:{cls.port}"
        SCRIPT.wait_healthy(cls.docker, "jdbc:mysql://mysql:3306/yeobaek", cls.url + "/health", 120)

    def cli(self, *arguments, success=True):
        result = subprocess.run([sys.executable, str(HERE / "load-test.py"), *self.common, *arguments],
                                text=True, capture_output=True)
        if success:
            self.assertEqual(result.returncode, 0, result.stderr)
            return json.loads(result.stdout)
        self.assertNotEqual(result.returncode, 0)
        return result

    def api(self, path):
        request = urllib.request.Request(self.url + path, headers={"X-Member-Id": "1"})
        with urllib.request.urlopen(request, timeout=15) as response:
            return json.load(response)

    def counts(self, name):
        with self.docker.database(name) as db:
            return {table: db.query(f"SELECT COUNT(*) FROM `{table}`")[0][0] for table in DATA.TABLES}

    def test_prepare_seed_switch_persist_and_return_with_one_real_app(self):
        before = self.counts("yeobaek")
        mysql_id = self.docker.container("mysql")["Id"]
        image_id = self.docker.container("app")["Image"]
        self.cli("init")
        self.cli("init")  # Configuration initialization is idempotent.
        self.assertEqual(set(self.counts("yeobaek_load_test").values()), {"0"})
        self.assertEqual(self.counts("yeobaek"), before)
        self.assertEqual(self.docker.directory.stat().st_mode & 0o777, 0o700)
        self.assertEqual(self.docker.state_file.stat().st_mode & 0o777, 0o600)
        self.cli("use", "load", "--health-url", self.url + "/health", success=False)
        self.assertEqual(self.cli("status")["active_mode"], "dev")

        expected = DATA.plan(SCRIPT.parse_args(DATA_ARGS))
        db = SCRIPT.Mysql(self.docker.mysql_command("yeobaek_load_test"))
        query = db.query

        def fail_at_comments(sql):
            if sql.startswith("INSERT INTO `comments`"):
                return query("INSERT INTO author_books (id, author_id, book_id) VALUES (99, -1, -1)")
            return query(sql)

        db.query = fail_at_comments
        try:
            with self.assertRaises(RuntimeError):
                DATA.seed(db, SCRIPT.parse_args(DATA_ARGS))
        finally:
            db.close()
        self.assertEqual(set(self.counts("yeobaek_load_test").values()), {"0"})
        result = self.cli(*DATA_ARGS, "--sentence-text", "일관된 본문 😀 ' ; $()", "--comment-text", "일관된 댓글 😀 '")
        self.assertEqual(result["rows"], expected)
        self.assertEqual(self.counts("yeobaek"), before)
        self.cli(*DATA_ARGS, success=False)  # No implicit reset or duplicate seeding.
        with self.docker.database("yeobaek_load_test") as db:
            self.assertEqual(db.query("SELECT COUNT(DISTINCT sentence_id), COUNT(*) FROM comments"), [["12", "12"]])
            self.assertEqual(db.query("SELECT content FROM sentences WHERE id=1"), [["일관된 본문 😀 ' ; $()"]])
            invalid = db.query("""
                SELECT COUNT(*) FROM club_comments cc
                JOIN comments c ON c.id=cc.comment_id
                JOIN sentences s ON s.id=c.sentence_id
                JOIN passages p ON p.id=s.passage_id
                JOIN chapters ch ON ch.id=p.chapter_id
                JOIN club_books cb ON cb.club_id=cc.club_id
                LEFT JOIN club_members cm ON cm.club_id=cc.club_id AND cm.member_id=c.writer_id
                WHERE ch.book_id <> cb.book_id OR cm.id IS NULL
            """)
            self.assertEqual(invalid, [["0"]])
            self.assertEqual(db.query("""
                SELECT COUNT(*) FROM public_room_comments pc JOIN comments c ON c.id=pc.comment_id
                JOIN sentences s ON s.id=c.sentence_id JOIN passages p ON p.id=s.passage_id
                JOIN chapters ch ON ch.id=p.chapter_id JOIN public_rooms pr ON pr.id=pc.public_room_id
                LEFT JOIN public_room_members pm ON pm.public_room_id=pr.id AND pm.member_id=c.writer_id
                WHERE ch.book_id <> pr.book_id OR pm.id IS NULL
            """), [["0"]])

        # The Compose image tag can drift; a DB switch must use the running image ID.
        self.compose.write_text(self.compose.read_text().replace("image: " + self.image, "image: mysql:8.4"))
        self.cli("use", "load", "--health-url", self.url + "/health")
        self.assertEqual(self.docker.container("app")["Image"], image_id)
        self.assertTrue(all("부하테스트 책" in book["title"] for book in self.api("/api/books")["books"]))
        comment_list = self.api("/api/clubs/1/commented-sentences?currentPassageId=1")["commentedSentences"]
        self.assertTrue(comment_list)
        self.assertTrue(all(row["content"] == "일관된 본문 😀 ' ; $()" and row["commentCount"] == 1 for row in comment_list))
        self.assertEqual(self.docker.container("mysql")["Id"], mysql_id)
        self.assertEqual(self.cli("status")["configured_mode"], "load")
        self.assertEqual(len(self.docker.command("ps", "-q", "app").split()), 1)
        load_before = self.counts("yeobaek_load_test")
        self.cli("reset", success=False)
        self.assertIn("Stop the app or use dev", self.cli(*DATA_ARGS, success=False).stderr)
        self.assertEqual(self.counts("yeobaek_load_test"), load_before)

        # Simulate CI's temporary image override while retaining the persisted DB selection.
        override = self.root / "ci-image.json"
        override.write_text(json.dumps({"services": {"app": {"image": self.image}}}))
        self.docker.command("-f", str(override), "up", "-d", "--no-deps", "--force-recreate", "app")
        SCRIPT.wait_healthy(self.docker, "jdbc:mysql://mysql:3306/yeobaek_load_test", self.url + "/health", 120)
        self.assertEqual(self.cli("status")["active_mode"], "load")
        # Repair a server Compose overwrite without changing the active DB or deleting data.
        selected_template = self.compose.read_text()
        self.compose.write_text(SCRIPT.compose_database_variable(selected_template, "app", "jdbc:mysql://mysql:3306/yeobaek")
                                .replace('DB_URL: "${YEOBAEK_DATABASE_URL:-jdbc:mysql://mysql:3306/yeobaek}"',
                                         'DB_URL: jdbc:mysql://mysql:3306/yeobaek'))
        self.cli("use", "load", "--health-url", self.url + "/health", success=False)
        self.cli("init")
        self.assertEqual(self.cli("status")["configured_mode"], "load")
        self.cli("use", "dev", "--health-url", self.url + "/health")
        self.assertEqual(self.api("/api/books")["books"][0]["title"], "개발 DB 보존 책")
        self.assertEqual(self.counts("yeobaek"), before)

        # Keep load selected, stop the app, reset/reseed, then start the same container.
        self.cli("use", "load", "--health-url", self.url + "/health")
        app_id = self.docker.container("app")["Id"]
        self.docker.command("stop", "app")
        self.assertFalse(self.docker.container("app")["State"]["Running"])
        self.cli("reset")
        self.assertEqual(set(self.counts("yeobaek_load_test").values()), {"0"})
        self.assertEqual(self.cli(*DATA_ARGS)["rows"], expected)
        self.assertEqual(self.counts("yeobaek_load_test"), {table: str(count) for table, count in expected.items()})
        self.assertFalse(self.docker.container("app")["State"]["Running"])
        self.assertEqual(self.cli("status")["configured_mode"], "load")
        self.assertEqual(self.counts("yeobaek"), before)
        self.docker.command("start", "app")
        SCRIPT.wait_healthy(self.docker, "jdbc:mysql://mysql:3306/yeobaek_load_test", self.url + "/health", 120)
        self.assertEqual(self.docker.container("app")["Id"], app_id)
        self.assertEqual(self.docker.container("mysql")["Id"], mysql_id)
        self.assertEqual(len(self.api("/api/books")["books"]), 2)
        self.cli("use", "dev", "--health-url", self.url + "/health")
        self.assertEqual(self.counts("yeobaek_load_test"), {table: str(count) for table, count in expected.items()})

        # A timed-out switch is visible and can be recovered even if the app container is removed.
        self.cli("use", "load", "--health-url", self.url + "/invalid-health", "--health-timeout", "1", success=False)
        self.assertIsNotNone(self.cli("status")["pending"])
        self.docker.command("rm", "-s", "-f", "app")
        self.assertEqual(self.cli("status")["active_mode"], "unknown")
        self.cli("use", "dev", "--health-url", self.url + "/health")
        self.assertEqual(self.cli("status")["active_mode"], "dev")
        self.assertIsNone(self.cli("status")["pending"])
        self.assertEqual(self.counts("yeobaek"), before)

        # Reset also removes data created by later API scenarios, including leaf tables.
        with self.docker.database("yeobaek_load_test") as db:
            db.query("""
                INSERT INTO member_blocks (blocker_id, blocked_id) VALUES (1, 2);
                INSERT INTO comment_views (member_id, comment_id) VALUES (2, 1);
                INSERT INTO reports (reporter_id, comment_id) VALUES (2, 1)
            """)
        load_before = self.counts("yeobaek_load_test")
        with self.docker.database("yeobaek") as db:
            db.query("""
                CREATE TABLE reset_external_reference (
                    id BIGINT PRIMARY KEY, book_id BIGINT,
                    FOREIGN KEY (book_id) REFERENCES yeobaek_load_test.books(id) ON DELETE CASCADE
                );
                INSERT INTO reset_external_reference VALUES (1, 1)
            """)
        self.cli("reset", success=False)
        self.assertEqual(self.counts("yeobaek_load_test"), load_before)
        with self.docker.database("yeobaek") as db:
            self.assertEqual(db.query("SELECT COUNT(*) FROM reset_external_reference"), [["1"]])
            db.query("DROP TABLE reset_external_reference")

        # A mid-reset error must restore every prior DELETE in the same transaction.
        db = SCRIPT.Mysql(self.docker.mysql_command("yeobaek_load_test"))
        original_query = db.query

        def fail_reset_at_books(sql):
            if sql == "DELETE FROM `books`":
                return original_query("DELETE FROM nonexistent_reset_table")
            return original_query(sql)

        db.query = fail_reset_at_books
        try:
            with self.assertRaises(RuntimeError):
                SCRIPT.reset_rows(db, "yeobaek_load_test")
        finally:
            db.close()
        self.assertEqual(self.counts("yeobaek_load_test"), load_before)

        state_before = self.docker.state_file.read_text()
        tampered = json.loads(state_before)
        tampered["load_database"] = tampered["dev_database"]
        tampered["load_url"] = tampered["dev_url"]
        self.docker.state_file.write_text(json.dumps(tampered))
        try:
            self.cli("reset", success=False)
        finally:
            self.docker.state_file.write_text(state_before)
        self.assertEqual(self.counts("yeobaek"), before)
        self.assertEqual(self.counts("yeobaek_load_test"), load_before)

        cleared = self.cli("reset")
        self.assertEqual(cleared["removed_rows"], {table: int(count) for table, count in load_before.items()})
        self.assertEqual(set(self.counts("yeobaek_load_test").values()), {"0"})
        with self.docker.database("yeobaek_load_test") as db:
            self.assertEqual(SCRIPT.schema_tables(db, "yeobaek_load_test"), set(DATA.TABLES))
        self.assertEqual(self.counts("yeobaek"), before)
        self.cli("reset")  # Empty reset is also safe.
        resized = list(DATA_ARGS)
        resized[resized.index("--chapters-per-book") + 1] = "10"
        resized[resized.index("--passages-per-chapter") + 1] = "300"
        expected_resized = DATA.plan(SCRIPT.parse_args(resized))
        self.assertEqual(self.cli(*resized)["rows"], expected_resized)
        self.assertEqual(expected_resized["passages"], 6000)
        self.assertEqual(expected_resized["comments"], 3600)
        self.assertEqual(self.counts("yeobaek_load_test"), {table: str(count) for table, count in expected_resized.items()})
        self.assertEqual(self.counts("yeobaek"), before)


if __name__ == "__main__":
    unittest.main()
