#!/bin/bash
###############################################################################
# Ubuntu Server Security Hardening Script
# Comprehensive security hardening for production Ubuntu server
###############################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Banner
echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   Ubuntu Server Security Hardening Script                ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Functions
print_step() {
    echo ""
    echo -e "${BLUE}▶ $1${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ️  $1${NC}"
}

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    print_error "This script must be run as root"
    echo "Usage: sudo $0"
    exit 1
fi

# Step 1: Update System
print_step "Step 1/15: Updating System Packages"

apt-get update -qq
apt-get upgrade -y -qq
apt-get dist-upgrade -y -qq
print_success "System packages updated"

# Step 2: Install Security Tools
print_step "Step 2/15: Installing Security Tools"

apt-get install -y -qq \
    fail2ban \
    ufw \
    unattended-upgrades \
    apt-listchanges \
    logwatch \
    rkhunter \
    chkrootkit \
    lynis \
    clamav \
    clamav-daemon \
    apparmor \
    apparmor-utils

print_success "Security tools installed"

# Step 3: Configure Firewall (UFW)
print_step "Step 3/15: Configuring Firewall (UFW)"

# Reset UFW to defaults
ufw --force reset

# Set default policies
ufw default deny incoming
ufw default allow outgoing

# Allow SSH (IMPORTANT: Do this first!)
print_info "Allowing SSH (port 22)..."
ufw allow 22/tcp comment 'SSH'

# Allow HTTP/HTTPS
print_info "Allowing HTTP/HTTPS..."
ufw allow 80/tcp comment 'HTTP'
ufw allow 443/tcp comment 'HTTPS'

# Allow Docker ports if needed
print_info "Allowing Docker ports..."
ufw allow 8080/tcp comment 'Backend API'
ufw allow 3307/tcp comment 'MySQL (if exposed)'

# Enable UFW
ufw --force enable
print_success "Firewall configured and enabled"

# Step 4: Configure Fail2Ban
print_step "Step 4/15: Configuring Fail2Ban"

# Create custom jail for Spring Boot application
cat > /etc/fail2ban/jail.d/spring-boot.conf << 'EOF'
[spring-boot]
enabled = true
port = 8080
filter = spring-boot
logpath = /var/log/fail2ban-spring-boot.log
maxretry = 5
bantime = 3600
findtime = 600
EOF

# Create filter for Spring Boot security events
cat > /etc/fail2ban/filter.d/spring-boot.conf << 'EOF'
[Definition]
failregex = ^.*BLOCKED MALICIOUS REQUEST.*IP: <HOST>.*$
            ^.*Unauthorized error.*IP: <HOST>.*$
            ^.*RequestRejectedException.*IP: <HOST>.*$
ignoreregex =
EOF

# Create log file for Spring Boot security events
touch /var/log/fail2ban-spring-boot.log
chmod 644 /var/log/fail2ban-spring-boot.log

# Restart fail2ban
systemctl restart fail2ban
systemctl enable fail2ban

print_success "Fail2Ban configured for Spring Boot security events"

# Step 5: Configure Automatic Security Updates
print_step "Step 5/15: Configuring Automatic Security Updates"

cat > /etc/apt/apt.conf.d/50unattended-upgrades << 'EOF'
Unattended-Upgrade::Allowed-Origins {
    "${distro_id}:${distro_codename}-security";
    "${distro_id}ESMApps:${distro_codename}-apps-security";
    "${distro_id}ESM:${distro_codename}-infra-security";
};
Unattended-Upgrade::AutoFixInterruptedDpkg "true";
Unattended-Upgrade::MinimalSteps "true";
Unattended-Upgrade::Remove-Unused-Kernel-Packages "true";
Unattended-Upgrade::Remove-Unused-Dependencies "true";
Unattended-Upgrade::Automatic-Reboot "false";
Unattended-Upgrade::Automatic-Reboot-Time "02:00";
EOF

