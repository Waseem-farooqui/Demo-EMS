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
        if (alertConfigurationRepository.findByDocumentType("PASSPORT").isEmpty()) {
            AlertConfiguration passportConfig = new AlertConfiguration(
                "PASSPORT",
                90, // 90 days before expiry
                "waseem.farooqui19@gmail.com"
            );
            alertConfigurationRepository.save(passportConfig);
        }

        if (alertConfigurationRepository.findByDocumentType("VISA").isEmpty()) {
            AlertConfiguration visaConfig = new AlertConfiguration(
                "VISA",
                60, // 60 days before expiry
                "waseem.farooqui19@gmail.com"
            );
            alertConfigurationRepository.save(visaConfig);
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

    public AlertConfigurationDTO getConfigurationByDocumentType(String documentType) {
        if (!securityUtils.isAdminOrSuperAdmin()) {
            throw new RuntimeException("Access denied. Only admins can view alert configurations.");
        }

        AlertConfiguration config = alertConfigurationRepository.findByDocumentType(documentType)
                .orElseThrow(() -> new RuntimeException("Configuration not found for document type: " + documentType));
        return convertToDTO(config);
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

        AlertConfiguration updated = alertConfigurationRepository.save(config);
        return convertToDTO(updated);
    }

    public AlertConfigurationDTO createConfiguration(AlertConfigurationDTO dto) {
        if (!securityUtils.isAdminOrSuperAdmin()) {
            throw new RuntimeException("Access denied. Only admins can create alert configurations.");
        }

        if (alertConfigurationRepository.findByDocumentType(dto.getDocumentType()).isPresent()) {
            throw new RuntimeException("Configuration already exists for document type: " + dto.getDocumentType());
        }

        AlertConfiguration config = new AlertConfiguration();
        config.setDocumentType(dto.getDocumentType());
        config.setAlertDaysBefore(dto.getAlertDaysBefore());
        config.setAlertEmail(dto.getAlertEmail());
        config.setEnabled(dto.isEnabled());

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
        return dto;
    }
}

