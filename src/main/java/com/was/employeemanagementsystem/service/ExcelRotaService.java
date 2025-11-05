package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.dto.RotaUploadPreviewDTO;
import com.was.employeemanagementsystem.entity.Employee;
import com.was.employeemanagementsystem.entity.Rota;
import com.was.employeemanagementsystem.entity.RotaSchedule;
import com.was.employeemanagementsystem.entity.User;
import com.was.employeemanagementsystem.repository.EmployeeRepository;
import com.was.employeemanagementsystem.repository.RotaRepository;
import com.was.employeemanagementsystem.repository.RotaScheduleRepository;
import com.was.employeemanagementsystem.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for parsing ROTA from Excel files directly
 * This is 100% accurate compared to OCR-based image parsing
 */
@Service
@Slf4j
public class ExcelRotaService {

    @Autowired
    private RotaRepository rotaRepository;

    @Autowired
    private RotaScheduleRepository rotaScheduleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Upload and parse ROTA from Excel file (.xlsx)
     * Returns preview of extracted data for user confirmation
     */
    public RotaUploadPreviewDTO uploadExcelRota(MultipartFile file, String username) throws Exception {
        log.info("📊 Starting Excel ROTA upload for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Read Excel file
        InputStream inputStream = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);

        // Parse metadata and schedules
        Map<String, String> metadata = parseExcelMetadata(sheet);
        List<Employee> allEmployees = employeeRepository.findAll();
        List<RotaSchedule> schedules = parseExcelSchedules(sheet, allEmployees, metadata);

        // Create ROTA entity
        Rota rota = new Rota();
        rota.setHotelName(metadata.getOrDefault("hotelName", "Unknown Hotel"));
        rota.setDepartment(metadata.getOrDefault("department", "Unknown Department"));
        rota.setFileName(file.getOriginalFilename());
        rota.setFilePath("/uploads/rota/" + UUID.randomUUID() + "_" + file.getOriginalFilename());
        rota.setFileData(file.getBytes());
        rota.setExtractedText("Excel file - no OCR needed");
        rota.setStartDate(LocalDate.parse(metadata.get("startDate")));
        rota.setEndDate(LocalDate.parse(metadata.get("endDate")));
        rota.setUploadedDate(LocalDateTime.now());
        rota.setUploadedBy(user.getId());
        rota.setUploadedByName(user.getUsername());

        // Save ROTA
        Rota savedRota = rotaRepository.save(rota);
        log.info("✅ ROTA saved with ID: {}", savedRota.getId());

        // Save schedules
        schedules.forEach(schedule -> schedule.setRota(savedRota));
        List<RotaSchedule> savedSchedules = rotaScheduleRepository.saveAll(schedules);
        log.info("✅ Saved {} schedules", schedules.size());

        workbook.close();

        // Generate preview DTO
        RotaUploadPreviewDTO preview = generatePreviewDTO(savedRota, savedSchedules);
        log.info("📋 Generated preview with {} employees and {} schedules",
                 preview.getTotalEmployees(), preview.getTotalSchedules());

        return preview;
    }

