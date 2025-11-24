# Employee Management System

A comprehensive employee management system with document management, leave tracking, attendance, and rota management.

## Quick Start

### Production Deployment

```bash
chmod +x production-deploy.sh
./production-deploy.sh
```

## Deployment Scripts

- **production-deploy.sh** - Complete production deployment (recommended)
- **fresh-deploy.sh** - Fresh deployment on new Ubuntu VM
- **update.sh** - Zero-downtime updates
- **backup.sh** - Automated backups
- **monitor.sh** - Health monitoring
- **rollback.sh** - Rollback to previous version
- **security-fixes.sh** - Security validation
- **cleanup-vm.sh** - VM cleanup and reset
- **docker-deploy.sh** - Docker-based deployment

## Configuration

1. Copy `.env.example` to `.env`
2. Update `.env` with your production values
3. Run `./production-deploy.sh`

## Documentation

Comprehensive documentation will be created separately.
