# SSL/HTTPS Setup Guide

This guide explains how to set up SSL/HTTPS for the Employee Management System.

## Overview

The application now supports HTTPS with two options:
1. **Self-signed certificates** (for development/testing)
2. **Let's Encrypt certificates** (for production)

## Quick Start

### For Development/Testing (Self-Signed Certificates)

1. Generate SSL certificates:
```bash
chmod +x generate-ssl-cert.sh
./generate-ssl-cert.sh
```

This will create:
- `ssl/localhost.crt` - Certificate
- `ssl/localhost.key` - Private key
- `ssl/dhparam.pem` - DH parameters (for better security)

2. Deploy with SSL:
```bash
./production-deploy.sh
```

The deployment script will automatically:
- Generate certificates if they don't exist
- Configure nginx for HTTPS
- Set up HTTP to HTTPS redirect

3. Access the application:
- HTTPS: `https://localhost` (or your domain)
- HTTP: `http://localhost` (automatically redirects to HTTPS)

**Note:** Browsers will show a security warning for self-signed certificates. Click "Advanced" → "Proceed to localhost" to continue.

### For Production (Let's Encrypt)

1. Prerequisites:
   - Domain name pointing to your server's IP
   - Port 80 and 443 open in firewall
   - Email address for certificate notifications

2. Generate Let's Encrypt certificates:
```bash
chmod +x setup-letsencrypt.sh
DOMAIN=yourdomain.com EMAIL=your@email.com ./setup-letsencrypt.sh
```

3. Update `.env` file:
```bash
DOMAIN=yourdomain.com
FRONTEND_PORT=80
FRONTEND_HTTPS_PORT=443
```

4. Deploy:
```bash
./production-deploy.sh
```

5. Set up auto-renewal:
```bash
# Test renewal
sudo certbot renew --dry-run

# Add to crontab for automatic renewal
sudo crontab -e
# Add this line:
0 0 * * * certbot renew --quiet && docker-compose restart frontend
```

## Configuration Files

### Nginx Configuration (`frontend/nginx.conf`)

The nginx configuration includes:
- **HTTP server** (port 80): Redirects to HTTPS, allows Let's Encrypt challenges
- **HTTPS server** (port 443): Serves the application with SSL
- **Modern SSL configuration**: TLS 1.2/1.3, secure ciphers
- **Security headers**: HSTS, CSP, X-Frame-Options, etc.

### Docker Compose (`compose.yaml`)

The frontend service now:
- Exposes ports 80 (HTTP) and 443 (HTTPS)
- Mounts SSL certificates from `./ssl` directory
- Mounts certbot webroot for Let's Encrypt challenges

### Environment Variables

Add to your `.env` file:
```bash
# SSL Configuration
DOMAIN=yourdomain.com
SSL_DIR=./ssl

# Ports
FRONTEND_PORT=80
FRONTEND_HTTPS_PORT=443

# URLs (use HTTPS)
FRONTEND_URL=https://yourdomain.com
API_URL=https://yourdomain.com:8080/api
API_BASE_URL=https://yourdomain.com:8080
```

## SSL Certificate Files

### Self-Signed Certificates

- `ssl/localhost.crt` - Certificate
- `ssl/localhost.key` - Private key
- `ssl/dhparam.pem` - DH parameters
- `ssl/cert.crt` - Symlink/copy for nginx (auto-created)
- `ssl/cert.key` - Symlink/copy for nginx (auto-created)

### Let's Encrypt Certificates

- `ssl/yourdomain.com-fullchain.crt` - Full chain certificate
- `ssl/yourdomain.com.crt` - Certificate
- `ssl/yourdomain.com.key` - Private key
- `ssl/dhparam.pem` - DH parameters

## Security Features

### SSL/TLS Configuration

- **Protocols**: TLS 1.2 and TLS 1.3 only
- **Ciphers**: Modern, secure cipher suites
- **OCSP Stapling**: Enabled for better performance
- **DH Parameters**: 2048-bit for perfect forward secrecy

### Security Headers

- **HSTS**: Forces HTTPS for 1 year
- **CSP**: Content Security Policy
- **X-Frame-Options**: Prevents clickjacking
- **X-Content-Type-Options**: Prevents MIME sniffing
- **X-XSS-Protection**: XSS protection

## Troubleshooting

### Certificate Not Found Error

If nginx fails to start with "certificate not found":
1. Check certificates exist: `ls -la ssl/`
2. Ensure `cert.crt` and `cert.key` exist (they're auto-created)
3. Check file permissions: `chmod 644 ssl/*.crt && chmod 600 ssl/*.key`

### Browser Shows "Not Secure"

- **Self-signed certificates**: This is normal. Click "Advanced" → "Proceed"
- **Let's Encrypt**: Check domain points to your server IP
- **Mixed content**: Ensure all resources use HTTPS

### Let's Encrypt Challenge Fails

1. Check port 80 is accessible: `curl http://yourdomain.com/.well-known/acme-challenge/test`
2. Verify DNS: `nslookup yourdomain.com`
3. Check firewall: `sudo ufw status`
4. Ensure nginx is stopped during certificate generation

### Certificate Expired

Let's Encrypt certificates expire after 90 days:
```bash
# Renew manually
sudo certbot renew

# Restart frontend
docker-compose restart frontend
```

## Testing SSL Configuration

### Test SSL Certificate
```bash
openssl x509 -in ssl/localhost.crt -text -noout
```

### Test SSL Connection
```bash
# Test HTTPS
curl -k https://localhost

# Test with certificate validation
curl --cacert ssl/localhost.crt https://localhost
```

### Check SSL Rating
Visit: https://www.ssllabs.com/ssltest/analyze.html?d=yourdomain.com

## Production Checklist

- [ ] Domain name configured and pointing to server
- [ ] Ports 80 and 443 open in firewall
- [ ] Let's Encrypt certificates generated
- [ ] Auto-renewal configured (cron job)
- [ ] HTTPS URLs configured in `.env`
- [ ] Backend CORS configured for HTTPS origin
- [ ] SSL rating checked (A+ rating)
- [ ] Security headers verified
- [ ] Mixed content issues resolved

## Additional Resources

- [Let's Encrypt Documentation](https://letsencrypt.org/docs/)
- [SSL Labs SSL Test](https://www.ssllabs.com/ssltest/)
- [Mozilla SSL Configuration Generator](https://ssl-config.mozilla.org/)
- [OWASP Transport Layer Protection](https://cheatsheetseries.owasp.org/cheatsheets/Transport_Layer_Protection_Cheat_Sheet.html)

