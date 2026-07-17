#!/bin/bash

# ============================================================
#  CRUD Engine — Build, Push & Run Script
#  Builds the Java backend, pushes the image to Docker Hub,
#  then starts all project services in the correct order.
# ============================================================

set -e

DOCKERHUB_USERNAME="73n37"
IMAGE_NAME="$DOCKERHUB_USERNAME/crud-app-sample:latest"

# ── 1. Build Java Backend ────────────────────────────────────
echo ""
echo "☕  Building the Java backend..."
mvn clean package -pl crud-app-sample -am -DskipTests -B

# ── 2. Build Docker Image ────────────────────────────────────
echo ""
echo "🐳  Building Docker image: $IMAGE_NAME"
docker build -t "$IMAGE_NAME" .

# ── 3. Push to Docker Hub ────────────────────────────────────
echo ""
echo "☁️   Pushing image to Docker Hub..."
echo "     (make sure you have run 'docker login' first)"
docker push "$IMAGE_NAME"
echo "✅  Image pushed successfully → $IMAGE_NAME"

# ── 4. Start all services ────────────────────────────────────
# Services are started in dependency order so each one is
# healthy before the next group comes up.

echo ""
echo "🚀  Starting infrastructure..."

# Databases & object storage first
docker-compose up -d postgres mongodb minio

echo "⏳  Waiting for databases and MinIO to be ready (15 s)..."
sleep 15

# MinIO bucket provisioning (runs once then exits)
echo "🪣  Provisioning MinIO buckets..."
docker-compose up minio-setup

# Auth layer
echo "🔐  Starting Keycloak..."
docker-compose up -d keycloak

echo "⏳  Waiting for Keycloak to import realm (20 s)..."
sleep 20

# Application layer
echo "🌐  Starting crud-api..."
docker-compose up -d crud-api

# Reverse proxy & CD
echo "🔁  Starting Caddy proxy and Watchtower CD..."
docker-compose up -d caddy watchtower

# ── 5. Status summary ────────────────────────────────────────
echo ""
echo "============================================================"
echo "  ✅  All services started!"
echo "============================================================"
docker-compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "  📡  API        → http://localhost:8080"
echo "  🔐  Keycloak   → http://localhost:8081/auth"
echo "  🪣  MinIO UI   → http://localhost:9001"
echo "  🌍  Caddy      → http://localhost:80 / https://localhost:443"
echo ""
echo "  Logs: docker-compose logs -f crud-api"
echo "============================================================"
