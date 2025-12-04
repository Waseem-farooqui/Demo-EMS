package com.was.employeemanagementsystem.enums;

import lombok.Getter;

/**
 * Enum for common hotel positions across different departments
 */
@Getter
public enum HotelPositions {
    // Food & Beverage - Executive & Managerial
    FOOD_BEVERAGE_DIRECTOR("Food & Beverage Director / F&B Manager", "Heads the entire F&B department. Responsible for menus, budgets, service standards, and operations"),
    ASSISTANT_FB_MANAGER("Assistant F&B Manager", "Supports the F&B Manager. Oversees day-to-day restaurant, bar, room service, and banquet operations"),
    OUTLET_MANAGER("Outlet Manager / Restaurant Manager", "Manages a specific outlet (restaurant, café, bar, buffet, etc.). Responsible for staffing, guest satisfaction, and revenue"),
    BANQUET_MANAGER("Banquet Manager / Events Manager", "Oversees banquet halls, event catering, meetings, and functions"),
    BAR_MANAGER("Bar Manager / Beverage Manager", "Manages bar operations, drink menus, stock, and bartenders"),
    ROOM_SERVICE_MANAGER("Room Service Manager / In-Room Dining Manager", "Oversees food delivery service to guest rooms"),

    // Food & Beverage - Supervisory
    RESTAURANT_SUPERVISOR("Restaurant Supervisor / Outlet Supervisor", "Assists the manager in staff supervision and floor operations"),
    BANQUET_SUPERVISOR("Banquet Supervisor", "Coordinates event setup, service, and breakdown"),
    BAR_SUPERVISOR("Bar Supervisor", "Supervises bar operations and staff"),

    // Housekeeping - Executive & Managerial
    EXECUTIVE_HOUSEKEEPER("Executive Housekeeper / Director of Housekeeping", "Heads the entire housekeeping department. Responsible for budgeting, staffing, policies, and overall cleanliness standards"),
    ASSISTANT_EXECUTIVE_HOUSEKEEPER("Assistant Executive Housekeeper", "Supports the executive housekeeper. Oversees daily operations, schedules, and staff supervision"),

    // Housekeeping - Supervisory
    HOUSEKEEPING_SUPERVISOR("Housekeeping Supervisor / Floor Supervisor", "Manages a specific floor or section. Checks room conditions, assigns tasks, trains staff"),
    PUBLIC_AREA_SUPERVISOR("Public Area Supervisor", "Oversees cleaning of lobbies, hallways, restrooms, and public spaces"),
    LINEN_ROOM_SUPERVISOR("Linen Room Supervisor", "Responsible for linen inventory, laundry procedures, and distribution"),
    NIGHT_SUPERVISOR("Night Supervisor", "Manages housekeeping operations during the night shift"),

    // Housekeeping - Operational Staff
    ROOM_ATTENDANT("Room Attendant / Housekeeper / Maid", "Cleans guest rooms. Makes beds, replenishes supplies, reports maintenance issues"),
    PUBLIC_AREA_ATTENDANT("Public Area Attendant", "Cleans public areas (lobby, corridors, restaurants, restrooms)"),
    LAUNDRY_ATTENDANT("Laundry Attendant", "Washes, dries, irons, folds linens and uniforms"),
    LINEN_ROOM_ATTENDANT("Linen Room Attendant", "Manages clean and soiled linen, distributes supplies"),
    TURNDOWN_ATTENDANT("Turndown Attendant", "Performs evening guest room service in luxury hotels"),

    // Housekeeping - Support and Special Roles
    HOUSEKEEPING_RUNNER("Runner / Housekeeping Porter", "Delivers supplies, moves furniture, assists room attendants"),
    UNIFORM_ROOM_ATTENDANT("Uniform Room Attendant", "Maintains and issues staff uniforms"),
    TAILOR("Tailor / Seamstress", "Repairs linens, drapes, and uniforms"),
    HOUSEKEEPING_COORDINATOR("Housekeeping Coordinator (Office Staff)", "Handles staff schedules, communication, and guest requests"),

    // Front Office - Executive & Managerial
    FRONT_OFFICE_MANAGER("Front Office Manager", "Head of the front office. Oversees reception, concierge, guest services, reservations, and bell desk"),
    ASSISTANT_FRONT_OFFICE_MANAGER("Assistant Front Office Manager / Duty Manager", "Supports the Front Office Manager. Manages operations during shifts and resolves guest issues"),
    GUEST_RELATIONS_MANAGER("Guest Relations Manager", "Ensures VIP and special-guest satisfaction. Handles complaints and personalized services"),

    // Front Office - Supervisory
    FRONT_DESK_SUPERVISOR("Front Desk Supervisor / Reception Supervisor", "Oversees reception staff. Ensures smooth check-in/check-out. Trains and monitors receptionists"),

    // Front Office - Operational
    RECEPTIONIST("Receptionist / Front Desk Agent", "Handles check-in, check-out. Manages room keys, payments, and guest inquiries"),
    GUEST_RELATIONS_OFFICER("Guest Relations Officer (GRO)", "Welcomes VIP guests. Provides assistance, upgrades, and personalized service"),
    TELEPHONE_OPERATOR("Telephone Operator / PBX Operator", "Handles internal and external calls. Manages wake-up calls and phone communication"),
    CASHIER("Cashier (Front Office Cashier)", "Manages billing, foreign exchange (in some hotels), and guest payments"),
    NIGHT_AUDITOR("Night Auditor", "Works overnight. Balances daily transactions, prepares reports"),

    // Concierge - Executive & Managerial
    CHIEF_CONCIERGE("Chief Concierge", "Manages all concierge operations. Coordinates with transportation, tours, and external services"),
    ASSISTANT_CHIEF_CONCIERGE("Assistant Chief Concierge", "Supports the chief concierge. Supervises concierge and bell team"),

    // Concierge - Supervisory
    CONCIERGE_SUPERVISOR("Concierge Supervisor", "Oversees concierge desk staff. Ensures guest requests are handled efficiently"),

    // Concierge - Operational
    CONCIERGE_AGENT("Concierge Agent / Concierge", "Arranges transportation, tours, restaurant bookings. Offers local information and guest assistance"),
    BELL_CAPTAIN("Bell Captain", "Leads the bell staff. Assigns luggage handling tasks"),
    BELLMAN("Bellman / Bellboy / Porter", "Handles guest luggage. Escorts guests to rooms, assists with messaging and deliveries"),
    DOORMAN("Doorman", "Greets guests at entrance. Helps with doors, taxis, and traffic coordination"),
    VALET_PARKING_ATTENDANT("Valet Parking Attendant", "Parks guest vehicles and manages valet operations");

    private final String positionName;
    private final String description;

    HotelPositions(String positionName, String description) {
        this.positionName = positionName;
        this.description = description;
    }

    /**
     * Get position name for display
     */
    public String getDisplayName() {
        return positionName;
    }
}

