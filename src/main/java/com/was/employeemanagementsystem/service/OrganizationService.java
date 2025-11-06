package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.dto.CreateOrganizationRequest;
import com.was.employeemanagementsystem.dto.OrganizationCreationResponse;
import com.was.employeemanagementsystem.dto.OrganizationDTO;
import com.was.employeemanagementsystem.entity.Department;
import com.was.employeemanagementsystem.entity.Employee;
import com.was.employeemanagementsystem.entity.Organization;
import com.was.employeemanagementsystem.entity.User;
import com.was.employeemanagementsystem.repository.DepartmentRepository;
import com.was.employeemanagementsystem.repository.EmployeeRepository;
import com.was.employeemanagementsystem.repository.OrganizationRepository;
import com.was.employeemanagementsystem.repository.UserRepository;
import com.was.employeemanagementsystem.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;
    private final EmailService emailService;


    public OrganizationService(OrganizationRepository organizationRepository,
                              UserRepository userRepository,
                              EmployeeRepository employeeRepository,
                              DepartmentRepository departmentRepository,
                              PasswordEncoder passwordEncoder,
                              SecurityUtils securityUtils,
                              EmailService emailService) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityUtils = securityUtils;
        this.emailService = emailService;
    }

    /**
     * Create a new organization with a SUPER_ADMIN user
     * This can only be called by ROOT user
     * Returns organization details AND generated credentials for display
     */
    public OrganizationCreationResponse createOrganization(CreateOrganizationRequest request) {
        log.info("🏢 Creating organization: {}", request.getOrganizationName());

        // Check if ROOT user exists and is making this request
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null || !currentUser.getRoles().contains("ROOT")) {
            throw new RuntimeException("Only ROOT user can create organizations");
        }

        // Check if organization name already exists
        if (organizationRepository.existsByName(request.getOrganizationName())) {
            throw new RuntimeException("Organization with name '" + request.getOrganizationName() + "' already exists");
        }

        // Note: We don't check for username/email here because they are now unique per organization
        // Different organizations can have users with the same username/email
        // The database unique constraint (username + organization_id) will enforce this

        // Create organization (INACTIVE until SUPER_ADMIN first login)
        Organization organization = new Organization();
        organization.setName(request.getOrganizationName());
        organization.setDescription(request.getOrganizationDescription());
        organization.setContactEmail(request.getContactEmail());
        organization.setContactPhone(request.getContactPhone());
        organization.setAddress(request.getAddress());
        organization.setIsActive(false);  // ⏸️ INACTIVE until SUPER_ADMIN logs in

        Organization savedOrganization = organizationRepository.save(organization);
        log.info("✅ Organization created with ID: {} (INACTIVE - awaiting SUPER_ADMIN first login)", savedOrganization.getId());

        // Create default department for this organization
        Department defaultDept = new Department();
        defaultDept.setName("General");
        defaultDept.setCode("GEN");
        defaultDept.setDescription("Default department");
        defaultDept.setOrganizationId(savedOrganization.getId());
        defaultDept.setIsActive(true);
        Department savedDept = departmentRepository.save(defaultDept);
        log.info("✅ Default department created with ID: {}", savedDept.getId());

        // Always auto-generate password
        String plainPassword = generateSecurePassword();
        log.info("🔐 Auto-generated secure password for SUPER_ADMIN");

        // Auto-generate username from full name or email
        String baseUsername;
        if (request.getSuperAdminUsername() == null || request.getSuperAdminUsername().trim().isEmpty()) {
            // Try to extract from full name first, fallback to email
            if (request.getSuperAdminFullName() != null && !request.getSuperAdminFullName().trim().isEmpty()) {
                // Convert full name to username format: "John Smith" -> "john.smith"
                baseUsername = request.getSuperAdminFullName()
                        .toLowerCase()
                        .trim()
                        .replaceAll("\\s+", ".")
                        .replaceAll("[^a-z0-9.]", "");
                log.info("🏷️ Auto-generated base username from full name: {}", baseUsername);
            } else {
                // Extract username from email (part before @)
                baseUsername = request.getSuperAdminEmail().split("@")[0];
                log.info("🏷️ Auto-generated base username from email: {}", baseUsername);
            }
        } else {
            baseUsername = request.getSuperAdminUsername();
            log.info("🏷️ Using provided base username: {}", baseUsername);
        }

        // Create meaningful organization suffix from organization name
        // Format: "Acme Corporation" -> "acme" or "acmecorp"
        String orgSuffix = savedOrganization.getName()
                .toLowerCase()
                .trim()
                .replaceAll("\\s+", "")
                .replaceAll("[^a-z0-9]", "");

        // Limit org suffix to reasonable length (max 10 chars)
        if (orgSuffix.length() > 10) {
            orgSuffix = orgSuffix.substring(0, 10);
        }

        // Create username: baseUsername_orgSuffix (e.g., john.smith_acme)
        String prefixedUsername = baseUsername + "_" + orgSuffix;

        // Handle duplicates by adding number suffix
        String finalUsername = prefixedUsername;
        int counter = 1;
        while (userRepository.findByUsername(finalUsername).isPresent()) {
            finalUsername = prefixedUsername + counter;
            counter++;
            log.info("🔄 Username exists, trying: {}", finalUsername);
        }

        log.info("✅ Final generated username: {}", finalUsername);

        // Create SUPER_ADMIN user
        User superAdmin = new User();
        superAdmin.setUsername(finalUsername);  // Use meaningful username
        superAdmin.setEmail(request.getSuperAdminEmail());
        superAdmin.setPassword(passwordEncoder.encode(plainPassword));
        superAdmin.setOrganizationId(savedOrganization.getId());
        superAdmin.setOrganizationUuid(savedOrganization.getOrganizationUuid());  // Set UUID
        superAdmin.setEnabled(true);
        superAdmin.setEmailVerified(true);
        superAdmin.setFirstLogin(true);  // ✅ Force password change on first login
        superAdmin.setProfileCompleted(true);
        superAdmin.setTemporaryPassword(true);  // ✅ Mark as temporary password

        Set<String> roles = new HashSet<>();
        roles.add("SUPER_ADMIN");
        superAdmin.setRoles(roles);

        User savedUser = userRepository.save(superAdmin);
        log.info("✅ SUPER_ADMIN user created with ID: {} for organization UUID: {}",
                savedUser.getId(), savedOrganization.getOrganizationUuid());

        // Send welcome email with auto-generated credentials
        try {
            emailService.sendOrganizationCreatedEmail(
                request.getSuperAdminEmail(),
                request.getSuperAdminFullName(),
                savedOrganization.getName(),
                finalUsername,  // Send final generated username in email
                plainPassword,
                true  // Password is always auto-generated
            );
            log.info("📧 Welcome email sent to SUPER_ADMIN with auto-generated login credentials");
        } catch (Exception e) {
            log.error("❌ Failed to send welcome email: {}", e.getMessage());
            // Continue anyway - don't fail organization creation if email fails
        }

        // Create employee profile for SUPER_ADMIN
        Employee superAdminEmployee = new Employee();
        superAdminEmployee.setFullName(request.getSuperAdminFullName());
        superAdminEmployee.setPersonType("FULL_TIME");
        superAdminEmployee.setWorkEmail(request.getSuperAdminEmail());
        superAdminEmployee.setJobTitle("Super Administrator");
        superAdminEmployee.setDateOfJoining(LocalDate.now());
        superAdminEmployee.setUserId(savedUser.getId());
        superAdminEmployee.setOrganizationId(savedOrganization.getId());
        superAdminEmployee.setOrganizationUuid(savedOrganization.getOrganizationUuid());  // Set UUID
        superAdminEmployee.setDepartment(savedDept);
        superAdminEmployee.setEmploymentStatus("FULL_TIME");
        superAdminEmployee.setContractType("PERMANENT");

        employeeRepository.save(superAdminEmployee);
        log.info("✅ Employee profile created for SUPER_ADMIN");

        log.info("🎉 Organization setup complete!");

        // Return organization details WITH generated credentials for display
        OrganizationCreationResponse response = new OrganizationCreationResponse();
        response.setOrganization(convertToDTO(savedOrganization));

        OrganizationCreationResponse.CredentialsDTO credentials =
            new OrganizationCreationResponse.CredentialsDTO(finalUsername, plainPassword);
        response.setCredentials(credentials);

        log.info("📋 Returning organization details with credentials for display");
        return response;
    }

    /**
     * Upload or update organization logo
     * Only SUPER_ADMIN can update their organization's logo
     */
    public OrganizationDTO uploadLogo(Long organizationId, MultipartFile file) throws IOException {
        log.info("📷 Uploading logo for organization ID: {}", organizationId);

        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("Authentication required");
        }

        // Check permissions: ROOT can update any, SUPER_ADMIN can update their own org
        if (!currentUser.getRoles().contains("ROOT")) {
            if (!currentUser.getRoles().contains("SUPER_ADMIN") ||
                !organizationId.equals(currentUser.getOrganizationId())) {
                throw new RuntimeException("Access denied. You can only update your organization's logo");
            }
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed for logos");
        }

        // Validate file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("Logo file size must be less than 5MB");
        }

        // Store logo data in database as BLOB
        byte[] logoData = file.getBytes();
        organization.setLogoData(logoData);

        Organization updated = organizationRepository.save(organization);
        log.info("✅ Logo uploaded successfully for organization ID: {}", organizationId);

        return convertToDTO(updated);
    }

    /**
     * Get organization logo
     */
    public byte[] getOrganizationLogo(Long organizationId) {
        log.info("📷 Retrieving logo for organization ID: {}", organizationId);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        byte[] logoData = organization.getLogoData();
        if (logoData == null || logoData.length == 0) {
            throw new RuntimeException("No logo found for this organization");
        }

        return logoData;
    }

    /**
     * Get all organizations (ROOT only)
     */
    public List<OrganizationDTO> getAllOrganizations() {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null || !currentUser.getRoles().contains("ROOT")) {
            throw new RuntimeException("Only ROOT user can view all organizations");
        }

        return organizationRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get organization by ID
     * ROOT can view any, SUPER_ADMIN can view their own
     */
    public OrganizationDTO getOrganizationById(Long id) {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("Authentication required");
        }

        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        // Check permissions
        if (!currentUser.getRoles().contains("ROOT")) {
            if (!id.equals(currentUser.getOrganizationId())) {
                throw new RuntimeException("Access denied");
            }
        }

        return convertToDTO(organization);
    }

    /**
     * Get current user's organization
     * Accessible by any authenticated user (except ROOT)
     */
    public OrganizationDTO getCurrentUserOrganization() {
        User currentUser = securityUtils.getCurrentUser();
        log.info("🔍 getCurrentUserOrganization - Current user: {}", currentUser != null ? currentUser.getUsername() : "null");

        if (currentUser == null) {
            throw new RuntimeException("Authentication required");
        }

        if (currentUser.getRoles().contains("ROOT")) {
            throw new RuntimeException("ROOT user does not belong to any organization");
        }

        Long organizationId = currentUser.getOrganizationId();
        log.info("🔍 User's organization ID: {}", organizationId);

        if (organizationId == null) {
            throw new RuntimeException("User does not belong to any organization");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        log.info("✅ Found organization: {} (ID: {})", organization.getName(), organization.getId());
        return convertToDTO(organization);
    }

    /**
     * Update organization details
     */
    public OrganizationDTO updateOrganization(Long id, OrganizationDTO dto) {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("Authentication required");
        }

        // Check permissions
        if (!currentUser.getRoles().contains("ROOT")) {
            if (!currentUser.getRoles().contains("SUPER_ADMIN") ||
                !id.equals(currentUser.getOrganizationId())) {
                throw new RuntimeException("Access denied");
            }
        }

        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        // Update fields
        if (dto.getName() != null && !dto.getName().equals(organization.getName())) {
            if (organizationRepository.existsByName(dto.getName())) {
                throw new RuntimeException("Organization name already exists");
            }
            organization.setName(dto.getName());
        }

        if (dto.getDescription() != null) {
            organization.setDescription(dto.getDescription());
        }

        if (dto.getContactEmail() != null) {
            organization.setContactEmail(dto.getContactEmail());
        }

        if (dto.getContactPhone() != null) {
            organization.setContactPhone(dto.getContactPhone());
        }

        if (dto.getAddress() != null) {
            organization.setAddress(dto.getAddress());
        }

        if (dto.getIsActive() != null && currentUser.getRoles().contains("ROOT")) {
            // Only ROOT can change active status
            organization.setIsActive(dto.getIsActive());
        }

        Organization updated = organizationRepository.save(organization);
        log.info("✅ Organization updated: {}", id);

        return convertToDTO(updated);
    }

    private OrganizationDTO convertToDTO(Organization organization) {
        OrganizationDTO dto = new OrganizationDTO();
        dto.setId(organization.getId());
        dto.setOrganizationUuid(organization.getOrganizationUuid());  // Include UUID
        dto.setName(organization.getName());
        dto.setDescription(organization.getDescription());
        dto.setContactEmail(organization.getContactEmail());
        dto.setContactPhone(organization.getContactPhone());
        dto.setAddress(organization.getAddress());
        dto.setIsActive(organization.getIsActive());
        dto.setCreatedAt(organization.getCreatedAt());
        dto.setUpdatedAt(organization.getUpdatedAt());

        if (organization.getLogoData() != null && organization.getLogoData().length > 0) {
            dto.setLogoUrl("/api/organizations/" + organization.getId() + "/logo");
        }

        return dto;
    }

    /**
     * Deactivate organization - Block access for ALL users in organization
     * ROOT only
     */
    public OrganizationDTO deactivateOrganization(Long id) {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null || !currentUser.getRoles().contains("ROOT")) {
            throw new RuntimeException("Access denied. Only ROOT can deactivate organizations.");
        }

        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found with id: " + id));

        if (!organization.getIsActive()) {
            throw new RuntimeException("Organization is already deactivated");
        }

        organization.setIsActive(false);
        Organization saved = organizationRepository.save(organization);

        // Disable all users in this organization
        List<User> orgUsers = userRepository.findByOrganizationId(id);
        int disabledCount = 0;
        for (User user : orgUsers) {
            if (user.isEnabled()) {
                user.setEnabled(false);
                userRepository.save(user);
                disabledCount++;
            }
        }

        log.warn("⏸️ Organization DEACTIVATED: {} (ID: {}). {} users disabled.",
                organization.getName(), id, disabledCount);

        return convertToDTO(saved);
    }

    /**
     * Activate organization - Restore access for all users in organization
     * ROOT only
     */
    public OrganizationDTO activateOrganization(Long id) {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null || !currentUser.getRoles().contains("ROOT")) {
            throw new RuntimeException("Access denied. Only ROOT can activate organizations.");
        }

        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found with id: " + id));

        if (organization.getIsActive()) {
            throw new RuntimeException("Organization is already active");
        }

        organization.setIsActive(true);
        Organization saved = organizationRepository.save(organization);

        // Re-enable all users in this organization
        List<User> orgUsers = userRepository.findByOrganizationId(id);
        int enabledCount = 0;
        for (User user : orgUsers) {
            if (!user.isEnabled()) {
                user.setEnabled(true);
                userRepository.save(user);
                enabledCount++;
            }
        }

        log.info("✅ Organization ACTIVATED: {} (ID: {}). {} users enabled.",
                organization.getName(), id, enabledCount);

        return convertToDTO(saved);
    }

    /**
     * Generate a secure random password for new users
     * Password contains: uppercase, lowercase, digits, and special characters
     */
    private String generateSecurePassword() {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*";
        String all = upper + lower + digits + special;

        java.util.Random random = new java.util.Random();
        StringBuilder password = new StringBuilder(12);

        // Ensure at least one character from each category
        password.append(upper.charAt(random.nextInt(upper.length())));
        password.append(lower.charAt(random.nextInt(lower.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));

        // Fill the rest with random characters
        for (int i = 4; i < 12; i++) {
            password.append(all.charAt(random.nextInt(all.length())));
        }

        // Shuffle the password
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }

        return new String(passwordArray);
    }
}

