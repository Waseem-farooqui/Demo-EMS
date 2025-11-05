package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.dto.RootDashboardDTO;
import com.was.employeemanagementsystem.entity.Organization;
import com.was.employeemanagementsystem.entity.User;
import com.was.employeemanagementsystem.repository.OrganizationRepository;
import com.was.employeemanagementsystem.repository.UserRepository;
import com.was.employeemanagementsystem.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for ROOT user dashboard
 * Provides organization-level statistics and management
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RootDashboardService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    /**
     * Get ROOT dashboard statistics
     * Only accessible by ROOT user
     */
    public RootDashboardDTO getRootDashboardStats() {
        // Only ROOT can access this dashboard
        if (!securityUtils.isRoot()) {
            log.warn("⚠️ Non-ROOT user attempted to access ROOT dashboard");
            throw new AccessDeniedException("Only ROOT user can access ROOT dashboard");
        }

        log.info("👑 Generating ROOT dashboard statistics");

        RootDashboardDTO dashboard = new RootDashboardDTO();

        // Get all organizations
        List<Organization> allOrganizations = organizationRepository.findAll();

        // Organization counts
        dashboard.setTotalOrganizations((long) allOrganizations.size());
        dashboard.setActiveOrganizations(
            allOrganizations.stream().filter(Organization::getIsActive).count()
        );
        dashboard.setInactiveOrganizations(
            allOrganizations.stream().filter(org -> !org.getIsActive()).count()
        );

        // Get all SUPER_ADMIN users
        List<User> superAdmins = userRepository.findAll().stream()
                .filter(u -> u.getRoles() != null && u.getRoles().contains("SUPER_ADMIN"))
                .collect(Collectors.toList());
        dashboard.setTotalSuperAdmins((long) superAdmins.size());

        // System start date (first organization created)
        allOrganizations.stream()
                .map(Organization::getCreatedAt)
                .min(Comparator.naturalOrder())
                .ifPresent(dashboard::setSystemStartDate);

        // Recent onboardings (organizations with their SUPER_ADMIN first login date)
        List<RootDashboardDTO.OrganizationOnboardingDTO> onboardings = allOrganizations.stream()
                .map(org -> {
                    RootDashboardDTO.OrganizationOnboardingDTO dto = new RootDashboardDTO.OrganizationOnboardingDTO();
                    dto.setOrganizationId(org.getId());
                    dto.setOrganizationUuid(org.getOrganizationUuid());
                    dto.setOrganizationName(org.getName());
                    dto.setIsActive(org.getIsActive());

                    // Find SUPER_ADMIN for this organization
                    User superAdmin = superAdmins.stream()
                            .filter(u -> org.getId().equals(u.getOrganizationId()))
                            .findFirst()
                            .orElse(null);

                    if (superAdmin != null) {
                        dto.setSuperAdminUsername(superAdmin.getUsername());
                        dto.setSuperAdminEmail(superAdmin.getEmail());

                        // Onboarding date is when organization was created
                        // In future, you could track actual first login date
                        dto.setOnboardingDate(org.getCreatedAt());

                        // Calculate days active
                        if (org.getCreatedAt() != null) {
                            long days = ChronoUnit.DAYS.between(org.getCreatedAt(), LocalDateTime.now());
                            dto.setDaysActive(days);
                        }
                    } else {
                        // Organization exists but no SUPER_ADMIN yet (shouldn't happen in normal flow)
                        dto.setOnboardingDate(org.getCreatedAt());
                        dto.setSuperAdminUsername("N/A");
                        dto.setSuperAdminEmail("N/A");
                        if (org.getCreatedAt() != null) {
                            long days = ChronoUnit.DAYS.between(org.getCreatedAt(), LocalDateTime.now());
                            dto.setDaysActive(days);
                        }
                    }

                    return dto;
                })
                .sorted(Comparator.comparing(RootDashboardDTO.OrganizationOnboardingDTO::getOnboardingDate).reversed())
                .collect(Collectors.toList());

        dashboard.setRecentOnboardings(onboardings);

        log.info("✅ ROOT dashboard generated: {} organizations, {} active, {} inactive",
                dashboard.getTotalOrganizations(),
                dashboard.getActiveOrganizations(),
                dashboard.getInactiveOrganizations());

        return dashboard;
    }

    /**
     * Get specific organization onboarding details
     */
    public RootDashboardDTO.OrganizationOnboardingDTO getOrganizationOnboardingDetails(Long organizationId) {
        if (!securityUtils.isRoot()) {
            throw new AccessDeniedException("Only ROOT user can access organization details");
        }

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        RootDashboardDTO.OrganizationOnboardingDTO dto = new RootDashboardDTO.OrganizationOnboardingDTO();
        dto.setOrganizationId(org.getId());
        dto.setOrganizationUuid(org.getOrganizationUuid());
        dto.setOrganizationName(org.getName());
        dto.setIsActive(org.getIsActive());
        dto.setOnboardingDate(org.getCreatedAt());

        // Find SUPER_ADMIN
        User superAdmin = userRepository.findAll().stream()
                .filter(u -> u.getRoles() != null && u.getRoles().contains("SUPER_ADMIN"))
                .filter(u -> organizationId.equals(u.getOrganizationId()))
                .findFirst()
                .orElse(null);

        if (superAdmin != null) {
            dto.setSuperAdminUsername(superAdmin.getUsername());
            dto.setSuperAdminEmail(superAdmin.getEmail());
        }

        if (org.getCreatedAt() != null) {
            long days = ChronoUnit.DAYS.between(org.getCreatedAt(), LocalDateTime.now());
            dto.setDaysActive(days);
        }

        return dto;
    }
}

