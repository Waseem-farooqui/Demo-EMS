package com.was.employeemanagementsystem.repository.inventory;

import com.was.employeemanagementsystem.entity.inventory.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    List<InventoryTransaction> findByOrganizationUuidOrderByTransactionDateDesc(String organizationUuid);

    List<InventoryTransaction> findByItemIdOrderByTransactionDateDesc(Long itemId);

    @Query("SELECT t FROM InventoryTransaction t WHERE t.organizationUuid = :orgUuid AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<InventoryTransaction> findTransactionsByDateRange(
            @Param("orgUuid") String organizationUuid,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT t FROM InventoryTransaction t WHERE t.organizationUuid = :orgUuid AND t.transactionType = :type ORDER BY t.transactionDate DESC")
    List<InventoryTransaction> findByTransactionType(
            @Param("orgUuid") String organizationUuid,
            @Param("type") InventoryTransaction.TransactionType transactionType
    );
}

