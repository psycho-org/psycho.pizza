#!/bin/bash

IMAGE="dnya0/psycho:sos-latest"

echo "Using image: $IMAGE"

docker rmi $IMAGE || true
docker pull $IMAGE

docker stop sos || true
docker rm sos || true

docker run -d \
  --name sos \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -v /home/ec2-user/logs:/app/logs \
  -v /home/ec2-user/config:/app/config \
  $IMAGE
