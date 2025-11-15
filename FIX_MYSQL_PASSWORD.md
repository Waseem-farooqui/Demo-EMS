# Fix MySQL Password "Access Denied" Error

## Problem
You're getting: `Access denied for user 'emsuser'@'172.18.0.3' (using password: YES)`

This happens when the password in your `.env` file doesn't match what MySQL has stored.

## Quick Fix (Option 1 - Recommended)

### Step 1: Check your .env file
Make sure `DB_PASSWORD` in your `.env` file matches what you want to use.

### Step 2: Reset the MySQL user password

Run this command to fix the password (replace `your_password` with your actual password from `.env`):

```bash
# Get the password from your .env file
DB_PASSWORD=$(grep "^DB_PASSWORD=" .env | cut -d '=' -f2)

# Fix the password in MySQL
docker-compose exec mysql mysql -u root -p"${DB_ROOT_PASSWORD:-rootpassword}" -e "
ALTER USER 'emsuser'@'%' IDENTIFIED BY '${DB_PASSWORD:-emspassword}';
FLUSH PRIVILEGES;
"
```

Or if you know the password is `emspassword`:

```bash
docker-compose exec mysql mysql -u root -p"${DB_ROOT_PASSWORD:-rootpassword}" -e "
ALTER USER 'emsuser'@'%' IDENTIFIED BY 'emspassword';
FLUSH PRIVILEGES;
"
```

### Step 3: Restart backend
```bash
docker-compose restart backend
```

## Alternative Fix (Option 2 - Fresh Start)

If you don't have important data, recreate MySQL with the correct password:

```bash
# Stop and remove MySQL container and volume
docker-compose stop mysql
docker volume rm employeemanagementsystem_mysql_data

# Start MySQL again (will recreate with correct password from .env)
docker-compose up -d mysql

# Wait for MySQL to be ready
sleep 30

# Start backend
docker-compose up -d backend
```

## Verify Fix

Check if the connection works:

```bash
# Check backend logs
docker-compose logs backend | grep -i "hikari\|database\|mysql"

# Should see: "HikariPool-1 - Start completed" (not errors)
```

## Prevention

The updated `init.sql` now explicitly sets the password, so this won't happen on fresh deployments.

