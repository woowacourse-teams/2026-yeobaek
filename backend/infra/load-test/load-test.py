#!/usr/bin/env python3
"""Prepare dummy data and switch one development app between two MySQL databases."""

import argparse
from contextlib import contextmanager
import fcntl
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
import uuid

import dummy_data


DATABASE_KEY = "YEOBAEK_DATABASE_URL"


def run(command, **kwargs):
    result = subprocess.run(command, capture_output=True, text=True, **kwargs)
    if result.returncode:
        raise RuntimeError(result.stderr.strip() or "Command failed: " + command[0])
    return result.stdout


class Mysql:
    """Use one client connection so uncommitted inserts stay atomic."""

    def __init__(self, command):
        self.errors = tempfile.TemporaryFile(mode="w+t")
        try:
            self.process = subprocess.Popen(command, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                            stderr=self.errors, text=True, encoding="utf-8", bufsize=1)
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
        try:
            self.process.stdin.close()
        except BrokenPipeError:
            pass
        self.process.wait()
        self.process.stdout.close()
        self.errors.close()


def identifier(value):
    if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_]{0,63}", value):
        raise ValueError("Database name must use ASCII letters, digits and underscores (max 64 characters)")
    if value.lower() in {"mysql", "sys", "information_schema", "performance_schema"}:
        raise ValueError("System databases cannot be used")
    return value


def positive_integer(value):
    number = int(value)
    if number <= 0:
        raise argparse.ArgumentTypeError("Must be a positive integer")
    return number


def jdbc_parts(url):
    match = re.fullmatch(r"jdbc:mysql://([A-Za-z0-9_.-]+)(?::([0-9]+))?/([A-Za-z][A-Za-z0-9_]{0,63})(\?[^\s'$\"{}]*)?", url)
    if not match:
        raise ValueError("Only a single-host jdbc:mysql://host[:port]/database URL is supported")
    host, port, database, query = match.groups()
    identifier(database)
    return host, port or "3306", database, query or ""


def replace_database(url, database):
    host, port, _, query = jdbc_parts(url)
    return f"jdbc:mysql://{host}:{port}/{identifier(database)}{query}"


def atomic_write(path, text, mode=0o600):
    path = Path(path)
    descriptor, temporary = tempfile.mkstemp(prefix=path.name + ".", dir=path.parent)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8") as output:
            output.write(text)
            output.flush()
            os.fsync(output.fileno())
            os.fchmod(output.fileno(), mode)
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def compose_database_variable(text, app_service, dev_url):
    """Change only the DB_URL scalar in the selected service's environment block."""
    lines = text.splitlines(keepends=True)
    service_indent = environment_indent = None
    in_services = False
    in_app = False
    changed = 0
    for index, line in enumerate(lines):
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        indent = len(line) - len(line.lstrip())
        if indent == 0:
            in_services = stripped == "services:"
            in_app = False
            service_indent = environment_indent = None
            continue
        if not in_services:
            continue
        if service_indent is None:
            service_indent = indent
        if indent == service_indent:
            in_app = stripped == app_service + ":"
            environment_indent = None
            continue
        if not in_app:
            continue
        if environment_indent is not None and indent <= environment_indent:
            environment_indent = None
        if stripped == "environment:":
            environment_indent = indent
            continue
        if environment_indent is not None:
            value = json.dumps("${" + DATABASE_KEY + ":-" + dev_url + "}")
            if re.match(r"DB_URL\s*:", stripped):
                lines[index] = " " * indent + "DB_URL: " + value + "\n"
                changed += 1
            elif re.match(r"-\s*DB_URL=", stripped):
                lines[index] = " " * indent + "- " + json.dumps("DB_URL=${" + DATABASE_KEY + ":-" + dev_url + "}") + "\n"
                changed += 1
    if changed != 1:
        raise ValueError("Expected exactly one DB_URL in app.environment; inline YAML, anchors and quoted keys are unsupported")
    return "".join(lines)


