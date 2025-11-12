package com.was.employeemanagementsystem.controller;

import com.was.employeemanagementsystem.constants.AppConstants;
import com.was.employeemanagementsystem.dto.LeaveBalanceDTO;
import com.was.employeemanagementsystem.dto.LeaveDTO;
import com.was.employeemanagementsystem.service.LeaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(AppConstants.API_LEAVES_PATH)
@CrossOrigin(origins = "${app.cors.origins}")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    /**
     * Apply leave with validation and medical certificate support
     * ROOT cannot access leave features
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> applyLeave(
            @RequestParam("employeeId") Long employeeId,
            @RequestParam("leaveType") String leaveType,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("reason") String reason,
            @RequestParam(value = "medicalCertificate", required = false) MultipartFile medicalCertificate) {
        try {
            LeaveDTO leaveDTO = new LeaveDTO();
            leaveDTO.setEmployeeId(employeeId);
            leaveDTO.setLeaveType(leaveType);
            leaveDTO.setStartDate(LocalDate.parse(startDate));
            leaveDTO.setEndDate(LocalDate.parse(endDate));
            leaveDTO.setReason(reason);

            LeaveDTO createdLeave = leaveService.applyLeaveWithValidation(leaveDTO, medicalCertificate);
            return new ResponseEntity<>(createdLeave, HttpStatus.CREATED);
        } catch (IOException e) {
            log.error("Error processing medical certificate", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process medical certificate: " + e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all leaves - Only USER, ADMIN, SUPER_ADMIN
     * ROOT cannot access leave features
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<LeaveDTO>> getAllLeaves() {
        List<LeaveDTO> leaves = leaveService.getAllLeaves();
        return ResponseEntity.ok(leaves);
    }

    /**
     * Get leave by ID - Only USER, ADMIN, SUPER_ADMIN
     * ROOT cannot access leave features
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<LeaveDTO> getLeaveById(@PathVariable Long id) {
        LeaveDTO leave = leaveService.getLeaveById(id);
        return ResponseEntity.ok(leave);
    }

    /**
     * Get leaves by employee ID - Only USER, ADMIN, SUPER_ADMIN
     * ROOT cannot access leave features
     */
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<LeaveDTO>> getLeavesByEmployeeId(@PathVariable Long employeeId) {
        List<LeaveDTO> leaves = leaveService.getLeavesByEmployeeId(employeeId);
        return ResponseEntity.ok(leaves);
    }

    /**
     * Get leaves by status - Only USER, ADMIN, SUPER_ADMIN
     * ROOT cannot access leave features
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<LeaveDTO>> getLeavesByStatus(@PathVariable String status) {
        List<LeaveDTO> leaves = leaveService.getLeavesByStatus(status);
        return ResponseEntity.ok(leaves);
    }

    /**
     * Update leave - Only USER, ADMIN, SUPER_ADMIN
     * ROOT cannot access leave features
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<LeaveDTO> updateLeave(@PathVariable Long id, @RequestBody LeaveDTO leaveDTO) {
        LeaveDTO updatedLeave = leaveService.updateLeave(id, leaveDTO);
        return ResponseEntity.ok(updatedLeave);
    }

    /**
     * Approve leave with automatic balance deduction - Only ADMIN, SUPER_ADMIN
     * ROOT cannot access leave features
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> approveLeave(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String approvedBy = request.get("approvedBy");
            String remarks = request.get("remarks");
            // Use new method that deducts from balance
            LeaveDTO approvedLeave = leaveService.approveLeaveAndDeduct(id, approvedBy, remarks);
            return ResponseEntity.ok(approvedLeave);
        } catch (RuntimeException e) {
            log.error("Error approving leave: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Reject leave - Only ADMIN, SUPER_ADMIN
     * ROOT cannot access leave features
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> rejectLeave(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String rejectedBy = request.get("rejectedBy");
            String remarks = request.get("remarks");
            LeaveDTO rejectedLeave = leaveService.rejectLeave(id, rejectedBy, remarks);
            return ResponseEntity.ok(rejectedLeave);
        } catch (RuntimeException e) {
            log.error("Error rejecting leave: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get leave balances for an employee
     */
    @GetMapping("/balances/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getLeaveBalances(@PathVariable Long employeeId) {
        try {
            List<LeaveBalanceDTO> balances = leaveService.getLeaveBalances(employeeId);
            return ResponseEntity.ok(balances);
        } catch (RuntimeException e) {
            log.error("Error fetching leave balances: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get blocked dates (already approved or pending leaves) for an employee
     */
    @GetMapping("/blocked-dates/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getBlockedDates(@PathVariable Long employeeId) {
        try {
            List<LeaveDTO> leaves = leaveService.getLeavesByEmployeeId(employeeId);

            // Filter for APPROVED and PENDING leaves
            List<Map<String, Object>> blockedDates = leaves.stream()
                    .filter(leave -> "APPROVED".equals(leave.getStatus()) ||
                                   "PENDING".equals(leave.getStatus()))
                    .map(leave -> {
                        Map<String, Object> dateInfo = new HashMap<>();
                        dateInfo.put("startDate", leave.getStartDate());
                        dateInfo.put("endDate", leave.getEndDate());
                        dateInfo.put("status", leave.getStatus());
                        dateInfo.put("leaveType", leave.getLeaveType());
                        return dateInfo;
                    })
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(blockedDates);
        } catch (RuntimeException e) {
            log.error("Error fetching blocked dates: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get medical certificate for a leave
     */
    @GetMapping("/{id}/certificate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getMedicalCertificate(@PathVariable Long id) {
        try {
            byte[] certificate = leaveService.getMedicalCertificate(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG); // Default to JPEG
            headers.setContentDispositionFormData("attachment", "medical-certificate-" + id + ".jpg");

            return new ResponseEntity<>(certificate, headers, HttpStatus.OK);
        } catch (RuntimeException e) {
            log.error("Error fetching medical certificate: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete leave - Only USER, ADMIN, SUPER_ADMIN
     * ROOT cannot access leave features
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteLeave(@PathVariable Long id) {
        leaveService.deleteLeave(id);
        return ResponseEntity.noContent().build();
    }
}

