#!/usr/bin/env python3
from __future__ import annotations

import os
import re
import sys
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parent.parent
SEED_DIR = ROOT_DIR / "src" / "main" / "resources" / "db" / "seed"
ENV_FILE = ROOT_DIR / ".env"


def read_env_value(key: str, file_path: Path) -> str | None:
    if not file_path.exists():
        return None
    for line in file_path.read_text(encoding="utf-8").splitlines():
        if line.startswith(f"{key}="):
            return line.split("=", 1)[1]
    return None


def load_env() -> tuple[str, str, str]:
    url = os.environ.get("SPRING_DATASOURCE_URL") or read_env_value("SPRING_DATASOURCE_URL", ENV_FILE)
    username = os.environ.get("SPRING_DATASOURCE_USERNAME") or read_env_value("SPRING_DATASOURCE_USERNAME", ENV_FILE)
    password = os.environ.get("SPRING_DATASOURCE_PASSWORD") or read_env_value("SPRING_DATASOURCE_PASSWORD", ENV_FILE)

    if not url or not username or password is None:
        raise SystemExit(
            "SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD are required."
        )

    os.environ["SPRING_DATASOURCE_URL"] = url
    os.environ["SPRING_DATASOURCE_USERNAME"] = username
    os.environ["SPRING_DATASOURCE_PASSWORD"] = password
    return url, username, password


def ordered_seed_files() -> list[Path]:
    return sorted(SEED_DIR.glob("[0-9][0-9]-analysis-report-seed-*.sql"))


def sync_sprint_goals_sql() -> str:
    return """
update public.sprints
set goal = left(split_part(name, ' - ', 2), 120),
    updated_at = now()
where name like 'W% Sprint % - %'
  and goal is distinct from left(split_part(name, ' - ', 2), 120);
""".strip()


def sync_task_priorities_sql() -> str:
    return """
with task_context as (
    select
        t.id,
        t.workspace_id,
        t.status,
        t.assignee_id,
        coalesce(nullif(substring(t.title from 'P([0-9]{2})'), ''), '01')::int as project_order,
        case
            when t.title like '% - 현황 및 자료 수집 - %' then 1
            when t.title like '% - 초안 정리 - %' then 2
            when t.title like '% - 검토 및 합의 - %' then 3
            when t.title like '% - 실행 준비 - %' then 4
            when t.title like '% - 운영 반영 - %' then 5
            when t.title like '% - 결과 점검 - %' then 6
            when t.title like '% - 후속 보완 - %' then 7
            when t.title like '% - 마감 정리 - %' then 8
            else null
        end as task_order
    from public.tasks t
    where t.title like 'W%'
),
computed as (
    select
        tc.id,
        case
            when tc.workspace_id in (
                '10000000-0000-0000-0000-000000000005'::uuid,
                '10000000-0000-0000-0000-000000000006'::uuid,
                '10000000-0000-0000-0000-000000000007'::uuid,
                '10000000-0000-0000-0000-000000000008'::uuid,
                '10000000-0000-0000-0000-000000000009'::uuid,
                '10000000-0000-0000-0000-000000000010'::uuid,
                '10000000-0000-0000-0000-000000000011'::uuid,
                '10000000-0000-0000-0000-000000000012'::uuid
            )
            and tc.status = 'TODO'
            and (
                tc.assignee_id is null
                or tc.task_order >= 5
                or mod(tc.project_order + tc.task_order, 4) = 0
            ) then null
            when tc.status = 'IN_PROGRESS' then 'HIGH'
            when tc.status = 'TODO' and tc.task_order <= 2 then 'HIGH'
            when tc.status = 'TODO' then 'MEDIUM'
            when tc.status = 'DONE' and tc.task_order <= 2 then 'MEDIUM'
            when tc.status = 'CANCELLED' then 'LOW'
            else 'LOW'
        end as priority
    from task_context tc
)
update public.tasks t
set priority = computed.priority,
    updated_at = now()
from computed
where t.id = computed.id
  and t.priority is distinct from computed.priority;
""".strip()


def summary_queries() -> list[str]:
    return [
        "select count(*) from public.sprints where goal is not null;",
        "select count(*) from public.tasks where priority is null;",
        "select count(*) from public.tasks where priority = 'HIGH';",
        "select count(*) from public.analysis_metric_count;",
        "select count(*) from public.reasons;",
    ]