    /**
     * Parse metadata from Excel sheet
     */
    private Map<String, String> parseExcelMetadata(Sheet sheet) {
        Map<String, String> metadata = new HashMap<>();

        // Row 0: Hotel name and department
        Row headerRow = sheet.getRow(0);
        if (headerRow != null) {
            Cell headerCell = headerRow.getCell(0);
            if (headerCell != null) {
                String headerText = headerCell.getStringCellValue();
                log.info("📄 Header text: {}", headerText);

                // Extract hotel name and department
                if (headerText.contains("HOTEL") && headerText.contains("TIMESHEET")) {
                    String[] parts = headerText.split("TIMESHEET");
                    if (parts.length >= 2) {
                        String hotelPart = parts[0].replace("HOTEL", "").trim();
                        String deptPart = parts[1].trim();
                        metadata.put("hotelName", hotelPart);
                        metadata.put("department", deptPart);
                        log.info("🏨 Hotel: {}", hotelPart);
                        log.info("🏢 Department: {}", deptPart);
                    }
                }
            }
        }

        // Row 2: Dates (10-Jun, 11-Jun, etc.)
        Row dateRow = sheet.getRow(1);
        List<LocalDate> dates = new ArrayList<>();

        if (dateRow != null) {
            for (int i = 1; i < dateRow.getLastCellNum(); i++) {
                Cell dateCell = dateRow.getCell(i);
                if (dateCell != null) {
                    String dateText = getCellValueAsString(dateCell);
                    LocalDate date = parseDateFromCell(dateText);
                    if (date != null) {
                        dates.add(date);
                        log.info("📅 Found date: {} → {}", dateText, date);
                    }
                }
            }
        }

        if (!dates.isEmpty()) {
            metadata.put("startDate", dates.get(0).toString());
            metadata.put("endDate", dates.get(dates.size() - 1).toString());
            log.info("✅ Date range: {} to {}", dates.get(0), dates.get(dates.size() - 1));
        }

        return metadata;
    }

    /**
     * Parse schedules from Excel sheet
     */
    private List<RotaSchedule> parseExcelSchedules(Sheet sheet, List<Employee> allEmployees, Map<String, String> metadata) {
        List<RotaSchedule> schedules = new ArrayList<>();

        // Get dates from row 2
        Row dateRow = sheet.getRow(1);
        List<LocalDate> dates = new ArrayList<>();
        for (int i = 1; i < dateRow.getLastCellNum(); i++) {
            Cell dateCell = dateRow.getCell(i);
            if (dateCell != null) {
                String dateText = getCellValueAsString(dateCell);
                LocalDate date = parseDateFromCell(dateText);
                if (date != null) {
                    dates.add(date);
                }
            }
        }

        log.info("📊 Processing employee schedules from row 4 onwards...");

        // Start from row 4 (skip header, day names, dates, unit row)
        for (int rowIndex = 3; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            // First cell is employee name
            Cell nameCell = row.getCell(0);
            if (nameCell == null) continue;

            String employeeName = getCellValueAsString(nameCell).trim();
            if (employeeName.isEmpty() || employeeName.length() < 3) continue;

            // Match employee
            Employee matchedEmployee = findEmployeeByName(employeeName, allEmployees);
            if (matchedEmployee == null) {
                log.warn("⚠️ Could not match employee: '{}'", employeeName);
                continue;
            }

            log.info("✅ Found employee: {} → {}", employeeName, matchedEmployee.getFullName());

            // Read time slots for each date
            for (int colIndex = 1; colIndex < row.getLastCellNum() && colIndex - 1 < dates.size(); colIndex++) {
                Cell timeCell = row.getCell(colIndex);
                if (timeCell == null) continue;

                String timeValue = getCellValueAsString(timeCell).trim();
                if (timeValue.isEmpty()) continue;

                LocalDate date = dates.get(colIndex - 1);
                RotaSchedule schedule = createSchedule(matchedEmployee, date, timeValue);
                if (schedule != null) {
                    schedules.add(schedule);
                }
            }
        }

        log.info("✅ Created {} schedules", schedules.size());
        return schedules;
    }

