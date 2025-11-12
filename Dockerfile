# Multi-stage build for Employee Management System Backend

# Stage 1: Build
FROM maven:3.8-openjdk-11-slim AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -Pprod

# Stage 2: Runtime
FROM openjdk:11-jre-slim

# Install Tesseract OCR and curl for health checks
RUN apt-get update && \
    apt-get install -y tesseract-ocr tesseract-ocr-eng curl && \
    rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Create uploads directory
RUN mkdir -p /app/uploads && \
    chmod 755 /app/uploads

# Create non-root user
RUN useradd -r -u 1001 -g root appuser && \
    chown -R appuser:root /app

USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s \
  CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "-jar", \
    "app.jar"]

