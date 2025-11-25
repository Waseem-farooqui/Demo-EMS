package com.was.employeemanagementsystem.controller;

import com.was.employeemanagementsystem.dto.SmtpConfigurationDTO;
import com.was.employeemanagementsystem.service.SmtpConfigurationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/smtp-configuration")
@CrossOrigin(origins = "${app.cors.origins}")
public class SmtpConfigurationController {

    private final SmtpConfigurationService smtpConfigService;

    public SmtpConfigurationController(SmtpConfigurationService smtpConfigService) {
        this.smtpConfigService = smtpConfigService;
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SmtpConfigurationDTO> getSmtpConfiguration() {
        try {
            SmtpConfigurationDTO config = smtpConfigService.getSmtpConfiguration();
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error("Error getting SMTP configuration", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SmtpConfigurationDTO> saveSmtpConfiguration(@RequestBody SmtpConfigurationDTO dto) {
        try {
            SmtpConfigurationDTO saved = smtpConfigService.saveSmtpConfiguration(dto);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Error saving SMTP configuration", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/check")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Boolean> isSmtpConfigured() {
        try {
            boolean configured = smtpConfigService.isSmtpConfigured();
            return ResponseEntity.ok(configured);
        } catch (Exception e) {
            log.error("Error checking SMTP configuration", e);
            return ResponseEntity.ok(false);
        }
    }
}

