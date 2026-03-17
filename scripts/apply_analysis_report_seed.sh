#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SEED_DIR="$ROOT_DIR/src/main/resources/db/seed"
ENV_FILE="$ROOT_DIR/.env"

read_env_value() {
  local key="$1"
  local file="$2"
  [ -f "$file" ] || return 0
  awk -F= -v target="$key" '
    $1 == target {
      sub(/^[^=]*=/, "", $0)
      print $0
      exit
    }
  ' "$file"
}

if [ -f "$ENV_FILE" ]; then
  export SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-$(read_env_value SPRING_DATASOURCE_URL "$ENV_FILE")}"
  export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-$(read_env_value SPRING_DATASOURCE_USERNAME "$ENV_FILE")}"
  export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-$(read_env_value SPRING_DATASOURCE_PASSWORD "$ENV_FILE")}"
fi

: "${SPRING_DATASOURCE_URL:?SPRING_DATASOURCE_URL is required}"
: "${SPRING_DATASOURCE_USERNAME:?SPRING_DATASOURCE_USERNAME is required}"
: "${SPRING_DATASOURCE_PASSWORD:?SPRING_DATASOURCE_PASSWORD is required}"

JAVA_HOME_21="${JAVA_HOME:-$(/usr/libexec/java_home -v 21)}"
JAVAC="$JAVA_HOME_21/bin/javac"
JAVA="$JAVA_HOME_21/bin/java"

POSTGRES_JAR="$(find "$HOME/.gradle/caches/modules-2/files-2.1/org.postgresql/postgresql" -name 'postgresql-*.jar' | grep -v sources | sort | tail -n 1)"

if [ ! -f "$POSTGRES_JAR" ]; then
  echo "[ERROR] Missing PostgreSQL JDBC jar: $POSTGRES_JAR" >&2
  exit 1
fi

CLASSPATH="$POSTGRES_JAR"
RUNNER_SRC="/tmp/ApplyAnalysisReportSeed.java"
RUNNER_CLASS="/tmp/ApplyAnalysisReportSeed.class"