def selected_env(text, url):
    lines = text.splitlines(keepends=True)
    indexes = [i for i, line in enumerate(lines) if re.match(r"\s*(?:export\s+)?" + DATABASE_KEY + r"\s*=", line)]
    if len(indexes) > 1:
        raise ValueError("Duplicate " + DATABASE_KEY + " entries in .env")
    replacement = DATABASE_KEY + "='" + url + "'\n"
    if indexes:
        lines[indexes[0]] = replacement
    else:
        if lines and not lines[-1].endswith("\n"):
            lines[-1] += "\n"
        lines.append(replacement)
    return "".join(lines)


class Docker:
    def __init__(self, args):
        self.args = args
        self.compose_file = args.compose_file.resolve()
        self.env_file = self.compose_file.parent / ".env"
        self.directory = self.compose_file.parent / ".load-test-state"
        self.state_file = self.directory / "state.json"
        self.base = ["docker", "compose", "--project-directory", str(self.compose_file.parent),
                     "--env-file", str(self.env_file), "-p", args.project, "-f", str(self.compose_file)]

    def command(self, *arguments, selected_url=None):
        environment = os.environ.copy()
        # Shell exports must not override the persisted DB choice.
        environment.pop(DATABASE_KEY, None)
        if selected_url is not None:
            environment[DATABASE_KEY] = selected_url
        return run(self.base + list(arguments), env=environment)

    def container(self, service, allow_missing=False):
        ids = self.command("ps", "-a", "-q", service).split()
        if not ids and allow_missing:
            return None
        if len(ids) != 1:
            raise RuntimeError(f"Expected one {service} container, found {len(ids)}")
        info = json.loads(run(["docker", "inspect", ids[0]]))[0]
        labels = info["Config"].get("Labels", {})
        if labels.get("com.docker.compose.project") != self.args.project or labels.get("com.docker.compose.service") != service:
            raise RuntimeError("Container does not belong to the requested Compose project/service")
        return info

    def config(self, url=None):
        return json.loads(self.command("config", "--format", "json", selected_url=url))

    def mysql_command(self, database):
        client = ('MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4 '
                  '--batch --raw --skip-column-names --unbuffered --skip-reconnect "$1"')
        return self.base + ["exec", "-T", self.args.mysql_service, "sh", "-c", client, "sh", identifier(database)]

    @contextmanager
    def database(self, name):
        db = Mysql(self.mysql_command(name))
        try:
            yield db
        finally:
            db.close()

    def state(self):
        data = json.loads(self.state_file.read_text())
        expected = (self.args.project, self.args.app_service, self.args.mysql_service)
        if (data["project"], data["app_service"], data["mysql_service"]) != expected:
            raise RuntimeError("State belongs to another project or service")
        identifier(data["dev_database"])
        identifier(data["load_database"])
        if data["dev_database"].lower() == data["load_database"].lower():
            raise RuntimeError("Development and load-test databases must differ")
        if jdbc_parts(data["dev_url"])[2] != data["dev_database"] or jdbc_parts(data["load_url"])[2] != data["load_database"]:
            raise RuntimeError("Invalid database URLs in state")
        if replace_database(data["dev_url"], data["load_database"]) != data["load_url"]:
            raise RuntimeError("Databases must use the same MySQL endpoint")
        with self.database(data["dev_database"]) as db:
            if db.query("SELECT @@server_uuid")[0][0] != data["server_uuid"]:
                raise RuntimeError("MySQL server identity differs from the prepared server")
        return data

    def save(self, data):
        atomic_write(self.state_file, json.dumps(data, ensure_ascii=False, indent=2) + "\n")


def runtime_env(info):
    return dict(item.split("=", 1) for item in info["Config"]["Env"] if "=" in item)


def schema_tables(db, name):
    return {row[0] for row in db.query(
        "SELECT table_name FROM information_schema.tables "
        f"WHERE table_schema='{identifier(name)}' AND table_type='BASE TABLE'"
    )}


def verify_schema(db, name):
    if schema_tables(db, name) != set(dummy_data.TABLES):
        raise RuntimeError("Expected the current 20-table reading schema; prepare/migrate the development DB first")
    invalid = db.query(
        "SELECT COUNT(*) FROM information_schema.tables "
        f"WHERE table_schema='{identifier(name)}' AND engine <> 'InnoDB'"
    )
    if int(invalid[0][0]):
        raise RuntimeError("All tables must use InnoDB")
    foreign = db.query(
        "SELECT COUNT(*) FROM information_schema.key_column_usage "
        f"WHERE table_schema='{name}' AND referenced_table_schema IS NOT NULL "
        f"AND referenced_table_schema <> '{name}'"
    )
    if int(foreign[0][0]):
        raise RuntimeError("Cross-database foreign keys are unsupported")


