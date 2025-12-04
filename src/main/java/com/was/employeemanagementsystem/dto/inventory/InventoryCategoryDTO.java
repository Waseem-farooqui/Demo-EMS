package com.was.employeemanagementsystem.dto.inventory;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCategoryDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean isActive;
    private String organizationUuid;
}

