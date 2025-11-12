package com.was.employeemanagementsystem.controller;

import com.was.employeemanagementsystem.constants.AppConstants;
import com.was.employeemanagementsystem.dto.RotaChangeLogDTO;
import com.was.employeemanagementsystem.dto.RotaDTO;
import com.was.employeemanagementsystem.dto.RotaScheduleDTO;
import com.was.employeemanagementsystem.dto.RotaScheduleEntryDTO;
import com.was.employeemanagementsystem.dto.RotaScheduleUpdateDTO;
import com.was.employeemanagementsystem.entity.Rota;
import com.was.employeemanagementsystem.entity.RotaSchedule;
import com.was.employeemanagementsystem.service.ExcelRotaService;
import com.was.employeemanagementsystem.service.RotaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(AppConstants.API_ROTA_PATH)
@CrossOrigin(origins = "${app.cors.origins}")
@Slf4j
public class RotaController {

    @Autowired
    private RotaService rotaService;

    @Autowired
    private ExcelRotaService excelRotaService;

    /**
     * Upload ROTA Excel file (.xlsx) - 100% ACCURATE, NO OCR NEEDED!
     * Admins and Super Admins can upload
     * Returns preview of extracted records for user confirmation
     */
    @PostMapping("/upload-excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> uploadExcelRota(@RequestParam("file") MultipartFile file) {
        try {
            log.info("📊 Received Excel ROTA upload request: {}", file.getOriginalFilename());

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Please select a file to upload"));
            }

            // Validate file type - must be Excel
            String contentType = file.getContentType();
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Please upload an Excel file (.xlsx)"));
            }