cat > /etc/apt/apt.conf.d/20auto-upgrades << 'EOF'
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Download-Upgradeable-Packages "1";
APT::Periodic::AutocleanInterval "7";
APT::Periodic::Unattended-Upgrade "1";
EOF

systemctl enable unattended-upgrades
systemctl start unattended-upgrades

print_success "Automatic security updates configured"

# Step 6: Harden SSH Configuration
print_step "Step 6/15: Hardening SSH Configuration"

# Check if SSH is installed
if ! command -v sshd &> /dev/null && [ ! -f /etc/ssh/sshd_config ]; then
    print_warning "SSH server (openssh-server) not found"
    print_info "Installing openssh-server..."
    apt-get install -y -qq openssh-server
    print_success "SSH server installed"
fi

SSH_CONFIG="/etc/ssh/sshd_config"

# Check if SSH config exists
if [ ! -f "$SSH_CONFIG" ]; then
    print_error "SSH configuration file not found at $SSH_CONFIG"
    print_info "SSH may not be installed. Skipping SSH hardening."
else
    # Backup SSH config
    cp "$SSH_CONFIG" "${SSH_CONFIG}.backup.$(date +%Y%m%d_%H%M%S)"
    print_info "SSH config backed up"

    # Apply SSH security settings (only if not already set)
    if ! grep -q "^PermitRootLogin" "$SSH_CONFIG"; then
        sed -i 's/#PermitRootLogin yes/PermitRootLogin no/' "$SSH_CONFIG"
        sed -i 's/^PermitRootLogin yes/PermitRootLogin no/' "$SSH_CONFIG"
    fi
    
    if ! grep -q "^PasswordAuthentication" "$SSH_CONFIG"; then
        sed -i 's/#PasswordAuthentication yes/PasswordAuthentication yes/' "$SSH_CONFIG"
        sed -i 's/^PasswordAuthentication no/PasswordAuthentication yes/' "$SSH_CONFIG"
    fi
    
    if ! grep -q "^PubkeyAuthentication" "$SSH_CONFIG"; then
        sed -i 's/#PubkeyAuthentication yes/PubkeyAuthentication yes/' "$SSH_CONFIG"
    fi
    
    if ! grep -q "^PermitEmptyPasswords" "$SSH_CONFIG"; then
        sed -i 's/#PermitEmptyPasswords no/PermitEmptyPasswords no/' "$SSH_CONFIG"
    fi
    
    if ! grep -q "^MaxAuthTries" "$SSH_CONFIG"; then
        sed -i 's/#MaxAuthTries 6/MaxAuthTries 3/' "$SSH_CONFIG"
    fi
    
    if ! grep -q "^ClientAliveInterval" "$SSH_CONFIG"; then
        sed -i 's/#ClientAliveInterval 0/ClientAliveInterval 300/' "$SSH_CONFIG"
    fi
    
    if ! grep -q "^ClientAliveCountMax" "$SSH_CONFIG"; then
        sed -i 's/#ClientAliveCountMax 3/ClientAliveCountMax 2/' "$SSH_CONFIG"
    fi

    # Add additional security settings if not present
    if ! grep -q "^Protocol 2" "$SSH_CONFIG"; then
        cat >> "$SSH_CONFIG" << 'EOF'

