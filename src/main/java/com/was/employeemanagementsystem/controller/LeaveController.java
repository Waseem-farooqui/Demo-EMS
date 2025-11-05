package com.was.employeemanagementsystem.controller;

import com.was.employeemanagementsystem.dto.LeaveDTO;
import com.was.employeemanagementsystem.service.LeaveService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaves")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    /**
     * Apply leave - Only USER, ADMIN, SUPER_ADMIN
     * ROOT cannot access leave features
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<LeaveDTO> applyLeave(@RequestBody LeaveDTO leaveDTO) {
        LeaveDTO createdLeave = leaveService.applyLeave(leaveDTO);
        return new ResponseEntity<>(createdLeave, HttpStatus.CREATED);
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
     * Approve leave - Only ADMIN, SUPER_ADMIN
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
            LeaveDTO approvedLeave = leaveService.approveLeave(id, approvedBy, remarks);
            return ResponseEntity.ok(approvedLeave);
        } catch (RuntimeException e) {
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

