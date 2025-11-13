#!/bin/bash
# backup.sh - Automated Backup Script for Employee Management System

set -e

# Configuration
BACKUP_DIR="${BACKUP_DIR:-/backups/ems}"
RETENTION_DAYS=${RETENTION_DAYS:-7}
DATE=$(date +%Y%m%d_%H%M%S)

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "📦 Employee Management System - Backup Script"
echo "=============================================="
echo "Backup Directory: $BACKUP_DIR"
echo "Retention Period: $RETENTION_DAYS days"
echo "Timestamp: $DATE"
echo ""

# Create backup directory
mkdir -p "$BACKUP_DIR"

# 1. Backup Database
echo "📊 Backing up database..."
docker-compose exec -T mysql mysqldump \
    -u root \
    -p"${DB_ROOT_PASSWORD}" \
    --single-transaction \
    --routines \
    --triggers \
    --events \
    --add-drop-database \
    --databases employee_management_system 2>/dev/null | gzip > "$BACKUP_DIR/db_backup_$DATE.sql.gz"

if [ $? -eq 0 ]; then
    DB_SIZE=$(du -h "$BACKUP_DIR/db_backup_$DATE.sql.gz" | cut -f1)
    echo -e "${GREEN}✅ Database backed up successfully ($DB_SIZE)${NC}"
else
    echo "❌ Database backup failed!"
    exit 1
fi

# 2. Backup Uploads Directory
echo "📁 Backing up uploads..."
docker cp ems-backend:/app/uploads "$BACKUP_DIR/uploads_$DATE" 2>/dev/null

if [ $? -eq 0 ]; then
    cd "$BACKUP_DIR"
    tar -czf "uploads_$DATE.tar.gz" "uploads_$DATE"
    rm -rf "uploads_$DATE"
    UPLOADS_SIZE=$(du -h "$BACKUP_DIR/uploads_$DATE.tar.gz" | cut -f1)
    echo -e "${GREEN}✅ Uploads backed up successfully ($UPLOADS_SIZE)${NC}"
else
    echo -e "${YELLOW}⚠️  Uploads backup failed (might be empty)${NC}"
fi

# 3. Backup Configuration Files
echo "⚙️  Backing up configuration..."
if [ -f .env ]; then
    cp .env "$BACKUP_DIR/env_$DATE"
    echo -e "${GREEN}✅ Environment file backed up${NC}"
fi

if [ -f docker-compose.yml ]; then
    cp docker-compose.yml "$BACKUP_DIR/docker-compose_$DATE.yml"
    echo -e "${GREEN}✅ Docker compose file backed up${NC}"
fi

# 4. Create backup manifest
echo "📝 Creating backup manifest..."
cat > "$BACKUP_DIR/manifest_$DATE.txt" << EOL
Employee Management System Backup
=================================
Date: $(date)
Hostname: $(hostname)
Docker Version: $(docker --version)

Files:
------
$(ls -lh $BACKUP_DIR/*_$DATE*)

Database Size: $DB_SIZE
Uploads Size: ${UPLOADS_SIZE:-N/A}

Services Status:
----------------
$(docker-compose ps)

EOL

echo -e "${GREEN}✅ Manifest created${NC}"

# 5. Retention - Remove old backups
echo "🗑️  Applying retention policy ($RETENTION_DAYS days)..."
find "$BACKUP_DIR" -name "db_backup_*.sql.gz" -mtime +$RETENTION_DAYS -delete
find "$BACKUP_DIR" -name "uploads_*.tar.gz" -mtime +$RETENTION_DAYS -delete
find "$BACKUP_DIR" -name "env_*" -mtime +$RETENTION_DAYS -delete
find "$BACKUP_DIR" -name "docker-compose_*.yml" -mtime +$RETENTION_DAYS -delete
find "$BACKUP_DIR" -name "manifest_*.txt" -mtime +$RETENTION_DAYS -delete

REMAINING=$(ls -1 "$BACKUP_DIR"/db_backup_*.sql.gz 2>/dev/null | wc -l)
echo -e "${GREEN}✅ Old backups cleaned (${REMAINING} backups remaining)${NC}"

# 6. Backup Summary
echo ""
echo "=============================================="
echo "✅ Backup completed successfully!"
echo ""
echo "Backup Location: $BACKUP_DIR"
echo "Files created:"
ls -lh "$BACKUP_DIR"/*_$DATE* 2>/dev/null
echo ""
echo "Total backup size:"
du -sh "$BACKUP_DIR"
echo ""

# Optional: Upload to cloud storage (uncomment to use)
# echo "☁️  Uploading to cloud storage..."
# aws s3 sync "$BACKUP_DIR" s3://your-bucket/ems-backups/ --exclude "*" --include "*_$DATE*"
# rclone copy "$BACKUP_DIR" remote:ems-backups/

exit 0

