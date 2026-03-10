#!/bin/bash
set -e

APP_DIR="/home/ubuntu/app"
ENV_FILE="${APP_DIR}/.env"
REGION="ap-northeast-2"
SSM_PREFIX="/psycho/prod"

echo "========================================"
echo " AfterInstall: $(date)"
echo "========================================"

# SSM Parameter Store에서 값 조회
get_param() {
  aws ssm get-parameter \
    --name "${SSM_PREFIX}/$1" \
    --with-decryption \
    --query Parameter.Value \
    --output text \
    --region ${REGION}
}

echo "Loading environment variables from SSM..."

# .env 파일 생성 (systemd EnvironmentFile 형식: KEY=VALUE)
cat > ${ENV_FILE} <<EOF
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=$(get_param db-url)
SPRING_DATASOURCE_USERNAME=$(get_param db-username)
SPRING_DATASOURCE_PASSWORD=$(get_param db-password)
JWT_ISSUER=$(get_param jwt-issuer)
JWT_ACCESS_TOKEN_VALIDITY_SECONDS=$(get_param jwt-access-token-validity)
JWT_REFRESH_TOKEN_VALIDITY_SECONDS=$(get_param jwt-refresh-token-validity)
JWT_REFRESH_COOKIE_NAME=$(get_param jwt-refresh-cookie-name)
JWT_REFRESH_COOKIE_PATH=$(get_param jwt-refresh-cookie-path)
JWT_REFRESH_COOKIE_SAME_SITE=$(get_param jwt-refresh-cookie-same-site)
JWT_REFRESH_COOKIE_SECURE=$(get_param jwt-refresh-cookie-secure)
JWT_SECRET=$(get_param jwt-secret)
MAIL_USERNAME=$(get_param mail-username)
MAIL_PASSWORD=$(get_param mail-password)
EOF

# 보안: .env 파일 권한 제한
chmod 600 ${ENV_FILE}
chown ubuntu:ubuntu ${ENV_FILE}

echo "Environment file created: ${ENV_FILE}"

# systemd 서비스 파일 배치
cp ${APP_DIR}/sos-app.service /etc/systemd/system/sos-app.service
chmod 644 /etc/systemd/system/sos-app.service
systemctl daemon-reload
systemctl enable sos-app

echo "systemd service file installed."
echo "AfterInstall completed."