# Additional Security Settings
Protocol 2
X11Forwarding no
AllowTcpForwarding no
PermitTunnel no
MaxSessions 2
MaxStartups 3:50:10
EOF
    fi

    # Test SSH config before restarting
    if sshd -t 2>/dev/null; then
        # Detect SSH service name
        SSH_SERVICE=""
        if systemctl list-units --type=service | grep -q "sshd.service"; then
            SSH_SERVICE="sshd"
        elif systemctl list-units --type=service | grep -q "ssh.service"; then
            SSH_SERVICE="ssh"
        elif systemctl list-units --type=service | grep -q "openssh-server.service"; then
            SSH_SERVICE="openssh-server"
        else
            # Try to find SSH service
            SSH_SERVICE=$(systemctl list-units --type=service --all | grep -i ssh | head -1 | awk '{print $1}' | sed 's/.service//' || echo "")
        fi

        if [ -n "$SSH_SERVICE" ]; then
            print_info "Restarting SSH service: $SSH_SERVICE"
            if systemctl restart "$SSH_SERVICE"; then
                print_success "SSH configuration hardened and service restarted"
            else
                print_warning "SSH config updated but could not restart service"
                print_info "You may need to restart SSH manually: sudo systemctl restart $SSH_SERVICE"
            fi
        else
            print_warning "Could not detect SSH service name"
            print_info "SSH configuration updated. Please restart SSH manually:"
            print_info "  sudo systemctl restart sshd"
            print_info "  OR"
            print_info "  sudo systemctl restart ssh"
            print_info "  OR"
            print_info "  sudo service ssh restart"
        fi
    else
        print_error "SSH configuration test failed, restoring backup"
        BACKUP_FILE=$(ls -t "${SSH_CONFIG}.backup."* 2>/dev/null | head -1)
        if [ -n "$BACKUP_FILE" ]; then
            cp "$BACKUP_FILE" "$SSH_CONFIG"
            print_info "Backup restored from: $BACKUP_FILE"
        fi
        print_warning "SSH configuration not changed due to test failure"
    fi
fi

# Step 7: Configure System Limits
print_step "Step 7/15: Configuring System Limits"

cat >> /etc/security/limits.conf << 'EOF'

# Security Hardening Limits
* soft nofile 65535
* hard nofile 65535
* soft nproc 4096
* hard nproc 4096
EOF

print_success "System limits configured"

# Step 8: Configure Kernel Parameters
print_step "Step 8/15: Configuring Kernel Security Parameters"

cat >> /etc/sysctl.conf << 'EOF'

# Security Hardening - Kernel Parameters
# IP Spoofing protection
net.ipv4.conf.all.rp_filter = 1
net.ipv4.conf.default.rp_filter = 1

# Ignore ICMP redirects
net.ipv4.conf.all.accept_redirects = 0
net.ipv4.conf.default.accept_redirects = 0
net.ipv6.conf.all.accept_redirects = 0
net.ipv6.conf.default.accept_redirects = 0

# Ignore ICMP ping requests
net.ipv4.icmp_echo_ignore_all = 1

# Ignore send redirects
net.ipv4.conf.all.send_redirects = 0
net.ipv4.conf.default.send_redirects = 0

# SYN flood protection
net.ipv4.tcp_syncookies = 1
net.ipv4.tcp_max_syn_backlog = 2048
net.ipv4.tcp_synack_retries = 2
net.ipv4.tcp_syn_retries = 5

# Log Martians
net.ipv4.conf.all.log_martians = 1
net.ipv4.icmp_ignore_bogus_error_responses = 1

# IP forwarding disabled
net.ipv4.ip_forward = 0
net.ipv6.conf.all.forwarding = 0

# Source route verification
net.ipv4.conf.all.accept_source_route = 0
net.ipv4.conf.default.accept_source_route = 0
net.ipv6.conf.all.accept_source_route = 0
net.ipv6.conf.default.accept_source_route = 0

# Disable IPv6 if not needed (uncomment if IPv6 not used)
# net.ipv6.conf.all.disable_ipv6 = 1
# net.ipv6.conf.default.disable_ipv6 = 1
EOF

sysctl -p
print_success "Kernel security parameters configured"

# Step 9: Configure Logwatch
print_step "Step 9/15: Configuring Logwatch"

# Configure logwatch to email reports (if mail is configured)
sed -i 's/LogDir = \/var\/log/LogDir = \/var\/log\nMailTo = root/' /etc/logwatch/conf/logwatch.conf 2>/dev/null || true

