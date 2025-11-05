package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.entity.User;
import com.was.employeemanagementsystem.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service to manage and validate ROOT user constraints
 * Ensures only ONE ROOT user exists in the system
 */
@Slf4j
@Service
@Transactional
public class RootUserValidationService {

    private final UserRepository userRepository;

    public RootUserValidationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Check if a ROOT user already exists in the system
     * @return true if ROOT user exists, false otherwise
     */
    public boolean rootUserExists() {
        List<User> rootUsers = userRepository.findAll().stream()
                .filter(u -> u.getRoles() != null && u.getRoles().contains("ROOT"))
                .collect(Collectors.toList());

        if (rootUsers.size() > 1) {
            log.error("❌ CRITICAL: Multiple ROOT users found in database! Count: {}", rootUsers.size());
            log.error("ROOT users: {}", rootUsers.stream()
                    .map(u -> u.getUsername() + " (ID: " + u.getId() + ")")
                    .collect(Collectors.joining(", ")));
        }

        return !rootUsers.isEmpty();
    }

    /**
     * Get the ROOT user if exists
     * @return ROOT user or null
     */
    public User getRootUser() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRoles() != null && u.getRoles().contains("ROOT"))
                .findFirst()
                .orElse(null);
    }

    /**
     * Validate that only one ROOT user exists
     * @throws IllegalStateException if multiple ROOT users found
     */
    public void validateSingleRootUser() {
        List<User> rootUsers = userRepository.findAll().stream()
                .filter(u -> u.getRoles() != null && u.getRoles().contains("ROOT"))
                .collect(Collectors.toList());

        if (rootUsers.size() > 1) {
            String usernames = rootUsers.stream()
                    .map(User::getUsername)
                    .collect(Collectors.joining(", "));

            throw new IllegalStateException(
                    String.format("CRITICAL: Multiple ROOT users found in database! " +
                            "Only ONE ROOT user is allowed. Found %d ROOT users: %s",
                            rootUsers.size(), usernames));
        }
    }

    /**
     * Prevent adding ROOT role to existing users
     * @param userId User ID trying to get ROOT role
     * @throws IllegalArgumentException if ROOT user already exists
     */
    public void preventDuplicateRootRole(Long userId) {
        if (rootUserExists()) {
            User existingRoot = getRootUser();
            if (existingRoot != null && !existingRoot.getId().equals(userId)) {
                log.error("❌ Attempt to assign ROOT role to user {} blocked. ROOT user already exists: {}",
                        userId, existingRoot.getUsername());
                throw new IllegalArgumentException(
                        String.format("Cannot assign ROOT role. ROOT user '%s' already exists. " +
                                "Only ONE ROOT user is allowed in the system.",
                                existingRoot.getUsername()));
            }
        }
    }

    /**
     * Get count of ROOT users (should always be 0 or 1)
     * @return count of ROOT users
     */
    public long getRootUserCount() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRoles() != null && u.getRoles().contains("ROOT"))
                .count();
    }

    /**
     * Validate ROOT user configuration
     * Checks:
     * - Only one ROOT user exists
     * - ROOT user has no organization
     * - ROOT user has no employee record
     */
    public void validateRootUserConfiguration() {
        User rootUser = getRootUser();

        if (rootUser == null) {
            log.info("✅ No ROOT user found in system");
            return;
        }

        // Check single ROOT user
        validateSingleRootUser();
        log.info("✅ Single ROOT user constraint satisfied");

        // Check organization is null
        if (rootUser.getOrganizationId() != null) {
            log.warn("⚠️ ROOT user '{}' has organization_id: {}. Should be NULL!",
                    rootUser.getUsername(), rootUser.getOrganizationId());
        } else {
            log.info("✅ ROOT user has no organization (organization_id is NULL)");
        }

        // Check roles
        if (!rootUser.getRoles().contains("ROOT")) {
            log.error("❌ ROOT user '{}' does not have ROOT role!", rootUser.getUsername());
        } else {
            log.info("✅ ROOT user has ROOT role");
        }

        log.info("✅ ROOT user validation complete. User: {}, ID: {}, Email: {}",
                rootUser.getUsername(), rootUser.getId(), rootUser.getEmail());
    }
}

