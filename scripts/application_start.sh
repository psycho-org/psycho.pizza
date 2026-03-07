#!/bin/bash
set -e

APP_DIR="/home/ubuntu/app"
SERVICE_NAME="sos-app"

echo "========================================"
echo " ApplicationStart: $(date)"
echo "========================================"

# JAR 파일 탐색 및 심볼릭 링크 생성
JAR_FILE=$(ls ${APP_DIR}/*.jar 2>/dev/null | head -1)

if [ -z "$JAR_FILE" ]; then
  echo "[ERROR] JAR file not found in ${APP_DIR}"
  exit 1
fi

# systemd unit에서 고정 경로로 참조할 수 있도록 심볼릭 링크
ln -sf "${JAR_FILE}" "${APP_DIR}/app.jar"
echo "Symlink created: ${APP_DIR}/app.jar -> ${JAR_FILE}"

# systemd 서비스 시작
systemctl daemon-reload
systemctl start ${SERVICE_NAME}

echo "ApplicationStart completed."
