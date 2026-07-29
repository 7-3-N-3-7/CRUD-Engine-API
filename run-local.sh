#!/bin/bash

# This script prepares the local development environment for the project
# by starting the necessary infrastructure services using Docker Compose.
#
# It is based on the instructions provided in the project's README.md file.
#
# Prerequisites:
# - Docker & Docker Compose
# - Java 25 JDK
# - Maven 3.8+
# - Node.js & npm

# Exit immediately if a command exits with a non-zero status.
set -e

echo "INFO: Initializing and updating Git submodules..."
git submodule update --init --recursive
echo "SUCCESS: Submodules are up to date."

echo "INFO: Starting infrastructure services (PostgreSQL, Keycloak, MongoDB) with Docker Compose..."
# We only start the databases and authentication services, not the full application stack,
# to allow for local development of the backend and frontend.
docker-compose up -d postgres keycloak mongodb
echo "SUCCESS: Infrastructure services are running in the background."
echo ""
echo "------------------------------------------------------------------"
echo "Your local development environment is ready."
echo ""
echo "Next steps:"
echo ""
echo "1. Run the Backend Server:"
echo "   In a new terminal, execute the following command:"
echo "   mvn spring-boot:run -pl crud-app-sample"
echo ""
echo "2. Run the Frontend Application:"
echo "   In another new terminal, execute these commands:"
echo "   cd crud-frontend"
echo "   npm install"
echo "   npm run dev"
echo ""
echo "For more details, please refer to the README.md file."
echo "------------------------------------------------------------------"

