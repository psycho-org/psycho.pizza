#!/bin/bash
set -e

SERVICE_NAME="sos-app"

if systemctl is-active --quiet ${SERVICE_NAME}; then
  echo "Stopping ${SERVICE_NAME}..."
  systemctl stop ${SERVICE_NAME}
  echo "Application stopped."
else
  echo "Application is not running. Skipping stop."
fi
