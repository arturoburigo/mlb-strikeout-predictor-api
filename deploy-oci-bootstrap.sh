#!/usr/bin/env bash

set -euo pipefail

APP_DIR="${APP_DIR:-/opt/mlb-strikeout-predictor-api}"
ENV_FILE="${ENV_FILE:-${APP_DIR}/.env}"
COMPOSE_PLUGIN_DIR="/usr/local/lib/docker/cli-plugins"
COMPOSE_BIN="${COMPOSE_PLUGIN_DIR}/docker-compose"
COMPOSE_VERSION="${COMPOSE_VERSION:-v2.40.1}"

sudo dnf -y update
sudo dnf -y install docker git curl
sudo systemctl enable --now docker
sudo usermod -aG docker opc || true

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker installation failed." >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  sudo mkdir -p "${COMPOSE_PLUGIN_DIR}"
  sudo curl -fsSL "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-linux-aarch64" -o "${COMPOSE_BIN}"
  sudo chmod +x "${COMPOSE_BIN}"
fi

sudo mkdir -p "${APP_DIR}"
sudo chown -R opc:opc "${APP_DIR}"
cd "${APP_DIR}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing ${ENV_FILE}. Upload the application .env before running this script." >&2
  exit 1
fi

export COMPOSE_PARALLEL_LIMIT="${COMPOSE_PARALLEL_LIMIT:-1}"

docker compose --env-file "${ENV_FILE}" build --pull api
docker compose --env-file "${ENV_FILE}" up -d --no-build api nginx
docker compose --env-file "${ENV_FILE}" ps
