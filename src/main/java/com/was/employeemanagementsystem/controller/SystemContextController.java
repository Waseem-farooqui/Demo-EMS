package com.was.employeemanagementsystem.controller;

import com.was.employeemanagementsystem.security.UserDetailsImpl;
import com.was.employeemanagementsystem.service.SystemContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("${api.base-path:/api}/system")
@RequiredArgsConstructor
public class SystemContextController {

    private final SystemContextService systemContextService;

    /**
     * Get system context and available features for the logged-in user's organization
     */
    @GetMapping("/context")
    public ResponseEntity<Map<String, Object>> getSystemContext(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            log.info("🔍 GET /api/system/context - User: {}, Org: {}",
                    userDetails.getUsername(), userDetails.getOrganizationUuid());

            SystemContextService.SystemFeatures features =
                    systemContextService.getAvailableFeatures(userDetails.getOrganizationUuid());

            Map<String, Object> response = new HashMap<>();
            response.put("systemType", features.getSystemType().getCode());
            response.put("systemName", features.getSystemType().getDisplayName());
            response.put("features", Map.of(
                    "employeeManagement", features.isEmployeeManagement(),
                    "inventoryManagement", features.isInventoryManagement()
            ));
            response.put("organizationUuid", userDetails.getOrganizationUuid());
            response.put("username", userDetails.getUsername());
            response.put("role", userDetails.getRole());

            log.info("✅ System context retrieved successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Failed to get system context", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve system context");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Check if specific feature is available
     */
    @GetMapping("/features/{featureName}")
    public ResponseEntity<Map<String, Boolean>> checkFeature(
            @PathVariable String featureName,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            log.info("🔍 GET /api/system/features/{} - Org: {}", featureName, userDetails.getOrganizationUuid());

            boolean available = false;

            switch (featureName.toLowerCase()) {
                case "employee":
                case "employee-management":
                    available = systemContextService.hasEmployeeManagement(userDetails.getOrganizationUuid());
                    break;
                case "inventory":
                case "inventory-management":
                    available = systemContextService.hasInventoryManagement(userDetails.getOrganizationUuid());
                    break;
            }

            Map<String, Boolean> response = new HashMap<>();
            response.put("available", available);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Failed to check feature availability", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