def install_selection(docker, data, selected_url):
    compose_before = docker.compose_file.read_text()
    env_before = docker.env_file.read_text()
    compose_after = compose_database_variable(compose_before, docker.args.app_service, data["dev_url"])
    env_after = selected_env(env_before, selected_url)
    for filename, content in (("compose.before", compose_before), ("env.before", env_before)):
        backup = docker.directory / filename
        if not backup.exists():
            atomic_write(backup, content)
    data["configuration_pending"] = True
    docker.save(data)
    atomic_write(docker.compose_file, compose_after, docker.compose_file.stat().st_mode & 0o777)
    atomic_write(docker.env_file, env_after)
    if docker.config()["services"][docker.args.app_service]["environment"]["DB_URL"] != selected_url:
        raise RuntimeError("Compose did not resolve the saved DB selection; rerun init after checking configuration")
    data.pop("configuration_pending", None)
    docker.save(data)


def init(docker, args):
    if docker.state_file.exists():
        data = docker.state()
        if data["load_database"] != args.load_database:
            raise RuntimeError("A different load-test database is already configured")
        with docker.database(data["load_database"]) as db:
            verify_schema(db, data["load_database"])
        selected_url = runtime_env(docker.container(args.app_service))["DB_URL"]
        if selected_url not in (data["dev_url"], data["load_url"]):
            raise RuntimeError("Running app points to an unrecognized database")
        install_selection(docker, data, selected_url)
        return {"initialized": True, "already_initialized": True, "load_database": data["load_database"]}
    config = docker.config()
    configured = config["services"][args.app_service]["environment"]
    app = docker.container(args.app_service)
    current = runtime_env(app)
    if configured["DB_URL"] != current["DB_URL"]:
        raise RuntimeError("Compose and running app DB_URL differ; align them before init")
    dev_url = current["DB_URL"]
    host, port, dev_database, _ = jdbc_parts(dev_url)
    load_database = identifier(args.load_database)
    if dev_database.lower() == load_database.lower():
        raise ValueError("Load-test database must differ from the development database")
    mysql = docker.container(args.mysql_service)
    aliases = {args.mysql_service, mysql["Name"].lstrip("/")}
    for network in mysql["NetworkSettings"]["Networks"].values():
        aliases.update(network.get("Aliases") or [])
    if host not in aliases:
        raise RuntimeError("App DB_URL must point to the selected MySQL service in this project")
    load_url = replace_database(dev_url, load_database)
    compose_before = docker.compose_file.read_text()
    env_before = docker.env_file.read_text()
    compose_database_variable(compose_before, args.app_service, dev_url)
    selected_env(env_before, dev_url)
    with docker.database(dev_database) as db:
        verify_schema(db, dev_database)
        if db.query("SELECT @@port")[0][0] != port:
            raise RuntimeError("App DB_URL port differs from the MySQL server port")
        exists = int(db.query(f"SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='{load_database}'")[0][0])
        if exists:
            raise RuntimeError("Load-test database already exists; init never overwrites or deletes a DB")
        server_uuid = db.query("SELECT @@server_uuid")[0][0]
        dump_client = ('MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot '
                       '--no-data --skip-comments --skip-add-drop-table --skip-triggers '
                       '--no-tablespaces --set-gtid-purged=OFF --skip-lock-tables "$@"')
        schema = docker.command("exec", "-T", args.mysql_service, "sh", "-c", dump_client,
                                "sh", dev_database, *dummy_data.TABLES)
        schema = re.sub(r"\bAUTO_INCREMENT=\d+", "", schema)
        # DDL only targets the new database. No source data is dumped or imported.
        db.query(f"CREATE DATABASE `{load_database}` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci")
    with docker.database(load_database) as db:
        db.query(schema)
        verify_schema(db, load_database)
    data = {"project": args.project, "app_service": args.app_service, "mysql_service": args.mysql_service,
            "dev_database": dev_database, "load_database": load_database,
            "dev_url": dev_url, "load_url": load_url, "server_uuid": server_uuid}
    install_selection(docker, data, dev_url)
    return {"initialized": True, "dev_database": dev_database, "load_database": load_database}