cat > "$RUNNER_SRC" <<'EOF'
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ApplyAnalysisReportSeed {
    public static void main(String[] args) throws Exception {
        String url = System.getenv("SPRING_DATASOURCE_URL");
        String user = System.getenv("SPRING_DATASOURCE_USERNAME");
        String password = System.getenv("SPRING_DATASOURCE_PASSWORD");
        if (url == null || user == null || password == null) {
            throw new IllegalStateException("Datasource env vars are required");
        }
        if (args.length == 0) {
            throw new IllegalArgumentException("Seed file paths are required");
        }

        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            connection.setAutoCommit(false);
            try {
                for (String file : args) {
                    executeSqlFile(connection, file);
                    System.out.println("Applied: " + file);
                }

                syncSprintGoals(connection);
                syncTaskPriorities(connection);
                rebuildSeedDerivedTables(connection, args[args.length - 2], args[args.length - 1]);

                connection.commit();
                printSummary(connection);
            } catch (Throwable t) {
                connection.rollback();
                throw t;
            }
        }
    }

    private static void syncSprintGoals(Connection connection) throws SQLException {
        String sql = """
            update public.sprints
            set goal = left(split_part(name, ' - ', 2), 120),
                updated_at = now()
            where name like 'W% Sprint % - %'
              and goal is distinct from left(split_part(name, ' - ', 2), 120)
            """;
        try (Statement statement = connection.createStatement()) {
            int count = statement.executeUpdate(sql);
            System.out.println("Updated sprint goals: " + count);
        }
    }

    private static void syncTaskPriorities(Connection connection) throws SQLException {
        String sql = """
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
              and t.priority is distinct from computed.priority
            """;
        try (Statement statement = connection.createStatement()) {
            int count = statement.executeUpdate(sql);
            System.out.println("Updated task priorities: " + count);
        }
    }

    private static void rebuildSeedDerivedTables(
        Connection connection,
        String analysisMetricSeedFile,
        String reasonSeedFile
    ) throws SQLException, IOException {
        try (Statement statement = connection.createStatement()) {
            int deletedMetric = statement.executeUpdate("delete from public.analysis_metric_count");
            System.out.println("Deleted analysis_metric_count rows: " + deletedMetric);
            executeSqlText(statement, Files.readString(Path.of(analysisMetricSeedFile), StandardCharsets.UTF_8));

            int deletedReason = statement.executeUpdate(
                "delete from public.reasons where event_type in ('CANCEL', 'DELETE')"
            );
            System.out.println("Deleted reasons: " + deletedReason);
            executeSqlText(statement, Files.readString(Path.of(reasonSeedFile), StandardCharsets.UTF_8));
        }
    }

    private static void executeSqlFile(Connection connection, String file) throws SQLException, IOException {
        String sql = Files.readString(Path.of(file), StandardCharsets.UTF_8);
        try (Statement statement = connection.createStatement()) {
            executeSqlText(statement, sql);
        }
    }

    private static void executeSqlText(Statement statement, String sql) throws SQLException {
        for (String part : splitSqlStatements(sql)) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                statement.execute(trimmed);
            }
        }
    }

    private static List<String> splitSqlStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        String dollarQuoteTag = null;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';

            if (inLineComment) {
                current.append(c);
                if (c == '\n') {
                    inLineComment = false;
                }
                continue;
            }

            if (inBlockComment) {
                current.append(c);
                if (c == '*' && next == '/') {
                    current.append(next);
                    i++;
                    inBlockComment = false;
                }
                continue;
            }

            if (dollarQuoteTag != null) {
                current.append(c);
                if (c == '$') {
                    String remaining = sql.substring(i);
                    if (remaining.startsWith(dollarQuoteTag)) {
                        for (int j = 1; j < dollarQuoteTag.length(); j++) {
                            current.append(dollarQuoteTag.charAt(j));
                        }
                        i += dollarQuoteTag.length() - 1;
                        dollarQuoteTag = null;
                    }
                }
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote) {
                if (c == '-' && next == '-') {
                    current.append(c).append(next);
                    i++;
                    inLineComment = true;
                    continue;
                }

                if (c == '/' && next == '*') {
                    current.append(c).append(next);
                    i++;
                    inBlockComment = true;
                    continue;
                }

                if (c == '$') {
                    int end = i + 1;
                    while (end < sql.length()) {
                        char tagChar = sql.charAt(end);
                        if (tagChar == '$') {
                            String candidate = sql.substring(i, end + 1);
                            if (candidate.matches("\\$[A-Za-z0-9_]*\\$")) {
                                current.append(candidate);
                                i = end;
                                dollarQuoteTag = candidate;
                                break;
                            }
                            break;
                        }
                        if (!(Character.isLetterOrDigit(tagChar) || tagChar == '_')) {
                            break;
                        }
                        end++;
                    }
                    if (dollarQuoteTag != null) {
                        continue;
                    }
                }
            }

            if (c == '\'' && !inDoubleQuote) {
                current.append(c);
                if (inSingleQuote && next == '\'') {
                    current.append(next);
                    i++;
                } else {
                    inSingleQuote = !inSingleQuote;
                }
                continue;
            }

            if (c == '"' && !inSingleQuote) {
                current.append(c);
                if (inDoubleQuote && next == '"') {
                    current.append(next);
                    i++;
                } else {
                    inDoubleQuote = !inDoubleQuote;
                }
                continue;
            }

            if (c == ';' && !inSingleQuote && !inDoubleQuote) {
                statements.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(c);
        }

        if (!current.toString().trim().isEmpty()) {
            statements.add(current.toString());
        }

        return statements;
    }

    private static void printSummary(Connection connection) throws SQLException {
        List<String> checks = List.of(
            "select count(*) from public.sprints where goal is not null",
            "select count(*) from public.tasks where priority is null",
            "select count(*) from public.tasks where priority = 'HIGH'",
            "select count(*) from public.analysis_metric_count",
            "select count(*) from public.reasons"
        );
        for (String check : checks) {
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(check)) {
                resultSet.next();
                System.out.println(check + " => " + resultSet.getInt(1));
            }
        }
    }
}
EOF

"$JAVAC" -cp "$CLASSPATH" "$RUNNER_SRC"

SEED_FILES=()
while IFS= read -r file; do
  SEED_FILES+=("$file")
done < <(find "$SEED_DIR" -maxdepth 1 -type f -name '[0-9][0-9]-analysis-report-seed-*.sql' | sort)

if [ "${#SEED_FILES[@]}" -eq 0 ]; then
  echo "[ERROR] No ordered seed files found in $SEED_DIR" >&2
  exit 1
fi

echo "Applying seed files in order:"
printf ' - %s\n' "${SEED_FILES[@]}"

"$JAVA" -cp "/tmp:$CLASSPATH" ApplyAnalysisReportSeed "${SEED_FILES[@]}"

rm -f "$RUNNER_SRC" "$RUNNER_CLASS"
