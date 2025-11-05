package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.dto.DepartmentDTO;
import com.was.employeemanagementsystem.entity.Department;
import com.was.employeemanagementsystem.entity.Employee;
import com.was.employeemanagementsystem.entity.User;
import com.was.employeemanagementsystem.exception.DuplicateResourceException;
import com.was.employeemanagementsystem.exception.ResourceNotFoundException;
import com.was.employeemanagementsystem.exception.ValidationException;
import com.was.employeemanagementsystem.repository.DepartmentRepository;
import com.was.employeemanagementsystem.repository.EmployeeRepository;
import com.was.employeemanagementsystem.repository.UserRepository;
import com.was.employeemanagementsystem.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    public DepartmentDTO createDepartment(DepartmentDTO dto) {
        // Only SUPER_ADMIN can create departments
        if (!securityUtils.isSuperAdmin()) {
            throw new AccessDeniedException("Only Super Admins can create departments");
        }

        // Validate
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new ValidationException("Department name is required");
        }

        // Check duplicates
        if (departmentRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Department with name '" + dto.getName() + "' already exists");
        }

        if (dto.getCode() != null && departmentRepository.existsByCode(dto.getCode())) {
            throw new DuplicateResourceException("Department with code '" + dto.getCode() + "' already exists");
        }

        Department department = new Department();
        department.setName(dto.getName());
        department.setDescription(dto.getDescription());
        department.setCode(dto.getCode());
        department.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        // Set organizationId from current user
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser != null && currentUser.getOrganizationId() != null) {
            department.setOrganizationId(currentUser.getOrganizationId());
        }

        // Set manager if provided
        if (dto.getManagerId() != null) {
            Employee manager = employeeRepository.findById(dto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found with ID: " + dto.getManagerId()));
            department.setManager(manager);
        }

        Department saved = departmentRepository.save(department);
        log.info("✓ Department created: {} (ID: {})", saved.getName(), saved.getId());

        return convertToDTO(saved);
    }

    public List<DepartmentDTO> getAllDepartments() {
        // SUPER_ADMIN sees all departments
        if (securityUtils.isSuperAdmin()) {
            return departmentRepository.findAll().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }

        // ADMIN and SUPER_ADMIN with admin role see only their department (unless super admin)
        if (securityUtils.isAdmin() && !securityUtils.isSuperAdmin()) {
            var currentUser = securityUtils.getCurrentUser();
            if (currentUser != null) {
                var employee = employeeRepository.findByUserId(currentUser.getId());
                if (employee.isPresent() && employee.get().getDepartment() != null) {
                    Department dept = employee.get().getDepartment();
                    return List.of(convertToDTO(dept));
                }
            }
        }

        // Regular users see their department
        var currentUser = securityUtils.getCurrentUser();
        if (currentUser != null) {
            var employee = employeeRepository.findByUserId(currentUser.getId());
            if (employee.isPresent() && employee.get().getDepartment() != null) {
                Department dept = employee.get().getDepartment();
                return List.of(convertToDTO(dept));
            }
        }

        return List.of();
    }

    /**
     * Get all departments with admin assignment status
     * Only SUPER_ADMIN can access this to know which departments already have admins
     */
    public List<DepartmentDTO> getDepartmentsWithAdminStatus() {
        if (!securityUtils.isSuperAdmin()) {
            throw new AccessDeniedException("Only Super Admins can access this information");
        }

        return departmentRepository.findAll().stream()
                .map(this::convertToDTOWithAdminStatus)
                .collect(Collectors.toList());
    }

    public DepartmentDTO getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        // Check access
        if (!canAccessDepartment(department)) {
            throw new AccessDeniedException("You don't have permission to view this department");
        }

        return convertToDTO(department);
    }

    public DepartmentDTO updateDepartment(Long id, DepartmentDTO dto) {
        // Only SUPER_ADMIN can update departments
        if (!securityUtils.isSuperAdmin()) {
            throw new AccessDeniedException("Only Super Admins can update departments");
        }

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        department.setName(dto.getName());
        department.setDescription(dto.getDescription());
        department.setCode(dto.getCode());
        department.setIsActive(dto.getIsActive());

        if (dto.getManagerId() != null) {
            Employee manager = employeeRepository.findById(dto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));
            department.setManager(manager);
        }

        Department updated = departmentRepository.save(department);
        log.info("✓ Department updated: {} (ID: {})", updated.getName(), updated.getId());

        return convertToDTO(updated);
    }

    public void deleteDepartment(Long id) {
        // Only SUPER_ADMIN can delete departments
        if (!securityUtils.isSuperAdmin()) {
            throw new AccessDeniedException("Only Super Admins can delete departments");
        }

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        // Check if department has employees
        Long employeeCount = employeeRepository.countByDepartmentId(id);
        if (employeeCount > 0) {
            throw new ValidationException("Cannot delete department with " + employeeCount + " employees. Please reassign them first.");
        }

        departmentRepository.delete(department);
        log.info("✓ Department deleted: {} (ID: {})", department.getName(), id);
    }

    private boolean canAccessDepartment(Department department) {
        if (securityUtils.isSuperAdmin()) {
            return true;
        }

        if (securityUtils.isAdmin() || securityUtils.isUser()) {
            var currentUser = securityUtils.getCurrentUser();
            if (currentUser != null) {
                var employee = employeeRepository.findByUserId(currentUser.getId());
                if (employee.isPresent()) {
                    Department userDept = employee.get().getDepartment();
                    return userDept != null && userDept.getId().equals(department.getId());
                }
            }
        }

        return false;
    }

    private DepartmentDTO convertToDTO(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(department.getId());
        dto.setName(department.getName());
        dto.setDescription(department.getDescription());
        dto.setCode(department.getCode());
        dto.setIsActive(department.getIsActive());
        dto.setCreatedAt(department.getCreatedAt());
        dto.setUpdatedAt(department.getUpdatedAt());

        if (department.getManager() != null) {
            dto.setManagerId(department.getManager().getId());
            dto.setManagerName(department.getManager().getFullName());
        }

        // Count employees
        Long count = employeeRepository.countByDepartmentId(department.getId());
        dto.setEmployeeCount(count);

        return dto;
    }

    /**
     * Convert Department to DTO with admin status check
     */
    private DepartmentDTO convertToDTOWithAdminStatus(Department department) {
        DepartmentDTO dto = convertToDTO(department);

        // Check if this department has an ADMIN assigned
        boolean hasAdmin = employeeRepository.findByDepartmentId(department.getId())
                .stream()
                .anyMatch(emp -> {
                    if (emp.getUserId() != null) {
                        return userRepository.findById(emp.getUserId())
                                .map(user -> user.getRoles().contains("ADMIN"))
                                .orElse(false);
                    }
                    return false;
                });

        dto.setHasAdmin(hasAdmin);
        log.debug("Department {} - Has Admin: {}", department.getName(), hasAdmin);

        return dto;
    }
}

