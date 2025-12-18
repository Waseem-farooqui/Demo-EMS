package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.entity.Department;
import com.was.employeemanagementsystem.enums.CommonDepartments;
import com.was.employeemanagementsystem.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes common departments on application startup
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DepartmentInitializer implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;

    @Override
    public void run(String... args) {
        try {
            log.info("🏢 Initializing common departments...");

            // Create all common departments (check for missing ones even if some already exist)
            int created = 0;
            int skipped = 0;
            for (CommonDepartments commonDept : CommonDepartments.values()) {
                // Check if department already exists by code or name
                boolean existsByCode = departmentRepository.existsByCode(commonDept.getCode());
                boolean existsByName = departmentRepository.findByName(commonDept.getDepartmentName()).isPresent();
                
                if (!existsByCode && !existsByName) {
                    Department department = new Department();
                    department.setName(commonDept.getDepartmentName());
                    department.setCode(commonDept.getCode());
                    department.setDescription(commonDept.getDescription());
                    department.setIsActive(true);

                    departmentRepository.save(department);
                    created++;
                    log.info("  ✓ Created department: {} ({})", commonDept.getDepartmentName(), commonDept.getCode());
                } else {
                    skipped++;
                    log.debug("  ⊘ Department already exists: {} ({})", commonDept.getDepartmentName(), commonDept.getCode());
                }
            }

            if (created > 0) {
                log.info("🎉 Successfully initialized {} new departments ({} already existed)", created, skipped);
            } else {
                log.info("✓ All common departments already exist ({} total)", skipped);
            }

        } catch (Exception e) {
            log.error("❌ Error initializing departments: {}", e.getMessage(), e);
        }
    }
}

