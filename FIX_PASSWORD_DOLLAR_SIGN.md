# Fix Password with Dollar Sign ($) in Docker Compose

## Problem

Docker Compose interprets `$VARIABLE` as environment variable substitution. If your password contains `$`, Docker Compose will try to substitute it.

**Example:**
```env
MAIL_PASSWORD=wuf202019$WUF
```

Docker Compose sees `$WUF` and tries to find an environment variable named `WUF`. Since it doesn't exist, it replaces it with an empty string, making the password `wuf202019` instead of `wuf202019$WUF`.

## Solution

Escape the dollar sign by doubling it (`$$`):

```env
MAIL_PASSWORD=wuf202019$$WUF
```

Docker Compose will interpret `$$` as a literal `$`, so the password becomes `wuf202019$WUF`.

## Steps to Fix

1. **Edit your `.env` file:**
   ```bash
   nano .env
   ```

2. **Find the line:**
   ```env
   MAIL_PASSWORD=wuf202019$WUF
   ```

3. **Change it to:**
   ```env
   MAIL_PASSWORD=wuf202019$$WUF
   ```

4. **Save and restart backend:**
   ```bash
   docker compose restart backend
   ```

5. **Verify:**
   ```bash
   docker compose exec backend env | grep MAIL_PASSWORD
   # Should show: MAIL_PASSWORD=wuf202019$WUF
   ```

## Alternative Solutions

### Option 1: Use Quotes (May not work in all cases)
```env
MAIL_PASSWORD="wuf202019$WUF"
```

### Option 2: Use Different Password
If escaping doesn't work, consider changing the password in Hostinger to one without `$`.

## Verification

After fixing, check backend logs:
```bash
docker compose logs backend | grep -i "mail\|email" | tail -10
```

You should NOT see warnings about "WUF variable is not set" anymore.

## Other Variables That Might Need Escaping

Check your `.env` file for any other values with `$`:
```bash
grep '\$' .env
```

Common ones that might need escaping:
- Passwords with `$`
- URLs with `$` (rare)
- Any configuration value containing `$`

