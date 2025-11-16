package com.was.employeemanagementsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
// Explicitly scan entities to ensure JPA finds them
@EntityScan(basePackages = "com.was.employeemanagementsystem.entity")
@EnableJpaRepositories(basePackages = "com.was.employeemanagementsystem.repository")
public class EmployeeManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployeeManagementSystemApplication.class, args);
    }

}
