#!/bin/bash
set -e
cd ~/asklive_api

docker compose up -d --build
docker image prune -f