            // Validate file size (max 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("File size must be less than 10MB"));
            }

            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            var preview = excelRotaService.uploadExcelRota(file, username);

            log.info("✅ Excel ROTA uploaded successfully - ID: {}", preview.getRotaId());

            return ResponseEntity.ok(preview);

        } catch (Exception e) {
            log.error("❌ Error uploading Excel ROTA", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to upload Excel ROTA: " + e.getMessage()));
        }
    }

    /**
     * Upload ROTA image (OCR-based - less accurate)
     * Admins and Super Admins can upload
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> uploadRota(@RequestParam("file") MultipartFile file) {
        try {
            log.info("📤 Received ROTA upload request: {}", file.getOriginalFilename());

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Please select a file to upload"));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Please upload an image file (PNG, JPG, JPEG)"));
            }

            // Validate file size (max 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("File size must be less than 10MB"));
            }

            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            RotaDTO result = rotaService.uploadRota(file, username);

            log.info("✅ ROTA uploaded successfully - ID: {}", result.getId());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error uploading ROTA", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to upload ROTA: " + e.getMessage()));
        }
    }

    /**
     * Create ROTA manually (form-based entry)
     * Admins and Super Admins can create
     */
    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> createManualRota(@RequestBody Map<String, Object> rotaData) {
        try {
            log.info("✏️ Received manual ROTA creation request");

            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Rota result = rotaService.createManualRota(rotaData, username);

            log.info("✅ Manual ROTA created successfully - ID: {}", result.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "ROTA created successfully!");
            response.put("rotaId", result.getId());
            response.put("hotelName", result.getHotelName());
            response.put("department", result.getDepartment());
            response.put("startDate", result.getStartDate());
            response.put("endDate", result.getEndDate());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error creating manual ROTA", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to create manual ROTA: " + e.getMessage()));
        }
    }

    /**
     * Get all ROTAs
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getAllRotas() {
        try {
            List<RotaDTO> rotas = rotaService.getAllRotas();
            return ResponseEntity.ok(rotas);
        } catch (Exception e) {
            log.error("❌ Error fetching ROTAs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch ROTAs: " + e.getMessage()));
        }
    }

    /**
     * Get ROTA schedules by ID for editing
     * Only ADMIN and SUPER_ADMIN can access for editing
     */
    @GetMapping("/{rotaId}/schedules")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getRotaSchedulesForEdit(@PathVariable Long rotaId) {
        try {
            log.info("📋 Fetching schedules for rota ID: {}", rotaId);
            List<RotaScheduleEntryDTO> schedules = rotaService.getRotaSchedules(rotaId);
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            log.error("❌ Error fetching ROTA schedules", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch schedules: " + e.getMessage()));
        }
    }

    /**
     * Get employee's current week ROTA schedule
     */
    @GetMapping("/employee/{employeeId}/current-week")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getEmployeeCurrentWeekSchedule(@PathVariable Long employeeId) {
        try {
            List<RotaScheduleDTO.DaySchedule> schedules = rotaService.getEmployeeCurrentWeekSchedule(employeeId);
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            log.error("❌ Error fetching employee ROTA schedule", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch employee schedule: " + e.getMessage()));
        }
    }

    /**
     * Get raw extracted text and detailed extraction data for a ROTA
     * Only accessible by ADMIN and SUPER_ADMIN for debugging
     */
    @GetMapping("/{rotaId}/extraction-details")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getExtractionDetails(@PathVariable Long rotaId) {
        try {
            log.info("📊 Fetching extraction details for ROTA ID: {}", rotaId);
            Map<String, Object> details = rotaService.getExtractionDetails(rotaId);
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            log.error("❌ Error fetching extraction details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch extraction details: " + e.getMessage()));
        }
    }

    /**
     * Get preview of uploaded ROTA with all schedules
     * Returns detailed preview for review before finalizing
     */
    @GetMapping("/{rotaId}/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getRotaPreview(@PathVariable Long rotaId) {
        try {
            log.info("📋 Fetching preview for ROTA ID: {}", rotaId);
            var preview = excelRotaService.getRotaPreview(rotaId);
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            log.error("❌ Error fetching ROTA preview", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch ROTA preview: " + e.getMessage()));
        }
    }

    /**
     * Delete entire ROTA including all schedules
     * Only accessible by ADMIN and SUPER_ADMIN
     */
    @DeleteMapping("/{rotaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteRota(@PathVariable Long rotaId) {
        try {
            log.info("🗑️ Received request to delete ROTA ID: {}", rotaId);
            excelRotaService.deleteRota(rotaId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "ROTA deleted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error deleting ROTA", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to delete ROTA: " + e.getMessage()));
        }
    }


    /**
     * Get a single schedule by ID
     * Accessible by ADMIN and SUPER_ADMIN
     */
    @GetMapping("/schedules/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getRotaSchedule(@PathVariable Long scheduleId) {
        try {
            log.info("📋 Fetching schedule ID: {}", scheduleId);
            RotaScheduleEntryDTO schedule = rotaService.getRotaSchedule(scheduleId);
            return ResponseEntity.ok(schedule);
        } catch (Exception e) {
            log.error("❌ Error fetching rota schedule", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch rota schedule: " + e.getMessage()));
        }
    }

    /**
     * Update a rota schedule
     * Only ADMIN and SUPER_ADMIN can edit rotas
     */
    @PutMapping("/schedules/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> updateRotaSchedule(
            @PathVariable Long scheduleId,
            @RequestBody RotaScheduleUpdateDTO updateDTO,
            HttpServletRequest request) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            log.info("✏️ Updating schedule ID: {} by user: {}", scheduleId, username);

            RotaScheduleEntryDTO updatedSchedule = rotaService.updateRotaSchedule(scheduleId, updateDTO, username, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Rota schedule updated successfully");
            response.put("schedule", updatedSchedule);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error updating rota schedule", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to update rota schedule: " + e.getMessage()));
        }
    }

    /**
     * Delete a single rota schedule
     * Only ADMIN and SUPER_ADMIN can delete schedules
     */
    @DeleteMapping("/schedules/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteRotaSchedule(
            @PathVariable Long scheduleId,
            HttpServletRequest request) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            log.info("🗑️ Deleting schedule ID: {} by user: {}", scheduleId, username);

            rotaService.deleteRotaSchedule(scheduleId, username, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Rota schedule deleted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error deleting rota schedule", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to delete rota schedule: " + e.getMessage()));
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}