def connect_python(url: str, username: str, password: str):
    try:
        import psycopg  # type: ignore

        return psycopg.connect(url.replace("jdbc:", ""), user=username, password=password), "psycopg"
    except Exception:
        pass

    try:
        import psycopg2  # type: ignore

        return psycopg2.connect(url.replace("jdbc:", ""), user=username, password=password), "psycopg2"
    except Exception:
        pass

    raise SystemExit(
        "Python PostgreSQL driver not found even after bootstrap.\n"
        "Try one of these manually and rerun:\n"
        "  python3.9 -m pip install 'psycopg[binary]'\n"
        "  python3.9 -m pip install psycopg2-binary"
    )


def split_sql_statements(sql: str) -> list[str]:
    statements: list[str] = []
    current: list[str] = []
    in_single = False
    in_double = False
    in_line_comment = False
    in_block_comment = False
    dollar_tag: str | None = None
    i = 0

    while i < len(sql):
        c = sql[i]
        nxt = sql[i + 1] if i + 1 < len(sql) else "\0"

        if in_line_comment:
            current.append(c)
            if c == "\n":
                in_line_comment = False
            i += 1
            continue

        if in_block_comment:
            current.append(c)
            if c == "*" and nxt == "/":
                current.append(nxt)
                i += 2
                in_block_comment = False
                continue
            i += 1
            continue

        if dollar_tag is not None:
            current.append(c)
            if c == "$" and sql[i:].startswith(dollar_tag):
                current.extend(list(dollar_tag[1:]))
                i += len(dollar_tag)
                dollar_tag = None
                continue
            i += 1
            continue

        if not in_single and not in_double:
            if c == "-" and nxt == "-":
                current.extend([c, nxt])
                i += 2
                in_line_comment = True
                continue
            if c == "/" and nxt == "*":
                current.extend([c, nxt])
                i += 2
                in_block_comment = True
                continue
            if c == "$":
                end = i + 1
                while end < len(sql):
                    tag_char = sql[end]
                    if tag_char == "$":
                        candidate = sql[i : end + 1]
                        if re.fullmatch(r"\$[A-Za-z0-9_]*\$", candidate):
                            current.extend(list(candidate))
                            i = end + 1
                            dollar_tag = candidate
                            break
                        break
                    if not (tag_char.isalnum() or tag_char == "_"):
                        break
                    end += 1
                if dollar_tag is not None:
                    continue

        if c == "'" and not in_double:
            current.append(c)
            if in_single and nxt == "'":
                current.append(nxt)
                i += 2
                continue
            in_single = not in_single
            i += 1
            continue

        if c == '"' and not in_single:
            current.append(c)
            if in_double and nxt == '"':
                current.append(nxt)
                i += 2
                continue
            in_double = not in_double
            i += 1
            continue

        if c == ";" and not in_single and not in_double:
            statement = "".join(current).strip()
            if statement:
                statements.append(statement)
            current = []
            i += 1
            continue

        current.append(c)
        i += 1

    tail = "".join(current).strip()
    if tail:
        statements.append(tail)
    return statements


def execute_sql_text(cursor, sql: str) -> None:
    for statement in split_sql_statements(sql):
        cursor.execute(statement)


def run_python(url: str, username: str, password: str, files: Iterable[Path]) -> None:
    conn, driver_name = connect_python(url, username, password)
    print(f"Applying seed files with {driver_name}:")
    try:
        with conn:
            with conn.cursor() as cur:
                for file in files:
                    print(f" - {file}")
                    execute_sql_text(cur, file.read_text(encoding="utf-8"))

                execute_sql_text(cur, sync_sprint_goals_sql())
                execute_sql_text(cur, sync_task_priorities_sql())

                cur.execute("delete from public.analysis_metric_count")
                execute_sql_text(cur, (SEED_DIR / "19-analysis-report-seed-analysis-metric-count.sql").read_text(encoding="utf-8"))

                cur.execute("delete from public.reasons where event_type in ('CANCEL', 'DELETE')")
                execute_sql_text(cur, (SEED_DIR / "20-analysis-report-seed-reasons.sql").read_text(encoding="utf-8"))

                for query in summary_queries():
                    cur.execute(query)
                    row = cur.fetchone()
                    print(f"{query} => {row[0]}")
    finally:
        conn.close()


def main() -> None:
    url, username, password = load_env()
    files = ordered_seed_files()
    if not files:
        raise SystemExit(f"No ordered seed files found in {SEED_DIR}")

    run_python(url, username, password, files)


if __name__ == "__main__":
    main()
