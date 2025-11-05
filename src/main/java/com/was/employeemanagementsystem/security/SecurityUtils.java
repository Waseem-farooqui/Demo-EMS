package com.was.employeemanagementsystem.security;

import com.was.employeemanagementsystem.entity.User;
import com.was.employeemanagementsystem.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    public User getCurrentUser() {
        String username = getCurrentUsername();
        if (username != null) {
            Optional<User> user = userRepository.findByUsername(username);
            return user.orElse(null);
        }
        return null;
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            var authorities = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            log.debug("Checking isAdmin() for user: {}, authorities: {}",
                     authentication.getName(), authorities);
            boolean result = authorities.contains("ROLE_ADMIN");
            log.debug("isAdmin() result: {}", result);
            return result;
        }
        return false;
    }

    public boolean isSuperAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            var authorities = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            log.debug("Checking isSuperAdmin() for user: {}, authorities: {}",
                     authentication.getName(), authorities);
            boolean result = authorities.contains("ROLE_SUPER_ADMIN");
            log.debug("isSuperAdmin() result: {}", result);
            return result;
        }
        return false;
    }

    public boolean isAdminOrSuperAdmin() {
        boolean admin = isAdmin();
        boolean superAdmin = isSuperAdmin();
        log.debug("isAdminOrSuperAdmin() - isAdmin: {}, isSuperAdmin: {}, result: {}",
                 admin, superAdmin, admin || superAdmin);
        return admin || superAdmin;
    }

    public boolean isRoot() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            var authorities = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            log.debug("Checking isRoot() for user: {}, authorities: {}",
                     authentication.getName(), authorities);
            boolean result = authorities.contains("ROLE_ROOT");
            log.debug("isRoot() result: {}", result);
            return result;
        }
        return false;
    }

    public Long getCurrentUserOrganizationId() {
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            return currentUser.getOrganizationId();
        }
        return null;
    }

    public String getCurrentUserOrganizationUuid() {
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            return currentUser.getOrganizationUuid();
        }
        return null;
    }

    public boolean belongsToSameOrganization(Long organizationId) {
        // ROOT can access all organizations
        if (isRoot()) {
            return true;
        }

        Long currentOrgId = getCurrentUserOrganizationId();
        return currentOrgId != null && currentOrgId.equals(organizationId);
    }

    public boolean belongsToOrganizationUuid(String organizationUuid) {
        // ROOT can access all organizations
        if (isRoot()) {
            return true;
        }

        String currentOrgUuid = getCurrentUserOrganizationUuid();
        return currentOrgUuid != null && currentOrgUuid.equals(organizationUuid);
    }

    public boolean isUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(role -> role.equals("ROLE_USER"));
        }
        return false;
    }

    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(authority -> authority.equals("ROLE_" + role));
        }
        return false;
    }

    public User getUserById(Long userId) {
        if (userId == null) {
            return null;
        }
        Optional<User> user = userRepository.findById(userId);
        return user.orElse(null);
    }
}

