package com.was.employeemanagementsystem.controller;

import com.was.employeemanagementsystem.constants.AppConstants;
import com.was.employeemanagementsystem.dto.RootDashboardDTO;
import com.was.employeemanagementsystem.service.RootDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for ROOT user dashboard
 * Provides organization-level statistics and management
 * Completely separate from employee dashboard
 */
@Slf4j
@RestController
@RequestMapping(AppConstants.API_ROOT_DASHBOARD_PATH)
@CrossOrigin(origins = "${app.cors.origins}")
@RequiredArgsConstructor
public class RootDashboardController {

    private final RootDashboardService rootDashboardService;

    /**
     * Get ROOT dashboard statistics
     * Only accessible by ROOT user
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ROOT')")
    public ResponseEntity<RootDashboardDTO> getRootDashboardStats() {
        log.info("👑 ROOT dashboard stats request received");
        RootDashboardDTO stats = rootDashboardService.getRootDashboardStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get specific organization onboarding details
     */
    @GetMapping("/organization/{organizationId}")
    @PreAuthorize("hasRole('ROOT')")
    public ResponseEntity<RootDashboardDTO.OrganizationOnboardingDTO> getOrganizationDetails(
            @PathVariable Long organizationId) {
        log.info("👑 ROOT requesting organization details for ID: {}", organizationId);
        RootDashboardDTO.OrganizationOnboardingDTO details =
            rootDashboardService.getOrganizationOnboardingDetails(organizationId);
        return ResponseEntity.ok(details);
    }
}

