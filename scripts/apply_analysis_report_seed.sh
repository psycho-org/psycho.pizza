#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

if command -v python3.9 >/dev/null 2>&1; then
  PYTHON_BIN="python3.9"
elif command -v python3 >/dev/null 2>&1; then
  PYTHON_BIN="python3"
else
  echo "[ERROR] python3.9 or python3 is required" >&2
  exit 1
fi

if ! "$PYTHON_BIN" - <<'PY' >/dev/null 2>&1
import importlib.util
import sys
sys.exit(0 if importlib.util.find_spec("psycopg") or importlib.util.find_spec("psycopg2") else 1)
PY
then
  echo "Installing PostgreSQL Python driver with $PYTHON_BIN..."
  "$PYTHON_BIN" -m pip install "psycopg[binary]"
fi

exec "$PYTHON_BIN" "$ROOT_DIR/scripts/apply_analysis_report_seed.py" "$@"
