#!/bin/bash
set -e

APP_DIR="/home/ubuntu/app"
REGION="ap-northeast-2"
SSM_PREFIX="/psycho/prod"

echo "========================================"
echo " 배포 시작: $(date)"
echo "========================================"

# ── 1. 환경변수 로드 (SSM Parameter Store) ──
echo "  SSM Parameter Store에서 환경변수 로드 중..."

get_param() {
  aws ssm get-parameter \
    --name "${SSM_PREFIX}/$1" \
    --with-decryption \
    --query Parameter.Value \
    --output text \
    --region $REGION
}

export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL=$(get_param db-url)
export SPRING_DATASOURCE_USERNAME=$(get_param db-username)
export SPRING_DATASOURCE_PASSWORD=$(get_param db-password)
export JWT_ISSUER=$(get_param jwt-issuer)
export JWT_ACCESS_TOKEN_VALIDITY_SECONDS=$(get_param jwt-access-token-validity)
export JWT_REFRESH_TOKEN_VALIDITY_SECONDS=$(get_param jwt-refresh-token-validity)
export JWT_REFRESH_COOKIE_NAME=$(get_param jwt-refresh-cookie-name)
export JWT_REFRESH_COOKIE_PATH=$(get_param jwt-refresh-cookie-path)
export JWT_REFRESH_COOKIE_SAME_SITE=$(get_param jwt-refresh-cookie-same-site)
export JWT_REFRESH_COOKIE_SECURE=$(get_param jwt-refresh-cookie-secure)
export JWT_SECRET=$(get_param jwt-secret)

echo "  환경변수 로드 완료"

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
sudo chown -R ubuntu:ubuntu ${APP_DIR}

nohup java -jar \
  -Dspring.profiles.active=prod \
  "${JAR_FILE}" \
  > "${APP_DIR}/app.log" 2>&1 &

NEW_PID=$!
echo "  - PID $NEW_PID 으로 실행 완료"

# ── 5. 헬스체크 ────────────────────────────
MAX_RETRY=18
COUNT=0
until curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; do
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
