#!/bin/bash

PID=$(pgrep -f 'sos.*\.jar')

if [ -z "$PID" ]; then
  echo "No running application found."
  exit 0
fi

kill $PID

TIMEOUT=30
ELAPSED=0

while kill -0 $PID 2>/dev/null; do
  sleep 1
  ELAPSED=$((ELAPSED + 1))
  if [ $ELAPSED -ge $TIMEOUT ]; then
    echo "Graceful shutdown timed out. Force killing."
    kill -9 $PID
    break
  fi
done

echo "Application stopped."