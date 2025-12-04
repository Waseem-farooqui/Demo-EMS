package com.was.employeemanagementsystem.dto.inventory;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemDTO {
    private Long id;
    private String itemCode;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
    private Integer quantity;
    private Integer reorderLevel;
    private BigDecimal unitPrice;
    private String unit;
    private String supplier;
    private String barcode;
    private String imagePath;
    private Boolean isActive;
    private Boolean needsReorder;
    private String organizationUuid;
}

