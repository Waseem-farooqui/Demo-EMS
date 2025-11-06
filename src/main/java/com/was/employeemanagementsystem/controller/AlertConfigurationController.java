package com.was.employeemanagementsystem.controller;

import com.was.employeemanagementsystem.dto.AlertConfigurationDTO;
import com.was.employeemanagementsystem.service.AlertConfigurationService;
import com.was.employeemanagementsystem.service.DocumentExpiryScheduler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN.value()));
        }
    }

    @GetMapping("/type/{documentType}")
    public ResponseEntity<?> getConfigurationsByType(@PathVariable String documentType) {
        try {
            List<AlertConfigurationDTO> configurations = alertConfigurationService.getConfigurationsByDocumentType(documentType);
            return ResponseEntity.ok(configurations);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN.value()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createConfiguration(@RequestBody AlertConfigurationDTO dto) {
        try {
            AlertConfigurationDTO created = alertConfigurationService.createConfiguration(dto);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateConfiguration(@PathVariable Long id, @RequestBody AlertConfigurationDTO dto) {
        try {
            AlertConfigurationDTO updated = alertConfigurationService.updateConfiguration(id, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PostMapping("/test-alerts")
    public ResponseEntity<?> testAlerts() {
        try {
            documentExpiryScheduler.checkDocumentExpiryManually();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Alert check triggered successfully. Check logs and email.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error triggering alerts: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    private Map<String, Object> createErrorResponse(String message, int status) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("message", message);
        error.put("status", status);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}

