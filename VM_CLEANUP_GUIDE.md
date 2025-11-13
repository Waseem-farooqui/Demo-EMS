# 🧹 Complete VM Cleanup Guide

Complete guide to remove all Employee Management System installations and restore your VM to a fresh state.

---

## ⚠️ WARNING

**This will PERMANENTLY DELETE:**
- ❌ All Docker containers, images, and volumes
- ❌ All application data and databases
- ❌ All uploaded documents
- ❌ All backups
- ❌ All configuration files
- ❌ All logs
- ❌ Application source code (optional)
- ❌ Docker installation (optional)

**THIS CANNOT BE UNDONE!**

Make sure you have backed up any important data before proceeding.

---

## 📋 Quick Start

### For Linux/Mac:

```bash
# Make script executable
chmod +x cleanup-vm.sh

# Run cleanup script
./cleanup-vm.sh
```

### For Windows:

```cmd
# Run as Administrator
cleanup-vm.bat
```

---

## 🔧 Manual Cleanup Steps

If you prefer to clean up manually or the script doesn't work:

### Step 1: Stop All Containers

```bash
# Stop EMS containers
docker stop ems-backend ems-frontend ems-mysql

# Stop all containers
docker stop $(docker ps -aq)
```

### Step 2: Remove Containers

```bash
# Remove EMS containers
docker rm -f ems-backend ems-frontend ems-mysql

# Remove all containers
docker rm -f $(docker ps -aq)
```

### Step 3: Remove Images

```bash
# Remove EMS images
docker rmi -f $(docker images | grep -E 'ems-|employeemanagementsystem' | awk '{print $3}')

# Remove all images
docker rmi -f $(docker images -aq)
```

### Step 4: Remove Volumes (⚠️ DATA LOSS)

```bash
# Remove EMS volumes
docker volume rm employeemanagementsystem_mysql_data
docker volume rm employeemanagementsystem_uploads_data

# Remove all volumes
docker volume prune -f
```

### Step 5: Remove Networks

```bash
# Remove EMS network
docker network rm employeemanagementsystem_ems-network

# Remove unused networks
docker network prune -f
```

### Step 6: Clean Docker System

```bash
# Complete cleanup (removes everything)
docker system prune -a --volumes -f

# Check disk space freed
docker system df
```

### Step 7: Remove Application Files

```bash
# Remove application directory
rm -rf /path/to/EmployeeManagementSystem

# Or if in application directory
cd ..
rm -rf EmployeeManagementSystem
```

### Step 8: Remove Backups

```bash
# Remove backup directory
rm -rf /backups/ems

# Remove local backups
rm -f backup_*.sql*
rm -rf uploads_backup_*
rm -f .env.backup_*
```

### Step 9: Remove Logs

```bash
# Remove log files
rm -rf /var/log/ems*
rm -rf logs/
```

### Step 10: Remove Cron Jobs

```bash
# Edit crontab
crontab -e

# Remove lines containing:
# - backup.sh
# - monitor.sh
# - ems

# Or remove all cron jobs
crontab -r
```

### Step 11: Uninstall Docker (Optional)

#### Ubuntu/Debian:
```bash
sudo apt-get purge -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo apt-get autoremove -y
sudo rm -rf /var/lib/docker
sudo rm -rf /var/lib/containerd
sudo rm -rf /etc/docker
```

#### RHEL/CentOS:
```bash
sudo yum remove -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo rm -rf /var/lib/docker
sudo rm -rf /var/lib/containerd
sudo rm -rf /etc/docker
```

#### Windows:
1. Open Windows Settings
2. Go to Apps & Features
3. Find "Docker Desktop"
4. Click Uninstall

#### Mac:
```bash
# Uninstall Docker Desktop
/Applications/Docker.app/Contents/MacOS/uninstall

# Or manually
rm -rf /Applications/Docker.app
rm -rf ~/Library/Group\ Containers/group.com.docker
rm -rf ~/Library/Containers/com.docker.*
```

### Step 12: Remove Docker Compose

```bash
sudo rm -f /usr/local/bin/docker-compose
sudo rm -f /usr/bin/docker-compose
```

---

## ✅ Verification

After cleanup, verify everything is removed:

### Check Docker:
```bash
# Should show nothing or "command not found"
docker ps -a
docker images
docker volume ls
docker network ls
```

### Check Disk Space:
```bash
# Check freed space
df -h

# Check Docker space (if still installed)
docker system df
```

