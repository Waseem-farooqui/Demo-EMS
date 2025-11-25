package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.dto.SmtpConfigurationDTO;
import com.was.employeemanagementsystem.entity.SmtpConfiguration;
import com.was.employeemanagementsystem.repository.SmtpConfigurationRepository;
import com.was.employeemanagementsystem.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

@Slf4j
@Service
public class SmtpConfigurationService {

    private final SmtpConfigurationRepository smtpConfigRepository;
    private final SecurityUtils securityUtils;

    @Autowired
    public SmtpConfigurationService(
            SmtpConfigurationRepository smtpConfigRepository,
            SecurityUtils securityUtils) {
        this.smtpConfigRepository = smtpConfigRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Get SMTP configuration for current organization
     */
    public SmtpConfigurationDTO getSmtpConfiguration() {
        if (!securityUtils.isSuperAdmin()) {
            throw new RuntimeException("Only SUPER_ADMIN can access SMTP configuration");
        }

        Long organizationId = securityUtils.getCurrentUser().getOrganizationId();
        SmtpConfiguration config = smtpConfigRepository.findByOrganizationId(organizationId)
                .orElse(null);

        SmtpConfigurationDTO dto = new SmtpConfigurationDTO();
        if (config != null) {
            dto.setId(config.getId());
            dto.setOrganizationId(config.getOrganizationId());
            dto.setProvider(config.getProvider());
            dto.setHost(config.getHost());
            dto.setPort(config.getPort());
            dto.setUsername(config.getUsername());
            dto.setFromEmail(config.getFromEmail());
            dto.setFromName(config.getFromName());
            dto.setEnabled(config.getEnabled());
            dto.setUseDefault(config.getUseDefault());
            dto.setIsConfigured(true);
        } else {
            dto.setIsConfigured(false);
            dto.setUseDefault(true);
        }
        return dto;
    }

    /**
     * Check if SMTP is configured for current organization
     */
    public boolean isSmtpConfigured() {
        if (!securityUtils.isSuperAdmin()) {
            return false;
        }

        Long organizationId = securityUtils.getCurrentUser().getOrganizationId();
        return smtpConfigRepository.existsByOrganizationId(organizationId);
    }

    /**
     * Save or update SMTP configuration
     */
    @Transactional
    public SmtpConfigurationDTO saveSmtpConfiguration(SmtpConfigurationDTO dto) {
        if (!securityUtils.isSuperAdmin()) {
            throw new RuntimeException("Only SUPER_ADMIN can configure SMTP");
        }

        Long organizationId = securityUtils.getCurrentUser().getOrganizationId();
        Long userId = securityUtils.getCurrentUser().getId();

        SmtpConfiguration config = smtpConfigRepository.findByOrganizationId(organizationId)
                .orElse(new SmtpConfiguration());

        // If useDefault is true, delete the configuration
        if (dto.getUseDefault() != null && dto.getUseDefault()) {
            if (config.getId() != null) {
                smtpConfigRepository.delete(config);
                log.info("✓ SMTP configuration deleted for organization {}, using default env settings", organizationId);
            }
            SmtpConfigurationDTO result = new SmtpConfigurationDTO();
            result.setUseDefault(true);
            result.setIsConfigured(false);
            return result;
        }

        // Set organization and user
        config.setOrganizationId(organizationId);
        config.setCreatedBy(userId);

        // Set provider-specific defaults
        if (dto.getProvider() != null) {
            config.setProvider(dto.getProvider().toUpperCase());
            if ("GMAIL".equals(config.getProvider())) {
                config.setHost("smtp.gmail.com");
                config.setPort(587);
            } else if ("OUTLOOK".equals(config.getProvider())) {
                config.setHost("smtp-mail.outlook.com");
                config.setPort(587);
            } else if ("CUSTOM".equals(config.getProvider())) {
                // Custom provider - use provided host and port
                if (dto.getHost() != null) config.setHost(dto.getHost());
                if (dto.getPort() != null) config.setPort(dto.getPort());
            }
        }

        // Update fields
        if (dto.getHost() != null && !"GMAIL".equals(config.getProvider()) && !"OUTLOOK".equals(config.getProvider())) {
            config.setHost(dto.getHost());
        }
        if (dto.getPort() != null && !"GMAIL".equals(config.getProvider()) && !"OUTLOOK".equals(config.getProvider())) {
            config.setPort(dto.getPort());
        }
        if (dto.getUsername() != null) {
            config.setUsername(dto.getUsername());
        }
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            // Encrypt password (simple Base64 encoding - in production, use proper encryption)
            config.setPassword(Base64.getEncoder().encodeToString(dto.getPassword().getBytes()));
        }
        if (dto.getFromEmail() != null) {
            config.setFromEmail(dto.getFromEmail());
        }
        if (dto.getFromName() != null) {
            config.setFromName(dto.getFromName());
        }
        if (dto.getEnabled() != null) {
            config.setEnabled(dto.getEnabled());
        }
        config.setUseDefault(false);

        config = smtpConfigRepository.save(config);
        log.info("✓ SMTP configuration saved for organization {}", organizationId);

        return convertToDTO(config);
    }

    /**
     * Get SMTP configuration for email sending (used by EmailService)
     */
    public SmtpConfiguration getSmtpConfigForSending(Long organizationId) {
        return smtpConfigRepository.findByOrganizationId(organizationId)
                .orElse(null);
    }

    /**
     * Decrypt password from stored configuration
     */
    public String decryptPassword(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            return null;
        }
        try {
            return new String(Base64.getDecoder().decode(encryptedPassword));
        } catch (Exception e) {
            log.error("Failed to decrypt SMTP password", e);
            return null;
        }
    }

    private SmtpConfigurationDTO convertToDTO(SmtpConfiguration config) {
        SmtpConfigurationDTO dto = new SmtpConfigurationDTO();
        dto.setId(config.getId());
        dto.setOrganizationId(config.getOrganizationId());
        dto.setProvider(config.getProvider());
        dto.setHost(config.getHost());
        dto.setPort(config.getPort());
        dto.setUsername(config.getUsername());
        dto.setFromEmail(config.getFromEmail());
        dto.setFromName(config.getFromName());
        dto.setEnabled(config.getEnabled());
        dto.setUseDefault(config.getUseDefault());
        dto.setIsConfigured(true);
        return dto;
    }
}

