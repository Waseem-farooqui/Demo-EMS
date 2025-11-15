# Fix MySQL Root Password "Access Denied" Error

## Problem
You're getting: `Access denied for user 'root'@'localhost' (using password: YES)`

This means the `DB_ROOT_PASSWORD` in your `.env` file doesn't match what MySQL was initialized with.

## Quick Fix Options

### Option 1: Manual SQL Fix (Recommended if you know the root password)

1. **Connect to MySQL interactively:**
   ```bash
   docker-compose exec mysql mysql -u root -p
   ```
   Enter the root password when prompted (try the one from your `.env` file, or common defaults like `rootpassword`, `root`, or empty)

2. **Once connected, run these SQL commands:**
   ```sql
   ALTER USER 'emsuser'@'%' IDENTIFIED BY 'wud19@WUD';
   GRANT ALL PRIVILEGES ON employee_management_system.* TO 'emsuser'@'%';
   GRANT CREATE ON *.* TO 'emsuser'@'%';
   FLUSH PRIVILEGES;
   EXIT;
   ```

3. **Restart backend:**
   ```bash
   docker-compose restart backend
   ```

### Option 2: Reset Root Password (If you don't know it)

If you can't connect with root, you can reset MySQL container:

```bash
# Stop MySQL
docker-compose stop mysql

# Remove MySQL volume (WILL DELETE ALL DATA!)
docker volume rm employeemanagementsystem_mysql_data

# Update .env file with correct passwords:
# DB_ROOT_PASSWORD=your_desired_root_password
# DB_PASSWORD=wud19@WUD

# Start MySQL again (will recreate with passwords from .env)
docker-compose up -d mysql

# Wait for MySQL to initialize
sleep 30

# Start backend
docker-compose up -d backend
```

### Option 3: Use MySQL without root password (Temporary)

If MySQL was initialized without a root password, try:

```bash
docker-compose exec mysql mysql -u root <<EOF
ALTER USER 'emsuser'@'%' IDENTIFIED BY 'wud19@WUD';
GRANT ALL PRIVILEGES ON employee_management_system.* TO 'emsuser'@'%';
GRANT CREATE ON *.* TO 'emsuser'@'%';
FLUSH PRIVILEGES;
EOF
```

## Verify Your .env File

Make sure your `.env` file has:
```bash
DB_ROOT_PASSWORD=your_actual_root_password
DB_PASSWORD=wud19@WUD
DB_USERNAME=emsuser
```

## After Fixing

Check backend logs to verify connection:
```bash
docker-compose logs -f backend
```

You should see: `HikariPool-1 - Start completed` (not errors)

