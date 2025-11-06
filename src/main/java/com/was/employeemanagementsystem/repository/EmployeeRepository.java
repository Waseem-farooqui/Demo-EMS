package com.was.employeemanagementsystem.repository;

import com.was.employeemanagementsystem.entity.Department;
import com.was.employeemanagementsystem.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByWorkEmail(String workEmail);
    boolean existsByWorkEmail(String workEmail);
    Optional<Employee> findByUserId(Long userId);

    // Department-related queries
    List<Employee> findByDepartment(Department department);
    List<Employee> findByDepartmentId(Long departmentId);
    Long countByDepartmentId(Long departmentId);

    // Organization-aware queries for multi-tenancy
    List<Employee> findByOrganizationId(Long organizationId);
    Long countByOrganizationId(Long organizationId);
}

