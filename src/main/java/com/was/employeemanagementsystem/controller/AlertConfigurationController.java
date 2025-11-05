package com.was.employeemanagementsystem.controller;

import com.was.employeemanagementsystem.dto.AlertConfigurationDTO;
import com.was.employeemanagementsystem.service.AlertConfigurationService;
import com.was.employeemanagementsystem.service.DocumentExpiryScheduler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alert-config")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class AlertConfigurationController {

    private final AlertConfigurationService alertConfigurationService;
    private final DocumentExpiryScheduler documentExpiryScheduler;

    public AlertConfigurationController(AlertConfigurationService alertConfigurationService,
                                       DocumentExpiryScheduler documentExpiryScheduler) {
        this.alertConfigurationService = alertConfigurationService;
        this.documentExpiryScheduler = documentExpiryScheduler;
    }

    @GetMapping
    public ResponseEntity<?> getAllConfigurations() {
        try {
            List<AlertConfigurationDTO> configurations = alertConfigurationService.getAllConfigurations();
            return ResponseEntity.ok(configurations);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/type/{documentType}")
    public ResponseEntity<?> getConfigurationByType(@PathVariable String documentType) {
        try {
            AlertConfigurationDTO configuration = alertConfigurationService.getConfigurationByDocumentType(documentType);
            return ResponseEntity.ok(configuration);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createConfiguration(@RequestBody AlertConfigurationDTO dto) {
        try {
            AlertConfigurationDTO created = alertConfigurationService.createConfiguration(dto);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateConfiguration(@PathVariable Long id, @RequestBody AlertConfigurationDTO dto) {
        try {
            AlertConfigurationDTO updated = alertConfigurationService.updateConfiguration(id, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PostMapping("/test-alerts")
    public ResponseEntity<?> testAlerts() {
        try {
            documentExpiryScheduler.checkDocumentExpiryManually();
            return ResponseEntity.ok("Alert check triggered successfully. Check logs and email.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error triggering alerts: " + e.getMessage());
        }
    }
}