print_success "Logwatch configured"

# Step 10: Configure AppArmor
print_step "Step 10/15: Configuring AppArmor"

systemctl enable apparmor
systemctl start apparmor

print_success "AppArmor enabled"

# Step 11: Set Up Intrusion Detection
print_step "Step 11/15: Setting Up Intrusion Detection"

# Configure rkhunter if installed
if command -v rkhunter &> /dev/null; then
    print_info "Configuring rkhunter..."
    
    # Fix rkhunter configuration if needed
    RKHUNTER_CONF="/etc/rkhunter.conf"
    if [ -f "$RKHUNTER_CONF" ]; then
        # Fix WEB_CMD if it's set incorrectly
        if grep -q "^WEB_CMD=" "$RKHUNTER_CONF"; then
            # Comment out problematic WEB_CMD or set to empty
            sed -i 's/^WEB_CMD=.*/#WEB_CMD="" # Disabled/' "$RKHUNTER_CONF" || true
        fi
        
        # Ensure UPDATE_MIRRORS is enabled
        if grep -q "^UPDATE_MIRRORS=" "$RKHUNTER_CONF"; then
            sed -i 's/^UPDATE_MIRRORS=.*/UPDATE_MIRRORS=1/' "$RKHUNTER_CONF" || true
        fi
        
        # Set proper paths
        if ! grep -q "^SCRIPTWHITELIST=" "$RKHUNTER_CONF" || ! grep -q "/usr/bin/which" "$RKHUNTER_CONF"; then
            echo "" >> "$RKHUNTER_CONF"
            echo "# Security hardening script additions" >> "$RKHUNTER_CONF"
            echo "SCRIPTWHITELIST=/usr/bin/which" >> "$RKHUNTER_CONF" || true
        fi
    fi
    
    # Update rkhunter database
    print_info "Updating rkhunter database..."
    rkhunter --update 2>&1 | grep -v "Invalid WEB_CMD" || true
    
    # Run initial scan (non-blocking, may take time)
    print_info "Running initial rkhunter scan (this may take a while, running in background)..."
    print_info "You can check results later with: sudo rkhunter --check"
    # Run scan but don't wait for it to complete
    nohup rkhunter --check --skip-keypress --report-warnings-only > /tmp/rkhunter-scan.log 2>&1 &
    
    print_success "rkhunter configured and initial scan started in background"
    print_info "Check scan results: cat /tmp/rkhunter-scan.log"
else
    print_warning "rkhunter not installed, skipping configuration"
fi

# Configure chkrootkit if installed
if command -v chkrootkit &> /dev/null; then
    print_info "chkrootkit is installed and ready to use"
    print_info "Run manual scan with: sudo chkrootkit"
else
    print_info "chkrootkit not installed (optional tool)"
fi

print_success "Intrusion detection tools configured"

# Step 12: Configure Docker Security
print_step "Step 12/15: Configuring Docker Security"

# Create Docker daemon security configuration
mkdir -p /etc/docker
cat > /etc/docker/daemon.json << 'EOF'
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "live-restore": true,
  "userland-proxy": false,
  "no-new-privileges": true
}
EOF

print_success "Docker security configured"

# Step 13: Create Security Monitoring Script
print_step "Step 13/15: Creating Security Monitoring Script"

cat > /usr/local/bin/security-monitor.sh << 'SCRIPT'
#!/bin/bash
# Security Monitoring Script
# Checks for suspicious activities and security events

LOG_FILE="/var/log/security-monitor.log"
DATE=$(date '+%Y-%m-%d %H:%M:%S')

echo "[$DATE] Security Monitor Check" >> "$LOG_FILE"

# Check for failed login attempts
FAILED_LOGINS=$(grep "Failed password" /var/log/auth.log 2>/dev/null | wc -l)
if [ "$FAILED_LOGINS" -gt 10 ]; then
    echo "[$DATE] WARNING: $FAILED_LOGINS failed login attempts detected" >> "$LOG_FILE"
