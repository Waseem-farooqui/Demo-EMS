# Hostinger DNS Configuration Guide

This guide explains how to configure DNS in Hostinger to point your domain to your production server.

## Prerequisites

- Domain purchased from Hostinger: `vertexdigitalsystem.com`
- Production server IP address (starts with 62.x.x.x)
- Access to Hostinger hPanel

## Step-by-Step DNS Configuration

### Step 1: Log in to Hostinger hPanel

1. Go to: https://hpanel.hostinger.com
2. Log in with your Hostinger account credentials
3. Select your domain: `vertexdigitalsystem.com`

### Step 2: Navigate to DNS Settings

1. In the domain management page, find **"DNS / Name Servers"** section
2. Click **"Manage"** or **"DNS Zone Editor"**
3. You'll see a list of DNS records

### Step 3: Configure A Records

You need to add/update two A records:

#### Record 1: Root Domain
- **Type:** A
- **Name:** `@` (or leave blank/empty for root domain)
- **Points to / Value:** `YOUR_SERVER_IP` (e.g., 62.169.20.104)
- **TTL:** 3600 (or use default)

#### Record 2: WWW Subdomain
- **Type:** A
- **Name:** `www`
- **Points to / Value:** `YOUR_SERVER_IP` (same IP as above)
- **TTL:** 3600 (or use default)

### Step 4: Remove Conflicting Records

1. Check for any existing A records for `@` or `www` that point to different IPs
2. Delete or update them to point to your server IP
3. Common conflicting records:
   - Records pointing to Hostinger's default IP (e.g., 84.32.84.32)
   - Records pointing to old server IPs

### Step 5: Save Changes

1. Click **"Save"** or **"Update"** button
2. Wait for changes to propagate (usually 1-2 hours, can take up to 24 hours)

## Verify DNS Configuration

### From Your Server

```bash
# Check DNS resolution
dig vertexdigitalsystem.com @8.8.8.8

# Should show your server IP (62.x.x.x)
# Example output:
# vertexdigitalsystem.com. 3600 IN A 62.169.20.104
```

### From Command Line (Anywhere)

```bash
# Using dig
dig vertexdigitalsystem.com @8.8.8.8

# Using nslookup
nslookup vertexdigitalsystem.com 8.8.8.8

# Using host
host vertexdigitalsystem.com 8.8.8.8
```

### Expected Result

```
vertexdigitalsystem.com -> YOUR_SERVER_IP (62.x.x.x)
www.vertexdigitalsystem.com -> YOUR_SERVER_IP (62.x.x.x)
```

## Common Issues

### Issue 1: Domain Points to Wrong IP

**Symptom:** DNS shows domain pointing to 84.32.84.32 or another IP

**Solution:**
1. Check Hostinger DNS records
2. Update A records to point to your server IP
3. Wait for DNS propagation
4. Verify with: `dig vertexdigitalsystem.com @8.8.8.8`

### Issue 2: DNS Not Propagated Yet

**Symptom:** DNS still shows old IP after updating

**Solution:**
- DNS propagation can take 1-24 hours
- Check from different DNS servers: `dig vertexdigitalsystem.com @1.1.1.1`
- Clear DNS cache: `sudo systemd-resolve --flush-caches` (Linux)

### Issue 3: Multiple A Records

**Symptom:** Multiple A records for same domain

**Solution:**
- Remove duplicate A records
- Keep only one A record pointing to your server IP

### Issue 4: CNAME Conflicts

**Symptom:** CNAME record conflicts with A record

**Solution:**
- Remove CNAME records for `@` (root domain)
- Use A records instead
- CNAME can only be used for subdomains, not root domain

## Testing DNS Before Certificate Generation

Before running `setup-letsencrypt.sh`, verify DNS:

```bash
# Check root domain
dig vertexdigitalsystem.com @8.8.8.8 +short
# Should return: YOUR_SERVER_IP

# Check www subdomain
dig www.vertexdigitalsystem.com @8.8.8.8 +short
# Should return: YOUR_SERVER_IP

# Verify from your server
curl -I http://vertexdigitalsystem.com
# Should connect to your server, not Hostinger default page
```

## Quick Reference

### Hostinger hPanel URL
https://hpanel.hostinger.com

### DNS Records Needed
```
Type: A
Name: @
Value: YOUR_SERVER_IP
TTL: 3600

Type: A
Name: www
Value: YOUR_SERVER_IP
TTL: 3600
```

### Verification Command
```bash
dig vertexdigitalsystem.com @8.8.8.8 +short
```

## After DNS Configuration

Once DNS is properly configured:

1. **Wait for propagation** (1-2 hours typically)
2. **Verify DNS** using the commands above
3. **Run Let's Encrypt setup:**
   ```bash
   sudo ./setup-letsencrypt.sh
   ```
4. **The script will automatically verify DNS before generating certificates**

## Support

If you continue to have issues:

1. Check Hostinger support documentation
2. Contact Hostinger support for DNS configuration help
3. Verify your domain is active in Hostinger
4. Check if domain has any restrictions or holds

