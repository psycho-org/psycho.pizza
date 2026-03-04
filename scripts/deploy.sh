#!/bin/bash
set -e

APP_DIR="/home/ec2-user/app"
SERVICE_NAME="sos"
JAR_NAME="${SERVICE_NAME}.jar"
ENV_FILE="/home/ec2-user/env/.env"

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

# ── 3. 빌드 ────────────────────────────────
echo "[1/3] Gradle 빌드..."
chmod +x ./gradlew
SPRING_PROFILES_ACTIVE=prod ./gradlew clean bootJar

# ── 4. 기존 프로세스 종료 ──────────────────
echo "[2/3] 기존 프로세스 종료..."
EXISTING_PID=$(pgrep -f "${JAR_NAME}" || true)
if [ -n "$EXISTING_PID" ]; then
  kill -15 "$EXISTING_PID"
  sleep 3
  if kill -0 "$EXISTING_PID" 2>/dev/null; then
    kill -9 "$EXISTING_PID"
  fi
  echo "  - PID $EXISTING_PID 종료 완료"
fi

# ── 5. JAR 복사 및 실행 ────────────────────
echo "[3/3] 애플리케이션 실행..."
mkdir -p "$APP_DIR"
cp build/libs/*.jar "${APP_DIR}/${JAR_NAME}"

nohup java -jar \
  -Dspring.profiles.active=prod \
  "${APP_DIR}/${JAR_NAME}" \
  > "${APP_DIR}/app.log" 2>&1 &

NEW_PID=$!
echo "  - PID $NEW_PID 으로 실행 완료"

# ── 6. 헬스체크 ────────────────────────────
MAX_RETRY=18
COUNT=0
until curl -sf http://localhost:8080/health > /dev/null 2>&1; do
  COUNT=$((COUNT + 1))
  if [ $COUNT -ge $MAX_RETRY ]; then
    echo "[ERROR] 헬스체크 실패"
    tail -50 "${APP_DIR}/app.log"
    kill -9 $NEW_PID 2>/dev/null || true  # ← 추가
    exit 1
  fi
  echo "  대기 중... ($COUNT/$MAX_RETRY)"
  sleep 10
done

echo "========================================"
echo " 배포 완료: $(date)"
echo "========================================"