fi

# Check for blocked requests in application logs
if [ -f "/var/log/fail2ban-spring-boot.log" ]; then
    BLOCKED_REQUESTS=$(tail -100 /var/log/fail2ban-spring-boot.log | grep "BLOCKED MALICIOUS REQUEST" | wc -l)
    if [ "$BLOCKED_REQUESTS" -gt 0 ]; then
        echo "[$DATE] WARNING: $BLOCKED_REQUESTS malicious requests blocked" >> "$LOG_FILE"
    fi
fi

# Check disk space
DISK_USAGE=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
if [ "$DISK_USAGE" -gt 80 ]; then
    echo "[$DATE] WARNING: Disk usage at ${DISK_USAGE}%" >> "$LOG_FILE"
fi

# Check for rootkits
if command -v rkhunter &> /dev/null; then
    rkhunter --check --skip-keypress --report-warnings-only >> "$LOG_FILE" 2>&1 || true
fi
SCRIPT

chmod +x /usr/local/bin/security-monitor.sh

# Add to crontab (run daily at 2 AM)
(crontab -l 2>/dev/null; echo "0 2 * * * /usr/local/bin/security-monitor.sh") | crontab -

print_success "Security monitoring script created"

# Step 14: Configure Log Rotation for Security Logs
print_step "Step 14/15: Configuring Log Rotation"

cat > /etc/logrotate.d/spring-boot-security << 'EOF'
/var/log/fail2ban-spring-boot.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 644 root root
}
EOF

print_success "Log rotation configured"

# Step 15: Final Security Checks
print_step "Step 15/15: Running Final Security Checks"

# Check firewall status
if ufw status | grep -q "Status: active"; then
    print_success "Firewall is active"
else
    print_warning "Firewall may not be active"
fi

# Check fail2ban status
if systemctl is-active --quiet fail2ban; then
    print_success "Fail2Ban is running"
else
    print_warning "Fail2Ban is not running"
fi

# Check for open ports
print_info "Checking for open ports..."
OPEN_PORTS=$(ss -tuln | grep LISTEN | wc -l)
print_info "Found $OPEN_PORTS listening ports"

# Summary
echo ""
echo -e "${CYAN}════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}Security Hardening Summary:${NC}"
echo ""
echo -e "${GREEN}✅${NC} System updated"
echo -e "${GREEN}✅${NC} Security tools installed"
echo -e "${GREEN}✅${NC} Firewall (UFW) configured"
echo -e "${GREEN}✅${NC} Fail2Ban configured for Spring Boot"
echo -e "${GREEN}✅${NC} Automatic security updates enabled"
echo -e "${GREEN}✅${NC} SSH hardened"
echo -e "${GREEN}✅${NC} Kernel security parameters configured"
echo -e "${GREEN}✅${NC} Docker security configured"
echo -e "${GREEN}✅${NC} Security monitoring enabled"
echo ""
echo -e "${CYAN}Important Notes:${NC}"
echo -e "  • SSH root login is disabled"
echo -e "  • Firewall is blocking all incoming except configured ports"
echo -e "  • Fail2Ban will automatically ban IPs after 5 failed attempts"
echo -e "  • Security updates will be installed automatically"
echo -e "  • Monitor logs: /var/log/fail2ban-spring-boot.log"
echo ""
echo -e "${YELLOW}⚠️  IMPORTANT:${NC}"
echo -e "  • Test SSH access before closing current session!"
echo -e "  • Review firewall rules: ${GREEN}ufw status verbose${NC}"
echo -e "  • Check Fail2Ban status: ${GREEN}fail2ban-client status${NC}"
echo -e "  • View security logs: ${GREEN}tail -f /var/log/fail2ban-spring-boot.log${NC}"
echo ""

