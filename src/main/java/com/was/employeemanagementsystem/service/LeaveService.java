package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.dto.LeaveBalanceDTO;
import com.was.employeemanagementsystem.dto.LeaveDTO;
import com.was.employeemanagementsystem.entity.Employee;
import com.was.employeemanagementsystem.entity.Leave;
import com.was.employeemanagementsystem.entity.LeaveBalance;
import com.was.employeemanagementsystem.entity.User;
import com.was.employeemanagementsystem.repository.EmployeeRepository;
import com.was.employeemanagementsystem.repository.LeaveBalanceRepository;
import com.was.employeemanagementsystem.repository.LeaveRepository;
import com.was.employeemanagementsystem.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class LeaveService {

    // Leave type constants
    private static final String LEAVE_TYPE_ANNUAL = "ANNUAL";
    private static final String LEAVE_TYPE_SICK = "SICK";
    private static final String LEAVE_TYPE_CASUAL = "CASUAL";
    private static final String LEAVE_TYPE_OTHER = "OTHER";

    // Leave allocations per financial year
    private static final int ANNUAL_LEAVE_ALLOCATION = 10;
    private static final int SICK_LEAVE_ALLOCATION = 5;
    private static final int CASUAL_LEAVE_ALLOCATION = 3;
    private static final int OTHER_LEAVE_ALLOCATION = 2;

    // Total leaves = 20
    private static final int TOTAL_LEAVE_ALLOCATION = ANNUAL_LEAVE_ALLOCATION +
                                                       SICK_LEAVE_ALLOCATION +
                                                       CASUAL_LEAVE_ALLOCATION +
                                                       OTHER_LEAVE_ALLOCATION;

    private final LeaveRepository leaveRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;

    public LeaveService(LeaveRepository leaveRepository,
                        LeaveBalanceRepository leaveBalanceRepository,
                        EmployeeRepository employeeRepository,
                        SecurityUtils securityUtils,
                        NotificationService notificationService) {
        this.leaveRepository = leaveRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.employeeRepository = employeeRepository;
        this.securityUtils = securityUtils;
        this.notificationService = notificationService;
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

    public LeaveDTO approveLeave(Long leaveId, String remarks) {
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
        leave.setApprovedBy(currentUser); // Store user ID via relationship
        leave.setApprovalDate(LocalDate.now());
        leave.setRemarks(remarks);

        Leave updatedLeave = leaveRepository.save(leave);

        // Create approval notification for the employee
        notificationService.createLeaveApprovalNotification(updatedLeave, currentUser.getUsername());

        return convertToDTO(updatedLeave);
    }

    public LeaveDTO rejectLeave(Long leaveId, String remarks) {
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
        leave.setApprovedBy(currentUser); // Store user ID via relationship (used for both approval and rejection)
        leave.setApprovalDate(LocalDate.now());
        leave.setRemarks(remarks);

        Leave updatedLeave = leaveRepository.save(leave);

        // Create rejection notification for the employee
        notificationService.createLeaveRejectionNotification(updatedLeave, currentUser.getUsername(), remarks);

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

    LeaveDTO convertToDTO(Leave leave) {
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
        // Extract username from User object for display
        dto.setApprovedBy(leave.getApprovedBy() != null ? leave.getApprovedBy().getUsername() : null);
        dto.setApprovalDate(leave.getApprovalDate());
        dto.setRemarks(leave.getRemarks());
        dto.setHasMedicalCertificate(leave.getMedicalCertificate() != null);
        dto.setHasHolidayForm(leave.getHolidayForm() != null);
        dto.setHolidayFormFileName(leave.getHolidayFormFileName());
        dto.setFinancialYear(leave.getFinancialYear());
        
        // Add leave balance information for admin/super admin users
        if (securityUtils.isAdminOrSuperAdmin()) {
            try {
                String financialYear = leave.getFinancialYear() != null 
                    ? leave.getFinancialYear() 
                    : getCurrentFinancialYear();
                
                // Initialize leave balances if they don't exist
                if (!leaveBalanceRepository.existsByEmployeeIdAndFinancialYear(
                        leave.getEmployee().getId(), financialYear)) {
                    log.info("Initializing leave balances for employee {} for financial year {}", 
                            leave.getEmployee().getId(), financialYear);
                    initializeLeaveBalances(leave.getEmployee().getId());
                }
                
                LeaveBalance balance = leaveBalanceRepository
                    .findByEmployeeIdAndFinancialYearAndLeaveType(
                        leave.getEmployee().getId(), 
                        financialYear, 
                        leave.getLeaveType()
                    )
                    .orElse(null);
                
                if (balance != null) {
                    dto.setTotalAllocated(balance.getTotalAllocated());
                    dto.setUsedLeaves(balance.getUsedLeaves());
                    dto.setRemainingLeaves(balance.getRemainingLeaves());
                } else {
                    log.warn("Leave balance not found for employee {}, financial year {}, leave type {}", 
                            leave.getEmployee().getId(), financialYear, leave.getLeaveType());
                }
            } catch (Exception e) {
                log.warn("Could not fetch leave balance for leave ID {}: {}", leave.getId(), e.getMessage());
                // Continue without balance information if there's an error
            }
        }
        
        return dto;
    }

    // ==================== NEW LEAVE BALANCE MANAGEMENT ====================

    /**
     * Initialize leave balances for a new employee
     */
    public void initializeLeaveBalances(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        String currentFinancialYear = getCurrentFinancialYear();

        // Check if already initialized
        if (leaveBalanceRepository.existsByEmployeeIdAndFinancialYear(employeeId, currentFinancialYear)) {
            log.info("Leave balances already initialized for employee: {} for FY: {}",
                    employeeId, currentFinancialYear);
            return;
        }

        User currentUser = securityUtils.getCurrentUser();
        Long organizationId = currentUser != null ? currentUser.getOrganizationId() : employee.getOrganizationId();

        // Create leave balances for all types
        createLeaveBalance(employee, currentFinancialYear, LEAVE_TYPE_ANNUAL, ANNUAL_LEAVE_ALLOCATION, organizationId);
        createLeaveBalance(employee, currentFinancialYear, LEAVE_TYPE_SICK, SICK_LEAVE_ALLOCATION, organizationId);
        createLeaveBalance(employee, currentFinancialYear, LEAVE_TYPE_CASUAL, CASUAL_LEAVE_ALLOCATION, organizationId);
        createLeaveBalance(employee, currentFinancialYear, LEAVE_TYPE_OTHER, OTHER_LEAVE_ALLOCATION, organizationId);

        log.info("✅ Leave balances initialized for employee: {} - Total: {} leaves",
                employee.getFullName(), TOTAL_LEAVE_ALLOCATION);
    }

    /**
     * Initialize leave balances for all employees who don't have them
     * This is useful for fixing existing employees who were created before leave balance initialization was added
     */
    public void initializeLeaveBalancesForAllEmployees() {
        log.info("🔍 Checking for employees without leave balances...");
        String currentFinancialYear = getCurrentFinancialYear();
        
        List<Employee> allEmployees = employeeRepository.findAll();
        int initializedCount = 0;
        int skippedCount = 0;
        
        for (Employee employee : allEmployees) {
            // Skip if already initialized
            if (leaveBalanceRepository.existsByEmployeeIdAndFinancialYear(employee.getId(), currentFinancialYear)) {
                skippedCount++;
                continue;
            }
            
            try {
                // Use employee's organization ID if available
                Long organizationId = employee.getOrganizationId();
                
                // Create leave balances for all types
                createLeaveBalance(employee, currentFinancialYear, LEAVE_TYPE_ANNUAL, ANNUAL_LEAVE_ALLOCATION, organizationId);
                createLeaveBalance(employee, currentFinancialYear, LEAVE_TYPE_SICK, SICK_LEAVE_ALLOCATION, organizationId);
                createLeaveBalance(employee, currentFinancialYear, LEAVE_TYPE_CASUAL, CASUAL_LEAVE_ALLOCATION, organizationId);
                createLeaveBalance(employee, currentFinancialYear, LEAVE_TYPE_OTHER, OTHER_LEAVE_ALLOCATION, organizationId);
                
                initializedCount++;
                log.info("✅ Initialized leave balances for employee: {} (ID: {})", 
                        employee.getFullName(), employee.getId());
            } catch (Exception e) {
                log.error("❌ Failed to initialize leave balances for employee {} (ID: {}): {}", 
                        employee.getFullName(), employee.getId(), e.getMessage());
            }
        }
        
        log.info("✅ Leave balance initialization complete - Initialized: {}, Already had balances: {}", 
                initializedCount, skippedCount);
    }

    /**
     * Get leave balances for an employee
     */
    public List<LeaveBalanceDTO> getLeaveBalances(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if (!canAccessEmployee(employee)) {
            throw new RuntimeException("Access denied");
        }

        String currentFinancialYear = getCurrentFinancialYear();

        // Initialize if not exists
        if (!leaveBalanceRepository.existsByEmployeeIdAndFinancialYear(employeeId, currentFinancialYear)) {
            initializeLeaveBalances(employeeId);
        }

        List<LeaveBalance> balances = leaveBalanceRepository.findByEmployeeIdAndFinancialYear(
                employeeId, currentFinancialYear);

        return balances.stream()
                .map(this::convertBalanceToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Apply leave with comprehensive validation
     */
    public LeaveDTO applyLeaveWithValidation(LeaveDTO leaveDTO, MultipartFile medicalCertificate, MultipartFile holidayForm) throws IOException {
        log.info("📝 Starting leave application process...");
        log.info("  - Employee ID: {}", leaveDTO.getEmployeeId());
        log.info("  - Leave Type: {}", leaveDTO.getLeaveType());
        log.info("  - Start Date: {}", leaveDTO.getStartDate());
        log.info("  - End Date: {}", leaveDTO.getEndDate());
        log.info("  - Reason: {}", leaveDTO.getReason());
        log.info("  - Medical Certificate: {}", medicalCertificate != null ? "Provided" : "Not provided");
        log.info("  - Holiday Form: {}", holidayForm != null ? "Provided" : "Not provided");

        Employee employee = employeeRepository.findById(leaveDTO.getEmployeeId())
                .orElseThrow(() -> {
                    log.error("❌ Employee not found with ID: {}", leaveDTO.getEmployeeId());
                    return new RuntimeException("Employee not found");
                });

        log.info("✅ Employee found: {} (ID: {})", employee.getFullName(), employee.getId());

        if (!canAccessEmployee(employee)) {
            log.error("❌ Access denied for employee: {}", employee.getId());
            throw new RuntimeException("Access denied");
        }

        String financialYear = getCurrentFinancialYear();
        log.info("📅 Current Financial Year: {}", financialYear);
        
        int leaveDays = calculateLeaveDays(leaveDTO.getStartDate(), leaveDTO.getEndDate());
        log.info("📊 Calculated leave days: {}", leaveDays);

        // Initialize leave balance if not exists
        if (!leaveBalanceRepository.existsByEmployeeIdAndFinancialYear(employee.getId(), financialYear)) {
            log.info("🔄 Leave balances not found, initializing...");
            initializeLeaveBalances(employee.getId());
        } else {
            log.info("✅ Leave balances already exist");
        }

        // Validate leave type
        validateLeaveType(leaveDTO.getLeaveType());
        log.info("✅ Leave type validated: {}", leaveDTO.getLeaveType());

        // Check leave balance
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndFinancialYearAndLeaveType(
                employee.getId(), financialYear, leaveDTO.getLeaveType())
                .orElseThrow(() -> {
                    log.error("❌ Leave balance not found - Employee: {}, FY: {}, Type: {}", 
                            employee.getId(), financialYear, leaveDTO.getLeaveType());
                    return new RuntimeException("Leave balance not found");
                });

        log.info("💰 Leave balance found - Total: {}, Used: {}, Remaining: {}", 
                balance.getTotalAllocated(), balance.getUsedLeaves(), balance.getRemainingLeaves());

        if (balance.getRemainingLeaves() < leaveDays) {
            log.error("❌ Insufficient leave balance - Available: {}, Requested: {}", 
                    balance.getRemainingLeaves(), leaveDays);
            throw new RuntimeException(String.format(
                    "Insufficient %s leave balance. Available: %d days, Requested: %d days",
                    leaveDTO.getLeaveType(), balance.getRemainingLeaves(), leaveDays));
        }

        // Rule: Sick leave > 2 days requires medical certificate
        if (LEAVE_TYPE_SICK.equals(leaveDTO.getLeaveType()) && leaveDays > 2) {
            if (medicalCertificate == null || medicalCertificate.isEmpty()) {
                log.error("❌ Medical certificate required for sick leave > 2 days");
                throw new RuntimeException(
                        "Medical certificate is required for sick leave more than 2 days");
            }
            log.info("✅ Medical certificate validated");
        }

        // Rule: Holiday form required for USER leave applications
        // - USER applying for their own leave: required
        // - ADMIN applying for their own leave: not required
        // - ADMIN applying for USER's leave: required
        User currentUser = securityUtils.getCurrentUser();
        boolean isCurrentUserAdmin = securityUtils.isAdminOrSuperAdmin();
        boolean isApplyingForOwnLeave = currentUser != null && employee.getUserId() != null 
                && employee.getUserId().equals(currentUser.getId());
        
        // Check if the employee (for whom leave is being applied) is a USER
        boolean isEmployeeUser = false;
        if (employee.getUserId() != null) {
            User employeeUser = securityUtils.getUserById(employee.getUserId());
            if (employeeUser != null) {
                isEmployeeUser = employeeUser.getRoles().contains("USER") 
                        && !employeeUser.getRoles().contains("ADMIN") 
                        && !employeeUser.getRoles().contains("SUPER_ADMIN");
            }
        }

        // Determine if holiday form is required
        boolean holidayFormRequired = false;
        if (isEmployeeUser) {
            if (isCurrentUserAdmin && !isApplyingForOwnLeave) {
                // ADMIN applying for USER's leave: required
                holidayFormRequired = true;
                log.info("📋 Holiday form required: ADMIN applying for USER's leave");
            } else if (!isCurrentUserAdmin) {
                // USER applying for their own leave: required
                holidayFormRequired = true;
                log.info("📋 Holiday form required: USER applying for own leave");
            } else {
                // ADMIN applying for their own leave: not required
                log.info("📋 Holiday form not required: ADMIN applying for own leave");
            }
        }

        // Validate holiday form requirement
        if (holidayFormRequired) {
            if (holidayForm == null || holidayForm.isEmpty()) {
                log.error("❌ Holiday form required for USER leave application");
                throw new RuntimeException(
                        "Holiday form is required for this leave application. Please upload the holiday form.");
            }
            log.info("✅ Holiday form validated");
        }

        // Rule: Casual leave cannot be consecutive
        if (LEAVE_TYPE_CASUAL.equals(leaveDTO.getLeaveType())) {
            if (leaveDays > 1) {
                log.error("❌ Casual leave cannot be > 1 day");
                throw new RuntimeException(
                        "Casual leave cannot be taken for more than 1 consecutive day");
            }

            // Check if there's adjacent casual leave
            if (hasAdjacentCasualLeave(employee.getId(), leaveDTO.getStartDate(), leaveDTO.getEndDate())) {
                log.error("❌ Adjacent casual leave found");
                throw new RuntimeException(
                        "Cannot apply consecutive casual leaves. Please choose a different date.");
            }
            log.info("✅ Casual leave validation passed");
        }

        // Create leave request
        log.info("🔨 Creating Leave entity...");
        Leave leave = new Leave();
        leave.setEmployee(employee);
        leave.setLeaveType(leaveDTO.getLeaveType());
        leave.setStartDate(leaveDTO.getStartDate());
        leave.setEndDate(leaveDTO.getEndDate());
        leave.setNumberOfDays(leaveDays);
        leave.setReason(leaveDTO.getReason());
        leave.setStatus("PENDING");
        leave.setAppliedDate(LocalDate.now());
        leave.setFinancialYear(financialYear);
        leave.setOrganizationId(employee.getOrganizationId());

        log.info("📋 Leave entity created with values:");
        log.info("  - Employee ID: {}", leave.getEmployee() != null ? leave.getEmployee().getId() : "NULL");
        log.info("  - Leave Type: {}", leave.getLeaveType());
        log.info("  - Start Date: {}", leave.getStartDate());
        log.info("  - End Date: {}", leave.getEndDate());
        log.info("  - Number of Days: {}", leave.getNumberOfDays());
        log.info("  - Status: {}", leave.getStatus());
        log.info("  - Applied Date: {}", leave.getAppliedDate());
        log.info("  - Financial Year: {}", leave.getFinancialYear());
        log.info("  - Organization ID: {}", leave.getOrganizationId());
        log.info("  - Reason: {}", leave.getReason());
        log.info("  - Medical Certificate: {}", leave.getMedicalCertificate() != null ? "Set" : "NULL");
        log.info("  - Certificate File Name: {}", leave.getCertificateFileName());
        log.info("  - Certificate Content Type: {}", leave.getCertificateContentType());

        // Store medical certificate if provided
        if (medicalCertificate != null && !medicalCertificate.isEmpty()) {
            leave.setMedicalCertificate(medicalCertificate.getBytes());
            leave.setCertificateFileName(medicalCertificate.getOriginalFilename());
            leave.setCertificateContentType(medicalCertificate.getContentType());
            log.info("📄 Medical certificate attached: {} ({} bytes)", 
                    medicalCertificate.getOriginalFilename(), medicalCertificate.getSize());
        }

        // Store holiday form if provided
        if (holidayForm != null && !holidayForm.isEmpty()) {
            leave.setHolidayForm(holidayForm.getBytes());
            leave.setHolidayFormFileName(holidayForm.getOriginalFilename());
            leave.setHolidayFormContentType(holidayForm.getContentType());
            log.info("📄 Holiday form attached: {} ({} bytes)", 
                    holidayForm.getOriginalFilename(), holidayForm.getSize());
        }

        try {
            log.info("💾 Attempting to save Leave entity to database...");
            Leave savedLeave = leaveRepository.save(leave);
            log.info("✅ Leave saved successfully - ID: {}", savedLeave.getId());
            log.info("✅ Leave application submitted - Employee: {}, Type: {}, Days: {}",
                    employee.getFullName(), leaveDTO.getLeaveType(), leaveDays);

            // Create notification
            try {
                notificationService.createLeaveRequestNotification(savedLeave);
                log.info("✅ Notification created");
            } catch (Exception e) {
                log.warn("⚠️ Failed to create notification: {}", e.getMessage());
                // Don't fail the leave creation if notification fails
            }

            return convertToDTO(savedLeave);
        } catch (Exception e) {
            log.error("❌ ERROR SAVING LEAVE ENTITY:");
            log.error("  - Error Type: {}", e.getClass().getName());
            log.error("  - Error Message: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("  - Cause: {}", e.getCause().getClass().getName());
                log.error("  - Cause Message: {}", e.getCause().getMessage());
            }
            log.error("  - Stack Trace:", e);
            
            // Log the actual SQL error if available
            
            throw e;
        }
    }

    /**
     * Approve leave and deduct from balance
     */
    public LeaveDTO approveLeaveAndDeduct(Long leaveId, String remarks) {
        User currentUser = securityUtils.getCurrentUser();
        
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (!"PENDING".equals(leave.getStatus())) {
            throw new RuntimeException("Leave is already " + leave.getStatus().toLowerCase());
        }

        // Validate approver permissions (existing logic)
        validateApprover(leave);

        // Approve leave
        leave.setStatus("APPROVED");
        leave.setApprovedBy(currentUser); // Store user ID via relationship
        leave.setApprovalDate(LocalDate.now());
        leave.setRemarks(remarks);

        // Deduct from leave balance
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndFinancialYearAndLeaveType(
                leave.getEmployee().getId(), leave.getFinancialYear(), leave.getLeaveType())
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));

        balance.setUsedLeaves(balance.getUsedLeaves() + leave.getNumberOfDays());
        balance.setRemainingLeaves(balance.getRemainingLeaves() - leave.getNumberOfDays());
        leaveBalanceRepository.save(balance);

        Leave savedLeave = leaveRepository.save(leave);

        log.info("✅ Leave approved and deducted - Employee: {}, Type: {}, Days: {}, Remaining: {}",
                leave.getEmployee().getFullName(), leave.getLeaveType(),
                leave.getNumberOfDays(), balance.getRemainingLeaves());

        // Create notification
        notificationService.createLeaveApprovalNotification(savedLeave, currentUser.getUsername());

        return convertToDTO(savedLeave);
    }

    /**
     * Get medical certificate
     */
    public byte[] getMedicalCertificate(Long leaveId) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        // Only ADMIN and SUPER_ADMIN can view certificates
        if (!securityUtils.isAdminOrSuperAdmin()) {
            throw new RuntimeException("Access denied. Only administrators can view medical certificates.");
        }

        if (leave.getMedicalCertificate() == null) {
            throw new RuntimeException("No medical certificate attached to this leave");
        }

        log.info("📄 Medical certificate viewed for leave ID: {} by: {}",
                leaveId, securityUtils.getCurrentUser().getUsername());

        return leave.getMedicalCertificate();
    }

    public byte[] getHolidayForm(Long leaveId) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        // Check access: USER can view their own holiday form, ADMIN/SUPER_ADMIN can view any
        if (!securityUtils.isAdminOrSuperAdmin()) {
            User currentUser = securityUtils.getCurrentUser();
            if (currentUser == null || leave.getEmployee().getUserId() == null 
                    || !leave.getEmployee().getUserId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied. You can only view your own holiday form.");
            }
        }

        if (leave.getHolidayForm() == null) {
            throw new RuntimeException("No holiday form attached to this leave");
        }

        log.info("📄 Holiday form viewed for leave ID: {} by: {}",
                leaveId, securityUtils.getCurrentUser().getUsername());

        return leave.getHolidayForm();
    }

    public String getHolidayFormFileName(Long leaveId) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));
        return leave.getHolidayFormFileName();
    }

    public String getHolidayFormContentType(Long leaveId) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));
        return leave.getHolidayFormContentType();
    }

    /**
     * Reset leave balances for new financial year
     */
    public void resetLeaveBalancesForNewFinancialYear() {
        String newFinancialYear = getCurrentFinancialYear();
        List<Employee> allEmployees = employeeRepository.findAll();

        for (Employee employee : allEmployees) {
            if (!leaveBalanceRepository.existsByEmployeeIdAndFinancialYear(
                    employee.getId(), newFinancialYear)) {
                initializeLeaveBalances(employee.getId());
            }
        }

        log.info("✅ Leave balances reset for financial year: {}", newFinancialYear);
    }

    // ==================== HELPER METHODS ====================

    private void createLeaveBalance(Employee employee, String financialYear,
                                    String leaveType, int allocation, Long organizationId) {
        LeaveBalance balance = new LeaveBalance();
        balance.setEmployee(employee);
        balance.setFinancialYear(financialYear);
        balance.setLeaveType(leaveType);
        balance.setTotalAllocated(allocation);
        balance.setUsedLeaves(0);
        balance.setRemainingLeaves(allocation);
        balance.setOrganizationId(organizationId);
        leaveBalanceRepository.save(balance);
    }

    private String getCurrentFinancialYear() {
        // Financial year runs from April to March
        // e.g., April 2024 to March 2025 = "2024-2025"
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        if (month >= 4) { // April to December
            return year + "-" + (year + 1);
        } else { // January to March
            return (year - 1) + "-" + year;
        }
    }

    private void validateLeaveType(String leaveType) {
        if (!List.of(LEAVE_TYPE_ANNUAL, LEAVE_TYPE_SICK, LEAVE_TYPE_CASUAL, LEAVE_TYPE_OTHER)
                .contains(leaveType)) {
            throw new RuntimeException("Invalid leave type: " + leaveType +
                    ". Valid types: ANNUAL, SICK, CASUAL, OTHER");
        }
    }

    private boolean hasAdjacentCasualLeave(Long employeeId, LocalDate startDate, LocalDate endDate) {
        // Check if there's a casual leave on the day before or after
        LocalDate dayBefore = startDate.minusDays(1);
        LocalDate dayAfter = endDate.plusDays(1);

        List<Leave> adjacentLeaves = leaveRepository.findByEmployeeIdAndStatus(employeeId, "APPROVED");

        return adjacentLeaves.stream()
                .filter(leave -> LEAVE_TYPE_CASUAL.equals(leave.getLeaveType()))
                .anyMatch(leave ->
                    (leave.getStartDate().equals(dayBefore) || leave.getEndDate().equals(dayBefore)) ||
                    (leave.getStartDate().equals(dayAfter) || leave.getEndDate().equals(dayAfter))
                );
    }

    private void validateApprover(Leave leave) {
        User currentUser = securityUtils.getCurrentUser();
        Employee leaveEmployee = leave.getEmployee();

        if (!securityUtils.isAdminOrSuperAdmin()) {
            throw new RuntimeException("Only administrators can approve leaves");
        }

        // Cannot approve own leave
        if (currentUser.getId().equals(leaveEmployee.getUserId())) {
            throw new RuntimeException("You cannot approve your own leave request");
        }

        // Check role-based approval rules
        if (leaveEmployee.getUserId() != null) {
            User leaveUser = securityUtils.getUserById(leaveEmployee.getUserId());

            if (leaveUser != null && leaveUser.getRoles().contains("ADMIN")) {
                if (!securityUtils.isSuperAdmin()) {
                    throw new RuntimeException("ADMIN leave requests can only be approved by SUPER_ADMIN");
                }
            }

            // ADMIN can only approve USER leaves in their department
            if (securityUtils.isAdmin() && !securityUtils.isSuperAdmin()) {
                Employee adminEmployee = employeeRepository.findByUserId(currentUser.getId())
                        .orElseThrow(() -> new RuntimeException("Admin employee profile not found"));

                if (leaveEmployee.getDepartment() == null || adminEmployee.getDepartment() == null ||
                    !adminEmployee.getDepartment().getId().equals(leaveEmployee.getDepartment().getId())) {
                    throw new RuntimeException("You can only approve leaves for employees in your department");
                }
            }
        }
    }

    private LeaveBalanceDTO convertBalanceToDTO(LeaveBalance balance) {
        LeaveBalanceDTO dto = new LeaveBalanceDTO();
        dto.setId(balance.getId());
        dto.setEmployeeId(balance.getEmployee().getId());
        dto.setEmployeeName(balance.getEmployee().getFullName());
        dto.setFinancialYear(balance.getFinancialYear());
        dto.setLeaveType(balance.getLeaveType());
        dto.setTotalAllocated(balance.getTotalAllocated());
        dto.setUsedLeaves(balance.getUsedLeaves());
        dto.setRemainingLeaves(balance.getRemainingLeaves());
        return dto;
    }
}

