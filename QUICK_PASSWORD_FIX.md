# Quick Password Fix Guide

## Current Situation
- Your `init.sql` uses password: `wud19@WUD`
- Backend is getting "Access denied" error
- This means the password in MySQL doesn't match what backend is using

## Immediate Fix

### Step 1: Check your .env file
Make sure it has:
```bash
DB_PASSWORD=wud19@WUD
DB_ROOT_PASSWORD=your_root_password_here
```

### Step 2: Connect to MySQL and fix the password

**Option A: If you know the root password**

```bash
# Connect to MySQL
docker-compose exec mysql mysql -u root -p
# Enter your root password when prompted
```

Then run:
```sql
ALTER USER 'emsuser'@'%' IDENTIFIED BY 'wud19@WUD';
GRANT ALL PRIVILEGES ON employee_management_system.* TO 'emsuser'@'%';
GRANT CREATE ON *.* TO 'emsuser'@'%';
FLUSH PRIVILEGES;
EXIT;
```

**Option B: Try common root passwords**

```bash
# Try these one by one:
docker-compose exec mysql mysql -u root -prootpassword
docker-compose exec mysql mysql -u root -proot
docker-compose exec mysql mysql -u root -p  # (then press Enter for empty password)
```

Once connected, run the SQL commands above.

### Step 3: Verify .env file has correct password

Edit your `.env` file and make sure:
```bash
DB_PASSWORD=wud19@WUD
```

### Step 4: Restart backend

```bash
docker-compose restart backend
```

### Step 5: Check logs

```bash
docker-compose logs -f backend
```

You should see: `HikariPool-1 - Start completed` (not errors)

## Alternative: Use the verification script

I created `verify-and-fix-password.sh` that will:
1. Check your .env file
2. Try to find the correct root password
3. Fix the emsuser password automatically
4. Verify it works
5. Restart the backend

Run it:
```bash
chmod +x verify-and-fix-password.sh
./verify-and-fix-password.sh
```

## If Nothing Works: Fresh Start

If you don't have important data:

```bash
# Stop everything
docker-compose down -v

# Make sure .env has:
# DB_PASSWORD=wud19@WUD
# DB_ROOT_PASSWORD=your_desired_root_password

# Start fresh
docker-compose up -d
```

