package com.was.employeemanagementsystem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.was.employeemanagementsystem.dto.RotaChangeLogDTO;
import com.was.employeemanagementsystem.dto.RotaDTO;
import com.was.employeemanagementsystem.dto.RotaScheduleDTO;
import com.was.employeemanagementsystem.dto.RotaScheduleEntryDTO;
import com.was.employeemanagementsystem.dto.RotaScheduleUpdateDTO;
import com.was.employeemanagementsystem.entity.*;
import com.was.employeemanagementsystem.repository.*;
import com.was.employeemanagementsystem.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RotaService {

    @Autowired
    private RotaRepository rotaRepository;

    @Autowired
    private RotaScheduleRepository rotaScheduleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RotaChangeLogRepository rotaChangeLogRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private Tesseract tesseract;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Upload and parse ROTA image
     */
    public RotaDTO uploadRota(MultipartFile file, String username) throws Exception {
        log.info("📋 Starting ROTA upload for user: {}", username);

        // Get current user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Read image and preprocess to handle colored backgrounds
        byte[] imageBytes = file.getBytes();
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        log.info("🖼️ Original image loaded: {}x{} pixels", originalImage.getWidth(), originalImage.getHeight());

        // Preprocess image to remove yellow tint and improve contrast
        // This is critical for reading text in yellow/colored cells
        BufferedImage processedImage = preprocessForColoredCells(originalImage);
        log.info("✨ Color removal preprocessing complete");

        // Perform OCR on preprocessed image
        String extractedText = tesseract.doOCR(processedImage);
        log.info("📝 OCR extracted {} characters", extractedText.length());

        // Log first 1000 characters for complete debugging
        String preview = extractedText.length() > 1000 ?
                extractedText.substring(0, 1000) + "..." : extractedText;
        log.info("📄 OCR Text Preview:\n{}", preview);

        // Parse ROTA metadata (hotel name, department, dates)
        Map<String, String> metadata = parseRotaMetadata(extractedText);

        // Parse employee schedules
        List<RotaSchedule> schedules = parseRotaSchedules(extractedText, metadata);

        // VALIDATION: Log warning if no or few employee records found
        if (schedules.isEmpty()) {
            log.warn("⚠️ WARNING: No employee records extracted from OCR");
            log.warn("📋 Extracted text length: {} characters", extractedText.length());
            log.warn("📋 Number of employees in database: {}", employeeRepository.count());
            log.warn("🏨 Hotel: {}, Department: {}",
                    metadata.getOrDefault("hotelName", "Unknown"),
                    metadata.getOrDefault("department", "Unknown"));

            // Log sample employee names from database
            List<Employee> sampleEmployees = employeeRepository.findAll();
            if (!sampleEmployees.isEmpty()) {
                log.warn("📝 Employee names in database (for manual entry):");
                sampleEmployees.forEach(e -> log.warn("   - '{}'", e.getFullName()));
            }

            // Save ROTA with empty schedules for manual data entry later
            log.info("💾 Saving ROTA with OCR data for manual schedule entry");
        } else if (schedules.size() < 5) {
            log.warn("⚠️ Only {} employee schedule(s) extracted - OCR quality may be poor", schedules.size());
            log.warn("💡 You may need to manually add missing employee schedules");
        } else {
            log.info("✅ Found {} employee schedules in ROTA", schedules.size());
        }

        // Create ROTA entity
        Rota rota = new Rota();
        rota.setHotelName(metadata.getOrDefault("hotelName", "Unknown Hotel"));
        rota.setDepartment(metadata.getOrDefault("department", "Unknown Department"));
        rota.setFileName(file.getOriginalFilename());
        rota.setFilePath("/uploads/rota/" + UUID.randomUUID() + "_" + file.getOriginalFilename());
        rota.setFileData(imageBytes);
        rota.setExtractedText(extractedText);
        rota.setStartDate(LocalDate.parse(metadata.get("startDate")));
        rota.setEndDate(LocalDate.parse(metadata.get("endDate")));
        rota.setUploadedDate(LocalDateTime.now());
        rota.setUploadedBy(user.getId());
        rota.setUploadedByName(user.getUsername());

        // Save ROTA
        Rota savedRota = rotaRepository.save(rota);
        log.info("✅ ROTA saved with ID: {}", savedRota.getId());

        // Save schedules
        for (RotaSchedule schedule : schedules) {
            schedule.setRota(savedRota);
        }
        rotaScheduleRepository.saveAll(schedules);
        log.info("✅ Saved {} schedule entries", schedules.size());

        // Count unique employees for DTO
        long uniqueEmployees = schedules.stream()
                .map(RotaSchedule::getEmployeeId)
                .distinct()
                .count();

        // Return DTO
        return convertToDTO(savedRota, (int) uniqueEmployees);
    }

    /**
     * Create ROTA manually from form data
     */
    public Rota createManualRota(Map<String, Object> rotaData, String username) throws Exception {
        log.info("✏️ Creating manual ROTA for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Extract basic info
        String hotelName = (String) rotaData.get("hotelName");
        String department = (String) rotaData.get("department");
        String startDateStr = (String) rotaData.get("startDate");
        String endDateStr = (String) rotaData.get("endDate");

        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        // Create ROTA entity
        Rota rota = new Rota();
        rota.setHotelName(hotelName);
        rota.setDepartment(department);
        rota.setFileName("Manual Entry");
        rota.setFilePath("/manual-entry");
        rota.setFileData(new byte[0]);
        rota.setExtractedText("Manually entered ROTA");
        rota.setStartDate(startDate);
        rota.setEndDate(endDate);
        rota.setUploadedDate(LocalDateTime.now());
        rota.setUploadedBy(user.getId());
        rota.setUploadedByName(user.getUsername());

        // Save ROTA
        Rota savedRota = rotaRepository.save(rota);
        log.info("✅ Manual ROTA saved with ID: {}", savedRota.getId());

        // Parse schedules
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> schedulesData = (List<Map<String, Object>>) rotaData.get("schedules");

        if (schedulesData != null && !schedulesData.isEmpty()) {
            List<RotaSchedule> schedules = new ArrayList<>();

            for (Map<String, Object> scheduleData : schedulesData) {
                Long employeeId = Long.valueOf(scheduleData.get("employeeId").toString());
                @SuppressWarnings("unchecked")
                List<String> shifts = (List<String>) scheduleData.get("shifts");

                Employee employee = employeeRepository.findById(employeeId)
                        .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

                // Generate dates
                List<LocalDate> dates = generateDateRange(startDate, endDate);

                for (int i = 0; i < Math.min(shifts.size(), dates.size()); i++) {
                    String shift = shifts.get(i);
                    if (shift == null || shift.trim().isEmpty()) continue;

                    LocalDate date = dates.get(i);
                    RotaSchedule schedule = new RotaSchedule();
                    schedule.setRota(savedRota);
                    schedule.setEmployeeId(employee.getId());
                    schedule.setEmployeeName(employee.getFullName());
                    schedule.setScheduleDate(date);
                    schedule.setDayOfWeek(date.getDayOfWeek().toString());
                    schedule.setDuty(shift);

                    // Parse time if available
                    parseTimeFromDuty(schedule, shift);

                    schedules.add(schedule);
                }
            }

            rotaScheduleRepository.saveAll(schedules);
            log.info("✅ Saved {} manual schedule entries", schedules.size());
        }

        return savedRota;
    }

    /**
     * Parse ROTA metadata (hotel name, department, dates)
     */
    private Map<String, String> parseRotaMetadata(String text) {
        Map<String, String> metadata = new HashMap<>();

        // Extract hotel name (before "TIMESHEET")
        Pattern hotelPattern = Pattern.compile("([A-Z\\s]+)\\s+HOTEL\\s+TIMESHEET", Pattern.CASE_INSENSITIVE);
        Matcher hotelMatcher = hotelPattern.matcher(text);
        if (hotelMatcher.find()) {
            String hotelName = hotelMatcher.group(1).trim();
            metadata.put("hotelName", hotelName);
            log.info("🏨 Extracted hotel name: '{}'", hotelName);
        } else {
            log.warn("⚠️ Could not extract hotel name from OCR text");
            metadata.put("hotelName", "Unknown Hotel");
        }

        // Extract department (after "TIMESHEET")
        Pattern deptPattern = Pattern.compile("TIMESHEET\\s+([A-Z\\s&]+)", Pattern.CASE_INSENSITIVE);
        Matcher deptMatcher = deptPattern.matcher(text);
        if (deptMatcher.find()) {
            String department = deptMatcher.group(1).trim();
            metadata.put("department", department);
            log.info("🏢 Extracted department: '{}'", department);
        } else {
            log.warn("⚠️ Could not extract department from OCR text");
            metadata.put("department", "Unknown Department");
        }

        // Extract dates from header - try multiple patterns
        List<LocalDate> dates = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();

        log.info("🔍 Searching for dates in OCR text...");

        // Pattern 1: "10-Jun", "11-Jun" format
        Pattern datePattern1 = Pattern.compile("(\\d{1,2})-(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)", Pattern.CASE_INSENSITIVE);
        Matcher dateMatcher1 = datePattern1.matcher(text);
        while (dateMatcher1.find()) {
            String day = dateMatcher1.group(1);
            String month = dateMatcher1.group(2);
            LocalDate date = parseDate(day, month, currentYear);
            dates.add(date);
            log.info("📅 Pattern 1 - Found date: {}-{} → {}", day, month, date);
        }

        // Pattern 2: "10/Jun", "11/Jun" format (alternative)
        if (dates.isEmpty()) {
            Pattern datePattern2 = Pattern.compile("(\\d{1,2})/(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)", Pattern.CASE_INSENSITIVE);
            Matcher dateMatcher2 = datePattern2.matcher(text);
            while (dateMatcher2.find()) {
                String day = dateMatcher2.group(1);
                String month = dateMatcher2.group(2);
                LocalDate date = parseDate(day, month, currentYear);
                dates.add(date);
                log.info("📅 Pattern 2 - Found date: {}/{} → {}", day, month, date);
            }
        }

        // Pattern 3: "Jun 10", "Jun 11" format (month first)
        if (dates.isEmpty()) {
            Pattern datePattern3 = Pattern.compile("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+(\\d{1,2})", Pattern.CASE_INSENSITIVE);
            Matcher dateMatcher3 = datePattern3.matcher(text);
            while (dateMatcher3.find()) {
                String month = dateMatcher3.group(1);
                String day = dateMatcher3.group(2);
                LocalDate date = parseDate(day, month, currentYear);
                dates.add(date);
                log.info("📅 Pattern 3 - Found date: {} {} → {}", month, day, date);
            }
        }

        // Pattern 4: Just month names with numbers nearby (more lenient)
        if (dates.isEmpty()) {
            log.warn("⚠️ Standard date patterns failed, trying lenient extraction...");
            Pattern datePattern4 = Pattern.compile("(\\d{1,2})\\s*[-/]?\\s*(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)", Pattern.CASE_INSENSITIVE);
            Matcher dateMatcher4 = datePattern4.matcher(text);
            while (dateMatcher4.find()) {
                String day = dateMatcher4.group(1);
                String month = dateMatcher4.group(2);
                LocalDate date = parseDate(day, month, currentYear);
                dates.add(date);
                log.info("📅 Pattern 4 (lenient) - Found date: {}-{} → {}", day, month, date);
            }
        }

        if (!dates.isEmpty()) {
            // Remove duplicates and sort
            dates = dates.stream().distinct().sorted().collect(Collectors.toList());

            // Check if dates are way in the past (more than 6 months) - if so, they're probably for next year
            LocalDate firstDate = dates.get(0);
            LocalDate today = LocalDate.now();

            if (firstDate.isBefore(today.minusMonths(6))) {
                // Dates are more than 6 months in the past, likely meant for next year
                log.info("📅 Dates appear to be for next year (detected: {}, today: {})", firstDate, today);
                int nextYear = today.getYear() + 1;
                List<LocalDate> adjustedDates = new ArrayList<>();

                // Re-extract dates with next year - Pattern 1
                Matcher adjustMatcher1 = datePattern1.matcher(text);
                while (adjustMatcher1.find()) {
                    String day = adjustMatcher1.group(1);
                    String month = adjustMatcher1.group(2);
                    LocalDate date = parseDate(day, month, nextYear);
                    adjustedDates.add(date);
                }

                if (!adjustedDates.isEmpty()) {
                    dates = adjustedDates.stream().distinct().sorted().collect(Collectors.toList());
                    log.info("✅ Adjusted dates to year {}: {} to {}", nextYear, dates.get(0), dates.get(dates.size() - 1));
                }
            }

            metadata.put("startDate", dates.get(0).toString());
            metadata.put("endDate", dates.get(dates.size() - 1).toString());

            log.info("✅ Extracted date range: {} to {} ({} dates)",
                    dates.get(0), dates.get(dates.size() - 1), dates.size());
        } else {
            log.warn("⚠️ NO DATES FOUND in OCR text! Defaulting to current week");
            log.warn("💡 Check if date header row (10-Jun, 11-Jun, etc.) is being extracted by OCR");

            // Show a sample of the OCR text for debugging
            String sampleText = text.length() > 500 ? text.substring(0, 500) : text;
            log.warn("📄 OCR text sample:\n{}", sampleText);

            // Default to current week
            LocalDate today = LocalDate.now();
            metadata.put("startDate", today.toString());
            metadata.put("endDate", today.plusDays(6).toString());

            log.warn("⚠️ Using fallback dates: {} to {}", today, today.plusDays(6));
        }

        log.info("📊 Parsed metadata: {}", metadata);
        return metadata;
    }

    /**
     * Parse employee schedules from OCR text
     */
    private List<RotaSchedule> parseRotaSchedules(String text, Map<String, String> metadata) {
        List<RotaSchedule> schedules = new ArrayList<>();

        // Get all employees from database
        List<Employee> allEmployees = employeeRepository.findAll();
        log.info("📋 Total employees in database: {}", allEmployees.size());

        if (allEmployees.isEmpty()) {
            log.warn("⚠️ No employees found in database! Please create employee profiles first.");
            return schedules;
        }

        // Log ALL employee names for debugging
        log.info("📝 All employee names in database:");
        allEmployees.forEach(e ->
            log.info("   👤 '{}'", e.getFullName())
        );

        Map<String, Employee> employeeMap = allEmployees.stream()
                .collect(Collectors.toMap(
                        e -> e.getFullName().toLowerCase().trim(),
                        e -> e
                ));

        // Split text into lines
        String[] lines = text.split("\\n");
        log.info("📝 OCR text split into {} lines", lines.length);

        // Log ALL lines for complete debugging
        log.info("📄 ALL OCR text lines:");
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].trim().isEmpty()) {
                log.info("  Line {}: '{}'", i + 1, lines[i].trim());
            }
        }

        LocalDate startDate = LocalDate.parse(metadata.get("startDate"));
        LocalDate endDate = LocalDate.parse(metadata.get("endDate"));
        List<LocalDate> weekDates = generateDateRange(startDate, endDate);

        // Parse each line for employee schedule
        int lineNumber = 0;
        for (String line : lines) {
            lineNumber++;
            line = line.trim();
            if (line.length() < 5) continue;

            String lineLower = line.toLowerCase().trim();

            // Skip header rows - these contain day names, dates, or unit labels
            if (isHeaderRow(lineLower)) {
                log.debug("⏭️ Skipping header/label row {}: '{}'", lineNumber, line.substring(0, Math.min(60, line.length())));
                continue;
            }

            // Check if line contains an employee name
            Employee matchedEmployee = null;
            String employeeNameFromLine = null;

            // Strategy 0: Check every employee name against this line (most thorough)
            log.debug("🔍 Checking line {}: '{}'", lineNumber, line.substring(0, Math.min(80, line.length())));

            // Special debug for names containing "mansoor" or "mudassir"
            if (lineLower.contains("mansoor") || lineLower.contains("mudassir") ||
                lineLower.contains("mansor") || lineLower.contains("mudasir")) {
                log.info("🔍 MANSOOR DEBUG - Line {} contains name variation: '{}'", lineNumber, line);
            }

            for (Employee emp : allEmployees) {
                String fullName = emp.getFullName().toLowerCase().trim();
                if (lineLower.contains(fullName)) {
                    matchedEmployee = emp;
                    employeeNameFromLine = emp.getFullName();
                    log.info("✅ Line {}: STRATEGY 0 (Full name exact) - Found '{}' in: '{}'",
                            lineNumber, employeeNameFromLine, line.substring(0, Math.min(80, line.length())));
                    break;
                }
            }

            // Strategy 1: Exact contains match using employeeMap
            if (matchedEmployee == null) {
                for (Map.Entry<String, Employee> entry : employeeMap.entrySet()) {
                    String empName = entry.getKey();
                    if (lineLower.contains(empName)) {
                        matchedEmployee = entry.getValue();
                        employeeNameFromLine = entry.getValue().getFullName();
                        log.info("✅ Line {}: STRATEGY 1 (Map match) - Found '{}' in: '{}'",
                                lineNumber, employeeNameFromLine, line.substring(0, Math.min(80, line.length())));
                        break;
                    }
                }
            }

            // Strategy 2: Match first and last name separately (handles OCR spacing issues)
            if (matchedEmployee == null) {
                for (Employee emp : allEmployees) {
                    String fullName = emp.getFullName().toLowerCase().trim();
                    String[] nameParts = fullName.split("\\s+");

                    if (nameParts.length >= 2) {
                        String firstName = nameParts[0];
                        String lastName = nameParts[nameParts.length - 1];

                        // Check if both first and last name appear in the line (even with other text between)
                        if (lineLower.contains(firstName) && lineLower.contains(lastName)) {
                            matchedEmployee = emp;
                            employeeNameFromLine = emp.getFullName();
                            log.info("✅ Line {}: STRATEGY 2 (Name parts) - Found '{}' (matched '{}' + '{}') in: '{}'",
                                    lineNumber, employeeNameFromLine, firstName, lastName,
                                    line.substring(0, Math.min(80, line.length())));
                            break;
                        }

                        // Try with partial matching (first 4-5 chars) for OCR errors
                        if (firstName.length() >= 4 && lastName.length() >= 4) {
                            String firstNamePrefix = firstName.substring(0, Math.min(5, firstName.length()));
                            String lastNamePrefix = lastName.substring(0, Math.min(5, lastName.length()));

                            if (lineLower.contains(firstNamePrefix) && lineLower.contains(lastNamePrefix)) {
                                matchedEmployee = emp;
                                employeeNameFromLine = emp.getFullName();
                                log.info("✅ Line {}: STRATEGY 2a (Partial name match) - Found '{}' (matched '{}*' + '{}*') in: '{}'",
                                        lineNumber, employeeNameFromLine, firstNamePrefix, lastNamePrefix,
                                        line.substring(0, Math.min(80, line.length())));
                                break;
                            }
                        }
                    }

                    // Also try matching just first name if it's long enough (>5 chars) and uncommon
                    if (nameParts.length >= 1 && nameParts[0].length() > 5) {
                        String firstName = nameParts[0];
                        if (lineLower.contains(firstName)) {
                            // Check if any other employee has same first name
                            long sameFirstNameCount = allEmployees.stream()
                                .filter(e -> e.getFullName().toLowerCase().startsWith(firstName))
                                .count();
                            if (sameFirstNameCount == 1) {
                                matchedEmployee = emp;
                                employeeNameFromLine = emp.getFullName();
                                log.info("✅ Line {}: STRATEGY 2b (Unique first name) - Found '{}' (matched '{}') in: '{}'",
                                        lineNumber, employeeNameFromLine, firstName,
                                        line.substring(0, Math.min(80, line.length())));
                                break;
                            }
                        }
                    }
                }
            }

            // Strategy 3: Fuzzy match on each name part separately (handles OCR errors)
            if (matchedEmployee == null) {
                for (Employee emp : allEmployees) {
                    String fullName = emp.getFullName().toLowerCase().trim();
                    String[] nameParts = fullName.split("\\s+");

                    int matchedParts = 0;
                    StringBuilder matchLog = new StringBuilder();

                    for (String part : nameParts) {
                        if (part.length() < 3) continue; // Skip very short parts

                        // Check exact match
                        if (lineLower.contains(part)) {
                            matchedParts++;
                            matchLog.append(part).append("(exact) ");
                        } else {
                            // Check fuzzy match for this part
                            String[] lineParts = lineLower.split("\\s+");
                            for (String linePart : lineParts) {
                                // Remove special characters from both for better comparison
                                String cleanLinePart = linePart.replaceAll("[^a-z0-9]", "");
                                String cleanNamePart = part.replaceAll("[^a-z0-9]", "");

                                if (cleanLinePart.length() >= 3 && cleanNamePart.length() >= 3) {
                                    double similarity = calculateSimilarity(cleanLinePart, cleanNamePart);

                                    // Lower threshold to 0.70 for better OCR error handling
                                    if (similarity > 0.70) {
                                        matchedParts++;
                                        matchLog.append(part).append("(fuzzy:").append(String.format("%.2f", similarity)).append(") ");
                                        log.debug("   Fuzzy match: '{}' ~= '{}' (similarity: {:.2f})",
                                                linePart, part, similarity);
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    // If at least 2 name parts matched (or all parts if name has only 2)
                    if (matchedParts >= Math.min(2, nameParts.length)) {
                        matchedEmployee = emp;
                        employeeNameFromLine = emp.getFullName();
                        log.info("✅ Line {}: STRATEGY 3 (Fuzzy name parts) - Found '{}' ({}/{} parts: {}) in: '{}'",
                                lineNumber, employeeNameFromLine, matchedParts, nameParts.length,
                                matchLog.toString().trim(),
                                line.substring(0, Math.min(60, line.length())));
                        break;
                    }
                }
            }

            // Strategy 4: Check if line contains pattern like "FirstName MiddleName"
            if (matchedEmployee == null) {
                Pattern namePattern = Pattern.compile("([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)+)");
                Matcher nameMatcher = namePattern.matcher(line);

                while (nameMatcher.find()) {
                    String possibleName = nameMatcher.group(1).toLowerCase().trim();

                    for (Employee emp : allEmployees) {
                        String fullName = emp.getFullName().toLowerCase().trim();
                        if (calculateSimilarity(possibleName, fullName) > 0.65) {
                            matchedEmployee = emp;
                            employeeNameFromLine = emp.getFullName();
                            log.info("✅ Line {}: Pattern match - Found employee '{}' from extracted name '{}' in line: '{}'",
                                    lineNumber, employeeNameFromLine, possibleName,
                                    line.substring(0, Math.min(60, line.length())));
                            break;
                        }
                    }
                    if (matchedEmployee != null) break;
                }
            }

            // Strategy 5: Aggressive whole-line fuzzy match (last resort for OCR errors)
            if (matchedEmployee == null) {
                for (Employee emp : allEmployees) {
                    String fullName = emp.getFullName().toLowerCase().trim();

                    // Extract first 30 chars of line (usually contains the name)
                    String lineStart = lineLower.length() > 30 ? lineLower.substring(0, 30) : lineLower;

                    // Check if the full name or significant parts appear in line start
                    double similarity = calculateSimilarity(lineStart, fullName);
                    if (similarity > 0.60) {
                        matchedEmployee = emp;
                        employeeNameFromLine = emp.getFullName();
                        log.info("✅ Line {}: STRATEGY 5 (Aggressive fuzzy) - Found '{}' (similarity: {}) in: '{}'",
                                lineNumber, employeeNameFromLine,
                                String.format("%.2f", similarity),
                                line.substring(0, Math.min(60, line.length())));
                        break;
                    }
                }
            }

            // If still no match, log the line for manual review
            if (matchedEmployee == null) {
                // Only log if line seems to have time information (might be employee row)
                if (line.matches(".*\\d{1,2}[:.]\\d{2}.*")) {
                    log.warn("⚠️ Line {}: Could not match employee for line with time data: '{}'",
                            lineNumber, line.substring(0, Math.min(80, line.length())));
                    log.warn("   💡 Hint: Check if employee name exists in database or if OCR misread the name");
                }
                continue;
            }

            if (matchedEmployee != null) {
                // Extract time slots from the line
                List<String> timeSlots = extractTimeSlots(line);

                if (timeSlots.isEmpty()) {
                    log.warn("⚠️ No time slots found for employee '{}' in line: '{}'",
                            employeeNameFromLine, line.substring(0, Math.min(60, line.length())));
                    continue;
                }

                log.info("📅 Found {} time slots for '{}': {}", timeSlots.size(), employeeNameFromLine, timeSlots);

                // Match time slots to dates
                for (int i = 0; i < Math.min(timeSlots.size(), weekDates.size()); i++) {
                    RotaSchedule schedule = new RotaSchedule();
                    schedule.setEmployeeId(matchedEmployee.getId());
                    schedule.setEmployeeName(employeeNameFromLine);
                    schedule.setScheduleDate(weekDates.get(i));
                    schedule.setDayOfWeek(weekDates.get(i).getDayOfWeek().toString());
                    schedule.setDuty(timeSlots.get(i));

                    // Parse start and end times
                    parseTimeFromDuty(schedule, timeSlots.get(i));

                    schedules.add(schedule);
                }
            }
        }

        log.info("📊 Total schedule records created: {}", schedules.size());

        return schedules;
    }

    /**
     * Extract time slots from a line (e.g., "08:00-18:00", "17:00-03:00", "08:18:00")
     */
    private List<String> extractTimeSlots(String line) {
        List<String> slots = new ArrayList<>();

        // Pattern 1: Compressed time format like "08:18:00" -> "08:00-18:00"
        // Where first 2 digits are check-in hour, next 2 are check-out hour
        Pattern compressedTimePattern = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})");
        Matcher compressedMatcher = compressedTimePattern.matcher(line);

        while (compressedMatcher.find()) {
            String checkInHour = compressedMatcher.group(1);
            String checkOutHour = compressedMatcher.group(2);
            String convertedTime = checkInHour + ":00-" + checkOutHour + ":00";
            slots.add(convertedTime);
            log.info("🕐 Converted compressed time '{}:{}:{}' to '{}'",
                    checkInHour, checkOutHour, compressedMatcher.group(3), convertedTime);
        }

        // Pattern 2: Standard time ranges: HH:MM-HH:MM
        Pattern timePattern = Pattern.compile("(\\d{1,2}:\\d{2})-(\\d{1,2}:\\d{2})");
        Matcher matcher = timePattern.matcher(line);

        while (matcher.find()) {
            slots.add(matcher.group(0)); // e.g., "08:00-18:00"
        }

        // Also check for keywords like "Set-Ups", "OFF", etc.
        Pattern keywordPattern = Pattern.compile("(Set-Ups|Set-ups|OFF|Holiday|Leave)", Pattern.CASE_INSENSITIVE);
        Matcher keywordMatcher = keywordPattern.matcher(line);

        while (keywordMatcher.find()) {
            slots.add(keywordMatcher.group(0));
        }

        return slots;
    }

    /**
     * Parse start and end time from duty string
     */
    private void parseTimeFromDuty(RotaSchedule schedule, String duty) {
        if (duty == null || duty.isEmpty()) {
            schedule.setIsOffDay(true);
            return;
        }

        // Check if it's an off day
        if (duty.matches("(?i)(OFF|Leave|Holiday)")) {
            schedule.setIsOffDay(true);
            return;
        }

        // Parse time range (e.g., "08:00-18:00")
        Pattern timePattern = Pattern.compile("(\\d{1,2}):(\\d{2})-(\\d{1,2}):(\\d{2})");
        Matcher matcher = timePattern.matcher(duty);

        if (matcher.find()) {
            int startHour = Integer.parseInt(matcher.group(1));
            int startMinute = Integer.parseInt(matcher.group(2));
            int endHour = Integer.parseInt(matcher.group(3));
            int endMinute = Integer.parseInt(matcher.group(4));

            schedule.setStartTime(LocalTime.of(startHour, startMinute));
            schedule.setEndTime(LocalTime.of(endHour, endMinute));
            schedule.setIsOffDay(false);
        } else {
            // For non-standard duties like "Set-Ups", mark as work day but no specific times
            schedule.setIsOffDay(false);
        }
    }

    /**
     * Get ROTA schedules filtered by reporting hierarchy
     */
    public List<RotaScheduleDTO> getRotaSchedules(Long rotaId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Long> accessibleEmployeeIds;

        if (user.getRoles().contains("SUPER_ADMIN")) {
            // SUPER_ADMIN can see all employees
            accessibleEmployeeIds = employeeRepository.findAll().stream()
                    .map(Employee::getId)
                    .collect(Collectors.toList());
        } else if (user.getRoles().contains("ADMIN")) {
            // ADMIN can see their department employees
            Employee adminEmployee = employeeRepository.findByWorkEmail(user.getEmail())
                    .orElseThrow(() -> new RuntimeException("Employee profile not found"));

            accessibleEmployeeIds = employeeRepository
                    .findByDepartment(adminEmployee.getDepartment()).stream()
                    .map(Employee::getId)
                    .collect(Collectors.toList());
        } else {
            // Regular USER can only see themselves
            Employee employee = employeeRepository.findByWorkEmail(user.getEmail())
                    .orElseThrow(() -> new RuntimeException("Employee profile not found"));
            accessibleEmployeeIds = Collections.singletonList(employee.getId());
        }

        // Get schedules for accessible employees
        List<RotaSchedule> schedules = rotaScheduleRepository.findByRotaIdAndEmployeeIdIn(rotaId, accessibleEmployeeIds);

        // Group by employee
        Map<Long, List<RotaSchedule>> groupedSchedules = schedules.stream()
                .collect(Collectors.groupingBy(RotaSchedule::getEmployeeId));

        // Convert to DTO
        List<RotaScheduleDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<RotaSchedule>> entry : groupedSchedules.entrySet()) {
            RotaScheduleDTO dto = new RotaScheduleDTO();
            dto.setEmployeeId(entry.getKey());
            dto.setEmployeeName(entry.getValue().get(0).getEmployeeName());

            Map<LocalDate, RotaScheduleDTO.DaySchedule> daySchedules = new LinkedHashMap<>();
            for (RotaSchedule schedule : entry.getValue()) {
                RotaScheduleDTO.DaySchedule daySchedule = new RotaScheduleDTO.DaySchedule();
                daySchedule.setDayOfWeek(schedule.getDayOfWeek());
                daySchedule.setDuty(schedule.getDuty());
                daySchedule.setStartTime(schedule.getStartTime() != null ? schedule.getStartTime().toString() : null);
                daySchedule.setEndTime(schedule.getEndTime() != null ? schedule.getEndTime().toString() : null);
                daySchedule.setIsOffDay(schedule.getIsOffDay());

                daySchedules.put(schedule.getScheduleDate(), daySchedule);
            }

            dto.setSchedules(daySchedules);
            result.add(dto);
        }

        return result;
    }

    /**
     * Get all ROTAs
     */
    public List<RotaDTO> getAllRotas() {
        List<Rota> rotas = rotaRepository.findAllByOrderByUploadedDateDesc();
        return rotas.stream()
                .map(r -> {
                    List<RotaSchedule> schedules = rotaScheduleRepository.findByRotaId(r.getId());
                    // Count unique employees, not total schedules
                    long uniqueEmployees = schedules.stream()
                            .map(RotaSchedule::getEmployeeId)
                            .distinct()
                            .count();
                    return convertToDTO(r, (int) uniqueEmployees);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get current/upcoming week ROTA schedules for a specific employee
     */
    public List<RotaScheduleDTO.DaySchedule> getEmployeeCurrentWeekSchedule(Long employeeId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(3); // Include last 3 days
        LocalDate weekEnd = today.plusDays(7); // Next 7 days

        List<RotaSchedule> schedules = rotaScheduleRepository
                .findByEmployeeIdAndScheduleDateBetween(employeeId, weekStart, weekEnd);

        // Sort by date
        schedules.sort(Comparator.comparing(RotaSchedule::getScheduleDate));

        // Convert to DTO
        List<RotaScheduleDTO.DaySchedule> result = new ArrayList<>();
        for (RotaSchedule schedule : schedules) {
            RotaScheduleDTO.DaySchedule daySchedule = new RotaScheduleDTO.DaySchedule();
            daySchedule.setDayOfWeek(schedule.getScheduleDate().toString() + " (" + schedule.getDayOfWeek() + ")");
            daySchedule.setDuty(schedule.getDuty());
            daySchedule.setStartTime(schedule.getStartTime() != null ? schedule.getStartTime().toString() : null);
            daySchedule.setEndTime(schedule.getEndTime() != null ? schedule.getEndTime().toString() : null);
            daySchedule.setIsOffDay(schedule.getIsOffDay());
            result.add(daySchedule);
        }

        return result;
    }

    /**
     * Get detailed extraction data for debugging and verification
     */
    public Map<String, Object> getExtractionDetails(Long rotaId) {
        Rota rota = rotaRepository.findById(rotaId)
                .orElseThrow(() -> new RuntimeException("ROTA not found with ID: " + rotaId));

        List<RotaSchedule> schedules = rotaScheduleRepository.findByRotaId(rotaId);

        Map<String, Object> details = new HashMap<>();

        // Basic info
        details.put("id", rota.getId());
        details.put("fileName", rota.getFileName());
        details.put("hotelName", rota.getHotelName());
        details.put("department", rota.getDepartment());
        details.put("startDate", rota.getStartDate());
        details.put("endDate", rota.getEndDate());
        details.put("uploadedDate", rota.getUploadedDate());
        details.put("uploadedByName", rota.getUploadedByName());

        // OCR extraction details
        details.put("extractedText", rota.getExtractedText());
        details.put("extractedTextLength", rota.getExtractedText() != null ? rota.getExtractedText().length() : 0);

        // Schedule statistics
        details.put("totalScheduleRecords", schedules.size());
        details.put("uniqueEmployees", schedules.stream()
                .map(RotaSchedule::getEmployeeName)
                .distinct()
                .count());
        details.put("offDays", schedules.stream()
                .filter(RotaSchedule::getIsOffDay)
                .count());
        details.put("workDays", schedules.stream()
                .filter(s -> !s.getIsOffDay())
                .count());

        // Group schedules by employee
        Map<String, List<Map<String, Object>>> employeeSchedules = new LinkedHashMap<>();
        Map<String, List<RotaSchedule>> groupedByEmployee = schedules.stream()
                .collect(Collectors.groupingBy(
                        RotaSchedule::getEmployeeName,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        for (Map.Entry<String, List<RotaSchedule>> entry : groupedByEmployee.entrySet()) {
            String employeeName = entry.getKey();
            List<RotaSchedule> empSchedules = entry.getValue();
            empSchedules.sort(Comparator.comparing(RotaSchedule::getScheduleDate));

            List<Map<String, Object>> scheduleDetails = new ArrayList<>();
            for (RotaSchedule schedule : empSchedules) {
                Map<String, Object> scheduleInfo = new LinkedHashMap<>();
                scheduleInfo.put("date", schedule.getScheduleDate());
                scheduleInfo.put("dayOfWeek", schedule.getDayOfWeek());
                scheduleInfo.put("duty", schedule.getDuty());
                scheduleInfo.put("startTime", schedule.getStartTime());
                scheduleInfo.put("endTime", schedule.getEndTime());
                scheduleInfo.put("isOffDay", schedule.getIsOffDay());
                scheduleDetails.add(scheduleInfo);
            }
            employeeSchedules.put(employeeName, scheduleDetails);
        }

        details.put("employeeSchedules", employeeSchedules);

        // Preview of raw text (first 500 characters)
        if (rota.getExtractedText() != null && rota.getExtractedText().length() > 500) {
            details.put("textPreview", rota.getExtractedText().substring(0, 500) + "...");
        } else {
            details.put("textPreview", rota.getExtractedText());
        }

        log.info("📊 Generated extraction details for ROTA ID: {}", rotaId);
        return details;
    }

    // Helper methods

    /**
     * Calculate similarity between two strings (Levenshtein distance based)
     * Returns a value between 0 (completely different) and 1 (identical)
     */
    private double calculateSimilarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) {
            longer = s2;
            shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) {
            return 1.0;
        }
        return (longerLength - editDistance(longer, shorter)) / (double) longerLength;
    }

    /**
     * Calculate edit distance (Levenshtein distance) between two strings
     */
    private int editDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                            newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                        }
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0) {
                costs[s2.length()] = lastValue;
            }
        }
        return costs[s2.length()];
    }

    /**
     * Check if a line is a header row that should be skipped
     * Headers include: day names, date rows, unit labels (Set-Ups), etc.
     */
    private boolean isHeaderRow(String line) {
        String lineLower = line.toLowerCase().trim();

        // Skip if line contains day names (Mon, Tue, Wed, etc.)
        String[] dayNames = {"mon", "tue", "wed", "thu", "fri", "sat", "sun",
                            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
        for (String day : dayNames) {
            if (lineLower.contains(day)) {
                return true;
            }
        }

        // Skip if line contains date patterns (10-Jun, 11-Jun, etc.)
        if (lineLower.matches(".*(\\d{1,2})[-/](jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec).*")) {
            return true;
        }

        // Skip if line is mostly "Set-Ups" or "SET-UPS" (the orange unit row)
        if (lineLower.contains("set-up") || lineLower.contains("setup")) {
            // Count how many times it appears
            int setupCount = (lineLower.split("set-?up", -1).length - 1);
            if (setupCount >= 3) { // If "set-up" appears 3+ times, it's the unit label row
                return true;
            }
        }

        // Skip if line contains hotel/timesheet header
        if (lineLower.contains("timesheet") || lineLower.contains("landmark hotel") ||
            lineLower.contains("conference") || lineLower.contains("banquet")) {
            return true;
        }

        // Skip if line contains "unit" label
        if (lineLower.matches(".*\\bunit\\b.*")) {
            return true;
        }

        // Skip lines that are mostly dashes, pipes, underscores, or special characters (table borders/noise)
        String cleanLine = lineLower.replaceAll("[^a-z0-9]", "");
        if (cleanLine.length() < 5) { // Less than 5 alphanumeric chars = probably a border or noise
            return true;
        }

        // Skip lines with too many special characters (likely OCR noise)
        int specialCharCount = line.replaceAll("[a-zA-Z0-9\\s]", "").length();
        int totalLength = line.length();
        if (totalLength > 0 && (double) specialCharCount / totalLength > 0.5) { // More than 50% special chars
            return true;
        }

        // Skip lines that look like garbage (many single letters separated by spaces)
        String[] words = lineLower.split("\\s+");
        int singleCharWords = 0;
        for (String word : words) {
            if (word.length() == 1 && !word.matches("\\d")) { // Single letter, not a number
                singleCharWords++;
            }
        }
        if (words.length > 5 && singleCharWords > words.length / 2) { // More than half are single letters
            return true;
        }

        return false;
    }

    private LocalDate parseDate(String day, String month, int year) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH);
        return LocalDate.parse(day + "-" + month + "-" + year, formatter);
    }

    private List<LocalDate> generateDateRange(LocalDate start, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            dates.add(current);
            current = current.plusDays(1);
        }
        return dates;
    }

    /**
     * Preprocess image specifically for colored cells (yellow, green, etc.)
     * Removes color tint and makes text more readable for OCR
     */
    private BufferedImage preprocessForColoredCells(BufferedImage original) {
        try {
            log.info("🎨 Starting color removal preprocessing...");

            // Step 1: Scale up for better OCR
            BufferedImage scaled = scaleImageIfNeeded(original);
            log.info("📐 Scaled to: {}x{}", scaled.getWidth(), scaled.getHeight());

            // Step 2: Remove yellow and colored backgrounds
            BufferedImage decolored = removeColoredBackground(scaled);
            log.info("🎨 Yellow/colored backgrounds removed");

            // Step 3: Convert to grayscale
            BufferedImage grayscale = convertToGrayscale(decolored);
            log.info("⚫ Converted to grayscale");

            // Step 4: Light contrast enhancement (reduced from 1.3 to 1.2)
            BufferedImage enhanced = increaseContrast(grayscale, 1.2f);
            log.info("🔆 Light contrast applied");

            // Step 5: Apply light sharpening to make text edges clearer
            BufferedImage sharpened = sharpenImage(enhanced);
            log.info("✨ Text sharpening applied");

            return sharpened;
        } catch (Exception e) {
            log.warn("⚠️ Color preprocessing failed, using original: {}", e.getMessage());
            return original;
        }
    }

    /**
     * Remove yellow and colored backgrounds from image
     * Converts colored cells to white background with black text
     * Enhanced version for heavily colored ROTAs
     */
    private BufferedImage removeColoredBackground(BufferedImage image) {
        BufferedImage result = new BufferedImage(
            image.getWidth(),
            image.getHeight(),
            BufferedImage.TYPE_INT_RGB
        );

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);

                // Extract RGB components
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                // Calculate brightness and color ratios
                int brightness = (red + green + blue) / 3;

                // Calculate color dominance
                int yellowScore = red + green - (2 * blue); // High if yellow
                int orangeScore = red - blue; // High if orange/red

                // If pixel is dark (text) - VERY lenient threshold to preserve all text
                if (brightness < 130) {
                    result.setRGB(x, y, 0xFF000000); // Black text
                }
                // Very light pixels (nearly white) - definitely background
                else if (brightness > 230) {
                    result.setRGB(x, y, 0xFFFFFFFF); // White
                }
                // Yellow cells - AGGRESSIVE detection (this is the key for your ROTA)
                else if (yellowScore > 100) {
                    result.setRGB(x, y, 0xFFFFFFFF); // White
                }
                // Orange cells - also very common in your ROTA
                else if (orangeScore > 80 && red > 150) {
                    result.setRGB(x, y, 0xFFFFFFFF); // White
                }
                // Light yellow cells (R and G high, B lower)
                else if (red > 180 && green > 180 && blue < 160) {
                    result.setRGB(x, y, 0xFFFFFFFF); // White
                }
                // Medium yellow cells
                else if (red > 150 && green > 140 && blue < 120) {
                    result.setRGB(x, y, 0xFFFFFFFF); // White
                }
                // Orange-yellow cells
                else if (red > 180 && green > 120 && blue < 120) {
                    result.setRGB(x, y, 0xFFFFFFFF); // White
                }
                // Gray/beige background cells
                else if (Math.abs(red - green) < 40 && Math.abs(green - blue) < 40 && brightness > 140) {
                    result.setRGB(x, y, 0xFFFFFFFF); // White
                }
                // Light colored backgrounds (any light color)
                else if (brightness > 180) {
                    result.setRGB(x, y, 0xFFFFFFFF); // White
                }
                // Medium brightness colored backgrounds
                else if (brightness > 140 && (red > blue + 20 || green > blue + 20)) {
                    result.setRGB(x, y, 0xFFFFFFFF); // White
                }
                // Otherwise keep the pixel
                else {
                    result.setRGB(x, y, rgb);
                }
            }
        }

        return result;
    }

    /**
     * Preprocess image for better OCR recognition
     * Minimal processing - just scale and grayscale
     */
    private BufferedImage preprocessImageForOCR(BufferedImage original) {
        try {
            // Step 1: Scale up if image is too small (for better text recognition)
            BufferedImage scaled = scaleImageIfNeeded(original);
            log.info("📐 Scaled to: {}x{}", scaled.getWidth(), scaled.getHeight());

            // Step 2: Convert to grayscale
            BufferedImage grayscale = convertToGrayscale(scaled);
            log.info("⚫ Converted to grayscale");

            // Return with minimal processing - no contrast or threshold
            // The aggressive preprocessing was destroying text quality
            return grayscale;
        } catch (Exception e) {
            log.warn("⚠️ Preprocessing failed, using original image: {}", e.getMessage());
            return original;
        }
    }

    /**
     * Scale image up if resolution is too low (less than 1500px width)
     */
    private BufferedImage scaleImageIfNeeded(BufferedImage image) {
        int targetWidth = 2000; // Higher resolution for better OCR

        if (image.getWidth() < targetWidth) {
            double scale = (double) targetWidth / image.getWidth();
            int newHeight = (int) (image.getHeight() * scale);

            BufferedImage scaledImage = new BufferedImage(targetWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = scaledImage.createGraphics();

            // Use high-quality scaling
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.drawImage(image, 0, 0, targetWidth, newHeight, null);
            g2d.dispose();

            return scaledImage;
        }

        return image;
    }

    /**
     * Convert image to grayscale
     */
    private BufferedImage convertToGrayscale(BufferedImage image) {
        BufferedImage grayscale = new BufferedImage(
            image.getWidth(),
            image.getHeight(),
            BufferedImage.TYPE_BYTE_GRAY
        );

        Graphics2D g2d = grayscale.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        return grayscale;
    }

    /**
     * Increase image contrast
     */
    private BufferedImage increaseContrast(BufferedImage image, float contrastFactor) {
        RescaleOp rescaleOp = new RescaleOp(contrastFactor, 0, null);
        return rescaleOp.filter(image, null);
    }

    /**
     * Sharpen image to make text edges clearer
     */
    private BufferedImage sharpenImage(BufferedImage image) {
        // Create sharpening kernel (light sharpening)
        float[] sharpenKernel = {
            0.0f, -0.5f,  0.0f,
           -0.5f,  3.0f, -0.5f,
            0.0f, -0.5f,  0.0f
        };

        java.awt.image.Kernel kernel = new java.awt.image.Kernel(3, 3, sharpenKernel);
        java.awt.image.ConvolveOp convolveOp = new java.awt.image.ConvolveOp(
            kernel,
            java.awt.image.ConvolveOp.EDGE_NO_OP,
            null
        );

        return convolveOp.filter(image, null);
    }

    /**
     * Apply threshold to convert to black and white
     * This helps OCR by making text stark against background
     */
    private BufferedImage applyThreshold(BufferedImage image, int threshold) {
        BufferedImage result = new BufferedImage(
            image.getWidth(),
            image.getHeight(),
            BufferedImage.TYPE_BYTE_BINARY
        );

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int gray = (rgb >> 16) & 0xff; // Extract gray value

                // Convert to pure black or white based on threshold
                int newRgb = gray < threshold ? 0xFF000000 : 0xFFFFFFFF;
                result.setRGB(x, y, newRgb);
            }
        }

        return result;
    }

    /**
     * Remove grid lines from image (helps with table OCR)
     * Uses morphological operations to detect and remove lines
     */
    private BufferedImage removeGridLines(BufferedImage image) {
        BufferedImage result = new BufferedImage(
            image.getWidth(),
            image.getHeight(),
            BufferedImage.TYPE_BYTE_BINARY
        );

        // Copy original
        Graphics2D g2d = result.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        // Detect horizontal lines
        for (int y = 0; y < image.getHeight(); y++) {
            int consecutiveBlack = 0;
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int gray = (rgb >> 16) & 0xff;

                if (gray < 128) { // Black pixel
                    consecutiveBlack++;
                } else {
                    consecutiveBlack = 0;
                }

                // If we found a long horizontal line, make it white
                if (consecutiveBlack > image.getWidth() * 0.8) {
                    for (int i = x - consecutiveBlack; i <= x; i++) {
                        if (i >= 0 && i < image.getWidth()) {
                            result.setRGB(i, y, 0xFFFFFFFF);
                        }
                    }
                }
            }
        }

        // Detect vertical lines
        for (int x = 0; x < image.getWidth(); x++) {
            int consecutiveBlack = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x, y);
                int gray = (rgb >> 16) & 0xff;

                if (gray < 128) {
                    consecutiveBlack++;
                } else {
                    consecutiveBlack = 0;
                }

                // If we found a long vertical line, make it white
                if (consecutiveBlack > image.getHeight() * 0.8) {
                    for (int i = y - consecutiveBlack; i <= y; i++) {
                        if (i >= 0 && i < image.getHeight()) {
                            result.setRGB(x, i, 0xFFFFFFFF);
                        }
                    }
                }
            }
        }

        return result;
    }


    private RotaDTO convertToDTO(Rota rota, int employeeCount) {
        RotaDTO dto = new RotaDTO();
        dto.setId(rota.getId());
        dto.setHotelName(rota.getHotelName());
        dto.setDepartment(rota.getDepartment());
        dto.setFileName(rota.getFileName());
        dto.setStartDate(rota.getStartDate());
        dto.setEndDate(rota.getEndDate());
        dto.setUploadedDate(rota.getUploadedDate());
        dto.setUploadedByName(rota.getUploadedByName());
        dto.setTotalEmployees(employeeCount);
        return dto;
    }

    // ==================== ROTA UPDATE METHODS ====================

    /**
     * Update a single ROTA schedule entry
     */
    public RotaSchedule updateRotaSchedule(Long scheduleId, RotaScheduleUpdateDTO updateDTO, HttpServletRequest request) {
        log.info("🔄 Updating ROTA schedule ID: {}", scheduleId);

        RotaSchedule schedule = rotaScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found with ID: " + scheduleId));

        // Capture old values for audit log
        Map<String, Object> oldValues = captureScheduleValues(schedule);

        // Update schedule fields
        if (updateDTO.getScheduleDate() != null) {
            schedule.setScheduleDate(updateDTO.getScheduleDate());
        }
        if (updateDTO.getDayOfWeek() != null) {
            schedule.setDayOfWeek(updateDTO.getDayOfWeek());
        }
        if (updateDTO.getStartTime() != null) {
            schedule.setStartTime(updateDTO.getStartTime());
        }
        if (updateDTO.getEndTime() != null) {
            schedule.setEndTime(updateDTO.getEndTime());
        }
        if (updateDTO.getDuty() != null) {
            schedule.setDuty(updateDTO.getDuty());
        }
        if (updateDTO.getIsOffDay() != null) {
            schedule.setIsOffDay(updateDTO.getIsOffDay());
        }

        // Save updated schedule
        RotaSchedule updatedSchedule = rotaScheduleRepository.save(schedule);

        // Capture new values for audit log
        Map<String, Object> newValues = captureScheduleValues(updatedSchedule);

        // Log the change
        logRotaChange(
                updatedSchedule.getRota().getId(),
                scheduleId,
                updatedSchedule.getEmployeeId(),
                updatedSchedule.getEmployeeName(),
                "UPDATED",
                oldValues,
                newValues,
                buildChangeDescription(oldValues, newValues, updateDTO.getChangeReason()),
                request
        );

        log.info("✅ Schedule updated successfully: {}", scheduleId);
        return updatedSchedule;
    }

    /**
     * Batch update multiple ROTA schedules
     */
    public List<RotaSchedule> batchUpdateRotaSchedules(List<RotaScheduleUpdateDTO> updates, HttpServletRequest request) {
        log.info("🔄 Batch updating {} ROTA schedules", updates.size());

        List<RotaSchedule> updatedSchedules = new ArrayList<>();

        for (RotaScheduleUpdateDTO updateDTO : updates) {
            try {
                RotaSchedule updated = updateRotaSchedule(updateDTO.getScheduleId(), updateDTO, request);
                updatedSchedules.add(updated);
            } catch (Exception e) {
                log.error("❌ Failed to update schedule {}: {}", updateDTO.getScheduleId(), e.getMessage());
            }
        }

        log.info("✅ Batch update complete: {} of {} schedules updated",
                updatedSchedules.size(), updates.size());

        return updatedSchedules;
    }

    /**
     * Delete a ROTA schedule entry
     */
    public void deleteRotaSchedule(Long scheduleId, String username, HttpServletRequest request) {
        log.info("🗑️ Deleting ROTA schedule ID: {} by user: {}", scheduleId, username);

        RotaSchedule schedule = rotaScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found with ID: " + scheduleId));

        // Capture values for audit log
        Map<String, Object> oldValues = captureScheduleValues(schedule);

        // Log the deletion
        logRotaChange(
                schedule.getRota().getId(),
                scheduleId,
                schedule.getEmployeeId(),
                schedule.getEmployeeName(),
                "DELETED",
                oldValues,
                new HashMap<>(),
                "Schedule deleted by " + username,
                request
        );

        // Delete the schedule
        rotaScheduleRepository.delete(schedule);

        log.info("✅ Schedule deleted successfully: {}", scheduleId);
    }

    /**
     * Delete entire ROTA with all schedules
     */
    public void deleteRota(Long rotaId, String reason, HttpServletRequest request) {
        log.info("🗑️ Deleting entire ROTA ID: {}", rotaId);

        Rota rota = rotaRepository.findById(rotaId)
                .orElseThrow(() -> new RuntimeException("ROTA not found with ID: " + rotaId));

        List<RotaSchedule> schedules = rotaScheduleRepository.findByRotaId(rotaId);

        // Log deletion of each schedule
        for (RotaSchedule schedule : schedules) {
            Map<String, Object> oldValues = captureScheduleValues(schedule);
            logRotaChange(
                    rotaId,
                    schedule.getId(),
                    schedule.getEmployeeId(),
                    schedule.getEmployeeName(),
                    "DELETED",
                    oldValues,
                    new HashMap<>(),
                    "Deleted as part of ROTA removal" + (reason != null ? ": " + reason : ""),
                    request
            );
        }

        // Delete all schedules first
        rotaScheduleRepository.deleteAll(schedules);

        // Delete the ROTA
        rotaRepository.delete(rota);

        log.info("✅ ROTA and {} schedules deleted successfully", schedules.size());
    }

    /**
     * Replace entire ROTA (delete old, upload new)
     */
    public RotaDTO replaceRota(Long oldRotaId, MultipartFile newFile, String username, String reason, HttpServletRequest request) throws Exception {
        log.info("🔄 Replacing ROTA ID: {} with new file", oldRotaId);

        // Delete old ROTA
        deleteRota(oldRotaId, reason != null ? reason : "Replaced with new ROTA upload", request);

        // Upload new ROTA
        RotaDTO newRota = uploadRota(newFile, username);

        log.info("✅ ROTA replaced successfully. Old ID: {}, New ID: {}", oldRotaId, newRota.getId());

        return newRota;
    }

    // ==================== CHANGE LOG METHODS ====================

    /**
     * Log a ROTA change to the audit log
     */
    private void logRotaChange(Long rotaId, Long scheduleId, Long employeeId, String employeeName,
                               String changeType, Map<String, Object> oldValues, Map<String, Object> newValues,
                               String description, HttpServletRequest request) {
        try {
            User currentUser = securityUtils.getCurrentUser();
            if (currentUser == null) {
                log.warn("⚠️ Could not log change: current user not found");
                return;
            }

            RotaChangeLog changeLog = new RotaChangeLog();
            changeLog.setRotaId(rotaId);
            changeLog.setScheduleId(scheduleId);
            changeLog.setEmployeeId(employeeId);
            changeLog.setEmployeeName(employeeName);
            changeLog.setChangeType(changeType);
            changeLog.setOldValue(toJsonString(oldValues));
            changeLog.setNewValue(toJsonString(newValues));
            changeLog.setChangeDescription(description);
            changeLog.setChangedAt(LocalDateTime.now());
            changeLog.setChangedBy(currentUser.getId());
            changeLog.setChangedByName(currentUser.getUsername());
            changeLog.setChangedByRole(String.join(", ", currentUser.getRoles()));

            if (request != null) {
                changeLog.setIpAddress(getClientIp(request));
                changeLog.setUserAgent(request.getHeader("User-Agent"));
            }

            rotaChangeLogRepository.save(changeLog);

            log.info("📝 Change logged: {} - {} for employee {}", changeType, description, employeeName);
        } catch (Exception e) {
            log.error("❌ Failed to log ROTA change: {}", e.getMessage(), e);
        }
    }

    /**
     * Get all change logs for a specific ROTA
     */
    public List<RotaChangeLogDTO> getRotaChangeLogs(Long rotaId) {
        List<RotaChangeLog> logs = rotaChangeLogRepository.findByRotaIdOrderByChangedAtDesc(rotaId);
        return logs.stream().map(this::convertToChangeLogDTO).collect(Collectors.toList());
    }

    /**
     * Get change logs for a specific employee
     */
    public List<RotaChangeLogDTO> getEmployeeChangeLogs(Long employeeId) {
        List<RotaChangeLog> logs = rotaChangeLogRepository.findByEmployeeIdOrderByChangedAtDesc(employeeId);
        return logs.stream().map(this::convertToChangeLogDTO).collect(Collectors.toList());
    }

    /**
     * Get all recent change logs (for dashboard)
     */
    public List<RotaChangeLogDTO> getRecentChangeLogs(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<RotaChangeLog> logs = rotaChangeLogRepository.findRecentChanges(startDate);
        return logs.stream().map(this::convertToChangeLogDTO).collect(Collectors.toList());
    }

    /**
     * Get all change logs (for admin dashboard)
     */
    public List<RotaChangeLogDTO> getAllChangeLogs() {
        List<RotaChangeLog> logs = rotaChangeLogRepository.findAllOrderByChangedAtDesc();
        return logs.stream().map(this::convertToChangeLogDTO).collect(Collectors.toList());
    }

    /**
     * Get top 10 most recent changes (for dashboard widget)
     */
    public List<RotaChangeLogDTO> getTop10RecentChanges() {
        List<RotaChangeLog> logs = rotaChangeLogRepository.findTop10ByOrderByChangedAtDesc();
        return logs.stream().map(this::convertToChangeLogDTO).collect(Collectors.toList());
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Capture current schedule values as a map
     */
    private Map<String, Object> captureScheduleValues(RotaSchedule schedule) {
        Map<String, Object> values = new HashMap<>();
        values.put("scheduleDate", schedule.getScheduleDate());
        values.put("dayOfWeek", schedule.getDayOfWeek());
        values.put("startTime", schedule.getStartTime());
        values.put("endTime", schedule.getEndTime());
        values.put("duty", schedule.getDuty());
        values.put("isOffDay", schedule.getIsOffDay());
        return values;
    }

    /**
     * Build a human-readable change description
     */
    private String buildChangeDescription(Map<String, Object> oldValues, Map<String, Object> newValues, String reason) {
        StringBuilder description = new StringBuilder();

        if (reason != null && !reason.isEmpty()) {
            description.append(reason).append(". ");
        }

        description.append("Changes: ");
        List<String> changes = new ArrayList<>();

        for (String key : newValues.keySet()) {
            Object oldValue = oldValues.get(key);
            Object newValue = newValues.get(key);

            if (!Objects.equals(oldValue, newValue)) {
                changes.add(String.format("%s: '%s' → '%s'", key, oldValue, newValue));
            }
        }

        if (changes.isEmpty()) {
            description.append("No changes detected");
        } else {
            description.append(String.join(", ", changes));
        }

        return description.toString();
    }

    /**
     * Convert map to JSON string
     */
    private String toJsonString(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert map to JSON: {}", e.getMessage());
            return map.toString();
        }
    }

    /**
     * Get client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * Convert RotaChangeLog entity to DTO
     */
    private RotaChangeLogDTO convertToChangeLogDTO(RotaChangeLog log) {
        RotaChangeLogDTO dto = new RotaChangeLogDTO();
        dto.setId(log.getId());
        dto.setRotaId(log.getRotaId());
        dto.setScheduleId(log.getScheduleId());
        dto.setEmployeeId(log.getEmployeeId());
        dto.setEmployeeName(log.getEmployeeName());
        dto.setChangeType(log.getChangeType());
        dto.setOldValue(log.getOldValue());
        dto.setNewValue(log.getNewValue());
        dto.setChangeDescription(log.getChangeDescription());
        dto.setChangedAt(log.getChangedAt());
        dto.setChangedBy(log.getChangedBy());
        dto.setChangedByName(log.getChangedByName());
        dto.setChangedByRole(log.getChangedByRole());
        dto.setIpAddress(log.getIpAddress());
        dto.setUserAgent(log.getUserAgent());
        return dto;
    }

    /**
     * Update a rota schedule entry
     * Only ADMIN and SUPER_ADMIN can update rotas
     */
    public RotaScheduleEntryDTO updateRotaSchedule(Long scheduleId, RotaScheduleUpdateDTO updateDTO,
                                               String username, HttpServletRequest request) {
        log.info("📝 Updating rota schedule ID: {} by user: {}", scheduleId, username);

        // Check if user is ADMIN or SUPER_ADMIN
        if (!securityUtils.isAdminOrSuperAdmin()) {
            throw new RuntimeException("Access denied. Only ADMIN and SUPER_ADMIN can edit rotas.");
        }

        // Find the schedule
        RotaSchedule schedule = rotaScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Rota schedule not found with ID: " + scheduleId));

        // Get user details for audit log
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Store old values for change log
        String oldDuty = schedule.getDuty();
        LocalTime oldStartTime = schedule.getStartTime();
        LocalTime oldEndTime = schedule.getEndTime();
        Boolean oldIsOffDay = schedule.getIsOffDay();

        // Update fields if provided
        boolean hasChanges = false;
        StringBuilder changeDescription = new StringBuilder("Updated: ");

        if (updateDTO.getDuty() != null && !updateDTO.getDuty().equals(oldDuty)) {
            schedule.setDuty(updateDTO.getDuty());
            changeDescription.append("Duty from '").append(oldDuty).append("' to '").append(updateDTO.getDuty()).append("'; ");
            hasChanges = true;
        }

        if (updateDTO.getStartTime() != null && !updateDTO.getStartTime().equals(oldStartTime)) {
            schedule.setStartTime(updateDTO.getStartTime());
            changeDescription.append("Start time from ").append(oldStartTime).append(" to ").append(updateDTO.getStartTime()).append("; ");
            hasChanges = true;
        }

        if (updateDTO.getEndTime() != null && !updateDTO.getEndTime().equals(oldEndTime)) {
            schedule.setEndTime(updateDTO.getEndTime());
            changeDescription.append("End time from ").append(oldEndTime).append(" to ").append(updateDTO.getEndTime()).append("; ");
            hasChanges = true;
        }

        if (updateDTO.getIsOffDay() != null && !updateDTO.getIsOffDay().equals(oldIsOffDay)) {
            schedule.setIsOffDay(updateDTO.getIsOffDay());
            changeDescription.append("Off day from ").append(oldIsOffDay).append(" to ").append(updateDTO.getIsOffDay()).append("; ");
            hasChanges = true;
        }

        if (!hasChanges) {
            log.warn("No changes detected for schedule ID: {}", scheduleId);
            return convertToScheduleEntryDTO(schedule);
        }

        // Save updated schedule
        RotaSchedule updatedSchedule = rotaScheduleRepository.save(schedule);
        log.info("✅ Rota schedule updated successfully - ID: {}", scheduleId);

        // Create audit log
        try {
            RotaChangeLog changeLog = new RotaChangeLog();
            changeLog.setRotaId(schedule.getRota().getId());
            changeLog.setScheduleId(scheduleId);
            changeLog.setEmployeeId(schedule.getEmployeeId());
            changeLog.setEmployeeName(schedule.getEmployeeName());
            changeLog.setChangeType("UPDATE");

            // Store old and new values as JSON
            Map<String, Object> oldValues = new HashMap<>();
            oldValues.put("duty", oldDuty);
            oldValues.put("startTime", oldStartTime != null ? oldStartTime.toString() : null);
            oldValues.put("endTime", oldEndTime != null ? oldEndTime.toString() : null);
            oldValues.put("isOffDay", oldIsOffDay);

            Map<String, Object> newValues = new HashMap<>();
            newValues.put("duty", schedule.getDuty());
            newValues.put("startTime", schedule.getStartTime() != null ? schedule.getStartTime().toString() : null);
            newValues.put("endTime", schedule.getEndTime() != null ? schedule.getEndTime().toString() : null);
            newValues.put("isOffDay", schedule.getIsOffDay());

            changeLog.setOldValue(objectMapper.writeValueAsString(oldValues));
            changeLog.setNewValue(objectMapper.writeValueAsString(newValues));
            changeLog.setChangeDescription(changeDescription.toString());
            changeLog.setChangedAt(LocalDateTime.now());
            changeLog.setChangedBy(user.getId());
            changeLog.setChangedByName(user.getUsername());
            changeLog.setChangedByRole(user.getRoles().stream().findFirst().orElse("USER"));
            changeLog.setIpAddress(getClientIp(request));
            changeLog.setUserAgent(request.getHeader("User-Agent"));

            rotaChangeLogRepository.save(changeLog);
            log.info("📝 Change log created for schedule update");
        } catch (JsonProcessingException e) {
            log.error("Failed to create change log", e);
            // Don't fail the update if logging fails
        }

        return convertToScheduleEntryDTO(updatedSchedule);
    }

    /**
     * Get all schedules for a specific rota
     */
    public List<RotaScheduleEntryDTO> getRotaSchedules(Long rotaId) {
        log.info("📋 Fetching schedules for rota ID: {}", rotaId);

        // Verify rota exists
        rotaRepository.findById(rotaId)
                .orElseThrow(() -> new RuntimeException("Rota not found with ID: " + rotaId));

        List<RotaSchedule> schedules = rotaScheduleRepository.findByRotaId(rotaId);
        log.info("✅ Found {} schedules for rota ID: {}", schedules.size(), rotaId);

        return schedules.stream()
                .map(this::convertToScheduleEntryDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a single schedule by ID
     */
    public RotaScheduleEntryDTO getRotaSchedule(Long scheduleId) {
        RotaSchedule schedule = rotaScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Rota schedule not found with ID: " + scheduleId));
        return convertToScheduleEntryDTO(schedule);
    }


    /**
     * Convert RotaSchedule entity to DTO for editing
     */
    private RotaScheduleEntryDTO convertToScheduleEntryDTO(RotaSchedule schedule) {
        RotaScheduleEntryDTO dto = new RotaScheduleEntryDTO();
        dto.setId(schedule.getId());
        dto.setRotaId(schedule.getRota().getId());
        dto.setEmployeeId(schedule.getEmployeeId());
        dto.setEmployeeName(schedule.getEmployeeName());
        dto.setScheduleDate(schedule.getScheduleDate());
        dto.setDayOfWeek(schedule.getDayOfWeek());
        dto.setStartTime(schedule.getStartTime());
        dto.setEndTime(schedule.getEndTime());
        dto.setDuty(schedule.getDuty());
        dto.setIsOffDay(schedule.getIsOffDay());
        return dto;
    }
}


