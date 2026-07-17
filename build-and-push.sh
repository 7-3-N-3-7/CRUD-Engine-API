#!/bin/bash

# Exit on any error
set -e

# NOTE: Replace <YOUR_DOCKERHUB_USERNAME> with your actual Docker Hub username!
DOCKERHUB_USERNAME="73n37"
IMAGE_NAME="$DOCKERHUB_USERNAME/crud-app-sample:latest"

echo "☕ Building the Java Backend..."
# Build the application using the local Maven wrapper or system Maven
mvn clean package -pl crud-app-sample -am -DskipTests -B

echo "🐳 Building Docker Image..."
# Build the Docker image from the root Dockerfile
docker build -t "$IMAGE_NAME" .

echo "☁️ Pushing Image to Docker Hub..."
# Note: Ensure you have run 'docker login' before running this script
docker push "$IMAGE_NAME"

echo "✅ Image pushed successfully to $IMAGE_NAME"

# This is meant for development and not deployment
docker-compose up crud-api
