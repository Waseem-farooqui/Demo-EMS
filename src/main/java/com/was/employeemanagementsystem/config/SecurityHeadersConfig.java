package com.was.employeemanagementsystem.config;

import org.springframework.context.annotation.Configuration;

/**
 * Security Headers Configuration
 *
 * Security headers are configured in SecurityConfig.java to protect against:
 * - XSS (Cross-Site Scripting) attacks
 * - Clickjacking attacks
 * - MIME-sniffing vulnerabilities
 * - Cross-origin attacks
 *
 * Headers configured:
 * - Content-Security-Policy: Restricts resource loading
 * - X-Content-Type-Options: Prevents MIME-sniffing
 * - X-Frame-Options: Prevents clickjacking
 * - Strict-Transport-Security: Forces HTTPS
 *
 * Note: These headers are OWASP recommended for web application security.
 */
@Configuration
public class SecurityHeadersConfig {
    // Security headers are configured in SecurityConfig.configure(HttpSecurity http)
    // This class serves as documentation for the security headers implementation
}



