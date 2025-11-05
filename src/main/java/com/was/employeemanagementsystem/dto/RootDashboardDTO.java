package com.was.employeemanagementsystem.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for ROOT user dashboard
 * Shows organization-level statistics only
 */
@Data
public class RootDashboardDTO {

    // Organization statistics
    private Long totalOrganizations;
    private Long activeOrganizations;
    private Long inactiveOrganizations;

    // Recent onboarding
    private List<OrganizationOnboardingDTO> recentOnboardings;

    // System statistics
    private LocalDateTime systemStartDate;  // First organization onboarding date
    private Long totalSuperAdmins;  // Total SUPER_ADMIN users across all organizations

    @Data
    public static class OrganizationOnboardingDTO {
        private Long organizationId;
        private String organizationUuid;
        private String organizationName;
        private LocalDateTime onboardingDate;  // Date when SUPER_ADMIN first logged in
        private String superAdminUsername;
        private String superAdminEmail;
        private Boolean isActive;
        private Long daysActive;  // Days since onboarding
    }
}