### Check Files:
```bash
# Check application directory removed
ls /opt/EmployeeManagementSystem  # Should not exist

# Check backups removed
ls /backups/ems  # Should not exist
```

### Check Processes:
```bash
# No Docker processes running
ps aux | grep docker
```

---

## 🔄 Reinstallation

To reinstall after cleanup:

### 1. Install Docker

#### Ubuntu/Debian:
```bash
# Update packages
sudo apt-get update

# Install dependencies
sudo apt-get install -y apt-transport-https ca-certificates curl gnupg lsb-release

# Add Docker GPG key
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

# Add Docker repository
echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Start Docker
sudo systemctl start docker
sudo systemctl enable docker
```

#### RHEL/CentOS:
```bash
# Install Docker
sudo yum install -y yum-utils
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Start Docker
sudo systemctl start docker
sudo systemctl enable docker
```

#### Windows/Mac:
Download and install Docker Desktop from: https://www.docker.com/products/docker-desktop

### 2. Clone Repository

```bash
git clone <repository-url>
cd EmployeeManagementSystem
```

### 3. Deploy Application

```bash
# Create .env file
cp .env.example .env
vim .env  # Configure your settings

# Deploy
docker-compose up -d --build

# Verify
docker-compose ps
```

---

## 📊 Cleanup Script Features

The automated cleanup script (`cleanup-vm.sh` or `cleanup-vm.bat`) includes:

✅ **Safety Checks:**
- Requires explicit confirmation
- Shows what will be deleted
- Provides options for partial cleanup

✅ **Comprehensive Cleanup:**
- Stops all containers
- Removes containers, images, volumes, networks
- Cleans application files
- Removes backups and logs
- Removes cron jobs
- Optional Docker uninstallation

✅ **User-Friendly:**
- Colored output for easy reading
- Step-by-step progress
- Summary of actions taken
- Verification commands provided

---

## 🆘 Troubleshooting

### "Permission Denied"
```bash
# Run with sudo
sudo ./cleanup-vm.sh

# Or for Windows
# Right-click cleanup-vm.bat → Run as Administrator
```

### Docker Commands Fail
```bash
# Start Docker service
sudo systemctl start docker

# Or restart Docker Desktop (Windows/Mac)
```

### Can't Remove Volumes
```bash
# Force remove
docker volume rm -f <volume_name>

# Or remove all
docker volume prune -f --all
```

### Disk Space Not Freed
```bash
# Additional cleanup
docker builder prune -a -f
docker image prune -a -f

# Check system
df -h
du -sh /var/lib/docker
```

### Script Fails Halfway
```bash
# Run commands manually from the script
# Or restart the script (it's idempotent)
./cleanup-vm.sh
```

---

## 📝 Cleanup Checklist

Before running cleanup:
- [ ] Backup important data
- [ ] Export database if needed
- [ ] Save configuration files
- [ ] Document current settings
- [ ] Notify team/users

After running cleanup:
- [ ] Verify containers removed: `docker ps -a`
- [ ] Verify images removed: `docker images`
- [ ] Verify volumes removed: `docker volume ls`
- [ ] Verify disk space freed: `df -h`
- [ ] Verify application directory removed
- [ ] Verify backups removed (if desired)
- [ ] Verify cron jobs removed: `crontab -l`

---

## 💡 Partial Cleanup Options

If you don't want to remove everything, you can selectively clean:

### Keep Application Code, Remove Docker Data:
```bash
docker-compose down -v
docker system prune -a --volumes -f
# Keep source code directory
```

### Keep Images, Remove Containers and Volumes:
```bash
docker-compose down -v
docker container prune -f
docker volume prune -f
```

### Keep Everything, Just Stop Services:
```bash
docker-compose down
# Everything preserved for later restart
```

---

## 🎯 Quick Commands

```bash
# Nuclear option - remove everything
docker stop $(docker ps -aq) && \
docker rm $(docker ps -aq) && \
docker rmi $(docker images -aq) && \
docker volume prune -f && \
docker network prune -f && \
docker system prune -a --volumes -f

# Verify cleanup
docker ps -a && docker images && docker volume ls

# Check freed space
df -h
```

---

## 📞 Support

If you encounter issues during cleanup:

1. Check the troubleshooting section above
2. Manually run each step from the manual cleanup guide
3. Verify each step completes successfully
4. Check system logs for errors

---

**Last Updated:** November 14, 2025
**Version:** 1.0.0

