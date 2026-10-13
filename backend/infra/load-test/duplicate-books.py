#!/usr/bin/env python3
"""Copy existing book trees to a requested total using the container's mysql client."""

import argparse
from collections import defaultdict
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import uuid


TABLES = ("books", "authors", "author_books", "chapters", "passages", "sentences")


class Mysql:
    """Keep all queries in one mysql connection, and thus one transaction."""

    def __init__(self, command):
        self.errors = tempfile.TemporaryFile(mode="w+t")
        try:
            self.process = subprocess.Popen(
                command, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                stderr=self.errors, text=True, encoding="utf-8", bufsize=1,
            )
        except Exception:
            self.errors.close()
            raise

    def query(self, sql):
        marker = "end_" + uuid.uuid4().hex
        try:
            self.process.stdin.write(sql + ";\nSELECT '" + marker + "';\n")
            self.process.stdin.flush()
        except BrokenPipeError:
            self.fail()
        rows = []
        for line in self.process.stdout:
            line = line.rstrip("\n")
            if line == marker:
                return rows
            rows.append(line.split("\t"))
        self.fail()

    def fail(self):
        self.process.wait()
        self.errors.seek(0)
        raise RuntimeError(self.errors.read().strip() or "MySQL connection closed unexpectedly")

    def close(self):
        # An uncommitted InnoDB transaction rolls back when this connection closes.
        try:
            self.process.stdin.close()
        except BrokenPipeError:
            pass
        self.process.wait()
        self.process.stdout.close()
        self.errors.close()


def positive_integer(value):
    number = int(value)
    if number <= 0:
        raise argparse.ArgumentTypeError("must be a positive integer")
    return number


def mysql_command(args):
    client = (
        'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot '
        '--default-character-set=utf8mb4 --batch --raw '
        '--skip-column-names --unbuffered --skip-reconnect "$1"'
    )
    if args.container:
        return ["docker", "exec", "-i", args.container, "sh", "-c", client, "sh", args.database]
    return [
        "docker", "compose", "-p", args.project, "-f", str(args.compose_file),
        "exec", "-T", args.service, "sh", "-c", client, "sh", args.database,
    ]


def clone_sql(source_id, chapters, passages):
    statements = [
        "INSERT INTO books (title, publisher, published_year, cover_image_key, passage_count, status) "
        "SELECT title, publisher, published_year, cover_image_key, passage_count, status "
        f"FROM books WHERE id = {source_id}",
        "SET @new_book = LAST_INSERT_ID()",
        "INSERT INTO author_books (author_id, book_id) "
        f"SELECT author_id, @new_book FROM author_books WHERE book_id = {source_id}",
    ]
    for chapter_id in chapters:
        statements.extend([
            "INSERT INTO chapters (book_id, title, `sequence`) "
            f"SELECT @new_book, title, `sequence` FROM chapters WHERE id = {chapter_id}",
            "SET @new_chapter = LAST_INSERT_ID()",
        ])
        for passage_id in passages[chapter_id]:
            statements.extend([
                "INSERT INTO passages (chapter_id, `sequence`) "
                f"SELECT @new_chapter, `sequence` FROM passages WHERE id = {passage_id}",
                "SET @new_passage = LAST_INSERT_ID()",
                "INSERT INTO sentences (passage_id, `sequence`, content) "
                f"SELECT @new_passage, `sequence`, content FROM sentences WHERE passage_id = {passage_id}",
            ])
    statements.append("SELECT @new_book")
    return ";\n".join(statements)


def duplicate(db, total, dry_run=False):
    engines = db.query(
        "SELECT table_name, engine FROM information_schema.tables "
        "WHERE table_schema = DATABASE() AND table_name IN ("
        + ",".join("'" + table + "'" for table in TABLES) + ")"
    )
    if {row[0] for row in engines} != set(TABLES) or any(row[1] != "InnoDB" for row in engines):
        raise RuntimeError("The six book tables must exist and use InnoDB")
    db.query("SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ; START TRANSACTION")
    # Lock the whole book range so concurrent book insertion cannot change the target total.
    source_ids = [int(row[0]) for row in db.query("SELECT id FROM books ORDER BY id FOR UPDATE")]
    initial = len(source_ids)
    if not initial:
        raise RuntimeError("No source books exist; add a book before running this script")
    if total < initial:
        raise RuntimeError(f"Current total is {initial}, larger than {total}; this script never deletes books")
    summary = {"before": initial, "target": total, "added": total - initial, "dry_run": dry_run}
    if total == initial or dry_run:
        db.query("ROLLBACK")
        return summary

    chapters = defaultdict(list)
    for chapter_id, book_id in db.query("SELECT id, book_id FROM chapters ORDER BY book_id, `sequence`, id FOR SHARE"):
        chapters[int(book_id)].append(int(chapter_id))
    passages = defaultdict(list)
    for passage_id, chapter_id in db.query("SELECT id, chapter_id FROM passages ORDER BY chapter_id, `sequence`, id FOR SHARE"):
        passages[int(chapter_id)].append(int(passage_id))

    source_counts = {
        int(book_id): int(count)
        for book_id, count in db.query("SELECT id, passage_count FROM books ORDER BY id")
    }
    for source_id in source_ids:
        actual = sum(len(passages[ch]) for ch in chapters[source_id])
        if actual != source_counts[source_id]:
            raise RuntimeError(f"Book {source_id}: passage_count={source_counts[source_id]}, actual={actual}")

    for index in range(total - initial):
        source_id = source_ids[index % initial]
        db.query(clone_sql(source_id, chapters[source_id], passages))
        print(f"Prepared {index + 1}/{total - initial} copies (source book {source_id})", file=sys.stderr)
    actual_total = int(db.query("SELECT COUNT(*) FROM books")[0][0])
    if actual_total != total:
        raise RuntimeError(f"Unexpected total {actual_total}; expected {total}")
    # Successful data stays in the DB. There is no cleanup/delete command.
    db.query("COMMIT")
    summary["after"] = actual_total
    return summary


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--total", type=positive_integer, required=True, help="Final books row count, including existing rows")
    parser.add_argument("--dry-run", action="store_true", help="Show counts without inserting data")
    parser.add_argument("--container", help="Use docker exec instead of Docker Compose")
    parser.add_argument("--compose-file", type=Path, default=Path("docker-compose.yml"))
    parser.add_argument("--project", default="yeobaek-dev")
    parser.add_argument("--service", default="mysql")
    parser.add_argument("--database", default="yeobaek")
    return parser.parse_args(argv)


def main(argv=None):
    args = parse_args(argv)
    db = None
    try:
        db = Mysql(mysql_command(args))
        print(json.dumps(duplicate(db, args.total, args.dry_run), ensure_ascii=False))
        return 0
    except (OSError, RuntimeError, KeyboardInterrupt) as error:
        print(f"Stopped: {error}", file=sys.stderr)
        print("Uncommitted changes are cancelled. If the connection failed during COMMIT, check the DB count before retrying.", file=sys.stderr)
        return 1
    finally:
        if db:
            db.close()


if __name__ == "__main__":
    sys.exit(main())
