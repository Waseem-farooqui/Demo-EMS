package com.was.employeemanagementsystem.dto.inventory;

import com.was.employeemanagementsystem.entity.inventory.InventoryTransaction;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransactionDTO {
    private Long id;
    private Long itemId;
    private String itemCode;
    private String itemName;
    private InventoryTransaction.TransactionType transactionType;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String referenceNumber;
    private String remarks;
    private Long performedBy;
    private String performedByName;
    private LocalDateTime transactionDate;
    private String organizationUuid;
}

