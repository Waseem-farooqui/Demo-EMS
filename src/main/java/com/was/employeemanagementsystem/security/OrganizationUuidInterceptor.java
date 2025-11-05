package com.was.employeemanagementsystem.security;

import com.was.employeemanagementsystem.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interceptor to validate organization UUID in requests
 * Ensures all non-ROOT users must have valid organization UUID
 * Prevents cross-organization data access
 */
@Slf4j
@Component
public class OrganizationUuidInterceptor implements HandlerInterceptor {

    private final SecurityUtils securityUtils;

    public OrganizationUuidInterceptor(SecurityUtils securityUtils) {
        this.securityUtils = securityUtils;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        // Skip validation for public endpoints
        String requestUri = request.getRequestURI();

        if (isPublicEndpoint(requestUri)) {
            log.debug("✓ Skipping organization UUID validation for public endpoint: {}", requestUri);
            return true;
        }

        // Skip validation for ROOT-specific endpoints
        if (requestUri.startsWith("/api/root/") || requestUri.startsWith("/api/init/")) {
            log.debug("✓ Skipping organization UUID validation for ROOT endpoint: {}", requestUri);
            return true;
        }

        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            log.debug("✓ No authentication found, allowing request to proceed to auth filters");
            return true;
        }

        // ROOT user doesn't need organization UUID
        if (securityUtils.isRoot()) {
            log.debug("✓ ROOT user accessing {}, no organization UUID required", requestUri);
            return true;
        }

        // Get current user's organization UUID
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            log.warn("⚠️ Authenticated user not found in database");
            throw new AccessDeniedException("User not found");
        }

        String userOrgUuid = currentUser.getOrganizationUuid();

        // All non-ROOT users MUST have organization UUID
        if (userOrgUuid == null || userOrgUuid.isEmpty()) {
            log.error("❌ User {} has no organization UUID! This should never happen for non-ROOT users",
                     currentUser.getUsername());
            throw new AccessDeniedException("User must be associated with an organization");
        }

        // Optional: Check for X-Organization-UUID header and validate it matches
        String headerOrgUuid = request.getHeader("X-Organization-UUID");
        if (headerOrgUuid != null && !headerOrgUuid.isEmpty()) {
            if (!userOrgUuid.equals(headerOrgUuid)) {
                log.error("❌ Organization UUID mismatch! User: {}, Header: {}, Endpoint: {}",
                         userOrgUuid, headerOrgUuid, requestUri);
                throw new AccessDeniedException("Organization UUID mismatch");
            }
            log.debug("✓ Organization UUID validated from header: {}", userOrgUuid);
        } else {
            log.debug("✓ Organization UUID validated from user session: {}", userOrgUuid);
        }

        // Store organization UUID in request attribute for easy access in controllers
        request.setAttribute("organizationUuid", userOrgUuid);
        request.setAttribute("organizationId", currentUser.getOrganizationId());

        return true;
    }

    /**
     * Check if endpoint is public (doesn't require organization UUID)
     */
    private boolean isPublicEndpoint(String uri) {
        return uri.startsWith("/api/auth/") ||
               uri.startsWith("/api/init/") ||
               uri.equals("/api/health") ||
               uri.equals("/error");
    }
}

