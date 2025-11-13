package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.dto.AlertConfigurationDTO;
import com.was.employeemanagementsystem.entity.AlertConfiguration;
import com.was.employeemanagementsystem.repository.AlertConfigurationRepository;
import com.was.employeemanagementsystem.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class AlertConfigurationService {

    private final AlertConfigurationRepository alertConfigurationRepository;
    private final SecurityUtils securityUtils;

    public AlertConfigurationService(AlertConfigurationRepository alertConfigurationRepository,
                                    SecurityUtils securityUtils) {
        this.alertConfigurationRepository = alertConfigurationRepository;
        this.securityUtils = securityUtils;
    }

    @PostConstruct
    public void initDefaultConfigurations() {
        log.info("🔔 Initializing default alert configurations...");

        // Create default configurations if they don't exist
        // Example: Passport with multiple priority levels
        createDefaultIfNotExists("PASSPORT", 90, "ATTENTION", "EMAIL");
        createDefaultIfNotExists("PASSPORT", 30, "WARNING", "BOTH");
        createDefaultIfNotExists("PASSPORT", 7, "CRITICAL", "BOTH");

        createDefaultIfNotExists("VISA", 60, "ATTENTION", "EMAIL");
        createDefaultIfNotExists("VISA", 30, "WARNING", "BOTH");
        createDefaultIfNotExists("VISA", 7, "CRITICAL", "BOTH");

        // Log all existing configurations
        List<AlertConfiguration> allConfigs = alertConfigurationRepository.findAll();
        log.info("📋 Total alert configurations in database: {}", allConfigs.size());
        for (AlertConfiguration config : allConfigs) {
            log.info("   ⚙️  [ID: {}] {} - {} priority - {} days - Enabled: {} - OrgId: {}",
                config.getId(),
                config.getDocumentType(),
                config.getAlertPriority(),
                config.getAlertDaysBefore(),
                config.isEnabled(),
                config.getOrganizationId());
        }
        log.info("✅ Default alert configurations initialized");
    }

    private void createDefaultIfNotExists(String docType, int days, String priority, String notifType) {
        boolean exists = alertConfigurationRepository.existsByDocumentTypeAndAlertPriority(docType, priority);

        if (!exists) {
            AlertConfiguration config = new AlertConfiguration(
                docType, days, "waseem.farooqui19@gmail.com", priority, notifType
            );
            alertConfigurationRepository.save(config);
            log.info("   ➕ Created default config: {} - {} priority - {} days", docType, priority, days);
        } else {
            log.debug("   ⏭️  Skipped (already exists): {} - {} priority", docType, priority);
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

        // Check if same document type + priority combination exists
        boolean exists = alertConfigurationRepository.existsByDocumentTypeAndAlertPriority(
                dto.getDocumentType(), dto.getAlertPriority());

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

