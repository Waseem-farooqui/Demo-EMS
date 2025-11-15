# Final Password Fix - Step by Step

## The Problem
Your `.env` has `DB_PASSWORD=wud19@WUD`, but the backend is using `emspassword` (the default).

## Root Cause
Spring Boot reads `SPRING_DATASOURCE_PASSWORD` first. If it's not in `.env`, `compose.yaml` sets it to `emspassword` (default), ignoring your `DB_PASSWORD`.

## Solution

### Step 1: Update your .env file

Add this line to your `.env` file:
```bash
SPRING_DATASOURCE_PASSWORD=wud19@WUD
```

Your `.env` should have:
```bash
DB_PASSWORD=wud19@WUD
SPRING_DATASOURCE_PASSWORD=wud19@WUD
DB_USERNAME=emsuser
DB_ROOT_PASSWORD=your_root_password
```

### Step 2: Fix MySQL user password

Connect to MySQL and update the password:

```bash
# Try one of these (whichever works):
docker-compose exec mysql mysql -u root -prootpassword -e "
ALTER USER 'emsuser'@'%' IDENTIFIED BY 'wud19@WUD';
FLUSH PRIVILEGES;
"

# Or try:
docker-compose exec mysql mysql -u root -proot -e "
ALTER USER 'emsuser'@'%' IDENTIFIED BY 'wud19@WUD';
FLUSH PRIVILEGES;
"

# Or connect interactively:
docker-compose exec mysql mysql -u root -p
# Then run:
# ALTER USER 'emsuser'@'%' IDENTIFIED BY 'wud19@WUD';
# FLUSH PRIVILEGES;
# EXIT;
```

### Step 3: Restart everything

```bash
docker-compose down
docker-compose up -d
```

### Step 4: Verify

```bash
docker-compose logs -f backend
```

You should see: `HikariPool-1 - Start completed` (not errors)

## Why This Works

1. `.env` sets `SPRING_DATASOURCE_PASSWORD=wud19@WUD`
2. `compose.yaml` passes it to backend container
3. Spring Boot reads `SPRING_DATASOURCE_PASSWORD` from environment
4. MySQL user has password `wud19@WUD`
5. ✅ Match! Connection succeeds.

## Quick One-Liner Fix

If you want to do it all at once:

```bash
# Fix MySQL password
docker-compose exec mysql mysql -u root -prootpassword -e "ALTER USER 'emsuser'@'%' IDENTIFIED BY 'wud19@WUD'; FLUSH PRIVILEGES;" 2>/dev/null || \
docker-compose exec mysql mysql -u root -proot -e "ALTER USER 'emsuser'@'%' IDENTIFIED BY 'wud19@WUD'; FLUSH PRIVILEGES;" 2>/dev/null || \
echo "Please connect manually: docker-compose exec mysql mysql -u root -p"

# Add to .env (if not already there)
echo "SPRING_DATASOURCE_PASSWORD=wud19@WUD" >> .env

# Restart
docker-compose restart backend
```

