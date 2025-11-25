package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.dto.CreateUserRequest;
import com.was.employeemanagementsystem.dto.CreateUserResponse;
import com.was.employeemanagementsystem.dto.EmploymentRecordDTO;
import com.was.employeemanagementsystem.entity.Department;
import com.was.employeemanagementsystem.entity.Employee;
import com.was.employeemanagementsystem.entity.EmploymentRecord;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final OrganizationRepository organizationRepository;
    private final SecurityUtils securityUtils;
    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerator passwordGenerator;
    private final EmailService emailService;
    private final LeaveService leaveService;

    /**
     * Create a new user with employee profile
     * SUPER_ADMIN creates ADMIN users with selected department
     * ADMIN creates USER under their own department
     */
    public CreateUserResponse createUser(CreateUserRequest request) {
        log.info("📝 Creating new user: {}", request.getEmail());

        // Validate requester permissions
        if (!securityUtils.isAdminOrSuperAdmin()) {
            throw new AccessDeniedException("Only administrators can create users");
        }

        // Validate required fields
        validateRequest(request);

        // Check for duplicates
        checkDuplicates(request.getEmail());

        // Determine role and department based on creator
        String assignedRole;
        Department department;

        if (securityUtils.isSuperAdmin()) {
            // SUPER_ADMIN can create ADMIN users and can optionally select department
            assignedRole = request.getRole() != null && request.getRole().equalsIgnoreCase("ADMIN")
                          ? "ADMIN" : "USER";

            // Department is optional - if not provided, set to null
            if (request.getDepartmentId() != null) {
                department = departmentRepository.findById(request.getDepartmentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));
            } else {
                department = null;
            }

            // Check if department already has an ADMIN (only when creating ADMIN role and department is provided)
            // Only check within SAME organization for multi-tenancy
            if (assignedRole.equals("ADMIN") && department != null) {
                User currentUser = securityUtils.getCurrentUser();
                Long currentOrgId = currentUser.getOrganizationId();

                boolean hasExistingAdmin = employeeRepository.findByDepartmentId(department.getId())
                        .stream()
                        .filter(emp -> currentOrgId.equals(emp.getOrganizationId())) // ✅ Filter by organization
                        .anyMatch(emp -> {
                            if (emp.getUserId() != null) {
                                return userRepository.findById(emp.getUserId())
                                        .map(u -> u.getRoles().contains("ADMIN"))
                                        .orElse(false);
                            }
                            return false;
                        });

                if (hasExistingAdmin) {
                    throw new ValidationException("Department '" + department.getName() + "' already has an ADMIN assigned in your organization. " +
                            "Each department can only have one ADMIN per organization. Please select a different department or remove the existing ADMIN first.");
                }
            }

            log.info("✓ SUPER_ADMIN creating {} in department: {}", assignedRole, department != null ? department.getName() : "None");

        } else {
            // ADMIN can only create USER and auto-assign to their department (if not specified)
            assignedRole = "USER";

            // If department is provided in request, use it; otherwise use admin's department
            if (request.getDepartmentId() != null) {
                department = departmentRepository.findById(request.getDepartmentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));
                log.info("✓ ADMIN creating USER in specified department: {}", department.getName());
            } else {
                User currentUser = securityUtils.getCurrentUser();
                Employee adminEmployee = employeeRepository.findByUserId(currentUser.getId())
                        .orElseThrow(() -> new ValidationException("Admin employee profile not found"));

                department = adminEmployee.getDepartment();
                // Department can be null - it's optional
                if (department != null) {
                    log.info("✓ ADMIN creating USER in their department: {}", department.getName());
                } else {
                    log.info("✓ ADMIN creating USER without department assignment");
                }
            }
        }

        // Generate username and temporary password
        String username = generateUsername(request.getEmail());
        String temporaryPassword = passwordGenerator.generateTemporaryPassword();

        log.info("📧 Generated credentials - Username: {}, Password: {}", username, temporaryPassword);

        // Get organization info for linking
        User currentUser = securityUtils.getCurrentUser();
        Long organizationId = currentUser.getOrganizationId();
        String organizationUuid = currentUser.getOrganizationUuid();

        // Create Employee
        Employee employee = new Employee();
        employee.setFullName(request.getFullName());
        employee.setWorkEmail(request.getEmail());
        employee.setJobTitle(request.getJobTitle() != null && !request.getJobTitle().trim().isEmpty() 
                            ? request.getJobTitle() : null); // Optional job title
        employee.setPersonType(request.getPersonType() != null ? request.getPersonType() : "Employee");
        employee.setReference(request.getReference());
        employee.setDepartment(department);
        employee.setOrganizationId(organizationId);  // ✅ Set organization ID
        employee.setOrganizationUuid(organizationUuid);  // ✅ Set organization UUID
        employee.setEmploymentStatus(request.getEmploymentStatus() != null ? request.getEmploymentStatus() : "FULL_TIME");
        employee.setContractType(request.getContractType() != null ? request.getContractType() : "PERMANENT");
        employee.setWorkingTiming(request.getWorkingTiming() != null ? request.getWorkingTiming() : "9:00 AM - 5:00 PM");
        employee.setHolidayAllowance(request.getHolidayAllowance() != null ? request.getHolidayAllowance() : 20);
        employee.setPersonalEmail(request.getPersonalEmail());
        employee.setPhoneNumber(request.getPhoneNumber());
        employee.setNationality(request.getNationality());
        employee.setAddress(request.getAddress());
        employee.setPresentAddress(request.getPresentAddress());
        employee.setPreviousAddress(request.getPreviousAddress());

        if (request.getDateOfBirth() != null && !request.getDateOfBirth().isEmpty()) {
            employee.setDateOfBirth(LocalDate.parse(request.getDateOfBirth(), DateTimeFormatter.ISO_DATE));
        }

        boolean hasMedicalCondition = Boolean.TRUE.equals(request.getHasMedicalCondition());
        employee.setHasMedicalCondition(hasMedicalCondition);
        employee.setMedicalConditionDetails(hasMedicalCondition ? request.getMedicalConditionDetails() : null);
        employee.setNextOfKinName(request.getNextOfKinName());
        employee.setNextOfKinContact(request.getNextOfKinContact());
        employee.setNextOfKinAddress(request.getNextOfKinAddress());
        employee.setBloodGroup(request.getBloodGroup());
        employee.setAllottedOrganization(request.getAllottedOrganization());

        // Parse date of joining
        if (request.getDateOfJoining() != null && !request.getDateOfJoining().isEmpty()) {
            employee.setDateOfJoining(LocalDate.parse(request.getDateOfJoining(), DateTimeFormatter.ISO_DATE));
        } else {
            employee.setDateOfJoining(LocalDate.now());
        }

        applyEmploymentRecords(employee, request.getEmploymentRecords());

        Employee savedEmployee = employeeRepository.save(employee);
        log.info("✓ Employee profile created - ID: {}", savedEmployee.getId());

        // Create User Account
        User user = new User();
        user.setUsername(username);
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.getRoles().add(assignedRole);
        user.setOrganizationId(organizationId);  // ✅ Set organization ID
        user.setOrganizationUuid(organizationUuid);  // ✅ Set organization UUID
        user.setEnabled(true);
        user.setEmailVerified(true); // Pre-verified by admin
        user.setFirstLogin(true);
        user.setProfileCompleted(true); // Profile created by admin
        user.setTemporaryPassword(true); // Must change password on first login

        User savedUser = userRepository.save(user);
        log.info("✓ User account created - Username: {}, Role: {}, Org UUID: {}",
                savedUser.getUsername(), assignedRole, organizationUuid);

        // Link employee to user
        savedEmployee.setUserId(savedUser.getId());
        employeeRepository.save(savedEmployee);
        log.info("✓ Employee linked to user");

        // Initialize leave balances for new employee
        try {
            leaveService.initializeLeaveBalances(savedEmployee.getId());
            log.info("✅ Leave balances initialized for new employee: {}", savedEmployee.getFullName());
        } catch (Exception e) {
            log.error("❌ Failed to initialize leave balances: {}", e.getMessage());
            // Don't fail user creation if leave balance initialization fails
        }

        // Send email with credentials
        boolean emailSent = false;
        try {
            emailService.sendAccountCreationEmail(
                request.getEmail(),
                request.getFullName(),
                username,
                temporaryPassword,
                currentUser.getOrganizationId() // Pass organization ID for SMTP configuration
            );
            emailSent = true;
            log.info("✓ Credentials email sent to: {}", request.getEmail());
        } catch (Exception e) {
            log.warn("⚠ Failed to send email to {}: {}", request.getEmail(), e.getMessage());
            // Continue - admin will see credentials in response
        }

        // Build response with credentials
        CreateUserResponse response = new CreateUserResponse();
        response.setEmployeeId(savedEmployee.getId());
        response.setUserId(savedUser.getId());
        response.setFullName(savedEmployee.getFullName());
        response.setEmail(savedEmployee.getWorkEmail());
        response.setUsername(username);
        response.setTemporaryPassword(temporaryPassword);
        response.setRole(assignedRole);
        response.setDepartmentName(department != null ? department.getName() : null);
        response.setEmailSent(emailSent);
        response.setMessage(emailSent
            ? "User created successfully! Credentials sent via email."
            : "User created successfully! Email failed - please share credentials manually.");

        log.info("✅ User creation complete - {} / {} in {}",
                 username, assignedRole, department != null ? department.getName() : "No Department");

        return response;
    }

    private void applyEmploymentRecords(Employee employee, List<EmploymentRecordDTO> recordDTOs) {
        if (employee.getEmploymentRecords() == null) {
            employee.setEmploymentRecords(new ArrayList<>());
        } else {
            employee.getEmploymentRecords().clear();
        }

        if (recordDTOs == null || recordDTOs.isEmpty()) {
            return;
        }

        recordDTOs.stream()
                .filter(dto -> !isEmploymentRecordEmpty(dto))
                .forEach(dto -> {
                    EmploymentRecord record = new EmploymentRecord();
                    record.setJobTitle(dto.getJobTitle());
                    record.setEmploymentPeriod(dto.getEmploymentPeriod());
                    record.setEmployerName(dto.getEmployerName());
                    record.setEmployerAddress(dto.getEmployerAddress());
                    record.setEmployee(employee);
                    employee.getEmploymentRecords().add(record);
                });
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

    private void validateRequest(CreateUserRequest request) {
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new ValidationException("Full name is required");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new ValidationException("Email is required");
        }
        if (request.getJobTitle() == null || request.getJobTitle().trim().isEmpty()) {
            throw new ValidationException("Job title is required");
        }
    }

    private void checkDuplicates(String email) {
        // Get current user's organization ID for multi-tenancy check
        User currentUser = securityUtils.getCurrentUser();
        Long organizationId = currentUser.getOrganizationId();

        // Check if user with same email exists in SAME organization only
        if (userRepository.existsByEmailAndOrganizationId(email, organizationId)) {
            throw new DuplicateResourceException("A user with email '" + email + "' already exists in your organization");
        }

        // Note: We don't check employeeRepository here because we're creating both user and employee together
        // The employee will be created with the same organization ID
    }

    private String generateUsername(String email) {
        // Get current user's organization ID for multi-tenancy check
        User currentUser = securityUtils.getCurrentUser();
        Long organizationId = currentUser.getOrganizationId();

        // Extract base username from email
        String baseUsername = email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9]", "");

        // Get organization suffix
        String orgSuffix = "";
        if (organizationId != null) {
            var orgOptional = organizationRepository.findById(organizationId);
            if (orgOptional.isPresent()) {
                var org = orgOptional.get();
                String suffix = org.getName()
                        .toLowerCase()
                        .trim()
                        .replaceAll("\\s+", "")
                        .replaceAll("[^a-z0-9]", "");

                // Limit to 10 chars
                orgSuffix = suffix.length() > 10 ? suffix.substring(0, 10) : suffix;
                log.info("🏷️ Adding org suffix '{}' to username", orgSuffix);
            }
        }

        // Create username with org suffix: baseUsername_orgSuffix
        String usernameBase = orgSuffix.isEmpty() ? baseUsername : baseUsername + "_" + orgSuffix;

        // Check for duplicates within SAME organization only and add number if needed
        String username = usernameBase;
        int counter = 1;
        while (userRepository.existsByUsernameAndOrganizationId(username, organizationId)) {
            username = usernameBase + counter;
            counter++;
        }

        log.info("✅ Generated username for SUPER_ADMIN: {}", username);
        return username;
    }
}

