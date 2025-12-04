package com.was.employeemanagementsystem.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Security Request Filter
 * Logs potentially malicious requests but lets Spring Security handle blocking
 * This filter only logs suspicious patterns - StrictHttpFirewall does the actual blocking
 */
@Slf4j
@Component
@Order(1) // Run before Spring Security filters
public class SecurityRequestFilter extends OncePerRequestFilter {

    @Autowired(required = false)
    private SecurityEventLogger securityEventLogger;

    // Patterns for potentially malicious requests (only in query string or path, not in normal API paths)
    // These are more specific to catch actual attacks, not legitimate API calls
    private static final Pattern[] MALICIOUS_PATTERNS = {
        // Only block semicolons in query strings or unusual positions (not in normal paths)
        Pattern.compile(".*[?&][^=]*[;].*", Pattern.CASE_INSENSITIVE), // Semicolons in query params
        Pattern.compile(".*[<].*[>].*", Pattern.CASE_INSENSITIVE), // HTML tags
        Pattern.compile(".*union.*select.*", Pattern.CASE_INSENSITIVE), // SQL injection
        Pattern.compile(".*drop.*table.*", Pattern.CASE_INSENSITIVE), // SQL injection
        Pattern.compile(".*exec.*\\(.*", Pattern.CASE_INSENSITIVE), // Command execution with parentheses
        Pattern.compile(".*eval.*\\(.*", Pattern.CASE_INSENSITIVE), // Code evaluation
        Pattern.compile(".*javascript:.*", Pattern.CASE_INSENSITIVE), // JavaScript protocol
        Pattern.compile(".*\\.\\./.*\\.\\./.*", Pattern.CASE_INSENSITIVE), // Multiple path traversal
        Pattern.compile(".*%00.*", Pattern.CASE_INSENSITIVE), // Null byte
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullRequest = requestURI + (queryString != null ? "?" + queryString : "");

        // Only check query string and path for malicious patterns
        // Don't block legitimate API paths - let StrictHttpFirewall handle URL structure
        boolean isMalicious = false;
        String matchedPattern = null;

        // Check query string for malicious patterns
        if (queryString != null) {
            for (Pattern pattern : MALICIOUS_PATTERNS) {
                if (pattern.matcher(queryString).matches()) {
                    isMalicious = true;
                    matchedPattern = pattern.pattern();
                    break;
                }
            }
        }

        // Check URI for path traversal (but allow normal API paths)
        if (!isMalicious && requestURI != null) {
            // Only block if URI contains multiple ../ or suspicious patterns
            if (requestURI.matches(".*\\.\\./.*\\.\\./.*") || 
                requestURI.matches(".*[<].*[>].*") ||
                requestURI.matches(".*%00.*")) {
                isMalicious = true;
                matchedPattern = "Path traversal or encoded attack";
            }
        }

        // Log malicious patterns but let Spring Security handle blocking
        // StrictHttpFirewall will block URLs with semicolons and other dangerous chars
        if (isMalicious) {
            String reason = "Malicious pattern detected: " + matchedPattern;
            log.warn("🚫 Suspicious request detected - URI: {}, Pattern: {}, IP: {}", 
                    fullRequest, matchedPattern, getClientIpAddress(request));
            
            if (securityEventLogger != null) {
                securityEventLogger.logSuspiciousActivity(request, reason);
            }
            
            // Don't block here - let StrictHttpFirewall handle it
            // This way authenticated requests with valid tokens can still pass
            // but malicious URL structures will be blocked by the firewall
        }

        // Always continue with the filter chain
        // StrictHttpFirewall will block malicious URLs at the security layer
        filterChain.doFilter(request, response);
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}

