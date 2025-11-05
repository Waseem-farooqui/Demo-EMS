package com.was.employeemanagementsystem.controller;

import com.was.employeemanagementsystem.entity.User;
import com.was.employeemanagementsystem.repository.UserRepository;
import com.was.employeemanagementsystem.service.RootUserValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/init")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class InitializationController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RootUserValidationService rootUserValidationService;

    // Basic Auth credentials for ROOT user creation
    private static final String BASIC_AUTH_USERNAME = "waseem";
    private static final String BASIC_AUTH_PASSWORD = "wud19@WUD";

    public InitializationController(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   RootUserValidationService rootUserValidationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.rootUserValidationService = rootUserValidationService;
    }

    /**
     * Create ROOT user - ONLY WORKS IF NO ROOT USER EXISTS
     * Protected with Basic Authentication
     * Username: waseem
     * Password: wud19@WUD
     */
    @PostMapping("/create-root")
    public ResponseEntity<?> createRootUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> request) {

        try {
            // Validate Basic Authentication
            if (!isValidBasicAuth(authHeader)) {
                log.warn("❌ Unauthorized attempt to create ROOT user");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                            "success", false,
                            "message", "Unauthorized. Valid Basic Authentication required."
                        ));
            }

            log.info("🔐 Valid Basic Auth credentials provided");

            // Check if ROOT user already exists using validation service
            if (rootUserValidationService.rootUserExists()) {
                User existingRoot = rootUserValidationService.getRootUser();
                log.warn("⚠️ ROOT user already exists: {} (ID: {})",
                        existingRoot != null ? existingRoot.getUsername() : "unknown",
                        existingRoot != null ? existingRoot.getId() : "unknown");
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", String.format("ROOT user already exists ('%s'). Only ONE ROOT user is allowed in the system.",
                            existingRoot != null ? existingRoot.getUsername() : "unknown")
                ));
            }

            // Validate no multiple ROOT users exist (additional safety check)
            try {
                rootUserValidationService.validateSingleRootUser();
            } catch (IllegalStateException e) {
                log.error("❌ ROOT user validation failed: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
                ));
            }

            String username = request.getOrDefault("username", "root");
            String email = request.getOrDefault("email", "root@system.local");
            String password = request.get("password");

            if (password == null || password.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Password is required"
                ));
            }

            // Validate password strength
            if (password.length() < 8) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Password must be at least 8 characters long"
                ));
            }

            log.info("🔧 Creating ROOT user: {}", username);

            // Create ROOT user
            User rootUser = new User();
            rootUser.setUsername(username);
            rootUser.setEmail(email);
            rootUser.setPassword(passwordEncoder.encode(password));
            rootUser.setEnabled(true);
            rootUser.setEmailVerified(true);
            rootUser.setFirstLogin(false);
            rootUser.setProfileCompleted(true);
            rootUser.setTemporaryPassword(false);
            rootUser.setOrganizationId(null); // ROOT has no organization

            Set<String> roles = new HashSet<>();
            roles.add("ROOT");
            rootUser.setRoles(roles);

            User saved = userRepository.save(rootUser);
            log.info("✅ ROOT user created successfully with ID: {}", saved.getId());

            // Validate ROOT user configuration
            try {
                rootUserValidationService.validateRootUserConfiguration();
            } catch (Exception e) {
                log.warn("⚠️ ROOT user validation warning: {}", e.getMessage());
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "ROOT user created successfully",
                "userId", saved.getId(),
                "username", username,
                "email", email,
                "warning", "This is the ONLY ROOT user allowed. Keep credentials secure!"
            ));

        } catch (Exception e) {
            log.error("❌ Error creating ROOT user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "success", false,
                        "message", "Error creating ROOT user: " + e.getMessage()
                    ));
        }
    }

    /**
     * Check if ROOT user exists
     * No authentication required for this endpoint
     */
    @GetMapping("/root-exists")
    public ResponseEntity<?> checkRootExists() {
        try {
            boolean rootExists = rootUserValidationService.rootUserExists();
            long rootCount = rootUserValidationService.getRootUserCount();

            if (rootCount > 1) {
                log.error("❌ CRITICAL: Multiple ROOT users detected! Count: {}", rootCount);
            }

            User rootUser = rootUserValidationService.getRootUser();

            Map<String, Object> response = Map.of(
                "exists", rootExists,
                "count", rootCount,
                "message", rootExists ? "ROOT user exists" : "ROOT user not found",
                "username", rootUser != null ? rootUser.getUsername() : "N/A",
                "warning", rootCount > 1 ? "CRITICAL: Multiple ROOT users found!" : ""
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking ROOT user existence: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "success", false,
                        "message", "Error checking ROOT user: " + e.getMessage()
                    ));
        }
    }

    /**
     * Validate ROOT user configuration
     * Protected with Basic Authentication
     * Checks system integrity regarding ROOT user
     */
    @GetMapping("/validate-root")
    public ResponseEntity<?> validateRootConfiguration(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // Validate Basic Authentication
            if (!isValidBasicAuth(authHeader)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                            "success", false,
                            "message", "Unauthorized. Valid Basic Authentication required."
                        ));
            }

            log.info("🔍 Validating ROOT user configuration...");

            long rootCount = rootUserValidationService.getRootUserCount();
            User rootUser = rootUserValidationService.getRootUser();

            // Perform validation
            try {
                rootUserValidationService.validateRootUserConfiguration();
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                            "success", false,
                            "message", "ROOT user validation failed: " + e.getMessage(),
                            "rootCount", rootCount
                        ));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "ROOT user configuration is valid",
                "rootCount", rootCount,
                "rootUsername", rootUser != null ? rootUser.getUsername() : "N/A",
                "rootId", rootUser != null ? rootUser.getId() : 0,
                "organizationId", rootUser != null ? rootUser.getOrganizationId() : null,
                "validation", Map.of(
                    "singleRootUser", rootCount == 1,
                    "noOrganization", rootUser != null && rootUser.getOrganizationId() == null,
                    "hasRootRole", rootUser != null && rootUser.getRoles().contains("ROOT")
                )
            ));

        } catch (Exception e) {
            log.error("Error validating ROOT user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "success", false,
                        "message", "Error validating ROOT user: " + e.getMessage()
                    ));
        }
    }

    /**
     * Validate Basic Authentication credentials
     */
    private boolean isValidBasicAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return false;
        }

        try {
            // Extract and decode Base64 credentials
            String base64Credentials = authHeader.substring(6);
            String credentials = new String(Base64.getDecoder().decode(base64Credentials));

            // Split username:password
            String[] parts = credentials.split(":", 2);
            if (parts.length != 2) {
                return false;
            }

            String username = parts[0];
            String password = parts[1];

            // Validate credentials
            boolean isValid = BASIC_AUTH_USERNAME.equals(username) && BASIC_AUTH_PASSWORD.equals(password);

            if (isValid) {
                log.info("✅ Basic Auth validated for user: {}", username);
            } else {
                log.warn("❌ Invalid Basic Auth credentials. Username: {}", username);
            }

            return isValid;

        } catch (Exception e) {
            log.error("Error validating Basic Auth: {}", e.getMessage());
            return false;
        }
    }
}

