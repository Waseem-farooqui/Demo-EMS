# Fix MySQL Root Access Denied

## Problem
Cannot connect to MySQL as root to fix the emsuser password.

## Solution Options

### Option 1: Check Your .env File

The root password is set by `DB_ROOT_PASSWORD` in your `.env` file:

```bash
# Check what root password is in .env
grep DB_ROOT_PASSWORD .env
```

Then use that password:
```bash
docker-compose exec mysql mysql -u root -p"YOUR_ACTUAL_ROOT_PASSWORD" -e "
ALTER USER 'emsuser'@'%' IDENTIFIED BY 'wud19@WUD';
FLUSH PRIVILEGES;
"
```

### Option 2: Connect Interactively

This will prompt you for the password:

```bash
docker-compose exec mysql mysql -u root -p
```

When prompted, try:
- The password from your `.env` file (`DB_ROOT_PASSWORD`)
- Common defaults: `rootpassword`, `root`, or just press Enter (empty)

Once connected, run:
```sql
ALTER USER 'emsuser'@'%' IDENTIFIED BY 'wud19@WUD';
GRANT ALL PRIVILEGES ON employee_management_system.* TO 'emsuser'@'%';
GRANT CREATE ON *.* TO 'emsuser'@'%';
FLUSH PRIVILEGES;
EXIT;
```

### Option 3: Use the Find Script

I created `find-root-password.sh` that tries common passwords:

```bash
chmod +x find-root-password.sh
./find-root-password.sh
```

### Option 4: Reset MySQL Container (Last Resort)

**⚠️ WARNING: This will delete all data!**

```bash
# Stop MySQL
docker-compose stop mysql

# Remove MySQL data volume
docker volume rm employeemanagementsystem_mysql_data

# Make sure .env has correct passwords:
# DB_ROOT_PASSWORD=your_desired_root_password
# DB_PASSWORD=wud19@WUD

# Start MySQL fresh
docker-compose up -d mysql

# Wait for initialization
sleep 30

# Start backend
docker-compose up -d backend
```

### Option 5: Access MySQL Without Root (If emsuser works)

If you can connect as emsuser with the current password, you can use that to check, but you'll need root to change passwords.

## After Fixing Root Access

Once you can connect as root, run:

```sql
ALTER USER 'emsuser'@'%' IDENTIFIED BY 'wud19@WUD';
GRANT ALL PRIVILEGES ON employee_management_system.* TO 'emsuser'@'%';
GRANT CREATE ON *.* TO 'emsuser'@'%';
FLUSH PRIVILEGES;
```

Then make sure your `.env` has:
```bash
DB_PASSWORD=wud19@WUD
SPRING_DATASOURCE_PASSWORD=wud19@WUD
```

Then restart backend:
```bash
docker-compose restart backend
```

