package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.dto.EmployeeDTO;
import com.was.employeemanagementsystem.dto.EmploymentRecordDTO;
import com.was.employeemanagementsystem.dto.PageResponse;
import com.was.employeemanagementsystem.entity.Department;
import com.was.employeemanagementsystem.entity.Employee;
import com.was.employeemanagementsystem.entity.EmploymentRecord;
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
                          RotaChangeLogRepository rotaChangeLogRepository) {
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

        // Send credentials via email (don't fail employee creation if email fails)
        try {
            emailService.sendAccountCreationEmail(
                savedEmployee.getWorkEmail(),
                savedEmployee.getFullName(),
                finalUsername,
                temporaryPassword,
                currentUser.getOrganizationId() // Pass organization ID for SMTP configuration
            );
            log.info("✅ Account creation email sent to: {}", savedEmployee.getWorkEmail());
        } catch (Exception e) {
            log.error("❌ Failed to send account creation email to {} (employee still created): {}",
                savedEmployee.getWorkEmail(), e.getMessage());
        }

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
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));

        // Check access permissions
        if (!canAccessEmployee(employee)) {
            throw new RuntimeException("Access denied. You can only update your own employee data.");
        }

        // Update basic information
        employee.setFullName(employeeDTO.getFullName());
        employee.setPersonType(employeeDTO.getPersonType());
        employee.setWorkEmail(employeeDTO.getWorkEmail());

        // Update personal information
        employee.setPersonalEmail(employeeDTO.getPersonalEmail());
        employee.setPhoneNumber(employeeDTO.getPhoneNumber());
        employee.setDateOfBirth(employeeDTO.getDateOfBirth());
        employee.setNationality(employeeDTO.getNationality());
        employee.setAddress(employeeDTO.getAddress());
        employee.setPresentAddress(employeeDTO.getPresentAddress());
        employee.setPreviousAddress(employeeDTO.getPreviousAddress());

        boolean hasMedicalCondition = Boolean.TRUE.equals(employeeDTO.getHasMedicalCondition());
        employee.setHasMedicalCondition(hasMedicalCondition);
        employee.setMedicalConditionDetails(hasMedicalCondition ? employeeDTO.getMedicalConditionDetails() : null);
        employee.setNextOfKinName(employeeDTO.getNextOfKinName());
        employee.setNextOfKinContact(employeeDTO.getNextOfKinContact());
        employee.setNextOfKinAddress(employeeDTO.getNextOfKinAddress());

        // Update job information
        employee.setJobTitle(employeeDTO.getJobTitle());
        employee.setReference(employeeDTO.getReference());
        employee.setDateOfJoining(employeeDTO.getDateOfJoining());
        employee.setEmploymentStatus(employeeDTO.getEmploymentStatus());
        employee.setContractType(employeeDTO.getContractType());
        employee.setWorkingTiming(employeeDTO.getWorkingTiming());
        employee.setHolidayAllowance(employeeDTO.getHolidayAllowance());

        // Only admins and super admins can change userId
        if (securityUtils.isAdminOrSuperAdmin() && employeeDTO.getUserId() != null) {
            employee.setUserId(employeeDTO.getUserId());
        }

        synchronizeEmploymentRecords(employee, employeeDTO.getEmploymentRecords());

        Employee updatedEmployee = employeeRepository.save(employee);
        log.info("✓ Employee updated - ID: {}, Name: {}", updatedEmployee.getId(), updatedEmployee.getFullName());
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

        // 8. Optionally delete user account if linked (but keep it for now to preserve login history)
        // We'll just unlink the employee from the user
        if (employee.getUserId() != null) {
            log.info("   👤 Unlinking user account (ID: {}) from employee", employee.getUserId());
            employee.setUserId(null);
            employeeRepository.save(employee);
        }

        // 9. Finally, delete the employee
        employeeRepository.delete(employee);
        log.info("✅ Employee ID: {} deleted successfully", id);
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
        dto.setFullName(employee.getFullName());
        dto.setPersonType(employee.getPersonType());
        dto.setWorkEmail(employee.getWorkEmail());

        // Personal information
        dto.setPersonalEmail(employee.getPersonalEmail());
        dto.setPhoneNumber(employee.getPhoneNumber());
        dto.setDateOfBirth(employee.getDateOfBirth());
        dto.setNationality(employee.getNationality());
        dto.setAddress(employee.getAddress());
        dto.setPresentAddress(employee.getPresentAddress());
        dto.setPreviousAddress(employee.getPreviousAddress());
        dto.setHasMedicalCondition(Boolean.TRUE.equals(employee.getHasMedicalCondition()));
        dto.setMedicalConditionDetails(employee.getMedicalConditionDetails());
        dto.setNextOfKinName(employee.getNextOfKinName());
        dto.setNextOfKinContact(employee.getNextOfKinContact());
        dto.setNextOfKinAddress(employee.getNextOfKinAddress());
        dto.setBloodGroup(employee.getBloodGroup());

        // Job information
        dto.setJobTitle(employee.getJobTitle());
        dto.setReference(employee.getReference());
        dto.setDateOfJoining(employee.getDateOfJoining());
        dto.setEmploymentStatus(employee.getEmploymentStatus());
        dto.setContractType(employee.getContractType());
        dto.setWorkingTiming(employee.getWorkingTiming());
        dto.setHolidayAllowance(employee.getHolidayAllowance());
        dto.setAllottedOrganization(employee.getAllottedOrganization());
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
        employee.setFullName(dto.getFullName());
        employee.setPersonType(dto.getPersonType());
        employee.setWorkEmail(dto.getWorkEmail());

        // Personal information
        employee.setPersonalEmail(dto.getPersonalEmail());
        employee.setPhoneNumber(dto.getPhoneNumber());
        employee.setDateOfBirth(dto.getDateOfBirth());
        employee.setNationality(dto.getNationality());
        employee.setAddress(dto.getAddress());
        employee.setPresentAddress(dto.getPresentAddress());
        employee.setPreviousAddress(dto.getPreviousAddress());

        boolean hasMedicalCondition = Boolean.TRUE.equals(dto.getHasMedicalCondition());
        employee.setHasMedicalCondition(hasMedicalCondition);
        employee.setMedicalConditionDetails(hasMedicalCondition ? dto.getMedicalConditionDetails() : null);
        employee.setNextOfKinName(dto.getNextOfKinName());
        employee.setNextOfKinContact(dto.getNextOfKinContact());
        employee.setNextOfKinAddress(dto.getNextOfKinAddress());
        employee.setBloodGroup(dto.getBloodGroup());

        // Job information
        employee.setJobTitle(dto.getJobTitle());
        employee.setReference(dto.getReference());
        employee.setDateOfJoining(dto.getDateOfJoining());
        employee.setEmploymentStatus(dto.getEmploymentStatus());
        employee.setContractType(dto.getContractType());
        employee.setWorkingTiming(dto.getWorkingTiming());
        employee.setHolidayAllowance(dto.getHolidayAllowance());
        employee.setAllottedOrganization(dto.getAllottedOrganization());
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
        return dto;
    }

    private boolean isEmploymentRecordEmpty(EmploymentRecordDTO dto) {
        if (dto == null) {
            return true;
        }

        return (dto.getJobTitle() == null || dto.getJobTitle().trim().isEmpty())
                && (dto.getEmploymentPeriod() == null || dto.getEmploymentPeriod().trim().isEmpty())
                && (dto.getEmployerName() == null || dto.getEmployerName().trim().isEmpty())
                && (dto.getEmployerAddress() == null || dto.getEmployerAddress().trim().isEmpty());
    }
}

