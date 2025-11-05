package com.was.employeemanagementsystem.repository;

import com.was.employeemanagementsystem.entity.Department;
import com.was.employeemanagementsystem.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    
    Optional<Department> findByName(String name);
    
    Optional<Department> findByCode(String code);
    
    List<Department> findByIsActiveTrue();
    
    Optional<Department> findByManager(Employee manager);
    
    List<Department> findByManagerId(Long managerId);
    
    boolean existsByName(String name);
    
    boolean existsByCode(String code);
}

