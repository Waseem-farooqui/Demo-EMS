package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.dto.EmployeeDTO;
import com.was.employeemanagementsystem.dto.EmploymentRecordDTO;
import com.was.employeemanagementsystem.dto.NextOfKinDTO;
import com.was.employeemanagementsystem.dto.PageResponse;
import com.was.employeemanagementsystem.entity.Department;
import com.was.employeemanagementsystem.entity.Employee;
import com.was.employeemanagementsystem.entity.EmploymentRecord;
import com.was.employeemanagementsystem.entity.NextOfKin;
import com.was.employeemanagementsystem.entity.User;
import com.was.employeemanagementsystem.entity.Document;
import com.was.employeemanagementsystem.entity.Leave;
import com.was.employeemanagementsystem.entity.LeaveBalance;
import com.was.employeemanagementsystem.entity.Attendance;
import com.was.employeemanagementsystem.entity.RotaSchedule;
import com.was.employeemanagementsystem.entity.RotaChangeLog;
import com.was.employeemanagementsystem.exception.DuplicateResourceException;
import com.was.employeemanagementsystem.exception.ResourceNotFoundException;
import com.was.employeemanagementsystem.exception.ValidationException;
import com.was.employeemanagementsystem.repository.DepartmentRepository;
import com.was.employeemanagementsystem.repository.EmployeeRepository;
import com.was.employeemanagementsystem.repository.OrganizationRepository;
import com.was.employeemanagementsystem.repository.UserRepository;
import com.was.employeemanagementsystem.repository.DocumentRepository;
import com.was.employeemanagementsystem.repository.LeaveRepository;
import com.was.employeemanagementsystem.repository.LeaveBalanceRepository;
import com.was.employeemanagementsystem.repository.AttendanceRepository;
import com.was.employeemanagementsystem.repository.RotaScheduleRepository;
import com.was.employeemanagementsystem.repository.RotaChangeLogRepository;
import com.was.employeemanagementsystem.repository.NotificationRepository;
import com.was.employeemanagementsystem.entity.Notification;
import com.was.employeemanagementsystem.security.SecurityUtils;
import com.was.employeemanagementsystem.util.PasswordGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final OrganizationRepository organizationRepository;
    private final SecurityUtils securityUtils;
    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerator passwordGenerator;
    private final EmailService emailService;
    private final LeaveService leaveService;
    private final DocumentRepository documentRepository;
    private final LeaveRepository leaveRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final AttendanceRepository attendanceRepository;
    private final RotaScheduleRepository rotaScheduleRepository;
    private final RotaChangeLogRepository rotaChangeLogRepository;
    private final NotificationRepository notificationRepository;

    public EmployeeService(EmployeeRepository employeeRepository,
                          UserRepository userRepository,
                          DepartmentRepository departmentRepository,
                          OrganizationRepository organizationRepository,
                          SecurityUtils securityUtils,
                          PasswordEncoder passwordEncoder,
                          PasswordGenerator passwordGenerator,
                          EmailService emailService,
                          LeaveService leaveService,
                          DocumentRepository documentRepository,
                          LeaveRepository leaveRepository,
                          LeaveBalanceRepository leaveBalanceRepository,
                          AttendanceRepository attendanceRepository,
                          RotaScheduleRepository rotaScheduleRepository,
                          RotaChangeLogRepository rotaChangeLogRepository,
                          NotificationRepository notificationRepository) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.organizationRepository = organizationRepository;
        this.securityUtils = securityUtils;
        this.passwordEncoder = passwordEncoder;
        this.passwordGenerator = passwordGenerator;
        this.emailService = emailService;
        this.leaveService = leaveService;
        this.documentRepository = documentRepository;
        this.leaveRepository = leaveRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.attendanceRepository = attendanceRepository;
        this.rotaScheduleRepository = rotaScheduleRepository;
        this.rotaChangeLogRepository = rotaChangeLogRepository;
        this.notificationRepository = notificationRepository;
    }

    public EmployeeDTO createEmployee(EmployeeDTO employeeDTO) {
        // Only SUPER_ADMIN and ADMIN can create employees
        if (!securityUtils.isAdminOrSuperAdmin()) {
            log.warn("Non-admin user attempted to create employee");
            throw new AccessDeniedException("Access denied. Only administrators can create employees.");
        }

        // CRITICAL: Validate organization UUID
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null || currentUser.getOrganizationUuid() == null) {
            log.error("❌ Admin user has no organization UUID!");
            throw new AccessDeniedException("User must be associated with an organization");
        }

        log.debug("✓ Creating employee for organization UUID: {}", currentUser.getOrganizationUuid());

        // Check for duplicate work email within SAME organization only (multi-tenancy)
        if (employeeRepository.existsByWorkEmailAndOrganizationId(
                employeeDTO.getWorkEmail(),
                currentUser.getOrganizationId())) {
            log.warn("Attempt to create employee with duplicate email: {} in organization: {}",
                    employeeDTO.getWorkEmail(), currentUser.getOrganizationId());
            throw new DuplicateResourceException(
                "An employee with email '" + employeeDTO.getWorkEmail() +
                "' already exists in your organization"
            );
        }

        // Create employee
        Employee employee = convertToEntity(employeeDTO);

        // Set organization ID and UUID from current user
        employee.setOrganizationId(currentUser.getOrganizationId());
        employee.setOrganizationUuid(currentUser.getOrganizationUuid());
        log.debug("✓ Set organization UUID for new employee: {}", currentUser.getOrganizationUuid());

        // If ADMIN (not SUPER_ADMIN), automatically assign to admin's department
        if (securityUtils.isAdmin() && !securityUtils.isSuperAdmin()) {
            if (currentUser != null) {
                employeeRepository.findByUserId(currentUser.getId()).ifPresent(adminEmployee -> {
                    if (adminEmployee.getDepartment() != null) {
                        employee.setDepartment(adminEmployee.getDepartment());
                    }
                });
            }
        }

        Employee savedEmployee = employeeRepository.save(employee);
        log.info("✓ Employee created - ID: {}, Name: {}", savedEmployee.getId(), savedEmployee.getFullName());

        // Automatically create user account with organization-specific username
        log.info("📝 Starting username generation process...");
        log.info("📝 Employee email: {}", savedEmployee.getWorkEmail());
        log.info("📝 Employee name: {}", savedEmployee.getFullName());
        log.info("📝 Current user: {} (ID: {}, OrgID: {})",
                currentUser.getUsername(), currentUser.getId(), currentUser.getOrganizationId());

        String username = generateUsername(savedEmployee.getWorkEmail(), savedEmployee.getFullName(), currentUser);
        String temporaryPassword = passwordGenerator.generateTemporaryPassword();

        // Username is already organization-specific, but check for duplicates within same organization
        String finalUsername = username;
        int counter = 1;
        while (userRepository.existsByUsernameAndOrganizationId(finalUsername, currentUser.getOrganizationId())) {
            finalUsername = username + counter;
            counter++;
            log.info("🔄 Username exists, trying: {}", finalUsername);
        }

        log.info("✅ Generated username: {} for employee: {}", finalUsername, savedEmployee.getFullName());

        // Check if email already exists in SAME organization only (multi-tenancy)
        // Different organizations CAN have users with same email
        if (userRepository.existsByEmailAndOrganizationId(savedEmployee.getWorkEmail(), currentUser.getOrganizationId())) {
            log.warn("User with email {} already exists in organization {}",
                    savedEmployee.getWorkEmail(), currentUser.getOrganizationId());
            throw new DuplicateResourceException(
                "A user account with email '" + savedEmployee.getWorkEmail() +
                "' already exists in your organization"
            );
        }

        User user = new User();
        user.setUsername(finalUsername);
        user.setEmail(savedEmployee.getWorkEmail());
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.getRoles().add("USER"); // Default role
        user.setEnabled(true);
        user.setEmailVerified(true); // Pre-verified by admin
        user.setFirstLogin(true);
        user.setProfileCompleted(false);
        user.setTemporaryPassword(true);
        user.setOrganizationId(currentUser.getOrganizationId());
        user.setOrganizationUuid(currentUser.getOrganizationUuid());
        log.debug("✓ Set organization UUID for new user: {}", currentUser.getOrganizationUuid());

        User savedUser = userRepository.save(user);
        log.info("✓ User account created - Username: {}", savedUser.getUsername());

        // Link employee to user
        savedEmployee.setUserId(savedUser.getId());
        employeeRepository.save(savedEmployee);

        // Initialize leave balances for new employee
        try {
            leaveService.initializeLeaveBalances(savedEmployee.getId());
            log.info("✅ Leave balances initialized for new employee: {}", savedEmployee.getFullName());
        } catch (Exception e) {
            log.error("❌ Failed to initialize leave balances: {}", e.getMessage());
        }

        // Skip sending email for USER role employees
        // Credentials are only displayed to admin, not emailed to the user
        log.info("⏭️  Skipping email for USER role employee - credentials will be displayed to admin only");

        log.info("✓ Account creation complete for employee: {} with username: {}",
            savedEmployee.getFullName(), finalUsername);

        return convertToDTO(savedEmployee);
    }

    /**
     * Allow users to create their own employee profile
     */
    public EmployeeDTO createSelfProfile(EmployeeDTO employeeDTO) {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ValidationException("User authentication required. Please log in again.");
        }

        // Check if user already has an employee profile
        if (employeeRepository.findByUserId(currentUser.getId()).isPresent()) {
            log.warn("User {} already has an employee profile", currentUser.getUsername());
            throw new DuplicateResourceException(
                "You already have an employee profile. Please contact your administrator if you need to update it."
            );
        }

        // Validate required fields
        if (employeeDTO.getFullName() == null || employeeDTO.getFullName().trim().isEmpty()) {
            throw new ValidationException("Full name is required");
        }
        if (employeeDTO.getJobTitle() == null || employeeDTO.getJobTitle().trim().isEmpty()) {
            throw new ValidationException("Job title is required");
        }
        if (employeeDTO.getWorkEmail() == null || employeeDTO.getWorkEmail().trim().isEmpty()) {
            throw new ValidationException("Work email is required");
        }

        // Check if email already exists in SAME organization only (multi-tenancy)
        if (employeeRepository.existsByWorkEmailAndOrganizationId(
                employeeDTO.getWorkEmail(),
                currentUser.getOrganizationId())) {
            log.warn("Attempt to create profile with duplicate email: {} in organization: {}",
                    employeeDTO.getWorkEmail(), currentUser.getOrganizationId());
            throw new DuplicateResourceException(
                "An employee with email '" + employeeDTO.getWorkEmail() +
                "' already exists in your organization. " +
                "If this is your email, please contact your administrator."
            );
        }

        // Create employee profile linked to current user
        Employee employee = convertToEntity(employeeDTO);
        employee.setUserId(currentUser.getId());
        employee.setOrganizationId(currentUser.getOrganizationId());
        employee.setOrganizationUuid(currentUser.getOrganizationUuid());
        Employee savedEmployee = employeeRepository.save(employee);

        log.info("✓ Self-service profile created for user: {}, employee ID: {}, Org UUID: {}",
                currentUser.getUsername(), savedEmployee.getId(), currentUser.getOrganizationUuid());

        return convertToDTO(savedEmployee);
    }

    private String generateUsername(String email, String fullName, User currentUser) {
        log.info("🔍 Generating username for: {} (email: {})", fullName, email);
        log.info("🔍 Current user org ID: {}", currentUser.getOrganizationId());

        // Try to generate from full name first for better readability
        String baseUsername;

        if (fullName != null && !fullName.trim().isEmpty()) {
            // Convert full name to username format: "John Smith" -> "john.smith"
            baseUsername = fullName
                    .toLowerCase()
                    .trim()
                    .replaceAll("\\s+", ".")
                    .replaceAll("[^a-z0-9.]", "");
            log.info("🏷️ Generated base username from full name: {}", baseUsername);
        } else {
            // Fallback to email if no full name
            baseUsername = email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9]", "");
            log.info("🏷️ Generated base username from email: {}", baseUsername);
        }

        // Get organization suffix from organization
        String orgSuffix = "";
        if (currentUser.getOrganizationId() != null) {
            var orgOptional = organizationRepository.findById(currentUser.getOrganizationId());
            if (orgOptional.isPresent()) {
                var org = orgOptional.get();
                log.info("🏢 Organization found: {} (ID: {})", org.getName(), org.getId());

                // Create meaningful organization suffix from organization name
                String suffix = org.getName()
                        .toLowerCase()
                        .trim()
                        .replaceAll("\\s+", "")
                        .replaceAll("[^a-z0-9]", "");

                // Limit to 10 chars
                orgSuffix = suffix.length() > 10 ? suffix.substring(0, 10) : suffix;
                log.info("🏷️ Organization suffix: {}", orgSuffix);
            } else {
                log.warn("⚠️ Organization not found for ID: {}", currentUser.getOrganizationId());
            }
        } else {
            log.warn("⚠️ Current user has no organization ID");
        }

        // Create username: baseUsername_orgSuffix (e.g., john.smith_acme)
        String username = orgSuffix.isEmpty() ? baseUsername : baseUsername + "_" + orgSuffix;

        log.info("✅ Final generated username: {}", username);
        return username;
    }

    public List<EmployeeDTO> getAllEmployees() {
        // ROOT user has NO access to employees - only organizations
        if (securityUtils.isRoot()) {
            log.warn("⚠️ ROOT user attempted to access employees - Access denied");
            throw new AccessDeniedException("ROOT user cannot access employee data. ROOT can only manage organizations.");
        }

        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null || currentUser.getOrganizationUuid() == null) {
            log.error("❌ User has no organization UUID!");
            throw new AccessDeniedException("User must be associated with an organization");
        }

        String userOrgUuid = currentUser.getOrganizationUuid();
        log.debug("✓ Fetching employees for organization UUID: {}", userOrgUuid);

        // SUPER_ADMIN can see ALL employees in their organization only (excluding other SUPER_ADMINs)
        if (securityUtils.isSuperAdmin()) {
            return employeeRepository.findAll().stream()
                    .filter(emp -> userOrgUuid.equals(emp.getOrganizationUuid()))
                    .filter(emp -> !isSuperAdmin(emp)) // Exclude SUPER_ADMIN employees
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }

        // ADMIN (Department Manager) can see only employees in their department (excluding SUPER_ADMINs)
        if (securityUtils.isAdmin() && !securityUtils.isSuperAdmin()) {
            if (currentUser != null) {
                Optional<Employee> managerEmployee = employeeRepository.findByUserId(currentUser.getId());
                if (managerEmployee.isPresent() && managerEmployee.get().getDepartment() != null) {
                    Department department = managerEmployee.get().getDepartment();
                    return employeeRepository.findByDepartment(department).stream()
                            .filter(emp -> !isSuperAdmin(emp)) // Exclude SUPER_ADMIN employees
                            .map(this::convertToDTO)
                            .collect(Collectors.toList());
                }
            }
            return new ArrayList<>();
        }

        // Regular USER can only see themselves
        if (currentUser != null) {
            return employeeRepository.findByUserId(currentUser.getId())
                    .map(this::convertToDTO)
                    .map(dto -> {
                        List<EmployeeDTO> list = new ArrayList<>();
                        list.add(dto);
                        return list;
                    })
                    .orElse(new ArrayList<>());
        }

        return new ArrayList<>();
    }

    public PageResponse<EmployeeDTO> getAllEmployeesPaginated(int page, int size) {
        // ROOT user has NO access to employees - only organizations
        if (securityUtils.isRoot()) {
            log.warn("⚠️ ROOT user attempted to access employees - Access denied");
            throw new AccessDeniedException("ROOT user cannot access employee data. ROOT can only manage organizations.");
        }

        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null || currentUser.getOrganizationUuid() == null) {
            log.error("❌ User has no organization UUID!");
            throw new AccessDeniedException("User must be associated with an organization");
        }

        String userOrgUuid = currentUser.getOrganizationUuid();
        log.debug("✓ Fetching paginated employees for organization UUID: {}", userOrgUuid);

        // Validate pagination parameters
        if (page < 0) page = 0;
        if (size < 1) size = 10;
        if (size > 100) size = 100; // Max page size

        // Get all filtered employees first (same logic as getAllEmployees)
        List<Employee> allFilteredEmployees;

        // SUPER_ADMIN can see ALL employees in their organization only (excluding other SUPER_ADMINs)
        if (securityUtils.isSuperAdmin()) {
            allFilteredEmployees = employeeRepository.findAll().stream()
                    .filter(emp -> userOrgUuid.equals(emp.getOrganizationUuid()))
                    .filter(emp -> !isSuperAdmin(emp))
                    .collect(Collectors.toList());
        }
        // ADMIN (Department Manager) can see only employees in their department (excluding SUPER_ADMINs)
        else if (securityUtils.isAdmin() && !securityUtils.isSuperAdmin()) {
            Optional<Employee> managerEmployee = employeeRepository.findByUserId(currentUser.getId());
            if (managerEmployee.isPresent() && managerEmployee.get().getDepartment() != null) {
                Department department = managerEmployee.get().getDepartment();
                allFilteredEmployees = employeeRepository.findByDepartment(department).stream()
                        .filter(emp -> !isSuperAdmin(emp))
                        .collect(Collectors.toList());
            } else {
                allFilteredEmployees = new ArrayList<>();
            }
        }
        // Regular USER can only see themselves
        else {
            Optional<Employee> employee = employeeRepository.findByUserId(currentUser.getId());
            allFilteredEmployees = employee.map(List::of).orElse(new ArrayList<>());
        }

        // Apply pagination
        long totalElements = allFilteredEmployees.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = page * size;
        int end = Math.min(start + size, allFilteredEmployees.size());
        
        List<EmployeeDTO> content = allFilteredEmployees.stream()
                .skip(start)
                .limit(size)
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                page >= totalPages - 1 || totalPages == 0
        );
    }


    public EmployeeDTO getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));

        // Check access permissions
        if (!canAccessEmployee(employee)) {
            throw new AccessDeniedException("You don't have permission to view this employee's information");
        }

        return convertToDTO(employee);
    }

    public EmployeeDTO updateEmployee(Long id, EmployeeDTO employeeDTO) {
        log.info("🔄 updateEmployee called - Employee ID: {}", id);
        log.info("📋 Received EmployeeDTO - Name: {}, Email: {}, DepartmentId: {}", 
                employeeDTO.getFullName(), employeeDTO.getWorkEmail(), employeeDTO.getDepartmentId());
        
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("❌ Employee not found with id: {}", id);
                    return new ResourceNotFoundException("Employee not found with id: " + id);
                });

        log.info("✓ Employee found - Name: {}, Email: {}, OrganizationId: {}", 
                employee.getFullName(), employee.getWorkEmail(), employee.getOrganizationId());

        // Check access permissions
        if (!canAccessEmployee(employee)) {
            log.warn("⚠️ Access denied for employee update - Employee ID: {}", id);
            throw new AccessDeniedException("Access denied. You can only update your own employee data.");
        }
        
        log.info("✓ Access granted, proceeding with update...");

        // Validate email if it's being changed
        String currentEmail = employee.getWorkEmail();
        String newEmail = employeeDTO.getWorkEmail();
        
        if (newEmail == null || newEmail.trim().isEmpty()) {
            throw new ValidationException("Work email is required");
        }
        
        // Check if email is being changed and if new email already exists for another employee
        if (!currentEmail.equalsIgnoreCase(newEmail.trim())) {
            User currentUser = securityUtils.getCurrentUser();
            Long organizationId = currentUser != null ? currentUser.getOrganizationId() : employee.getOrganizationId();
            
            // Check if another employee (not the current one) has this email in the same organization
            Optional<Employee> existingEmployee = employeeRepository.findByWorkEmail(newEmail.trim());
            if (existingEmployee.isPresent()) {
                Employee existing = existingEmployee.get();
                if (!existing.getId().equals(id) && 
                    existing.getOrganizationId() != null && 
                    organizationId != null &&
                    existing.getOrganizationId().equals(organizationId)) {
                    log.warn("Attempt to update employee email to duplicate: {} in organization: {}",
                            newEmail, organizationId);
                    throw new DuplicateResourceException(
                        "An employee with email '" + newEmail + 
                        "' already exists in your organization. Please use a different email."
                    );
                }
            }
        }

        // Update basic information
        employee.setTitle(employeeDTO.getTitle());
        employee.setFullName(employeeDTO.getFullName());
        employee.setPersonType(employeeDTO.getPersonType());
        employee.setWorkEmail(newEmail.trim());

        // Update personal information
        employee.setPhoneNumber(employeeDTO.getPhoneNumber());
        employee.setDateOfBirth(employeeDTO.getDateOfBirth());
        employee.setNationality(employeeDTO.getNationality());
        employee.setPresentAddress(employeeDTO.getPresentAddress());
        employee.setPreviousAddress(employeeDTO.getPreviousAddress());

        boolean hasMedicalCondition = Boolean.TRUE.equals(employeeDTO.getHasMedicalCondition());
        employee.setHasMedicalCondition(hasMedicalCondition);
        employee.setMedicalConditionDetails(hasMedicalCondition ? employeeDTO.getMedicalConditionDetails() : null);
        // Legacy next of kin fields (kept for backward compatibility)
        employee.setNextOfKinName(employeeDTO.getNextOfKinName());
        employee.setNextOfKinContact(employeeDTO.getNextOfKinContact());
        employee.setNextOfKinAddress(employeeDTO.getNextOfKinAddress());
        
        // Handle multiple next of kin entries
        synchronizeNextOfKin(employee, employeeDTO.getNextOfKinList());
        
        employee.setEmergencyContactName(employeeDTO.getEmergencyContactName());
        employee.setEmergencyContactPhone(employeeDTO.getEmergencyContactPhone());
        employee.setEmergencyContactRelationship(employeeDTO.getEmergencyContactRelationship());

        // Update job information
        employee.setJobTitle(employeeDTO.getJobTitle());
        employee.setReference(employeeDTO.getReference());
        employee.setDateOfJoining(employeeDTO.getDateOfJoining());
        employee.setEmploymentStatus(employeeDTO.getEmploymentStatus());
        employee.setContractType(employeeDTO.getContractType());
        employee.setWorkingTiming(employeeDTO.getWorkingTiming());
        employee.setHolidayAllowance(employeeDTO.getHolidayAllowance());
        
        // Financial and Employment Details
        employee.setNationalInsuranceNumber(employeeDTO.getNationalInsuranceNumber());
        employee.setShareCode(employeeDTO.getShareCode());
        employee.setBankAccountNumber(employeeDTO.getBankAccountNumber());
        employee.setBankSortCode(employeeDTO.getBankSortCode());
        employee.setBankAccountHolderName(employeeDTO.getBankAccountHolderName());
        employee.setBankName(employeeDTO.getBankName());
        employee.setWageRate(employeeDTO.getWageRate());
        employee.setContractHours(employeeDTO.getContractHours());

        // Only admins and super admins can change userId
        if (securityUtils.isAdminOrSuperAdmin() && employeeDTO.getUserId() != null) {
            employee.setUserId(employeeDTO.getUserId());
        }

        synchronizeEmploymentRecords(employee, employeeDTO.getEmploymentRecords());

        // Update linked User account email if email was changed and employee has a linked user
        if (!currentEmail.equalsIgnoreCase(newEmail.trim()) && employee.getUserId() != null) {
            Optional<User> linkedUser = userRepository.findById(employee.getUserId());
            if (linkedUser.isPresent()) {
                User user = linkedUser.get();
                // Check if new email already exists for another user in the same organization
                User currentUser = securityUtils.getCurrentUser();
                Long organizationId = currentUser != null ? currentUser.getOrganizationId() : employee.getOrganizationId();
                
                if (userRepository.existsByEmailAndOrganizationId(newEmail.trim(), organizationId) &&
                    !user.getEmail().equalsIgnoreCase(newEmail.trim())) {
                    log.warn("Cannot update employee email: User with email {} already exists in organization {}",
                            newEmail, organizationId);
                    throw new DuplicateResourceException(
                        "A user account with email '" + newEmail + 
                        "' already exists in your organization. Please use a different email."
                    );
                }
                
                user.setEmail(newEmail.trim());
                userRepository.save(user);
                log.info("✓ Updated linked User account email - User ID: {}, New Email: {}", user.getId(), newEmail);
            }
        }

        Employee updatedEmployee = employeeRepository.save(employee);
        log.info("✓ Employee updated - ID: {}, Name: {}, Email: {}", 
                updatedEmployee.getId(), updatedEmployee.getFullName(), updatedEmployee.getWorkEmail());
        return convertToDTO(updatedEmployee);
    }

    public void deleteEmployee(Long id) {
        // Only admins and super admins can delete employees
        if (!securityUtils.isAdminOrSuperAdmin()) {
            throw new RuntimeException("Access denied. Only admins can delete employees.");
        }

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        // Check access permissions - ensure employee belongs to same organization
        if (!canAccessEmployee(employee)) {
            throw new AccessDeniedException("You don't have permission to delete this employee");
        }

        log.info("🗑️ Deleting employee ID: {} - {}", id, employee.getFullName());

        // Delete all related entities in proper order to avoid foreign key constraint violations

        // 1. Delete all documents associated with this employee (including physical files)
        List<Document> documents = documentRepository.findByEmployeeId(id);
        if (!documents.isEmpty()) {
            log.info("   📄 Deleting {} document(s) for employee {}", documents.size(), id);
            
            // Delete physical files if no other documents reference them
            for (Document doc : documents) {
                String filePath = doc.getFilePath();
                if (filePath != null) {
                    // Check if other documents reference this file
                    long referenceCount = documentRepository.countByFilePath(filePath);
                    if (referenceCount <= 1) {
                        // No other documents reference this file, safe to delete
                        try {
                            Path path = Paths.get(filePath);
                            if (Files.exists(path)) {
                                Files.deleteIfExists(path);
                                log.debug("   🗑️ Physical file deleted: {}", filePath);
                            }
                        } catch (IOException e) {
                            log.warn("   ⚠️ Could not delete physical file: {} - {}", filePath, e.getMessage());
                        }
                    } else {
                        log.debug("   ♻️ Physical file kept: {} ({} other document(s) still reference it)", filePath, referenceCount - 1);
                    }
                }
            }
            
            documentRepository.deleteAll(documents);
        }

        // 2. Delete all leaves associated with this employee
        List<Leave> leaves = leaveRepository.findByEmployeeId(id);
        if (!leaves.isEmpty()) {
            log.info("   🏖️ Deleting {} leave record(s) for employee {}", leaves.size(), id);
            leaveRepository.deleteAll(leaves);
        }

        // 3. Delete all leave balances associated with this employee
        List<LeaveBalance> leaveBalances = leaveBalanceRepository.findByEmployeeId(id);
        if (!leaveBalances.isEmpty()) {
            log.info("   📊 Deleting {} leave balance record(s) for employee {}", leaveBalances.size(), id);
            leaveBalanceRepository.deleteAll(leaveBalances);
        }

        // 4. Delete all attendance records associated with this employee
        List<Attendance> attendances = attendanceRepository.findByEmployeeOrderByWorkDateDesc(employee);
        if (!attendances.isEmpty()) {
            log.info("   ⏰ Deleting {} attendance record(s) for employee {}", attendances.size(), id);
            attendanceRepository.deleteAll(attendances);
        }

        // 5. Delete all rota schedules associated with this employee
        // Note: RotaSchedule uses employeeId (Long), not Employee entity
        List<RotaSchedule> rotaSchedules = rotaScheduleRepository.findByEmployeeId(id);
        if (!rotaSchedules.isEmpty()) {
            log.info("   📋 Deleting {} rota schedule record(s) for employee {}", rotaSchedules.size(), id);
            rotaScheduleRepository.deleteByEmployeeId(id);
        }

        // 6. Delete all rota change logs associated with this employee
        List<RotaChangeLog> rotaChangeLogs = rotaChangeLogRepository.findByEmployeeIdOrderByChangedAtDesc(id);
        if (!rotaChangeLogs.isEmpty()) {
            log.info("   📝 Deleting {} rota change log record(s) for employee {}", rotaChangeLogs.size(), id);
            rotaChangeLogRepository.deleteAll(rotaChangeLogs);
        }

        // 7. Delete all employment records associated with this employee
        // EmploymentRecord is managed through Employee's @OneToMany relationship
        // If cascade is configured, they will be deleted automatically
        // Otherwise, we need to delete them manually
        if (employee.getEmploymentRecords() != null && !employee.getEmploymentRecords().isEmpty()) {
            log.info("   💼 Deleting {} employment record(s) for employee {}", employee.getEmploymentRecords().size(), id);
            employee.getEmploymentRecords().clear();
            employeeRepository.save(employee); // Save to trigger cascade delete if configured
        }

        // 8. Delete all notifications related to this employee
        // Delete notifications by user ID (if employee has a user account)
        if (employee.getUserId() != null) {
            List<Notification> userNotifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(employee.getUserId());
            if (!userNotifications.isEmpty()) {
                log.info("   🔔 Deleting {} notification(s) for user ID: {}", userNotifications.size(), employee.getUserId());
                notificationRepository.deleteAll(userNotifications);
            }
        }

        // Delete notifications related to employee's documents
        if (!documents.isEmpty()) {
            for (Document doc : documents) {
                List<Notification> docNotifications = notificationRepository.findByReferenceTypeAndReferenceId("DOCUMENT", doc.getId());
                if (!docNotifications.isEmpty()) {
                    log.info("   🔔 Deleting {} notification(s) for document ID: {}", docNotifications.size(), doc.getId());
                    notificationRepository.deleteAll(docNotifications);
                }
            }
        }

        // Delete notifications related to employee's leaves
        if (!leaves.isEmpty()) {
            for (Leave leave : leaves) {
                List<Notification> leaveNotifications = notificationRepository.findByReferenceTypeAndReferenceId("LEAVE", leave.getId());
                if (!leaveNotifications.isEmpty()) {
                    log.info("   🔔 Deleting {} notification(s) for leave ID: {}", leaveNotifications.size(), leave.getId());
                    notificationRepository.deleteAll(leaveNotifications);
                }
            }
        }

        // 9. Delete user account if linked (to prevent duplication when same user registers again)
        // This will also cascade delete verification tokens if configured
        if (employee.getUserId() != null) {
            Long userId = employee.getUserId();
            log.info("   👤 Deleting user account (ID: {}) associated with employee", userId);
            
            try {
                // Check if user exists before deleting
                userRepository.findById(userId).ifPresent(user -> {
                    // Delete user roles first (if not cascaded)
                    user.getRoles().clear();
                    userRepository.save(user);
                    
                    // Delete the user account
                    // Note: Verification tokens will be deleted via cascade if configured
                    userRepository.delete(user);
                    log.info("   ✅ User account (ID: {}) deleted successfully", userId);
                });
            } catch (Exception e) {
                log.error("   ⚠️ Failed to delete user account (ID: {}): {}", userId, e.getMessage());
                // Continue with employee deletion even if user deletion fails
            }
        }

        // 10. Finally, delete the employee
        employeeRepository.delete(employee);
        log.info("✅ Employee ID: {} - {} deleted successfully with all related records (documents, leaves, attendance, rota, notifications, user account)", id, employee.getFullName());
    }

    /**
     * Check if employee has SUPER_ADMIN role
     * SUPER_ADMIN users should not appear in employee lists or ROTA schedules
     */
    private boolean isSuperAdmin(Employee employee) {
        if (employee.getUserId() == null) {
            return false;
        }

        User user = userRepository.findById(employee.getUserId()).orElse(null);
        if (user == null) {
            return false;
        }

        return user.getRoles().contains("SUPER_ADMIN");
    }

    private boolean canAccessEmployee(Employee employee) {
        // ROOT cannot access employees at all
        if (securityUtils.isRoot()) {
            log.warn("⚠️ ROOT user attempted to access employee {} - Access denied", employee.getId());
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

    EmployeeDTO convertToDTO(Employee employee) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(employee.getId());
        dto.setTitle(employee.getTitle());
        dto.setFullName(employee.getFullName());
        dto.setPersonType(employee.getPersonType());
        dto.setWorkEmail(employee.getWorkEmail());

        // Personal information
        dto.setPhoneNumber(employee.getPhoneNumber());
        dto.setDateOfBirth(employee.getDateOfBirth());
        dto.setNationality(employee.getNationality());
        dto.setPresentAddress(employee.getPresentAddress());
        dto.setPreviousAddress(employee.getPreviousAddress());
        dto.setHasMedicalCondition(Boolean.TRUE.equals(employee.getHasMedicalCondition()));
        dto.setMedicalConditionDetails(employee.getMedicalConditionDetails());
        // Legacy next of kin fields (kept for backward compatibility)
        dto.setNextOfKinName(employee.getNextOfKinName());
        dto.setNextOfKinContact(employee.getNextOfKinContact());
        dto.setNextOfKinAddress(employee.getNextOfKinAddress());
        
        // Multiple next of kin entries
        if (employee.getNextOfKinList() != null && !employee.getNextOfKinList().isEmpty()) {
            dto.setNextOfKinList(employee.getNextOfKinList().stream()
                    .map(this::convertNextOfKinToDTO)
                    .collect(Collectors.toList()));
        } else {
            dto.setNextOfKinList(new ArrayList<>());
        }
        
        dto.setBloodGroup(employee.getBloodGroup());
        dto.setEmergencyContactName(employee.getEmergencyContactName());
        dto.setEmergencyContactPhone(employee.getEmergencyContactPhone());
        dto.setEmergencyContactRelationship(employee.getEmergencyContactRelationship());

        // Job information
        dto.setJobTitle(employee.getJobTitle());
        dto.setReference(employee.getReference());
        dto.setDateOfJoining(employee.getDateOfJoining());
        dto.setEmploymentStatus(employee.getEmploymentStatus());
        dto.setContractType(employee.getContractType());
        dto.setWorkingTiming(employee.getWorkingTiming());
        dto.setHolidayAllowance(employee.getHolidayAllowance());
        dto.setAllottedOrganization(employee.getAllottedOrganization());
        
        // Financial and Employment Details
        dto.setNationalInsuranceNumber(employee.getNationalInsuranceNumber());
        dto.setShareCode(employee.getShareCode());
        dto.setBankAccountNumber(employee.getBankAccountNumber());
        dto.setBankSortCode(employee.getBankSortCode());
        dto.setBankAccountHolderName(employee.getBankAccountHolderName());
        dto.setBankName(employee.getBankName());
        dto.setWageRate(employee.getWageRate());
        dto.setContractHours(employee.getContractHours());
        dto.setUserId(employee.getUserId());

        // Add username and role if user exists
        if (employee.getUserId() != null) {
            userRepository.findById(employee.getUserId()).ifPresent(user -> {
                dto.setUsername(user.getUsername());
                // Get the primary role (first role in the set)
                if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                    dto.setRole(user.getRoles().iterator().next());
                }
            });
        }

        // Add department info
        if (employee.getDepartment() != null) {
            dto.setDepartmentId(employee.getDepartment().getId());
            dto.setDepartmentName(employee.getDepartment().getName());
        }

        if (employee.getEmploymentRecords() != null && !employee.getEmploymentRecords().isEmpty()) {
            dto.setEmploymentRecords(employee.getEmploymentRecords().stream()
                    .map(this::convertEmploymentRecordToDTO)
                    .collect(Collectors.toList()));
        } else {
            dto.setEmploymentRecords(new ArrayList<>());
        }

        return dto;
    }

    private Employee convertToEntity(EmployeeDTO dto) {
        Employee employee = new Employee();
        employee.setTitle(dto.getTitle());
        employee.setFullName(dto.getFullName());
        employee.setPersonType(dto.getPersonType());
        employee.setWorkEmail(dto.getWorkEmail());

        // Personal information
        employee.setPhoneNumber(dto.getPhoneNumber());
        employee.setDateOfBirth(dto.getDateOfBirth());
        employee.setNationality(dto.getNationality());
        employee.setPresentAddress(dto.getPresentAddress());
        employee.setPreviousAddress(dto.getPreviousAddress());

        boolean hasMedicalCondition = Boolean.TRUE.equals(dto.getHasMedicalCondition());
        employee.setHasMedicalCondition(hasMedicalCondition);
        employee.setMedicalConditionDetails(hasMedicalCondition ? dto.getMedicalConditionDetails() : null);
        // Legacy next of kin fields (kept for backward compatibility)
        employee.setNextOfKinName(dto.getNextOfKinName());
        employee.setNextOfKinContact(dto.getNextOfKinContact());
        employee.setNextOfKinAddress(dto.getNextOfKinAddress());
        
        // Handle multiple next of kin entries
        employee.setNextOfKinList(buildNextOfKinFromDto(dto.getNextOfKinList(), employee));
        
        employee.setBloodGroup(dto.getBloodGroup());
        employee.setEmergencyContactName(dto.getEmergencyContactName());
        employee.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        employee.setEmergencyContactRelationship(dto.getEmergencyContactRelationship());

        // Job information
        employee.setJobTitle(dto.getJobTitle());
        employee.setReference(dto.getReference());
        employee.setDateOfJoining(dto.getDateOfJoining());
        employee.setEmploymentStatus(dto.getEmploymentStatus());
        employee.setContractType(dto.getContractType());
        employee.setWorkingTiming(dto.getWorkingTiming());
        employee.setHolidayAllowance(dto.getHolidayAllowance());
        employee.setAllottedOrganization(dto.getAllottedOrganization());
        
        // Financial and Employment Details
        employee.setNationalInsuranceNumber(dto.getNationalInsuranceNumber());
        employee.setShareCode(dto.getShareCode());
        employee.setBankAccountNumber(dto.getBankAccountNumber());
        employee.setBankSortCode(dto.getBankSortCode());
        employee.setBankAccountHolderName(dto.getBankAccountHolderName());
        employee.setBankName(dto.getBankName());
        employee.setWageRate(dto.getWageRate());
        employee.setContractHours(dto.getContractHours());
        
        employee.setUserId(dto.getUserId());

        // Set department if provided (SUPER_ADMIN only)
        if (dto.getDepartmentId() != null && securityUtils.isSuperAdmin()) {
            departmentRepository.findById(dto.getDepartmentId()).ifPresent(employee::setDepartment);
        }

        employee.setEmploymentRecords(buildEmploymentRecordsFromDto(dto.getEmploymentRecords(), employee));

        return employee;
    }

    private void synchronizeEmploymentRecords(Employee employee, List<EmploymentRecordDTO> recordDTOs) {
        if (employee.getEmploymentRecords() == null) {
            employee.setEmploymentRecords(new ArrayList<>());
        } else {
            employee.getEmploymentRecords().clear();
        }
        if (recordDTOs == null || recordDTOs.isEmpty()) {
            return;
        }

        List<EmploymentRecord> records = recordDTOs.stream()
                .filter(dto -> !isEmploymentRecordEmpty(dto))
                .map(dto -> convertToEmploymentRecordEntity(dto, employee))
                .collect(Collectors.toList());

        employee.getEmploymentRecords().addAll(records);
    }

    private List<EmploymentRecord> buildEmploymentRecordsFromDto(List<EmploymentRecordDTO> recordDTOs, Employee employee) {
        if (recordDTOs == null || recordDTOs.isEmpty()) {
            return new ArrayList<>();
        }

        return recordDTOs.stream()
                .filter(dto -> !isEmploymentRecordEmpty(dto))
                .map(dto -> convertToEmploymentRecordEntity(dto, employee))
                .collect(Collectors.toList());
    }

    private EmploymentRecord convertToEmploymentRecordEntity(EmploymentRecordDTO dto, Employee employee) {
        EmploymentRecord record = new EmploymentRecord();
        record.setId(dto.getId());
        record.setJobTitle(dto.getJobTitle());
        record.setEmploymentPeriod(dto.getEmploymentPeriod());
        record.setEmployerName(dto.getEmployerName());
        record.setEmployerAddress(dto.getEmployerAddress());
        record.setContactPersonTitle(dto.getContactPersonTitle());
        record.setContactPersonName(dto.getContactPersonName());
        record.setContactPersonEmail(dto.getContactPersonEmail());
        record.setEmployee(employee);
        return record;
    }

    private EmploymentRecordDTO convertEmploymentRecordToDTO(EmploymentRecord record) {
        EmploymentRecordDTO dto = new EmploymentRecordDTO();
        dto.setId(record.getId());
        dto.setJobTitle(record.getJobTitle());
        dto.setEmploymentPeriod(record.getEmploymentPeriod());
        dto.setEmployerName(record.getEmployerName());
        dto.setEmployerAddress(record.getEmployerAddress());
        dto.setContactPersonTitle(record.getContactPersonTitle());
        dto.setContactPersonName(record.getContactPersonName());
        dto.setContactPersonEmail(record.getContactPersonEmail());
        return dto;
    }

    private boolean isEmploymentRecordEmpty(EmploymentRecordDTO dto) {
        if (dto == null) {
            return true;
        }

        return (dto.getJobTitle() == null || dto.getJobTitle().trim().isEmpty())
                && (dto.getEmploymentPeriod() == null || dto.getEmploymentPeriod().trim().isEmpty())
                && (dto.getEmployerName() == null || dto.getEmployerName().trim().isEmpty())
                && (dto.getEmployerAddress() == null || dto.getEmployerAddress().trim().isEmpty())
                && (dto.getContactPersonTitle() == null || dto.getContactPersonTitle().trim().isEmpty())
                && (dto.getContactPersonEmail() == null || dto.getContactPersonEmail().trim().isEmpty());
    }

    // Next of Kin methods
    private void synchronizeNextOfKin(Employee employee, List<NextOfKinDTO> nextOfKinDTOs) {
        if (employee.getNextOfKinList() == null) {
            employee.setNextOfKinList(new ArrayList<>());
        } else {
            employee.getNextOfKinList().clear();
        }

        if (nextOfKinDTOs == null || nextOfKinDTOs.isEmpty()) {
            return;
        }

        List<NextOfKin> nextOfKinList = nextOfKinDTOs.stream()
                .filter(dto -> !isNextOfKinEmpty(dto))
                .map(dto -> convertToNextOfKinEntity(dto, employee))
                .collect(Collectors.toList());
        employee.getNextOfKinList().addAll(nextOfKinList);
    }

    private List<NextOfKin> buildNextOfKinFromDto(List<NextOfKinDTO> nextOfKinDTOs, Employee employee) {
        if (nextOfKinDTOs == null || nextOfKinDTOs.isEmpty()) {
            return new ArrayList<>();
        }

        return nextOfKinDTOs.stream()
                .filter(dto -> !isNextOfKinEmpty(dto))
                .map(dto -> convertToNextOfKinEntity(dto, employee))
                .collect(Collectors.toList());
    }

    private NextOfKin convertToNextOfKinEntity(NextOfKinDTO dto, Employee employee) {
        NextOfKin nextOfKin = new NextOfKin();
        nextOfKin.setId(dto.getId());
        nextOfKin.setTitle(dto.getTitle());
        nextOfKin.setName(dto.getName());
        nextOfKin.setContact(dto.getContact());
        nextOfKin.setAddress(dto.getAddress());
        nextOfKin.setRelationship(dto.getRelationship());
        nextOfKin.setEmployee(employee);
        return nextOfKin;
    }

    private NextOfKinDTO convertNextOfKinToDTO(NextOfKin nextOfKin) {
        NextOfKinDTO dto = new NextOfKinDTO();
        dto.setId(nextOfKin.getId());
        dto.setTitle(nextOfKin.getTitle());
        dto.setName(nextOfKin.getName());
        dto.setContact(nextOfKin.getContact());
        dto.setAddress(nextOfKin.getAddress());
        dto.setRelationship(nextOfKin.getRelationship());
        return dto;
    }

    private boolean isNextOfKinEmpty(NextOfKinDTO dto) {
        if (dto == null) {
            return true;
        }

        return (dto.getTitle() == null || dto.getTitle().trim().isEmpty())
                && (dto.getName() == null || dto.getName().trim().isEmpty())
                && (dto.getContact() == null || dto.getContact().trim().isEmpty())
                && (dto.getAddress() == null || dto.getAddress().trim().isEmpty())
                && (dto.getRelationship() == null || dto.getRelationship().trim().isEmpty());
    }
}

