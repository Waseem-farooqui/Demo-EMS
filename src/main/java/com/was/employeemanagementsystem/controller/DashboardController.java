package com.was.employeemanagementsystem.controller;

import com.was.employeemanagementsystem.constants.AppConstants;
import com.was.employeemanagementsystem.dto.DashboardStatsDTO;
import com.was.employeemanagementsystem.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(AppConstants.API_DASHBOARD_PATH)
@CrossOrigin(origins = "${app.cors.origins}")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Get dashboard statistics for SUPER_ADMIN only
     * ROOT user has a separate dashboard endpoint
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        log.info("📊 Dashboard stats request received");
        final DashboardStatsDTO stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }
}