def generate(docker, args):
    expected = dummy_data.plan(args)
    if args.dry_run:
        return {"dry_run": True, "rows": expected}
    data = docker.state()
    with docker.database(data["load_database"]) as db:
        verify_schema(db, data["load_database"])
        result = dummy_data.seed(db, args)
    return {"database": data["load_database"], "rows": result}


def reset_rows(db, database):
    verify_schema(db, database)
    incoming = db.query(
        "SELECT COUNT(*) FROM information_schema.key_column_usage "
        f"WHERE referenced_table_schema='{identifier(database)}' AND table_schema <> '{database}'"
    )
    if int(incoming[0][0]):
        raise RuntimeError("Another database references load-test tables; reset is blocked")
    db.query("START TRANSACTION")
    before = {table: int(db.query(f"SELECT COUNT(*) FROM `{table}`")[0][0]) for table in dummy_data.TABLES}
    # Child rows go first. Keep foreign-key checks enabled and do not run DDL.
    for table in reversed(dummy_data.TABLES):
        db.query(f"DELETE FROM `{table}`")
    for table in dummy_data.TABLES:
        if int(db.query(f"SELECT EXISTS(SELECT 1 FROM `{table}` LIMIT 1)")[0][0]):
            raise RuntimeError("Reset did not empty " + table)
    db.query("COMMIT")
    return before


def reset(docker, args):
    data = docker.state()
    # The target is read only from validated state, never from an arbitrary CLI DB name.
    with docker.database(data["load_database"]) as db:
        removed = reset_rows(db, data["load_database"])
    return {"database": data["load_database"], "removed_rows": removed, "empty": True}


def wait_healthy(docker, target_url, url, timeout):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        info = docker.container(docker.args.app_service)
        if runtime_env(info).get("DB_URL") != target_url:
            raise RuntimeError("Running app has the wrong DB_URL")
        if info["State"]["Running"]:
            try:
                with urllib.request.urlopen(url, timeout=min(3, max(0.1, deadline - time.monotonic()))) as response:
                    if response.status == 200:
                        return
            except (urllib.error.URLError, TimeoutError, OSError):
                pass
        time.sleep(min(1, max(0, deadline - time.monotonic())))
    raise RuntimeError("App health check timed out; inspect status and run 'use dev' to return to the development DB")


def use(docker, args):
    data = docker.state()
    if data.get("configuration_pending"):
        raise RuntimeError("DB selection installation is incomplete; rerun init")
    mode = args.mode
    target_url = data[mode + "_url"]
    if mode == "load":
        with docker.database(data["load_database"]) as db:
            verify_schema(db, data["load_database"])
            if not int(db.query("SELECT EXISTS(SELECT 1 FROM books)")[0][0]):
                raise RuntimeError("Generate dummy data before switching to load")
    info = docker.container(args.app_service, allow_missing=True)
    configured = docker.config(target_url)["services"][args.app_service]["environment"]
    if configured["DB_URL"] != target_url:
        raise RuntimeError("Compose no longer uses the persisted DB selection; rerun init before switching")
    if info:
        current = runtime_env(info)
        if current["DB_URL"] not in (data["dev_url"], data["load_url"]):
            raise RuntimeError("Running app points to an unrecognized database")
        for key, value in configured.items():
            if key != "DB_URL" and str(value) != current.get(key):
                raise RuntimeError(f"Compose environment differs from the running app for {key}; align it before switching")
        image = info["Image"]
    else:
        image = data.get("pending", {}).get("image", "")
    if not re.fullmatch(r"sha256:[0-9a-f]{64}", image):
        raise RuntimeError("Unable to determine the running image ID")
    data["pending"] = {"mode": mode, "image": image}
    docker.save(data)
    atomic_write(docker.env_file, selected_env(docker.env_file.read_text(), target_url))
    descriptor, override = tempfile.mkstemp(prefix="load-db.", suffix=".json", dir=docker.compose_file.parent)
    try:
        with os.fdopen(descriptor, "w") as output:
            json.dump({"services": {args.app_service: {"image": image, "pull_policy": "never",
                                                       "environment": {"DB_URL": target_url}}}}, output)
        # Recreate only the existing app. MySQL and its volume stay in place.
        docker.command("-f", override, "up", "-d", "--no-deps", "--pull", "never", "--force-recreate", args.app_service,
                       selected_url=target_url)
    finally:
        os.unlink(override)
    wait_healthy(docker, target_url, args.health_url, args.health_timeout)
    data.pop("pending", None)
    docker.save(data)
    return {"mode": mode, "database": data[mode + "_database"], "healthy": True, "image": image}


