package com.was.employeemanagementsystem.enums;

/**
 * System types supported by the multi-tenant architecture
 */
public enum SystemType {
    EMPLOYEE_MANAGEMENT("EMS", "Employee Management System"),
    INVENTORY_MANAGEMENT("IMS", "Inventory Management System"),
    HYBRID("HYBRID", "Hybrid System");

    private final String code;
    private final String displayName;

    SystemType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static SystemType fromCode(String code) {
        for (SystemType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return EMPLOYEE_MANAGEMENT; // Default
    }
}

