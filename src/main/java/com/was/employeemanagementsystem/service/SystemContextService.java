package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.entity.Organization;
import com.was.employeemanagementsystem.enums.SystemType;
import com.was.employeemanagementsystem.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service to determine the system context based on user's organization
 * This enables the microservice architecture to serve different business domains
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemContextService {

    private final OrganizationRepository organizationRepository;

    /**
     * Get system type for an organization
     */
    public SystemType getSystemType(String organizationUuid) {
        log.debug("🔍 Determining system type for organization: {}", organizationUuid);

        Optional<Organization> org = organizationRepository.findByOrganizationUuid(organizationUuid);

        if (org.isPresent()) {
            SystemType systemType = org.get().getSystemType();
            log.info("✅ System type for {}: {}", organizationUuid, systemType);
            return systemType;
        }

        log.warn("⚠️ Organization not found, defaulting to EMPLOYEE_MANAGEMENT");
        return SystemType.EMPLOYEE_MANAGEMENT;
    }

    /**
     * Check if organization has employee management features
     */
    public boolean hasEmployeeManagement(String organizationUuid) {
        SystemType type = getSystemType(organizationUuid);
        return type == SystemType.EMPLOYEE_MANAGEMENT || type == SystemType.HYBRID;
    }

    /**
     * Check if organization has inventory management features
     */
    public boolean hasInventoryManagement(String organizationUuid) {
        SystemType type = getSystemType(organizationUuid);
        return type == SystemType.INVENTORY_MANAGEMENT || type == SystemType.HYBRID;
    }

    /**
     * Get available features for an organization
     */
    public SystemFeatures getAvailableFeatures(String organizationUuid) {
        SystemType type = getSystemType(organizationUuid);

        SystemFeatures features = new SystemFeatures();

        switch (type) {
            case EMPLOYEE_MANAGEMENT:
                features.setEmployeeManagement(true);
                features.setInventoryManagement(false);
                break;
            case INVENTORY_MANAGEMENT:
                features.setEmployeeManagement(false);
                features.setInventoryManagement(true);
                break;
            case HYBRID:
                features.setEmployeeManagement(true);
                features.setInventoryManagement(true);
                break;
        }

        features.setSystemType(type);
        log.info("📋 Available features for {}: {}", organizationUuid, features);
        return features;
    }

    /**
     * Inner class to represent available system features
     */
    public static class SystemFeatures {
        private SystemType systemType;
        private boolean employeeManagement;
        private boolean inventoryManagement;

        public SystemType getSystemType() {
            return systemType;
        }

        public void setSystemType(SystemType systemType) {
            this.systemType = systemType;
        }

        public boolean isEmployeeManagement() {
            return employeeManagement;
        }

        public void setEmployeeManagement(boolean employeeManagement) {
            this.employeeManagement = employeeManagement;
        }

        public boolean isInventoryManagement() {
            return inventoryManagement;
        }

        public void setInventoryManagement(boolean inventoryManagement) {
            this.inventoryManagement = inventoryManagement;
        }

        @Override
        public String toString() {
            return String.format("SystemFeatures{type=%s, employee=%s, inventory=%s}",
                    systemType, employeeManagement, inventoryManagement);
        }
    }
}