def status(docker, args):
    data = docker.state()
    info = docker.container(args.app_service, allow_missing=True)
    active_url = runtime_env(info)["DB_URL"] if info else None
    configured_url = docker.config()["services"][args.app_service]["environment"]["DB_URL"]
    modes = {data["dev_url"]: "dev", data["load_url"]: "load"}
    result = {"active_mode": modes.get(active_url, "unknown"),
              "configured_mode": modes.get(configured_url, "unknown"), "pending": data.get("pending"),
              "configuration_pending": data.get("configuration_pending", False)}
    if args.counts:
        with docker.database(data["load_database"]) as db:
            result["load_rows"] = {table: int(db.query(f"SELECT COUNT(*) FROM `{table}`")[0][0]) for table in dummy_data.TABLES}
    return result


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--compose-file", type=Path, default=Path("docker-compose.yml"))
    parser.add_argument("--project", default="yeobaek-dev")
    parser.add_argument("--app-service", default="app")
    parser.add_argument("--mysql-service", default="mysql")
    commands = parser.add_subparsers(dest="command", required=True)
    initialize = commands.add_parser("init", help="Create an empty load DB with schema only; install persistent DB selection")
    initialize.add_argument("--load-database", type=identifier, default="yeobaek_load_test")
    generate_parser = commands.add_parser("seed", help="Generate deterministic data in an empty load DB")
    for option in ("books", "chapters-per-book", "passages-per-chapter", "sentences-per-passage", "members", "clubs", "members-per-club"):
        generate_parser.add_argument("--" + option, type=positive_integer, required=True)
    generate_parser.add_argument("--sentence-text", default="부하테스트용 문장입니다.")
    generate_parser.add_argument("--comment-text", default="부하테스트용 댓글입니다.")
    generate_parser.add_argument("--batch-size", type=positive_integer, default=500)
    generate_parser.add_argument("--dry-run", action="store_true")
    commands.add_parser("reset", help="Physically delete all load DB rows; preserve schema")
    switch = commands.add_parser("use", help="Recreate the same app with another DB and wait for health")
    switch.add_argument("mode", choices=("dev", "load"))
    switch.add_argument("--health-url", default="http://127.0.0.1:8080/health")
    switch.add_argument("--health-timeout", type=positive_integer, default=120)
    inspect = commands.add_parser("status")
    inspect.add_argument("--counts", action="store_true", help="Also count load DB rows; avoid this during measurement")
    return parser.parse_args(argv)


def main(argv=None):
    args = parse_args(argv)
    if args.command == "seed" and args.dry_run:
        print(json.dumps({"dry_run": True, "rows": dummy_data.plan(args)}, ensure_ascii=False, indent=2))
        return 0
    docker = Docker(args)
    docker.directory.mkdir(mode=0o700, exist_ok=True)
    docker.directory.chmod(0o700)
    with (docker.directory / "lock").open("a") as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            raise RuntimeError("Another load-test setup/switch command is running")
        handlers = {"init": init, "seed": generate, "reset": reset, "use": use, "status": status}
        print(json.dumps(handlers[args.command](docker, args), ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, RuntimeError, KeyError, KeyboardInterrupt) as error:
        print(f"Stopped: {error}", file=sys.stderr)
        print("Development DB rows are not modified. Uncommitted data changes are cancelled. If COMMIT lost its connection, check counts before retrying. For a failed switch, check status and use dev to recover.", file=sys.stderr)
        sys.exit(1)
