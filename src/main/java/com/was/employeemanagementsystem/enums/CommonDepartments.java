package com.was.employeemanagementsystem.enums;

/**
 * Common department types for quick setup
 */
public enum CommonDepartments {
    INFORMATION_TECHNOLOGY("Information Technology", "IT", "Manages technology infrastructure and software development"),
    HUMAN_RESOURCES("Human Resources", "HR", "Manages employee relations, recruitment, and benefits"),
    FINANCE("Finance", "FIN", "Manages financial operations, accounting, and budgeting"),
    SALES("Sales", "SALES", "Manages customer relationships and revenue generation"),
    MARKETING("Marketing", "MKT", "Manages brand promotion and market research"),
    OPERATIONS("Operations", "OPS", "Manages day-to-day business operations"),
    CUSTOMER_SUPPORT("Customer Support", "CS", "Handles customer inquiries and support"),
    LEGAL("Legal", "LEGAL", "Manages legal compliance and contracts"),
    RESEARCH_AND_DEVELOPMENT("Research and Development", "R&D", "Manages innovation and product development"),
    QUALITY_ASSURANCE("Quality Assurance", "QA", "Ensures product and service quality"),
    ADMINISTRATION("Administration", "ADMIN", "Manages general administrative tasks"),
    PROCUREMENT("Procurement", "PROC", "Manages purchasing and vendor relationships"),
    LOGISTICS("Logistics", "LOG", "Manages supply chain and distribution"),
    TRAINING_AND_DEVELOPMENT("Training and Development", "T&D", "Manages employee training programs"),
    FACILITIES_MANAGEMENT("Facilities Management", "FM", "Manages building and infrastructure");

    private final String departmentName;
    private final String code;
    private final String description;

    CommonDepartments(String departmentName, String code, String description) {
        this.departmentName = departmentName;
        this.code = code;
        this.description = description;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}

