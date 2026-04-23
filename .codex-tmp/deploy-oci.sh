#!/usr/bin/env bash
set -euo pipefail

APP_DIR=${APP_DIR:-/opt/mlb-strikeout-predictor-api}
ENV_FILE=${ENV_FILE:-$APP_DIR/.env}

sudo dnf -y update
sudo dnf -y install docker git
sudo systemctl enable --now docker
sudo usermod -aG docker opc || true

if ! command -v docker >/dev/null 2>&1; then
  echo 'docker install failed' >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  sudo mkdir -p /usr/local/lib/docker/cli-plugins
  sudo curl -SL https://github.com/docker/compose/releases/download/v2.40.1/docker-compose-linux-aarch64 -o /usr/local/lib/docker/cli-plugins/docker-compose
  sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
fi

sudo mkdir -p "$APP_DIR"
sudo chown -R opc:opc "$APP_DIR"
cd "$APP_DIR"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE" >&2
  exit 1
fi

docker compose --env-file "$ENV_FILE" build api
docker compose --env-file "$ENV_FILE" up -d
docker compose --env-file "$ENV_FILE" ps
