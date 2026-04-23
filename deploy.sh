#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ROOT_DIR}/.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing ${ENV_FILE}. Copy .env.example to .env and fill the production values."
  exit 1
fi

cd "${ROOT_DIR}"

docker compose --env-file "${ENV_FILE}" build api
docker compose --env-file "${ENV_FILE}" up -d
docker compose --env-file "${ENV_FILE}" ps
