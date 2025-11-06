package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.dto.AlertConfigurationDTO;
import com.was.employeemanagementsystem.entity.AlertConfiguration;
import com.was.employeemanagementsystem.repository.AlertConfigurationRepository;
import com.was.employeemanagementsystem.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

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
        // Create default configurations if they don't exist
        // Example: Passport with multiple priority levels
        createDefaultIfNotExists("PASSPORT", 90, "ATTENTION", "EMAIL");
        createDefaultIfNotExists("PASSPORT", 30, "WARNING", "BOTH");
        createDefaultIfNotExists("PASSPORT", 7, "CRITICAL", "BOTH");

        createDefaultIfNotExists("VISA", 60, "ATTENTION", "EMAIL");
        createDefaultIfNotExists("VISA", 30, "WARNING", "BOTH");
        createDefaultIfNotExists("VISA", 7, "CRITICAL", "BOTH");
    }

    private void createDefaultIfNotExists(String docType, int days, String priority, String notifType) {
        if (!alertConfigurationRepository.existsByDocumentTypeAndAlertPriority(docType, priority)) {
            AlertConfiguration config = new AlertConfiguration(
                docType, days, "waseem.farooqui19@gmail.com", priority, notifType
            );
            alertConfigurationRepository.save(config);
        }
    }

    public List<AlertConfigurationDTO> getAllConfigurations() {
        if (!securityUtils.isAdminOrSuperAdmin()) {
            throw new RuntimeException("Access denied. Only admins can view alert configurations.");
        }

        return alertConfigurationRepository.findAll().stream()
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
            throw new RuntimeException("Access denied. Only admins can update alert configurations.");
        }

        AlertConfiguration config = alertConfigurationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Configuration not found with id: " + id));

        config.setAlertDaysBefore(dto.getAlertDaysBefore());
        config.setAlertEmail(dto.getAlertEmail());
        config.setEnabled(dto.isEnabled());
        config.setAlertPriority(dto.getAlertPriority());
        config.setNotificationType(dto.getNotificationType());

        AlertConfiguration updated = alertConfigurationRepository.save(config);
        return convertToDTO(updated);
    }

    public AlertConfigurationDTO createConfiguration(AlertConfigurationDTO dto) {
        if (!securityUtils.isAdminOrSuperAdmin()) {
            throw new RuntimeException("Access denied. Only admins can create alert configurations.");
        }

        // Check if same document type + priority combination exists
        if (alertConfigurationRepository.existsByDocumentTypeAndAlertPriority(
                dto.getDocumentType(), dto.getAlertPriority())) {
            throw new RuntimeException("Configuration already exists for " + dto.getDocumentType()
                + " with priority: " + dto.getAlertPriority());
        }

        AlertConfiguration config = new AlertConfiguration();
        config.setDocumentType(dto.getDocumentType());
        config.setAlertDaysBefore(dto.getAlertDaysBefore());
        config.setAlertEmail(dto.getAlertEmail());
        config.setEnabled(dto.isEnabled());
        config.setAlertPriority(dto.getAlertPriority());
        config.setNotificationType(dto.getNotificationType());

        AlertConfiguration saved = alertConfigurationRepository.save(config);
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
        dto.setOrganizationId(config.getOrganizationId());
        return dto;
    }
}

