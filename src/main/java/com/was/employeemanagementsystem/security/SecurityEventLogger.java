package com.was.employeemanagementsystem.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Security Event Logger
 * Logs security events to a file that can be monitored by Fail2Ban
 */
@Slf4j
@Component
public class SecurityEventLogger {

    private static final String SECURITY_LOG_FILE = "/var/log/fail2ban-spring-boot.log";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Log a blocked malicious request
     */
    public void logBlockedRequest(HttpServletRequest request, String reason) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String ip = getClientIpAddress(request);
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String userAgent = request.getHeader("User-Agent");
        String referer = request.getHeader("Referer");

        String logMessage = String.format(
            "[%s] BLOCKED MALICIOUS REQUEST - Method: %s, URI: %s, IP: %s, User-Agent: %s, Referer: %s, Reason: %s",
            timestamp, method, uri, ip, userAgent, referer, reason
        );

        // Log to application log
        log.warn("🚫 {}", logMessage);

        // Log to Fail2Ban log file
        writeToSecurityLog(logMessage);
    }

    /**
     * Log unauthorized access attempt
     */
    public void logUnauthorizedAccess(HttpServletRequest request, String reason) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String ip = getClientIpAddress(request);
        String uri = request.getRequestURI();
        String method = request.getMethod();

        String logMessage = String.format(
            "[%s] UNAUTHORIZED ACCESS ATTEMPT - Method: %s, URI: %s, IP: %s, Reason: %s",
            timestamp, method, uri, ip, reason
        );

        log.warn("🔒 {}", logMessage);
        writeToSecurityLog(logMessage);
    }

    /**
     * Log failed authentication attempt
     */
    public void logFailedAuthentication(HttpServletRequest request, String username) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String ip = getClientIpAddress(request);

        String logMessage = String.format(
            "[%s] FAILED AUTHENTICATION - Username: %s, IP: %s",
            timestamp, username, ip
        );

        log.warn("🔐 {}", logMessage);
        writeToSecurityLog(logMessage);
    }

    /**
     * Log suspicious activity
     */
    public void logSuspiciousActivity(HttpServletRequest request, String activity) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String ip = getClientIpAddress(request);
        String uri = request.getRequestURI();

        String logMessage = String.format(
            "[%s] SUSPICIOUS ACTIVITY - URI: %s, IP: %s, Activity: %s",
            timestamp, uri, ip, activity
        );

        log.warn("⚠️ {}", logMessage);
        writeToSecurityLog(logMessage);
    }

    /**
     * Write log message to security log file
     */
    private void writeToSecurityLog(String message) {
        try {
            // Try to write to Fail2Ban log file
            try (FileWriter writer = new FileWriter(SECURITY_LOG_FILE, true)) {
                writer.write(message + System.lineSeparator());
                writer.flush();
            }
        } catch (IOException e) {
            // If file doesn't exist or can't be written, just log to application log
            log.debug("Could not write to security log file {}: {}", SECURITY_LOG_FILE, e.getMessage());
        }
    }

    /**
     * Extract client IP address from request
     * Handles proxy headers (X-Forwarded-For, X-Real-IP)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}

