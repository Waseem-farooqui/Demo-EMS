package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.dto.EmployeeDTO;
import com.was.employeemanagementsystem.entity.Department;
import com.was.employeemanagementsystem.entity.Employee;
import com.was.employeemanagementsystem.entity.User;
import com.was.employeemanagementsystem.exception.DuplicateResourceException;
import com.was.employeemanagementsystem.exception.ResourceNotFoundException;
import com.was.employeemanagementsystem.exception.ValidationException;
import com.was.employeemanagementsystem.repository.DepartmentRepository;
import com.was.employeemanagementsystem.repository.EmployeeRepository;
import com.was.employeemanagementsystem.repository.OrganizationRepository;
import com.was.employeemanagementsystem.repository.UserRepository;
import com.was.employeemanagementsystem.security.SecurityUtils;
import com.was.employeemanagementsystem.util.PasswordGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public EmployeeService(EmployeeRepository employeeRepository,
                          UserRepository userRepository,
                          DepartmentRepository departmentRepository,
                          OrganizationRepository organizationRepository,
                          SecurityUtils securityUtils,
                          PasswordEncoder passwordEncoder,
                          PasswordGenerator passwordGenerator,
                          EmailService emailService,
                          LeaveService leaveService) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.organizationRepository = organizationRepository;
        this.securityUtils = securityUtils;
        this.passwordEncoder = passwordEncoder;
        this.passwordGenerator = passwordGenerator;
        this.emailService = emailService;
        this.leaveService = leaveService;
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

        // Send credentials via email
        emailService.sendAccountCreationEmail(
            savedEmployee.getWorkEmail(),
            savedEmployee.getFullName(),
            finalUsername,
            temporaryPassword
        );

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

        Employee updatedEmployee = employeeRepository.save(employee);
        log.info("✓ Employee updated - ID: {}, Name: {}", updatedEmployee.getId(), updatedEmployee.getFullName());
        return convertToDTO(updatedEmployee);
    }

    public void deleteEmployee(Long id) {
        // Only admins and super admins can delete employees
        if (!securityUtils.isAdminOrSuperAdmin()) {
            throw new RuntimeException("Access denied. Only admins can delete employees.");
        }

        if (!employeeRepository.existsById(id)) {
            throw new RuntimeException("Employee not found with id: " + id);
        }
        employeeRepository.deleteById(id);
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

    private EmployeeDTO convertToDTO(Employee employee) {
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

        // Job information
        dto.setJobTitle(employee.getJobTitle());
        dto.setReference(employee.getReference());
        dto.setDateOfJoining(employee.getDateOfJoining());
        dto.setEmploymentStatus(employee.getEmploymentStatus());
        dto.setContractType(employee.getContractType());
        dto.setWorkingTiming(employee.getWorkingTiming());
        dto.setHolidayAllowance(employee.getHolidayAllowance());
        dto.setUserId(employee.getUserId());

        // Add username if user exists
        if (employee.getUserId() != null) {
            userRepository.findById(employee.getUserId()).ifPresent(user -> {
                dto.setUsername(user.getUsername());
            });
        }

        // Add department info
        if (employee.getDepartment() != null) {
            dto.setDepartmentId(employee.getDepartment().getId());
            dto.setDepartmentName(employee.getDepartment().getName());
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

        // Job information
        employee.setJobTitle(dto.getJobTitle());
        employee.setReference(dto.getReference());
        employee.setDateOfJoining(dto.getDateOfJoining());
        employee.setEmploymentStatus(dto.getEmploymentStatus());
        employee.setContractType(dto.getContractType());
        employee.setWorkingTiming(dto.getWorkingTiming());
        employee.setHolidayAllowance(dto.getHolidayAllowance());
        employee.setUserId(dto.getUserId());

        // Set department if provided (SUPER_ADMIN only)
        if (dto.getDepartmentId() != null && securityUtils.isSuperAdmin()) {
            departmentRepository.findById(dto.getDepartmentId()).ifPresent(employee::setDepartment);
        }

        return employee;
    }
}

