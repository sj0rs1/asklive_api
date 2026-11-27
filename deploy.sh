#!/bin/bash
set -e
cd ~/asklive_api

docker compose up -d --build --no-deps api
docker image prune -f