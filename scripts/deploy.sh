#!/bin/bash
set -e

APP_DIR="/home/ubuntu/app"
ENV_FILE="/home/ubuntu/env/.env"

echo "========================================"
echo " 배포 시작: $(date)"
echo "========================================"

# ── 1. 환경변수 로드 ───────────────────────
if [ ! -f "$ENV_FILE" ]; then
  echo "[ERROR] 환경변수 파일 없음: $ENV_FILE"
  exit 1
fi
source "$ENV_FILE"

# ── 2. JDK 확인 ────────────────────────────
java -version || { echo "[ERROR] Java 미설치"; exit 1; }

# ── 3. JAR 파일 탐색 ───────────────────────
JAR_FILE=$(ls ${APP_DIR}/*.jar 2>/dev/null | head -1)

if [ -z "$JAR_FILE" ]; then
  echo "[ERROR] JAR 파일 없음: $APP_DIR"
  exit 1
fi

echo "  - JAR: $JAR_FILE"

# ── 4. 애플리케이션 실행 ───────────────────
nohup java -jar \
  -Dspring.profiles.active=prod \
  "${JAR_FILE}" \
  > "${APP_DIR}/app.log" 2>&1 &

NEW_PID=$!
echo "  - PID $NEW_PID 으로 실행 완료"

# ── 5. 헬스 체크 ────────────────────────────
MAX_RETRY=18
COUNT=0
until curl -sf http://localhost:8080/health > /dev/null 2>&1; do
  COUNT=$((COUNT + 1))
  if [ $COUNT -ge $MAX_RETRY ]; then
    echo "[ERROR] 헬스체크 실패"
    tail -50 "${APP_DIR}/app.log"
    kill -9 $NEW_PID 2>/dev/null || true
    exit 1
  fi
  echo "  대기 중... ($COUNT/$MAX_RETRY)"
  sleep 10
done

echo "========================================"
echo " 배포 완료: $(date)"
echo "========================================"