package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.dto.AlertConfigurationDTO;
import com.was.employeemanagementsystem.entity.AlertConfiguration;
import com.was.employeemanagementsystem.repository.AlertConfigurationRepository;
import com.was.employeemanagementsystem.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class AlertConfigurationService {

    private final AlertConfigurationRepository alertConfigurationRepository;
    private final SecurityUtils securityUtils;

    @PersistenceContext
    private EntityManager entityManager;

    public AlertConfigurationService(AlertConfigurationRepository alertConfigurationRepository,
                                    SecurityUtils securityUtils) {
        this.alertConfigurationRepository = alertConfigurationRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Create default alert configurations for a new organization
     * This is called when an organization is created
     */
    public void createDefaultConfigurationsForOrganization(Long organizationId, String contactEmail) {
        log.info("🔔 Creating default alert configurations for organization ID: {}", organizationId);

        try {
            // Create default configurations for this organization
            createDefaultIfNotExists(organizationId, "PASSPORT", 90, "ATTENTION", "EMAIL", contactEmail);
            createDefaultIfNotExists(organizationId, "PASSPORT", 30, "WARNING", "BOTH", contactEmail);
            createDefaultIfNotExists(organizationId, "PASSPORT", 7, "CRITICAL", "BOTH", contactEmail);

            createDefaultIfNotExists(organizationId, "VISA", 60, "ATTENTION", "EMAIL", contactEmail);
            createDefaultIfNotExists(organizationId, "VISA", 30, "WARNING", "BOTH", contactEmail);
            createDefaultIfNotExists(organizationId, "VISA", 7, "CRITICAL", "BOTH", contactEmail);

            log.info("✅ Default alert configurations created for organization ID: {}", organizationId);
        } catch (Exception e) {
            // Log error but don't fail organization creation
            log.error("❌ Error creating default alert configurations for organization ID: {}. Error: {}", 
                organizationId, e.getMessage());
            log.debug("Exception details: ", e);
            // Don't rethrow - allow organization creation to continue
        }
    }

    /**
     * Create a single default configuration if it doesn't exist.
     * Uses REQUIRES_NEW propagation to ensure each creation is in its own transaction.
     * Uses noRollbackFor to prevent Hibernate session corruption when duplicate entries occur.
     * Clears the EntityManager after catching exceptions to prevent session state issues.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = DataIntegrityViolationException.class)
    private void createDefaultIfNotExists(Long organizationId, String docType, int days, String priority, String notifType, String email) {
        // Check if configuration exists for this organization
        boolean exists = alertConfigurationRepository.existsByOrganizationIdAndDocumentTypeAndAlertPriority(
                organizationId, docType, priority);

        if (!exists) {
            try {
                AlertConfiguration config = new AlertConfiguration(
                    docType, days, email != null ? email : "admin@organization.com", priority, notifType
                );
                config.setOrganizationId(organizationId);
                alertConfigurationRepository.save(config);
                log.info("   ➕ Created default config for org {}: {} - {} priority - {} days", 
                    organizationId, docType, priority, days);
            } catch (DataIntegrityViolationException e) {
                // Handle race condition: another thread/process created it between check and save
                // Clear the EntityManager to remove any failed entity from the session
                entityManager.clear();
                log.warn("   ⚠️  Duplicate entry detected (race condition) for org {}: {} - {} priority. Config already exists.", 
                    organizationId, docType, priority);
                log.debug("   Exception details: {}", e.getMessage());
                // Exception is caught and handled, EntityManager cleared, transaction commits normally
            } catch (Exception e) {
                // Clear EntityManager for any other exceptions too
                entityManager.clear();
                log.error("   ❌ Error creating default config for org {}: {} - {} priority. Error: {}", 
                    organizationId, docType, priority, e.getMessage());
                log.debug("   Exception: ", e);
                throw e; // Re-throw non-duplicate exceptions
            }
        } else {
            log.debug("   ⏭️  Skipped (already exists for org {}): {} - {} priority", 
                organizationId, docType, priority);
        }
    }

    public List<AlertConfigurationDTO> getAllConfigurations() {
        if (!securityUtils.isAdminOrSuperAdmin()) {
            log.warn("⚠️  Access denied for non-admin user trying to view alert configurations");
            throw new RuntimeException("Access denied. Only admins can view alert configurations.");
        }

        List<AlertConfiguration> configs = alertConfigurationRepository.findAll();
        log.info("📋 Fetching all alert configurations. Total found: {}", configs.size());

        for (AlertConfiguration config : configs) {
            log.debug("   📄 Config [ID: {}]: {} - {} priority - {} days - Enabled: {} - OrgId: {}",
                config.getId(),
                config.getDocumentType(),
                config.getAlertPriority(),
                config.getAlertDaysBefore(),
                config.isEnabled(),
                config.getOrganizationId());
        }

        return configs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<AlertConfigurationDTO> getConfigurationsByDocumentType(String documentType) {
        if (!securityUtils.isAdminOrSuperAdmin()) {
            throw new RuntimeException("Access denied. Only admins can view alert configurations.");
        }

        return alertConfigurationRepository.findAllByDocumentType(documentType).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public AlertConfigurationDTO updateConfiguration(Long id, AlertConfigurationDTO dto) {
        if (!securityUtils.isAdminOrSuperAdmin()) {
            log.warn("⚠️  Access denied for non-admin user trying to update alert configuration");
            throw new RuntimeException("Access denied. Only admins can update alert configurations.");
        }

        log.info("🔄 Attempting to update alert configuration [ID: {}]", id);

        AlertConfiguration config = alertConfigurationRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("❌ Configuration not found with id: {}", id);
                    return new RuntimeException("Configuration not found with id: " + id);
                });

        log.info("   📋 Current values:");
        log.info("      Document Type: {}", config.getDocumentType());
        log.info("      Priority: {}", config.getAlertPriority());
        log.info("      Days Before: {}", config.getAlertDaysBefore());
        log.info("      Email: {}", config.getAlertEmail());
        log.info("      Enabled: {}", config.isEnabled());
        log.info("      Notification Type: {}", config.getNotificationType());

        log.info("   📝 New values:");
        log.info("      Days Before: {} -> {}", config.getAlertDaysBefore(), dto.getAlertDaysBefore());
        log.info("      Email: {} -> {}", config.getAlertEmail(), dto.getAlertEmail());
        log.info("      Enabled: {} -> {}", config.isEnabled(), dto.isEnabled());
        log.info("      Priority: {} -> {}", config.getAlertPriority(), dto.getAlertPriority());
        log.info("      Notification Type: {} -> {}", config.getNotificationType(), dto.getNotificationType());
        log.info("      Alert Frequency: {} -> {}", config.getAlertFrequency(), dto.getAlertFrequency());
        log.info("      Repeat Until Resolved: {} -> {}", config.isRepeatUntilResolved(), dto.isRepeatUntilResolved());

        config.setAlertDaysBefore(dto.getAlertDaysBefore());
        config.setAlertEmail(dto.getAlertEmail());
        config.setEnabled(dto.isEnabled());
        config.setAlertPriority(dto.getAlertPriority());
        config.setNotificationType(dto.getNotificationType());
        config.setAlertFrequency(dto.getAlertFrequency() != null ? dto.getAlertFrequency() : "ONCE");
        config.setRepeatUntilResolved(dto.isRepeatUntilResolved());

        AlertConfiguration updated = alertConfigurationRepository.save(config);

        log.info("✅ Alert configuration [ID: {}] updated successfully!", id);

        return convertToDTO(updated);
    }

    public void deleteConfiguration(Long id) {
        if (!securityUtils.isAdminOrSuperAdmin()) {
            log.warn("⚠️  Access denied for non-admin user trying to delete alert configuration");
            throw new RuntimeException("Access denied. Only admins can delete alert configurations.");
        }

        log.info("🗑️  Attempting to delete alert configuration [ID: {}]", id);

        AlertConfiguration config = alertConfigurationRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("❌ Configuration not found with id: {}", id);
                    return new RuntimeException("Configuration not found with id: " + id);
                });

        log.info("   📋 Deleting configuration:");
        log.info("      Document Type: {}", config.getDocumentType());
        log.info("      Priority: {}", config.getAlertPriority());
        log.info("      Days Before: {}", config.getAlertDaysBefore());
        log.info("      Enabled: {}", config.isEnabled());

        alertConfigurationRepository.delete(config);

        log.info("✅ Alert configuration [ID: {}] deleted successfully!", id);
    }

    public AlertConfigurationDTO createConfiguration(AlertConfigurationDTO dto) {
        if (!securityUtils.isAdminOrSuperAdmin()) {
            log.warn("⚠️  Access denied for non-admin user trying to create alert configuration");
            throw new RuntimeException("Access denied. Only admins can create alert configurations.");
        }

        log.info("🆕 Attempting to create alert configuration:");
        log.info("   📝 Document Type: {}", dto.getDocumentType());
        log.info("   📝 Priority: {}", dto.getAlertPriority());
        log.info("   📝 Days Before: {}", dto.getAlertDaysBefore());
        log.info("   📝 Email: {}", dto.getAlertEmail());
        log.info("   📝 Enabled: {}", dto.isEnabled());
        log.info("   📝 Notification Type: {}", dto.getNotificationType());
        log.info("   📝 Organization ID: {}", dto.getOrganizationId());

        // Check if same document type + priority combination exists for this organization
        if (dto.getOrganizationId() == null) {
            throw new RuntimeException("Organization ID is required for alert configuration");
        }
        
        boolean exists = alertConfigurationRepository.existsByOrganizationIdAndDocumentTypeAndAlertPriority(
                dto.getOrganizationId(), dto.getDocumentType(), dto.getAlertPriority());

        log.info("   🔍 Checking for existing config with same document type and priority...");

        if (exists) {
            // Get all matching configurations to show details
            List<AlertConfiguration> existingConfigs = alertConfigurationRepository
                .findAllByDocumentType(dto.getDocumentType());

            log.error("❌ Configuration creation FAILED - Duplicate found!");
            log.error("   📋 Existing configurations for document type '{}':", dto.getDocumentType());

            for (AlertConfiguration existing : existingConfigs) {
                log.error("      [ID: {}] Priority: {} - Days: {} - Enabled: {} - OrgId: {} - Email: {}",
                    existing.getId(),
                    existing.getAlertPriority(),
                    existing.getAlertDaysBefore(),
                    existing.isEnabled(),
                    existing.getOrganizationId(),
                    existing.getAlertEmail());

                if (existing.getAlertPriority().equals(dto.getAlertPriority())) {
                    log.error("      ⚠️  THIS IS THE DUPLICATE: [ID: {}] - You cannot create another {} priority config for {}",
                        existing.getId(), dto.getAlertPriority(), dto.getDocumentType());
                    log.error("      💡 Suggestion: Either UPDATE the existing config (ID: {}) or DELETE it first, then create new one",
                        existing.getId());
                }
            }

            throw new RuntimeException("Configuration already exists for " + dto.getDocumentType()
                + " with priority: " + dto.getAlertPriority()
                + ". Please update the existing configuration or delete it first.");
        }

        log.info("   ✅ No duplicate found, proceeding with creation...");

        AlertConfiguration config = new AlertConfiguration();
        config.setDocumentType(dto.getDocumentType());
        config.setAlertDaysBefore(dto.getAlertDaysBefore());
        config.setAlertEmail(dto.getAlertEmail());
        config.setEnabled(dto.isEnabled());
        config.setAlertPriority(dto.getAlertPriority());
        config.setNotificationType(dto.getNotificationType());
        config.setAlertFrequency(dto.getAlertFrequency() != null ? dto.getAlertFrequency() : "ONCE");
        config.setRepeatUntilResolved(dto.isRepeatUntilResolved());
        config.setOrganizationId(dto.getOrganizationId());

        AlertConfiguration saved = alertConfigurationRepository.save(config);

        log.info("✅ Alert configuration created successfully!");
        log.info("   🆔 ID: {}", saved.getId());
        log.info("   📄 Document Type: {}", saved.getDocumentType());
        log.info("   🎯 Priority: {}", saved.getAlertPriority());
        log.info("   📅 Days Before: {}", saved.getAlertDaysBefore());
        log.info("   📧 Email: {}", saved.getAlertEmail());
        log.info("   ✔️  Enabled: {}", saved.isEnabled());
        log.info("   🏢 Organization ID: {}", saved.getOrganizationId());

        return convertToDTO(saved);
    }

    private AlertConfigurationDTO convertToDTO(AlertConfiguration config) {
        AlertConfigurationDTO dto = new AlertConfigurationDTO();
        dto.setId(config.getId());
        dto.setDocumentType(config.getDocumentType());
        dto.setAlertDaysBefore(config.getAlertDaysBefore());
        dto.setAlertEmail(config.getAlertEmail());
        dto.setEnabled(config.isEnabled());
        dto.setAlertPriority(config.getAlertPriority());
        dto.setNotificationType(config.getNotificationType());
        dto.setAlertFrequency(config.getAlertFrequency() != null ? config.getAlertFrequency() : "ONCE");
        dto.setRepeatUntilResolved(config.isRepeatUntilResolved());
        dto.setOrganizationId(config.getOrganizationId());
        return dto;
    }
}