    /**
     * Create schedule from time value
     */
    private RotaSchedule createSchedule(Employee employee, LocalDate date, String timeValue) {
        RotaSchedule schedule = new RotaSchedule();
        schedule.setEmployeeId(employee.getId());
        schedule.setEmployeeName(employee.getFullName());
        schedule.setScheduleDate(date);
        schedule.setDayOfWeek(date.getDayOfWeek().toString());
        schedule.setDuty(timeValue);

        // Check for OFF/Holiday/Leave
        if (timeValue.matches("(?i)(OFF|Holiday|Leave|Set-?Ups?)")) {
            schedule.setIsOffDay(true);
            return schedule;
        }

        // Parse compressed time format (08:18:00 → 08:00-18:00)
        Pattern compressedPattern = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})");
        Matcher compressedMatcher = compressedPattern.matcher(timeValue);
        if (compressedMatcher.find()) {
            String checkInHour = compressedMatcher.group(1);
            String checkOutHour = compressedMatcher.group(2);

            LocalTime startTime = LocalTime.of(Integer.parseInt(checkInHour), 0);
            LocalTime endTime = LocalTime.of(Integer.parseInt(checkOutHour), 0);

            schedule.setStartTime(startTime);
            schedule.setEndTime(endTime);
            schedule.setDuty(checkInHour + ":00-" + checkOutHour + ":00");
            schedule.setIsOffDay(false);

            log.debug("🕐 Converted {} to {}", timeValue, schedule.getDuty());
            return schedule;
        }

        // Parse standard time range (08:00-18:00)
        Pattern rangePattern = Pattern.compile("(\\d{1,2}:\\d{2})-(\\d{1,2}:\\d{2})");
        Matcher rangeMatcher = rangePattern.matcher(timeValue);
        if (rangeMatcher.find()) {
            String startStr = rangeMatcher.group(1);
            String endStr = rangeMatcher.group(2);

            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm");
            LocalTime startTime = LocalTime.parse(startStr, timeFormatter);
            LocalTime endTime = LocalTime.parse(endStr, timeFormatter);

            schedule.setStartTime(startTime);
            schedule.setEndTime(endTime);
            schedule.setIsOffDay(false);
            return schedule;
        }

        // Unknown format
        schedule.setIsOffDay(false);
        return schedule;
    }

    /**
     * Get cell value as string
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * Parse date from cell text
     */
    private LocalDate parseDateFromCell(String dateText) {
        if (dateText == null || dateText.isEmpty()) return null;

        // Pattern: 10-Jun, 11-Jun
        Pattern pattern = Pattern.compile("(\\d{1,2})-(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(dateText);

        if (matcher.find()) {
            int day = Integer.parseInt(matcher.group(1));
            String monthStr = matcher.group(2);
            int month = getMonthNumber(monthStr);
            int year = LocalDate.now().getYear();

            return LocalDate.of(year, month, day);
        }

        return null;
    }

    /**
     * Get month number from month name
     */
    private int getMonthNumber(String monthName) {
        String[] months = {"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
        for (int i = 0; i < months.length; i++) {
            if (months[i].equalsIgnoreCase(monthName)) {
                return i + 1;
            }
        }
        return 1;
    }

    /**
     * Find employee by name with fuzzy matching
     */
    private Employee findEmployeeByName(String name, List<Employee> allEmployees) {
        String nameLower = name.toLowerCase().trim();

        // Exact match
        for (Employee emp : allEmployees) {
            if (emp.getFullName().equalsIgnoreCase(nameLower)) {
                return emp;
            }
        }

        // Partial match
        for (Employee emp : allEmployees) {
            String empNameLower = emp.getFullName().toLowerCase();
            if (empNameLower.contains(nameLower) || nameLower.contains(empNameLower)) {
                return emp;
            }
        }

        // Name parts match
        String[] nameParts = nameLower.split("\\s+");
        if (nameParts.length >= 2) {
            for (Employee emp : allEmployees) {
                String empNameLower = emp.getFullName().toLowerCase();
                boolean allMatch = true;
                for (String part : nameParts) {
                    if (part.length() > 2 && !empNameLower.contains(part)) {
                        allMatch = false;
                        break;
                    }
                }
                if (allMatch) {
                    return emp;
                }
            }
        }

        return null;
    }

    /**
     * Generate preview DTO from saved Rota and schedules
     */
    private RotaUploadPreviewDTO generatePreviewDTO(Rota rota, List<RotaSchedule> schedules) {
        RotaUploadPreviewDTO preview = new RotaUploadPreviewDTO();
        preview.setRotaId(rota.getId());
        preview.setHotelName(rota.getHotelName());
        preview.setDepartment(rota.getDepartment());
        preview.setFileName(rota.getFileName());
        preview.setStartDate(rota.getStartDate());
        preview.setEndDate(rota.getEndDate());
        preview.setUploadedDate(rota.getUploadedDate());
        preview.setUploadedByName(rota.getUploadedByName());
        preview.setTotalSchedules(schedules.size());

        // Group schedules by employee ID (filter out any null employeeIds)
        Map<Long, List<RotaSchedule>> schedulesByEmployee = schedules.stream()
                .filter(s -> s.getEmployeeId() != null)
                .collect(Collectors.groupingBy(RotaSchedule::getEmployeeId));

        int uniqueEmployeeCount = schedulesByEmployee.size();
        log.info("📊 Preview generation: {} total schedules grouped into {} unique employees",
                 schedules.size(), uniqueEmployeeCount);

        // Debug: Log employee IDs
        schedulesByEmployee.keySet().forEach(empId ->
            log.info("   Employee ID: {} has {} schedules", empId, schedulesByEmployee.get(empId).size())
        );

        preview.setTotalEmployees(uniqueEmployeeCount);

        // Create employee schedule previews
        List<RotaUploadPreviewDTO.EmployeeSchedulePreview> employeeSchedules = new ArrayList<>();

        for (Map.Entry<Long, List<RotaSchedule>> entry : schedulesByEmployee.entrySet()) {
            List<RotaSchedule> empSchedules = entry.getValue();
            RotaUploadPreviewDTO.EmployeeSchedulePreview empPreview = new RotaUploadPreviewDTO.EmployeeSchedulePreview();

            empPreview.setEmployeeId(entry.getKey());
            empPreview.setEmployeeName(empSchedules.get(0).getEmployeeName());
            empPreview.setTotalDays(empSchedules.size());

            long offDays = empSchedules.stream().filter(RotaSchedule::getIsOffDay).count();
            empPreview.setOffDays((int) offDays);
            empPreview.setWorkDays(empSchedules.size() - (int) offDays);

            // Create day schedule previews
            List<RotaUploadPreviewDTO.DaySchedulePreview> daySchedules = new ArrayList<>();
            for (RotaSchedule schedule : empSchedules) {
                RotaUploadPreviewDTO.DaySchedulePreview dayPreview = new RotaUploadPreviewDTO.DaySchedulePreview();
                dayPreview.setDate(schedule.getScheduleDate());
                dayPreview.setDayOfWeek(schedule.getDayOfWeek());
                dayPreview.setDuty(schedule.getDuty());
                dayPreview.setStartTime(schedule.getStartTime() != null ? schedule.getStartTime().toString() : null);
                dayPreview.setEndTime(schedule.getEndTime() != null ? schedule.getEndTime().toString() : null);
                dayPreview.setIsOffDay(schedule.getIsOffDay());
                daySchedules.add(dayPreview);
            }
            empPreview.setSchedules(daySchedules);
            employeeSchedules.add(empPreview);
        }

        preview.setEmployeeSchedules(employeeSchedules);
        return preview;
    }

    /**
     * Delete entire ROTA including all schedules
     */
    public void deleteRota(Long rotaId) {
        log.info("🗑️ Deleting ROTA with ID: {}", rotaId);

        // Check if rota exists
        Rota rota = rotaRepository.findById(rotaId)
                .orElseThrow(() -> new RuntimeException("ROTA not found with ID: " + rotaId));

        // Delete all associated schedules first
        List<RotaSchedule> schedules = rotaScheduleRepository.findByRotaId(rotaId);
        rotaScheduleRepository.deleteAll(schedules);
        log.info("✅ Deleted {} schedules", schedules.size());

        // Delete the rota
        rotaRepository.delete(rota);
        log.info("✅ Deleted ROTA with ID: {}", rotaId);
    }

    /**
     * Get preview of existing ROTA
     */
    public RotaUploadPreviewDTO getRotaPreview(Long rotaId) {
        log.info("📋 Fetching preview for ROTA ID: {}", rotaId);

        Rota rota = rotaRepository.findById(rotaId)
                .orElseThrow(() -> new RuntimeException("ROTA not found with ID: " + rotaId));

        List<RotaSchedule> schedules = rotaScheduleRepository.findByRotaId(rotaId);

        return generatePreviewDTO(rota, schedules);
    }
}
