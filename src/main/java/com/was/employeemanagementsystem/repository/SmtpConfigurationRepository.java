package com.was.employeemanagementsystem.repository;

import com.was.employeemanagementsystem.entity.SmtpConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SmtpConfigurationRepository extends JpaRepository<SmtpConfiguration, Long> {
    Optional<SmtpConfiguration> findByOrganizationId(Long organizationId);
    boolean existsByOrganizationId(Long organizationId);
}

