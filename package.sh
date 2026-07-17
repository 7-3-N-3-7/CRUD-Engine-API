#!/bin/bash

# Exit on any error
set -e

PACKAGE_NAME="crudapp-portable-infrastructure.tar.gz"
STAGING_DIR="crudapp-staging"

echo "📦 Packaging Docker infrastructure for portability..."

# Create staging directory
mkdir -p "$STAGING_DIR"

# Copy essential docker-compose files and configurations
echo "Copying docker-compose.yml..."
cp docker-compose.yml "$STAGING_DIR/"

if [ -f "Caddyfile" ]; then
    echo "Copying Caddyfile..."
    cp Caddyfile "$STAGING_DIR/"
fi

# Copy initialization data directories
echo "Copying data initialization scripts (Postgres, Mongo, Keycloak, MinIO)..."
cp -r data/ "$STAGING_DIR/data/"

# Create a convenient start script for the destination server
cat << 'EOF' > "$STAGING_DIR/start.sh"
#!/bin/bash
echo "🚀 Starting CRUD App Infrastructure..."
docker-compose up
echo "✅ Infrastructure is up and running!"
EOF

chmod +x "$STAGING_DIR/start.sh"

# Compress the staging directory
echo "🗜️ Compressing to $PACKAGE_NAME..."
tar -czf "$PACKAGE_NAME" -C "$STAGING_DIR" .

# Clean up staging directory
rm -rf "$STAGING_DIR"

echo "🎉 Done! You can now securely copy '$PACKAGE_NAME' to any VPS or server."
echo "   Usage on destination: tar -xzf $PACKAGE_NAME -C myfolder && cd myfolder && ./start.sh"
