package com.was.employeemanagementsystem.repository.inventory;

import com.was.employeemanagementsystem.entity.inventory.InventoryCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryCategoryRepository extends JpaRepository<InventoryCategory, Long> {

    List<InventoryCategory> findByOrganizationUuidAndIsActiveTrue(String organizationUuid);

    Optional<InventoryCategory> findByCodeAndOrganizationUuid(String code, String organizationUuid);

    boolean existsByCodeAndOrganizationUuid(String code, String organizationUuid);
}

