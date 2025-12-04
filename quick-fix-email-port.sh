#!/bin/bash

# Quick fix: Switch email to port 465 (SSL) to match prod.env
# This fixes the SSL error immediately without rebuilding

ENV_FILE="${1:-.env}"

if [ ! -f "$ENV_FILE" ]; then
    echo "Error: .env file not found: $ENV_FILE"
    exit 1
fi

echo "Updating $ENV_FILE to use port 465 (SSL)..."

# Backup
cp "$ENV_FILE" "${ENV_FILE}.backup.$(date +%Y%m%d_%H%M%S)"

# Update to port 465 configuration
sed -i 's/^MAIL_PORT=.*/MAIL_PORT=465/' "$ENV_FILE"
sed -i 's/^MAIL_SMTP_SSL_ENABLE=.*/MAIL_SMTP_SSL_ENABLE=true/' "$ENV_FILE"
sed -i 's/^MAIL_SMTP_STARTTLS_ENABLE=.*/MAIL_SMTP_STARTTLS_ENABLE=false/' "$ENV_FILE"
sed -i 's/^MAIL_SMTP_STARTTLS_REQUIRED=.*/MAIL_SMTP_STARTTLS_REQUIRED=false/' "$ENV_FILE"

# Ensure socket factory port is set
if ! grep -q "^MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=" "$ENV_FILE"; then
    echo "MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=465" >> "$ENV_FILE"
else
    sed -i 's/^MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=.*/MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=465/' "$ENV_FILE"
fi

echo "✓ Configuration updated to port 465"
echo ""
echo "Restart backend: docker compose restart backend"

