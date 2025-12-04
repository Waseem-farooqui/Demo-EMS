# Multi-stage build for Employee Management System Backend

# Stage 1: Build
FROM maven:3.8-openjdk-11-slim AS build
WORKDIR /app

# Install network tools for troubleshooting
RUN apt-get update && \
    apt-get install -y dnsutils iputils-ping curl && \
    rm -rf /var/lib/apt/lists/* || true

# Test DNS resolution before Maven build
RUN echo "Testing DNS resolution..." && \
    (nslookup repo.maven.apache.org || echo "DNS test completed") && \
    (ping -c 2 repo.maven.apache.org || echo "Ping test completed") || true

# Copy pom.xml and download dependencies with retry logic and timeout settings
COPY pom.xml .
RUN mvn dependency:go-offline -B \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.http.retryHandler.interval=10000 \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 \
    || (echo "First attempt failed, waiting 10 seconds and retrying..." && \
        sleep 10 && \
        mvn dependency:go-offline -B \
        -Dmaven.wagon.http.retryHandler.count=5 \
        -Dmaven.wagon.http.retryHandler.interval=10000 \
        -Dmaven.wagon.httpconnectionManager.ttlSeconds=120) || \
    (echo "Second attempt failed, waiting 20 seconds and retrying with verbose logging..." && \
        sleep 20 && \
        mvn dependency:go-offline -B -X \
        -Dmaven.wagon.http.retryHandler.count=5 \
        -Dmaven.wagon.http.retryHandler.interval=10000 \
        -Dmaven.wagon.httpconnectionManager.ttlSeconds=120)

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:11-jre

# Install Tesseract OCR, curl for health checks, netcat for MySQL wait script, and mysql-client for MySQL connectivity testing
# Add ImageMagick for better image processing performance
RUN apt-get update && \
    apt-get install -y tesseract-ocr tesseract-ocr-eng curl netcat-openbsd default-mysql-client imagemagick && \
    rm -rf /var/lib/apt/lists/*

# Configure ImageMagick for 8GB RAM system (reduced memory limits)
RUN sed -i 's/<policy domain="resource" name="memory" value="256MiB"\/>/<policy domain="resource" name="memory" value="256MiB"\/>/' /etc/ImageMagick-6/policy.xml 2>/dev/null || true && \
    sed -i 's/<policy domain="resource" name="map" value="512MiB"\/>/<policy domain="resource" name="map" value="512MiB"\/>/' /etc/ImageMagick-6/policy.xml 2>/dev/null || true

# Set JVM options for document processing workloads
# Optimized for 8GB RAM system: Reduced heap to 2GB to leave room for MySQL and system
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication -XX:+OptimizeStringConcat -Djava.awt.headless=true"

# Detect and set Tesseract tessdata path
# Tesseract 5.x uses /usr/share/tesseract-ocr/5/tessdata
# Tesseract 4.x uses /usr/share/tesseract-ocr/4.00/tessdata
RUN TESS_DATA_PATH="" && \
    if [ -d "/usr/share/tesseract-ocr/5/tessdata" ]; then \
        TESS_DATA_PATH="/usr/share/tesseract-ocr/5/tessdata"; \
    elif [ -d "/usr/share/tesseract-ocr/4.00/tessdata" ]; then \
        TESS_DATA_PATH="/usr/share/tesseract-ocr/4.00/tessdata"; \
    elif [ -d "/usr/share/tesseract-ocr/tessdata" ]; then \
        TESS_DATA_PATH="/usr/share/tesseract-ocr/tessdata"; \
    else \
        TESS_DATA_PATH="/usr/share/tesseract-ocr/5/tessdata"; \
    fi && \
    echo "TESSDATA_PREFIX=${TESS_DATA_PATH}" >> /etc/environment && \
    echo "Detected Tesseract tessdata at: ${TESS_DATA_PATH}" && \
    ls -la "${TESS_DATA_PATH}/eng.traineddata" 2>/dev/null || echo "WARNING: eng.traineddata not found at ${TESS_DATA_PATH}"

# Set default TESSDATA_PREFIX (can be overridden by environment variable)
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata

# Create app directory
WORKDIR /app

# Create wait script inline (avoids COPY issues)
# This script waits for MySQL to be ready by checking both port and actual MySQL connectivity
RUN printf '#!/bin/bash\n\
set -e\n\
host="$1"\n\
port="$2"\n\
shift 2\n\
echo "Waiting for MySQL at $host:$port..."\n\
# First wait for port to be open\n\
MAX_PORT_ATTEMPTS=30\n\
PORT_ATTEMPT=0\n\
while [ $PORT_ATTEMPT -lt $MAX_PORT_ATTEMPTS ]; do\n\
  if nc -z "$host" "$port" 2>/dev/null; then\n\
    >&2 echo "MySQL port is open"\n\
    break\n\
  fi\n\
  PORT_ATTEMPT=$((PORT_ATTEMPT + 1))\n\
  >&2 echo "MySQL port not open yet (attempt $PORT_ATTEMPT/$MAX_PORT_ATTEMPTS) - sleeping"\n\
  sleep 2\n\
done\n\
if [ $PORT_ATTEMPT -eq $MAX_PORT_ATTEMPTS ]; then\n\
  >&2 echo "ERROR: MySQL port never opened, proceeding anyway..."\n\
  exec "$@"\n\
fi\n\
# Then wait for MySQL to actually accept connections using mysqladmin ping\n\
>&2 echo "MySQL port is open, waiting for MySQL to accept connections..."\n\
MAX_MYSQL_ATTEMPTS=60\n\
MYSQL_ATTEMPT=0\n\
while [ $MYSQL_ATTEMPT -lt $MAX_MYSQL_ATTEMPTS ]; do\n\
  if mysqladmin ping -h "$host" -P "$port" --silent 2>/dev/null; then\n\
    >&2 echo "MySQL is ready and accepting connections - executing command"\n\
    exec "$@"\n\
  fi\n\
  MYSQL_ATTEMPT=$((MYSQL_ATTEMPT + 1))\n\
  >&2 echo "MySQL not ready yet (attempt $MYSQL_ATTEMPT/$MAX_MYSQL_ATTEMPTS) - sleeping"\n\
  sleep 2\n\
done\n\
>&2 echo "WARNING: MySQL wait timeout after $MAX_MYSQL_ATTEMPTS attempts, proceeding anyway..."\n\
exec "$@"\n' > /app/wait-for-mysql.sh && \
    chmod +x /app/wait-for-mysql.sh

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Create uploads directory
RUN mkdir -p /app/uploads && \
    chmod 755 /app/uploads

# Create non-root user
RUN useradd -r -u 1001 -g root appuser && \
    chown -R appuser:root /app && \
    chmod +x /app/wait-for-mysql.sh

USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s \
  CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# Run application with MySQL wait script
# Note: Use 'mysql' (service name) not 'ems-mysql' (container name) for DNS resolution
# The wait script will ensure MySQL is ready before starting Spring Boot
# JVM options optimized for document processing: G1GC, memory settings, headless mode
ENTRYPOINT ["/app/wait-for-mysql.sh", "mysql", "3306", "sh", "-c", \
        "exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=prod -jar app.jar"]

