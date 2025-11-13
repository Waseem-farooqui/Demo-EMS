package com.was.employeemanagementsystem.controller;

import com.was.employeemanagementsystem.constants.AppConstants;
import com.was.employeemanagementsystem.dto.DocumentDTO;
import com.was.employeemanagementsystem.service.DocumentService;
import com.was.employeemanagementsystem.service.DocumentExpiryNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(AppConstants.API_DOCUMENTS_PATH)
@CrossOrigin(origins = "${app.cors.origins}")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentExpiryNotificationService expiryNotificationService;

    public DocumentController(DocumentService documentService,
                            DocumentExpiryNotificationService expiryNotificationService) {
        this.documentService = documentService;
        this.expiryNotificationService = expiryNotificationService;
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("employeeId") Long employeeId,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file) {

        log.info("Document upload request - EmployeeId: {}, Type: {}, File: {}",
            employeeId, documentType, file != null ? file.getOriginalFilename() : "null");

        try {
            if (file.isEmpty()) {
                log.warn("Empty file uploaded");
                return ResponseEntity.badRequest().body("Please select a file to upload");
            }

            String contentType = file.getContentType();
            boolean isValidType = contentType != null && (
                contentType.startsWith("image/") ||
                contentType.equals("application/pdf") ||
                contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                contentType.equals("application/msword")
            );

            if (!isValidType) {
                log.warn("Invalid file type: {}", contentType);
                return ResponseEntity.badRequest().body("Only image files (PNG, JPG, JPEG), PDF files, and Word documents (DOC, DOCX) are allowed");
            }

            if (file.getSize() > 10 * 1024 * 1024) {
                log.warn("File too large: {} bytes", file.getSize());
                return ResponseEntity.badRequest().body("File size must be less than 10MB");
            }

            boolean isValidDocument = documentService.validateDocumentType(file, documentType);
            if (!isValidDocument) {
                log.warn("Document validation failed - does not appear to be a valid {} document", documentType);
                return ResponseEntity.badRequest()
                    .body("The uploaded file does not appear to be a valid " + documentType +
                          " document. Please upload a clear photo or scan of your " + documentType.toLowerCase());
            }

            DocumentDTO document = documentService.uploadDocument(employeeId, documentType, file);
            log.info("✓ Document uploaded successfully - ID: {}, Type: {}", document.getId(), documentType);
            return new ResponseEntity<>(document, HttpStatus.CREATED);

        } catch (IOException | TikaException e) {
            log.error("✗ Error processing document upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing document: " + e.getMessage());
        } catch (RuntimeException e) {
            log.error("✗ Access denied or validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<DocumentDTO>> getAllDocuments() {
        List<DocumentDTO> documents = documentService.getAllDocuments();
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getDocumentById(@PathVariable Long id) {
        try {
            DocumentDTO document = documentService.getDocumentById(id);
            return ResponseEntity.ok(document);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getDocumentsByEmployeeId(@PathVariable Long employeeId) {
        try {
            List<DocumentDTO> documents = documentService.getDocumentsByEmployeeId(employeeId);
            return ResponseEntity.ok(documents);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<DocumentDTO>> getExpiringDocuments(@RequestParam(defaultValue = "90") int days) {
        List<DocumentDTO> documents = documentService.getExpiringDocuments(days);
        return ResponseEntity.ok(documents);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        try {
            documentService.deleteDocument(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDocument(@PathVariable Long id, @RequestBody Map<String, Object> updateData) {
        try {
            log.info("Document update request - ID: {}", id);
            DocumentDTO updatedDocument = documentService.updateDocument(id, updateData);
            log.info("✓ Document updated successfully - ID: {}", id);
            return ResponseEntity.ok(updatedDocument);
        } catch (RuntimeException e) {
            log.error("✗ Failed to update document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/debug")
    public ResponseEntity<Map<String, Object>> debugDocument(@PathVariable Long id) {
        try {
            log.info("🐛 Debug request for document ID: {}", id);

            // This will throw exception if user doesn't have access
            DocumentDTO document = documentService.getDocumentById(id);

            Map<String, Object> debug = new HashMap<>();
            debug.put("id", id);
            debug.put("fileName", document.getFileName());
            debug.put("fileType", document.getFileType());
            debug.put("documentType", document.getDocumentType());
            debug.put("currentDirectory", System.getProperty("user.dir"));
            debug.put("uploadDirectory", "uploads/documents/");
            debug.put("message", "File path not exposed for security. Check backend logs for detailed path info.");

            // Try to get the file to see if it works
            try {
                byte[] fileData = documentService.getDocumentImage(id);
                debug.put("fileLoadSuccess", true);
                debug.put("fileSize", fileData.length);
                debug.put("fileSizeKB", fileData.length / 1024);
            } catch (Exception e) {
                debug.put("fileLoadSuccess", false);
                debug.put("fileLoadError", e.getMessage());
            }

            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("stackTrace", e.getClass().getName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<byte[]> getDocumentPreview(@PathVariable Long id) {
        try {
            log.info("📸 Document preview request - ID: {}", id);

            byte[] previewData = documentService.getDocumentPreview(id);

            if (previewData == null || previewData.length == 0) {
                log.warn("No preview data found for document ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.IMAGE_JPEG)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                    .body(previewData);
        } catch (RuntimeException e) {
            log.error("✗ Failed to retrieve document preview: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getDocumentImage(@PathVariable Long id) {
        try {
            log.info("📄 Document image request - ID: {}", id);

            DocumentDTO document = documentService.getDocumentById(id);
            byte[] imageData = documentService.getDocumentImage(id);

            if (imageData == null || imageData.length == 0) {
                log.warn("No image data found for document ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            String contentType;
            if ("CONTRACT".equals(document.getDocumentType()) || "SHARE_CODE".equals(document.getDocumentType())) {
                contentType = "application/pdf";
                log.info("✓ Returning {} PDF - ID: {}, Size: {} KB", document.getDocumentType(), id, imageData.length / 1024);
            } else if ("RESUME".equals(document.getDocumentType())) {
                if (document.getFileType() != null && document.getFileType().contains("word")) {
                    contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    log.info("✓ Returning RESUME DOCX - ID: {}, Size: {} KB", id, imageData.length / 1024);
                } else {
                    contentType = "application/pdf";
                    log.info("✓ Returning RESUME PDF - ID: {}, Size: {} KB", id, imageData.length / 1024);
                }
            } else {
                contentType = document.getFileType() != null ? document.getFileType() : "image/jpeg";
                log.info("✓ Returning document image - ID: {}, Size: {} KB, Type: {}", id, imageData.length / 1024, contentType);
            }

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getFileName() + "\"")
                    .body(imageData);
        } catch (RuntimeException e) {
            log.error("✗ Failed to retrieve document image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) {
        try {
            log.info("Document download request - ID: {}", id);
            byte[] documentData = documentService.getDocumentImage(id);

            if (documentData == null || documentData.length == 0) {
                log.warn("No document data found for document ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            DocumentDTO document = documentService.getDocumentById(id);
            String contentType = document.getFileType() != null ? document.getFileType() : "application/octet-stream";
            String fileName = document.getFileName() != null ? document.getFileName() : "document_" + id;

            log.info("✓ Downloading document - ID: {}, Size: {} bytes, Type: {}", id, documentData.length, contentType);

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(documentData);
        } catch (RuntimeException e) {
            log.error("✗ Failed to download document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping("/check-expiry")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> checkDocumentExpiry() {
        try {
            log.info("Manual document expiry check triggered");
            expiryNotificationService.triggerManualCheck();
            return ResponseEntity.ok(Map.of("message", "Document expiry check completed successfully"));
        } catch (Exception e) {
            log.error("Error during manual expiry check: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to check document expiry"));
        }
    }
}

