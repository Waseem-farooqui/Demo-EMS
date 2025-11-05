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

            // Check if departments already exist
            long existingCount = departmentRepository.count();
            if (existingCount > 0) {
                log.info("✓ Departments already initialized ({} departments exist)", existingCount);
                return;
            }

            // Create all common departments
            int created = 0;
            for (CommonDepartments commonDept : CommonDepartments.values()) {
                if (!departmentRepository.existsByCode(commonDept.getCode())) {
                    Department department = new Department();
                    department.setName(commonDept.getDepartmentName());
                    department.setCode(commonDept.getCode());
                    department.setDescription(commonDept.getDescription());
                    department.setIsActive(true);

                    departmentRepository.save(department);
                    created++;
                    log.info("  ✓ Created department: {} ({})", commonDept.getDepartmentName(), commonDept.getCode());
                }
            }

            log.info("🎉 Successfully initialized {} common departments", created);

        } catch (Exception e) {
            log.error("❌ Error initializing departments: {}", e.getMessage(), e);
        }
    }
}

