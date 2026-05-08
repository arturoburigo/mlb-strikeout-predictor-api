#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ROOT_DIR}/.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing ${ENV_FILE}. Copy .env.example to .env and fill the production values."
  exit 1
fi

cd "${ROOT_DIR}"

export COMPOSE_PARALLEL_LIMIT="${COMPOSE_PARALLEL_LIMIT:-1}"

docker compose --env-file "${ENV_FILE}" build --pull api
docker compose --env-file "${ENV_FILE}" up -d --no-build --force-recreate api nginx
docker compose --env-file "${ENV_FILE}" ps
