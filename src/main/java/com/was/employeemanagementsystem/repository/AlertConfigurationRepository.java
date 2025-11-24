package com.was.employeemanagementsystem.repository;

import com.was.employeemanagementsystem.entity.AlertConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertConfigurationRepository extends JpaRepository<AlertConfiguration, Long> {
    Optional<AlertConfiguration> findByDocumentType(String documentType);
    List<AlertConfiguration> findAllByDocumentType(String documentType);
    boolean existsByDocumentTypeAndAlertPriority(String documentType, String alertPriority);
    boolean existsByOrganizationIdAndDocumentTypeAndAlertPriority(Long organizationId, String documentType, String alertPriority);
    List<AlertConfiguration> findByDocumentTypeAndEnabled(String documentType, boolean enabled);
    List<AlertConfiguration> findByOrganizationId(Long organizationId);
}

