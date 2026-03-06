#!/bin/bash
set -e

echo "========================================"
echo " ValidateService: $(date)"
echo "========================================"

MAX_RETRY=18
COUNT=0

until curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; do
  COUNT=$((COUNT + 1))
  if [ $COUNT -ge $MAX_RETRY ]; then
    echo "[ERROR] Health check failed after ${MAX_RETRY} attempts"
    echo "--- Recent application logs ---"
    journalctl -u sos-app --no-pager -n 50
    exit 1
  fi
  echo "Waiting for application... (${COUNT}/${MAX_RETRY})"
  sleep 10
done

echo "Health check passed."
echo "========================================"
echo " Deployment validated: $(date)"
echo "========================================"
