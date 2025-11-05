package com.was.employeemanagementsystem.controller;

import com.was.employeemanagementsystem.dto.CreateOrganizationRequest;
import com.was.employeemanagementsystem.dto.OrganizationDTO;
import com.was.employeemanagementsystem.service.OrganizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/organizations")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    /**
     * Create a new organization (ROOT only)
     * SUPER_ADMIN, ADMIN, USER cannot access
     */
    @PostMapping
    @PreAuthorize("hasRole('ROOT')")
    public ResponseEntity<?> createOrganization(@Valid @RequestBody CreateOrganizationRequest request) {
        try {
            log.info("🏢 Organization creation request received: {}", request.getOrganizationName());
            OrganizationDTO organization = organizationService.createOrganization(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Organization created successfully");
            response.put("organization", organization);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("❌ Failed to create organization: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error creating organization", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to create organization: " + e.getMessage()));
        }
    }

    /**
     * Get all organizations (ROOT only)
     * SUPER_ADMIN, ADMIN, USER cannot access
     */
    @GetMapping
    @PreAuthorize("hasRole('ROOT')")
    public ResponseEntity<?> getAllOrganizations() {
        try {
            List<OrganizationDTO> organizations = organizationService.getAllOrganizations();
            return ResponseEntity.ok(organizations);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get organization by ID (ROOT only)
     * SUPER_ADMIN, ADMIN, USER cannot access
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROOT')")
    public ResponseEntity<?> getOrganizationById(@PathVariable Long id) {
        try {
            OrganizationDTO organization = organizationService.getOrganizationById(id);
            return ResponseEntity.ok(organization);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get current user's organization info
     * Accessible by SUPER_ADMIN, ADMIN, USER (not ROOT)
     */
    @GetMapping("/my-organization")
    public ResponseEntity<?> getMyOrganization() {
        try {
            log.info("📋 Getting current user's organization");
            OrganizationDTO organization = organizationService.getCurrentUserOrganization();
            log.info("✅ Successfully retrieved organization: {}", organization.getName());
            return ResponseEntity.ok(organization);
        } catch (RuntimeException e) {
            log.error("❌ Failed to get user organization: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Update organization
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrganization(@PathVariable Long id,
                                               @RequestBody OrganizationDTO dto) {
        try {
            OrganizationDTO updated = organizationService.updateOrganization(id, dto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Organization updated successfully");
            response.put("organization", updated);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("❌ Failed to update organization: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Upload organization logo
     */
    @PostMapping("/{id}/logo")
    public ResponseEntity<?> uploadLogo(@PathVariable Long id,
                                       @RequestParam("file") MultipartFile file) {
        try {
            log.info("📷 Logo upload request for organization ID: {}", id);
            OrganizationDTO organization = organizationService.uploadLogo(id, file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Logo uploaded successfully");
            response.put("organization", organization);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("❌ Failed to upload logo: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error uploading logo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to upload logo: " + e.getMessage()));
        }
    }

    /**
     * Get organization logo
     */
    @GetMapping("/{id}/logo")
    public ResponseEntity<byte[]> getOrganizationLogo(@PathVariable Long id) {
        try {
            byte[] logoData = organizationService.getOrganizationLogo(id);

            // Detect image type from byte array
            MediaType contentType = MediaType.IMAGE_JPEG; // Default
            if (logoData.length > 2) {
                // Check for PNG signature
                if (logoData[0] == (byte) 0x89 && logoData[1] == (byte) 0x50) {
                    contentType = MediaType.IMAGE_PNG;
                }
                // Check for GIF signature
                else if (logoData[0] == (byte) 0x47 && logoData[1] == (byte) 0x49) {
                    contentType = MediaType.IMAGE_GIF;
                }
            }

            return ResponseEntity.ok()
                    .contentType(contentType)
                    .cacheControl(org.springframework.http.CacheControl.maxAge(3600, java.util.concurrent.TimeUnit.SECONDS))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"org_logo_" + id + ".jpg\"")
                    .body(logoData);
        } catch (RuntimeException e) {
            log.error("❌ Failed to retrieve logo: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deactivate organization (ROOT only)
     * This will block access for ALL users in this organization including SUPER_ADMIN
     */
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ROOT')")
    public ResponseEntity<?> deactivateOrganization(@PathVariable Long id) {
        try {
            log.info("⏸️ Deactivating organization ID: {}", id);
            OrganizationDTO organization = organizationService.deactivateOrganization(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Organization deactivated successfully. All users are now blocked.");
            response.put("organization", organization);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("❌ Failed to deactivate organization: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Activate organization (ROOT only)
     * This will restore access for all users in this organization
     */
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ROOT')")
    public ResponseEntity<?> activateOrganization(@PathVariable Long id) {
        try {
            log.info("✅ Activating organization ID: {}", id);
            OrganizationDTO organization = organizationService.activateOrganization(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Organization activated successfully. All users can now access the system.");
            response.put("organization", organization);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("❌ Failed to activate organization: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return error;
    }
}

