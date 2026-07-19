#!/bin/bash

# =============================================================================
#  CRUD Engine — Build, Push & Run
#  Single command to build everything, push to Docker Hub, and start all
#  services in the correct order with health checks.
#
#  Prerequisites (one-time setup):
#    1. Copy .env.example → .env and fill in your values
#    2. Run: docker login
#
#  Usage:
#    bash build-and-push.sh
# =============================================================================

set -euo pipefail

# ── Colours ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; RESET='\033[0m'

ok()   { echo -e "${GREEN}✅  $*${RESET}"; }
info() { echo -e "${CYAN}ℹ️   $*${RESET}"; }
warn() { echo -e "${YELLOW}⚠️   $*${RESET}"; }
fail() { echo -e "${RED}❌  $*${RESET}"; exit 1; }

# ── 0. Preflight checks ───────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}============================================================${RESET}"
echo -e "${BOLD}  🚀  CRUD Engine — Build & Deploy${RESET}"
echo -e "${BOLD}============================================================${RESET}"
echo ""
info "Running preflight checks..."

# .env file
[ -f ".env" ] || fail ".env not found. Copy .env.example → .env and fill in your values."

# Load env vars
set -a
# shellcheck disable=SC1091
source .env
set +a

# Required variables
[ -n "${DOCKER_USERNAME:-}" ]    || fail "DOCKER_USERNAME is not set in .env"
[ -n "${POSTGRES_PASSWORD:-}" ]  || fail "POSTGRES_PASSWORD is not set in .env"
[ -n "${MINIO_ROOT_PASSWORD:-}" ] || fail "MINIO_ROOT_PASSWORD is not set in .env"

IMAGE_NAME="${DOCKER_USERNAME}/crud-app-sample:latest"

# Docker daemon running?
docker info > /dev/null 2>&1 || fail "Docker is not running. Start Docker Desktop / Docker daemon first."

# Docker login check — Docker Desktop on Windows stores creds in Windows
# Credential Manager, so `docker info` never shows a "Username" field.
# Instead check the config file for a credStore or stored auth entry.
DOCKER_CONFIG="${DOCKER_CONFIG:-$HOME/.docker}"
if [ -f "${DOCKER_CONFIG}/config.json" ]; then
  if ! grep -qE '"credsStore"|"credStore"|"auths"' "${DOCKER_CONFIG}/config.json" 2>/dev/null; then
    warn "Could not verify Docker Hub login. If the push fails, run 'docker login' first."
  fi
fi

ok "Preflight passed  (image: ${IMAGE_NAME})"

# ── Helper: wait_for_http ─────────────────────────────────────────────────────
# Polls an HTTP endpoint until it returns a non-error response.
# Usage: wait_for_http <label> <url> <max_seconds>
wait_for_http() {
  local label="$1" url="$2" max="$3" elapsed=0
  info "Waiting for ${label} to be ready..."
  until curl -sf --max-time 2 "$url" > /dev/null 2>&1; do
    sleep 2; elapsed=$((elapsed + 2))
    if [ $elapsed -ge "$max" ]; then
      fail "${label} did not respond after ${max}s. Check: docker compose logs"
    fi
    printf "."
  done
  echo ""
  ok "${label} is ready"
}

# ── Helper: wait_for_postgres ─────────────────────────────────────────────────
# Uses pg_isready inside the container — works regardless of network setup.
# Usage: wait_for_postgres <max_seconds>
wait_for_postgres() {
  local max="$1" elapsed=0
  info "Waiting for PostgreSQL to be ready..."
  until docker compose exec -T postgres \
        pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" > /dev/null 2>&1; do
    sleep 2; elapsed=$((elapsed + 2))
    if [ $elapsed -ge "$max" ]; then
      fail "PostgreSQL did not respond after ${max}s. Check: docker compose logs postgres"
    fi
    printf "."
  done
  echo ""
  ok "PostgreSQL is ready"
}

# ── 1. Build Java backend ─────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}☕  Step 1/5 — Building Java backend...${RESET}"
mvn clean package -pl crud-app-sample -am -DskipTests -B -q
ok "Java build complete"

# ── 2. Build & push API Docker image ─────────────────────────────────────────
echo ""
echo -e "${BOLD}🐳  Step 2/5 — Building Docker image: ${IMAGE_NAME}${RESET}"
docker build -t "${IMAGE_NAME}" .
ok "Docker image built"

echo ""
echo -e "${BOLD}☁️   Pushing to Docker Hub...${RESET}"
docker push "${IMAGE_NAME}"
ok "Pushed → ${IMAGE_NAME}"

# ── 3. Tear down any previous run cleanly ────────────────────────────────────
echo ""
echo -e "${BOLD}🧹  Step 3/5 — Stopping any running services...${RESET}"
docker compose down --remove-orphans 2>/dev/null || true
ok "Clean slate"

# ── 4. Start services in dependency order ─────────────────────────────────────
echo ""
echo -e "${BOLD}🏗️   Step 4/5 — Starting services...${RESET}"

# 4a. Databases & object storage
info "Starting PostgreSQL, MongoDB, MinIO..."
docker compose up -d postgres mongodb minio < /dev/null

# PostgreSQL: use pg_isready inside the container (TCP, not HTTP)
wait_for_postgres 60

# Extra pause for initdb scripts (create keycloak DB, seed products)
info "Letting PostgreSQL run init scripts..."
sleep 8

# MinIO: has a proper HTTP health endpoint
wait_for_http "MinIO" "http://localhost:9000/minio/health/live" 60

# 4b. MinIO bucket setup
info "Provisioning MinIO buckets..."
docker compose up -d minio-setup < /dev/null
ok "MinIO buckets ready"

# 4c. Keycloak (imports realm on first start — needs extra time)
info "Starting Keycloak..."
docker compose up -d keycloak < /dev/null
wait_for_http "Keycloak" "http://localhost:8081/auth/realms/master" 120

# 4d. Spring Boot API
info "Starting crud-api..."
docker compose up -d crud-api < /dev/null
wait_for_http "crud-api" "http://localhost:8080/actuator/health" 90

# 4e. Next.js frontend (builds image if not already built)
info "Building & starting Next.js frontend..."
docker compose up -d --build crud-frontend < /dev/null
wait_for_http "Frontend" "http://localhost:3000" 180

# 4f. Reverse proxy, Swagger UI, & Watchtower
info "Starting Caddy proxy, Swagger UI, and Watchtower..."
docker compose up -d caddy swagger-ui watchtower < /dev/null
ok "Caddy, Swagger UI, & Watchtower started"

# ── 5. Status summary ─────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}============================================================${RESET}"
echo -e "${GREEN}${BOLD}  ✅  All services are up and healthy!${RESET}"
echo -e "${BOLD}============================================================${RESET}"
echo ""
docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo -e "  🖥️   Frontend   → ${CYAN}http://localhost:3000${RESET}"
echo -e "  📡  API        → ${CYAN}http://localhost:8080${RESET}"
echo -e "  📖  Swagger UI → ${CYAN}http://localhost:${SWAGGER_UI_PORT:-8082}${RESET}"
echo -e "  🔐  Keycloak   → ${CYAN}http://localhost:8081/auth${RESET}  (admin: ${KEYCLOAK_ADMIN})"
echo -e "  🪣  MinIO UI   → ${CYAN}http://localhost:9001${RESET}"
echo -e "  🌍  Caddy      → ${CYAN}http://localhost:80${RESET}"
echo ""
echo -e "  📋  Logs:  docker compose logs -f crud-api"
echo -e "  🛑  Stop:  docker compose down"
echo -e "${BOLD}============================================================${RESET}"
