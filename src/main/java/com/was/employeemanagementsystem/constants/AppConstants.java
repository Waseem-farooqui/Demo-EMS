package com.was.employeemanagementsystem.constants;

/**
 * Application-wide constants
 */
public final class AppConstants {

    // Private constructor to prevent instantiation
    private AppConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // CORS Origins
    public static final String[] CORS_ORIGINS = {
            "http://localhost:4200",
            "http://127.0.0.1:4200"
    };

    // API Base Paths
    public static final String API_BASE_PATH = "/api";
    public static final String API_AUTH_PATH = API_BASE_PATH + "/auth";
    public static final String API_EMPLOYEES_PATH = API_BASE_PATH + "/employees";
    public static final String API_USERS_PATH = API_BASE_PATH + "/users";
    public static final String API_DEPARTMENTS_PATH = API_BASE_PATH + "/departments";
    public static final String API_DOCUMENTS_PATH = API_BASE_PATH + "/documents";
    public static final String API_LEAVES_PATH = API_BASE_PATH + "/leaves";
    public static final String API_ATTENDANCE_PATH = API_BASE_PATH + "/attendance";
    public static final String API_DASHBOARD_PATH = API_BASE_PATH + "/dashboard";
    public static final String API_ALERTS_PATH = API_BASE_PATH + "/alerts";
    public static final String API_ALERT_CONFIG_PATH = API_BASE_PATH + "/alert-config";
    public static final String API_ORGANIZATIONS_PATH = API_BASE_PATH + "/organizations";
    public static final String API_NOTIFICATIONS_PATH = API_BASE_PATH + "/notifications";
    public static final String API_ROTA_PATH = API_BASE_PATH + "/rota";
    public static final String API_ROOT_DASHBOARD_PATH = API_BASE_PATH + "/root/dashboard";
    public static final String API_INIT_PATH = API_BASE_PATH + "/init";

    // Security Roles
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    // Role Prefixes for Spring Security
    public static final String SPRING_ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    public static final String SPRING_ROLE_ADMIN = "ROLE_ADMIN";
    public static final String SPRING_ROLE_USER = "ROLE_USER";

    // Date Format
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // File Upload
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final String UPLOAD_DIR = "uploads";
    public static final String DOCUMENTS_DIR = UPLOAD_DIR + "/documents";

    // Document Types
    public static final String DOC_TYPE_PASSPORT = "PASSPORT";
    public static final String DOC_TYPE_VISA = "VISA";
    public static final String DOC_TYPE_ID = "ID_CARD";
    public static final String DOC_TYPE_LICENSE = "DRIVING_LICENSE";
    public static final String DOC_TYPE_OTHER = "OTHER";

    // Leave Status
    public static final String LEAVE_STATUS_PENDING = "PENDING";
    public static final String LEAVE_STATUS_APPROVED = "APPROVED";
    public static final String LEAVE_STATUS_REJECTED = "REJECTED";

    // Document Expiry Alert Thresholds (in days)
    public static final int ALERT_THRESHOLD_30_DAYS = 30;
    public static final int ALERT_THRESHOLD_60_DAYS = 60;
    public static final int ALERT_THRESHOLD_90_DAYS = 90;

    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // Error Messages
    public static final String ERROR_ACCESS_DENIED = "Access denied. You don't have permission to perform this action.";
    public static final String ERROR_RESOURCE_NOT_FOUND = "Resource not found.";
    public static final String ERROR_INVALID_INPUT = "Invalid input provided.";
    public static final String ERROR_DUPLICATE_ENTRY = "This entry already exists.";
    public static final String ERROR_INTERNAL_SERVER = "An internal server error occurred.";

    // Success Messages
    public static final String SUCCESS_CREATED = "Resource created successfully.";
    public static final String SUCCESS_UPDATED = "Resource updated successfully.";
    public static final String SUCCESS_DELETED = "Resource deleted successfully.";

    // JWT
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final long TOKEN_EXPIRATION = 86400000; // 24 hours

    // Email
    public static final String EMAIL_FROM = "noreply@employeemanagementsystem.com";
    public static final String EMAIL_SUBJECT_ACCOUNT_CREATION = "Your Account Has Been Created";
    public static final String EMAIL_SUBJECT_PASSWORD_RESET = "Password Reset Request";
    public static final String EMAIL_SUBJECT_DOCUMENT_EXPIRY = "Document Expiry Alert";

    // Work Locations
    public static final String WORK_LOCATION_OFFICE = "OFFICE";
    public static final String WORK_LOCATION_HOME = "HOME";
    public static final String WORK_LOCATION_CLIENT_SITE = "CLIENT_SITE";
    public static final String WORK_LOCATION_FIELD_WORK = "FIELD_WORK";
    public static final String WORK_LOCATION_HYBRID = "HYBRID";
}

