package com.was.employeemanagementsystem.service;


import com.was.employeemanagementsystem.dto.LeaveDTO;
import com.was.employeemanagementsystem.entity.Employee;
import com.was.employeemanagementsystem.entity.Leave;
import com.was.employeemanagementsystem.entity.User;
import com.was.employeemanagementsystem.repository.EmployeeRepository;
import com.was.employeemanagementsystem.repository.LeaveRepository;
import com.was.employeemanagementsystem.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.logging.LoggersEndpoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class LeaveService {

    private final LeaveRepository leaveRepository;
    private final EmployeeRepository employeeRepository;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;
    private final LoggersEndpoint loggersEndpoint;

    public LeaveService(LeaveRepository leaveRepository,
                        EmployeeRepository employeeRepository,
                        SecurityUtils securityUtils,
                        NotificationService notificationService, LoggersEndpoint loggersEndpoint) {
        this.leaveRepository = leaveRepository;
        this.employeeRepository = employeeRepository;
        this.securityUtils = securityUtils;
        this.notificationService = notificationService;
        this.loggersEndpoint = loggersEndpoint;
    }

    public LeaveDTO applyLeave(LeaveDTO leaveDTO) {
        Employee employee = employeeRepository.findById(leaveDTO.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + leaveDTO.getEmployeeId()));

        // Check if user can apply leave for this employee
        if (!canAccessEmployee(employee)) {
            throw new RuntimeException("Access denied. You can only apply leave for yourself.");
        }

        Leave leave = new Leave();
        leave.setEmployee(employee);
        leave.setLeaveType(leaveDTO.getLeaveType());
        leave.setStartDate(leaveDTO.getStartDate());
        leave.setEndDate(leaveDTO.getEndDate());

        // Calculate number of days
        int days = calculateLeaveDays(leaveDTO.getStartDate(), leaveDTO.getEndDate());
        leave.setNumberOfDays(days);

        leave.setReason(leaveDTO.getReason());
        leave.setStatus("PENDING");
        leave.setAppliedDate(LocalDate.now());

        Leave savedLeave = leaveRepository.save(leave);

        // Create notification for approvers
        notificationService.createLeaveRequestNotification(savedLeave);

        return convertToDTO(savedLeave);
    }

    public LeaveDTO approveLeave(Long leaveId, String approvedBy, String remarks) {
        User currentUser = securityUtils.getCurrentUser();

        // Only admins and super admins can approve leaves
        if (!securityUtils.isAdminOrSuperAdmin()) {
            throw new RuntimeException("Access denied. Only admins can approve leaves.");
        }

        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found with id: " + leaveId));

        if (!"PENDING".equals(leave.getStatus())) {
            throw new RuntimeException("Leave is already " + leave.getStatus().toLowerCase());
        }

        // Get the employee who requested the leave
        Employee leaveEmployee = leave.getEmployee();

        // Check if the employee has a user account and get their role
        if (leaveEmployee.getUserId() != null) {
            User leaveUser = securityUtils.getUserById(leaveEmployee.getUserId());

            // Rule 1: ADMINs cannot approve their own leaves
            if (currentUser.getId().equals(leaveEmployee.getUserId())) {
                throw new RuntimeException("You cannot approve your own leave request. Please contact your supervisor.");
            }

            // Rule 2: ADMIN leaves must be approved by SUPER_ADMIN only
            if (leaveUser != null && leaveUser.getRoles().contains("ADMIN")) {
                if (!securityUtils.isSuperAdmin()) {
                    throw new RuntimeException("ADMIN leave requests can only be approved by SUPER_ADMIN.");
                }
            }

            // Rule 3: ADMIN can approve USER leaves in their department
            if (securityUtils.isAdmin() && !securityUtils.isSuperAdmin()) {
                // Verify this is a USER role employee
                if (leaveUser != null && (leaveUser.getRoles().contains("ADMIN") || leaveUser.getRoles().contains("SUPER_ADMIN"))) {
                    throw new RuntimeException("You can only approve leaves for regular employees (USER role).");
                }

                // Verify same department
                Employee adminEmployee = employeeRepository.findByUserId(currentUser.getId())
                        .orElseThrow(() -> new RuntimeException("Admin employee profile not found"));

                if (adminEmployee.getDepartment() == null || leaveEmployee.getDepartment() == null ||
                    !adminEmployee.getDepartment().getId().equals(leaveEmployee.getDepartment().getId())) {
                    throw new RuntimeException("You can only approve leaves for employees in your department.");
                }
            }
        }

        leave.setStatus("APPROVED");
        leave.setApprovedBy(approvedBy);
        leave.setApprovalDate(LocalDate.now());
        leave.setRemarks(remarks);

        Leave updatedLeave = leaveRepository.save(leave);

        // Create approval notification for the employee
        notificationService.createLeaveApprovalNotification(updatedLeave, approvedBy);

        return convertToDTO(updatedLeave);
    }

    public LeaveDTO rejectLeave(Long leaveId, String rejectedBy, String remarks) {
        User currentUser = securityUtils.getCurrentUser();

        // Only admins and super admins can reject leaves
        if (!securityUtils.isAdminOrSuperAdmin()) {
            throw new RuntimeException("Access denied. Only admins can reject leaves.");
        }

        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found with id: " + leaveId));

        if (!"PENDING".equals(leave.getStatus())) {
            throw new RuntimeException("Leave is already " + leave.getStatus().toLowerCase());
        }

        // Get the employee who requested the leave
        Employee leaveEmployee = leave.getEmployee();

        // Check if the employee has a user account and get their role
        if (leaveEmployee.getUserId() != null) {
            User leaveUser = securityUtils.getUserById(leaveEmployee.getUserId());

            // Rule 1: ADMINs cannot reject their own leaves
            if (currentUser.getId().equals(leaveEmployee.getUserId())) {
                throw new RuntimeException("You cannot reject your own leave request. Please contact your supervisor.");
            }

            // Rule 2: ADMIN leaves must be rejected by SUPER_ADMIN only
            if (leaveUser != null && leaveUser.getRoles().contains("ADMIN")) {
                if (!securityUtils.isSuperAdmin()) {
                    throw new RuntimeException("ADMIN leave requests can only be rejected by SUPER_ADMIN.");
                }
            }

            // Rule 3: ADMIN can reject USER leaves in their department
            if (securityUtils.isAdmin() && !securityUtils.isSuperAdmin()) {
                // Verify this is a USER role employee
                if (leaveUser != null && (leaveUser.getRoles().contains("ADMIN") || leaveUser.getRoles().contains("SUPER_ADMIN"))) {
                    throw new RuntimeException("You can only reject leaves for regular employees (USER role).");
                }

                // Verify same department
                Employee adminEmployee = employeeRepository.findByUserId(currentUser.getId())
                        .orElseThrow(() -> new RuntimeException("Admin employee profile not found"));

                if (adminEmployee.getDepartment() == null || leaveEmployee.getDepartment() == null ||
                    !adminEmployee.getDepartment().getId().equals(leaveEmployee.getDepartment().getId())) {
                    throw new RuntimeException("You can only reject leaves for employees in your department.");
                }
            }
        }

        leave.setStatus("REJECTED");
        leave.setApprovedBy(rejectedBy);
        leave.setApprovalDate(LocalDate.now());
        leave.setRemarks(remarks);

        Leave updatedLeave = leaveRepository.save(leave);

        // Create rejection notification for the employee
        notificationService.createLeaveRejectionNotification(updatedLeave, rejectedBy, remarks);

        return convertToDTO(updatedLeave);
    }

    public List<LeaveDTO> getAllLeaves() {
        // Admins and Super Admins can see all leaves, users can only see their own
        if (securityUtils.isAdminOrSuperAdmin()) {
            return leaveRepository.findAll().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } else {
            // Regular users can only see their own leaves
            User currentUser = securityUtils.getCurrentUser();
            if (currentUser != null) {
                return employeeRepository.findByUserId(currentUser.getId())
                        .map(Employee::getId)
                        .map(leaveRepository::findByEmployeeId)
                        .orElseGet(() -> List.of())
                        .stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
            }
            return List.of();
        }
    }

    public List<LeaveDTO> getLeavesByEmployeeId(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

        // Check if user can access this employee's leaves
        if (!canAccessEmployee(employee)) {
            throw new RuntimeException("Access denied. You can only view your own leaves.");
        }

        return leaveRepository.findByEmployeeId(employeeId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<LeaveDTO> getLeavesByStatus(String status) {
        // Admins and Super Admins can see all leaves by status, users can only see their own
        if (securityUtils.isAdminOrSuperAdmin()) {
            return leaveRepository.findByStatus(status).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } else {
            User currentUser = securityUtils.getCurrentUser();
            if (currentUser != null) {
                return employeeRepository.findByUserId(currentUser.getId())
                        .map(Employee::getId)
                        .map(employeeId -> leaveRepository.findByEmployeeIdAndStatus(employeeId, status))
                        .orElseGet(() -> List.of())
                        .stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
            }
            return List.of();
        }
    }

    public LeaveDTO getLeaveById(Long id) {
        Leave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave not found with id: " + id));

        // Check if user can access this leave
        if (!canAccessEmployee(leave.getEmployee())) {
            throw new RuntimeException("Access denied. You can only view your own leaves.");
        }

        return convertToDTO(leave);
    }

    public void deleteLeave(Long id) {
        Leave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave not found with id: " + id));

        // Check if user can delete this leave
        if (!canAccessEmployee(leave.getEmployee())) {
            throw new RuntimeException("Access denied. You can only delete your own leaves.");
        }

        if ("APPROVED".equals(leave.getStatus())) {
            throw new RuntimeException("Cannot delete approved leave");
        }

        leaveRepository.deleteById(id);
    }

    public LeaveDTO updateLeave(Long id, LeaveDTO leaveDTO) {
        Leave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave not found with id: " + id));

        // Check if user can update this leave
        if (!canAccessEmployee(leave.getEmployee())) {
            throw new RuntimeException("Access denied. You can only update your own leaves.");
        }

        if (!"PENDING".equals(leave.getStatus())) {
            throw new RuntimeException("Cannot update leave that is not pending");
        }

        leave.setLeaveType(leaveDTO.getLeaveType());
        leave.setStartDate(leaveDTO.getStartDate());
        leave.setEndDate(leaveDTO.getEndDate());
        leave.setNumberOfDays(calculateLeaveDays(leaveDTO.getStartDate(), leaveDTO.getEndDate()));
        leave.setReason(leaveDTO.getReason());

        Leave updatedLeave = leaveRepository.save(leave);
        return convertToDTO(updatedLeave);
    }

    private boolean canAccessEmployee(Employee employee) {
        // ROOT cannot access employee data including leaves
        if (securityUtils.isRoot()) {
            log.warn("⚠️ ROOT user attempted to access employee leaves - Access denied");
            return false;
        }

        // Admins and Super Admins can access any employee
        if (securityUtils.isAdminOrSuperAdmin()) {
            return true;
        }

        // Users can only access their own employee record
        User currentUser = securityUtils.getCurrentUser();
        return currentUser != null && employee.getUserId() != null
                && employee.getUserId().equals(currentUser.getId());
    }

    private int calculateLeaveDays(LocalDate startDate, LocalDate endDate) {
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    private LeaveDTO convertToDTO(Leave leave) {
        LeaveDTO dto = new LeaveDTO();
        dto.setId(leave.getId());
        dto.setEmployeeId(leave.getEmployee().getId());
        dto.setEmployeeName(leave.getEmployee().getFullName());
        dto.setLeaveType(leave.getLeaveType());
        dto.setStartDate(leave.getStartDate());
        dto.setEndDate(leave.getEndDate());
        dto.setNumberOfDays(leave.getNumberOfDays());
        dto.setReason(leave.getReason());
        dto.setStatus(leave.getStatus());
        dto.setAppliedDate(leave.getAppliedDate());
        dto.setApprovedBy(leave.getApprovedBy());
        dto.setApprovalDate(leave.getApprovalDate());
        dto.setRemarks(leave.getRemarks());
        return dto;
    }
}

