package com.was.employeemanagementsystem.repository.inventory;

import com.was.employeemanagementsystem.entity.inventory.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByOrganizationUuidAndIsActiveTrue(String organizationUuid);

    Optional<InventoryItem> findByItemCodeAndOrganizationUuid(String itemCode, String organizationUuid);

    List<InventoryItem> findByOrganizationUuidAndCategoryIdAndIsActiveTrue(String organizationUuid, Long categoryId);

    @Query("SELECT i FROM InventoryItem i WHERE i.organizationUuid = :orgUuid AND i.quantity <= i.reorderLevel AND i.isActive = true")
    List<InventoryItem> findItemsNeedingReorder(@Param("orgUuid") String organizationUuid);

    @Query("SELECT i FROM InventoryItem i WHERE i.organizationUuid = :orgUuid AND i.isActive = true AND (LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(i.itemCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<InventoryItem> searchItems(@Param("orgUuid") String organizationUuid, @Param("search") String search);

    boolean existsByItemCodeAndOrganizationUuid(String itemCode, String organizationUuid);
}

