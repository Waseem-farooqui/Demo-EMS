# Hostinger Email Authentication Troubleshooting

## Error: 535 5.7.8 Error: authentication failed

This error indicates that Hostinger SMTP is rejecting your email credentials.

## Quick Fix Steps

### Step 1: Verify Email Account in Hostinger

1. **Log in to Hostinger hPanel:**
   - Go to: https://hpanel.hostinger.com
   - Log in with your Hostinger account

2. **Check Email Account:**
   - Navigate to **Email** section
   - Verify that `support@vertexdigitalsystem.com` exists
   - If it doesn't exist, create it:
     - Click "Create Email Account"
     - Email: `support@vertexdigitalsystem.com`
     - Password: (set a strong password)
     - Note down the password

3. **Verify Email Account is Active:**
   - Make sure the email account is not suspended
   - Check if there are any restrictions

### Step 2: Update .env File

On your production server, edit the `.env` file:

```bash
nano .env
```

**Update these values:**

```env
MAIL_HOST=smtp.hostinger.com
MAIL_PORT=465
MAIL_USERNAME=support@vertexdigitalsystem.com
MAIL_PASSWORD=YOUR_ACTUAL_HOSTINGER_EMAIL_PASSWORD_HERE
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=false
MAIL_SMTP_STARTTLS_REQUIRED=false
MAIL_SMTP_SSL_ENABLE=true
MAIL_SMTP_SSL_TRUST=smtp.hostinger.com
MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=465
EMAIL_FROM_ADDRESS=support@vertexdigitalsystem.com
```

**Important:**
- Use the **full email address** as `MAIL_USERNAME` (not just "support")
- Use the **exact password** you set in Hostinger (case-sensitive)
- No spaces around the `=` sign

### Step 3: Restart Backend

After updating `.env`:

```bash
docker compose restart backend
```

### Step 4: Test Email

1. Check backend logs:
   ```bash
   docker compose logs backend | grep -i mail
   ```

2. Try creating a new user or organization to trigger an email

## Alternative: Try Port 587 (TLS)

If port 465 doesn't work, try port 587:

```env
MAIL_HOST=smtp.hostinger.com
MAIL_PORT=587
MAIL_USERNAME=support@vertexdigitalsystem.com
MAIL_PASSWORD=YOUR_ACTUAL_HOSTINGER_EMAIL_PASSWORD_HERE
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_STARTTLS_REQUIRED=true
MAIL_SMTP_SSL_ENABLE=false
MAIL_SMTP_SSL_TRUST=smtp.hostinger.com
EMAIL_FROM_ADDRESS=support@vertexdigitalsystem.com
```

Then restart:
```bash
docker compose restart backend
```

## Common Issues

### Issue 1: Wrong Password
- **Symptom:** 535 Authentication failed
- **Solution:** 
  - Reset password in Hostinger hPanel
  - Update `.env` with new password
  - Restart backend

### Issue 2: Email Account Doesn't Exist
- **Symptom:** 535 Authentication failed
- **Solution:**
  - Create email account in Hostinger hPanel
  - Wait a few minutes for account activation
  - Update `.env` with correct credentials

### Issue 3: Wrong Username Format
- **Symptom:** 535 Authentication failed
- **Solution:**
  - Use full email: `support@vertexdigitalsystem.com`
  - NOT just: `support`

### Issue 4: Port Blocked
- **Symptom:** Connection timeout
- **Solution:**
  - Try port 587 instead of 465
  - Check firewall settings
  - Verify Hostinger SMTP is accessible

### Issue 5: SSL Configuration Mismatch
- **Symptom:** SSL handshake errors
- **Solution:**
  - For port 465: `MAIL_SMTP_SSL_ENABLE=true`, `MAIL_SMTP_STARTTLS_ENABLE=false`
  - For port 587: `MAIL_SMTP_SSL_ENABLE=false`, `MAIL_SMTP_STARTTLS_ENABLE=true`

## Verify Configuration

Check if environment variables are loaded correctly:

```bash
docker compose exec backend env | grep MAIL
```

You should see:
```
MAIL_HOST=smtp.hostinger.com
MAIL_PORT=465
MAIL_USERNAME=support@vertexdigitalsystem.com
MAIL_PASSWORD=...
MAIL_SMTP_AUTH=true
MAIL_SMTP_SSL_ENABLE=true
```

## Test SMTP Connection

From your server, test SMTP connection:

```bash
# Test port 465
telnet smtp.hostinger.com 465

# Or test port 587
telnet smtp.hostinger.com 587
```

If connection succeeds, SMTP server is reachable.

## Still Not Working?

1. **Check Hostinger Support:**
   - Verify SMTP is enabled for your account
   - Check if there are any account restrictions
   - Contact Hostinger support if needed

2. **Check Backend Logs:**
   ```bash
   docker compose logs backend | tail -50
   ```

3. **Verify Email Account:**
   - Log in to webmail: https://webmail.hostinger.com
   - Use `support@vertexdigitalsystem.com` and password
   - If you can't log in, the account/password is wrong

4. **Try Different Email Account:**
   - Create a test email: `test@vertexdigitalsystem.com`
   - Update `.env` with test credentials
   - See if it works

## Quick Checklist

- [ ] Email account exists in Hostinger hPanel
- [ ] Email account is active (not suspended)
- [ ] Password is correct (case-sensitive)
- [ ] `.env` file has correct `MAIL_PASSWORD` (not "CHANGE_THIS...")
- [ ] `MAIL_USERNAME` is full email address
- [ ] Backend restarted after `.env` update
- [ ] Port 465 or 587 is accessible
- [ ] SSL settings match the port (465=SSL, 587=TLS)

