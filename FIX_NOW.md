# Quick Fix - Using Root Password wuf27@1991

## Run This Command

```bash
sudo docker-compose exec mysql mysql -u root -p"wuf27@1991" -e "
ALTER USER 'emsuser'@'%' IDENTIFIED BY 'wud19@WUD';
GRANT ALL PRIVILEGES ON employee_management_system.* TO 'emsuser'@'%';
GRANT CREATE ON *.* TO 'emsuser'@'%';
FLUSH PRIVILEGES;
"
```

## Then Update .env File

Make sure your `.env` file has:
```bash
DB_ROOT_PASSWORD=wuf27@1991
DB_PASSWORD=wud19@WUD
SPRING_DATASOURCE_PASSWORD=wud19@WUD
DB_USERNAME=emsuser
```

## Then Restart Backend

```bash
sudo docker-compose stop backend
sudo docker-compose rm -f backend
sudo docker-compose up -d backend
```

## Or Use the Automated Script

I created `fix-password-now.sh` that does everything:

```bash
chmod +x fix-password-now.sh
sudo ./fix-password-now.sh
```

## Verify

```bash
sudo docker-compose logs -f backend
```

You should see: `HikariPool-1 - Start completed` (not errors)

