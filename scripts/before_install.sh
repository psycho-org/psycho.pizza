#!/bin/bash
set -e

APP_DIR="/home/ubuntu/app"

echo "========================================"
echo " BeforeInstall: $(date)"
echo "========================================"

# 이전 배포 잔여 파일 정리 (.env, 로그는 유지)
echo "Cleaning previous deployment artifacts..."
find ${APP_DIR} -name "*.jar" -delete 2>/dev/null || true

# 디렉토리 생성 및 권한 설정
mkdir -p ${APP_DIR}
chown -R ubuntu:ubuntu ${APP_DIR}

echo "BeforeInstall completed."
