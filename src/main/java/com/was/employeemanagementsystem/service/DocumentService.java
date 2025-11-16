package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.dto.DocumentDTO;
import com.was.employeemanagementsystem.entity.Document;
import com.was.employeemanagementsystem.entity.Employee;
import com.was.employeemanagementsystem.entity.User;
import com.was.employeemanagementsystem.repository.DocumentRepository;
import com.was.employeemanagementsystem.repository.EmployeeRepository;
import com.was.employeemanagementsystem.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final EmployeeRepository employeeRepository;
    private final OcrService ocrService;
    private final SecurityUtils securityUtils;

    private final String uploadDir = "uploads/documents/";

    public DocumentService(DocumentRepository documentRepository,
                          EmployeeRepository employeeRepository,
                          OcrService ocrService,
                          SecurityUtils securityUtils) {
        this.documentRepository = documentRepository;
        this.employeeRepository = employeeRepository;
        this.ocrService = ocrService;
        this.securityUtils = securityUtils;

        // Create upload directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            log.error("Could not create upload directory: {}", e.getMessage());
        }
    }

    /**
     * Validates that the uploaded file is actually a travel document (passport/visa/contract)
     * by checking for common keywords and patterns found in such documents
     */
    public boolean validateDocumentType(MultipartFile file, String expectedType) {
        try {
            // Extract text from the document
            // For CONTRACT documents, only extract from first page (optimization)
            boolean firstPageOnly = "CONTRACT".equals(expectedType);
            String extractedText = ocrService.extractTextFromDocument(file, firstPageOnly);

            if (extractedText == null || extractedText.trim().isEmpty()) {
                log.warn("No text extracted from document - cannot validate");
                return false; // If no text extracted, might not be a valid document
            }

            String textUpper = extractedText.toUpperCase();

            if ("PASSPORT".equals(expectedType)) {
                // Check for passport-specific keywords
                boolean hasPassportKeywords = textUpper.contains("PASSPORT") ||
                                             textUpper.contains("PASSEPORT") ||
                                             textUpper.contains("PASAPORTE") ||
                                             textUpper.contains("REISEPASS") ||
                                             textUpper.contains("جواز سفر") || // Arabic
                                             textUpper.contains("护照"); // Chinese

                // Check for common passport fields
                boolean hasCommonFields = textUpper.contains("NATIONALITY") ||
                                        textUpper.contains("DATE OF BIRTH") ||
                                        textUpper.contains("DOB") ||
                                        textUpper.contains("PLACE OF BIRTH") ||
                                        textUpper.contains("SURNAME") ||
                                        textUpper.contains("GIVEN NAME");

                // Check for passport number patterns (letters + numbers)
                boolean hasPassportNumber = extractedText.matches(".*\\b[A-Z]{1,3}[0-9]{6,9}\\b.*");

                log.debug("Passport validation - Keywords: {}, Fields: {}, Number: {}",
                    hasPassportKeywords, hasCommonFields, hasPassportNumber);

                return hasPassportKeywords || hasCommonFields || hasPassportNumber;

            } else if ("VISA".equals(expectedType)) {
                // Check for visa-specific keywords
                boolean hasVisaKeywords = textUpper.contains("PERMISSION TO WORK") ||
                        textUpper.contains("VISA") ||
                                        textUpper.contains("ENTRY") ||
                                        textUpper.contains("IMMIGRATION") ||
                                        textUpper.contains("PERMIT");

                boolean hasCommonFields = textUpper.contains("VALID") ||
                                        textUpper.contains("EXPIRY") ||
                                        textUpper.contains("UNTIL") ||
                                        textUpper.contains("DURATION");

                log.debug("Visa validation - Keywords: {}, Fields: {}",
                    hasVisaKeywords, hasCommonFields);

                return hasVisaKeywords || hasCommonFields;

            } else if ("CONTRACT".equals(expectedType)) {
                // Check for employment contract keywords
                boolean hasContractKeywords = textUpper.contains("CONTRACT") ||
                        textUpper.contains("AGREEMENT") ||
                        textUpper.contains("EMPLOYMENT") ||
                        textUpper.contains("EMPLOYER") ||
                        textUpper.contains("EMPLOYEE");

                boolean hasCommonFields = textUpper.contains("JOB TITLE") ||
                        textUpper.contains("POSITION") ||
                        textUpper.contains("SALARY") ||
                        textUpper.contains("START DATE") ||
                        textUpper.contains("COMMENCEMENT");

                log.debug("Contract validation - Keywords: {}, Fields: {}",
                    hasContractKeywords, hasCommonFields);

                return hasContractKeywords || hasCommonFields;
            }

            return true; // Default to true for unknown types

        } catch (Exception e) {
            log.error("Error validating document type", e);
            // In case of error, allow upload (fail open)
            return true;
        }
    }

    public DocumentDTO uploadDocument(Long employeeId, String documentType, MultipartFile file)
            throws IOException, TikaException {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

        // CRITICAL: Validate organization UUID (unless ROOT)
        if (!securityUtils.isRoot()) {
            User currentUser = securityUtils.getCurrentUser();
            if (currentUser == null || currentUser.getOrganizationUuid() == null) {
                log.error("❌ User has no organization UUID!");
                throw new AccessDeniedException("User must be associated with an organization");
            }

            // Ensure employee belongs to same organization
            if (!currentUser.getOrganizationUuid().equals(employee.getOrganizationUuid())) {
                log.error("❌ Organization UUID mismatch! User org: {}, Employee org: {}",
                         currentUser.getOrganizationUuid(), employee.getOrganizationUuid());
                throw new AccessDeniedException("Cannot access employee from different organization");
            }
            log.debug("✓ Organization UUID validated: {}", currentUser.getOrganizationUuid());
        }

        // Check access permissions - admins/super admins can upload for anyone, users can upload for themselves
        if (!canAccessEmployee(employee)) {
            throw new RuntimeException("Access denied. You can only upload documents for yourself.");
        }

        // Validate document type
        if (!documentType.equals("PASSPORT") && !documentType.equals("VISA") &&
            !documentType.equals("CONTRACT") && !documentType.equals("RESUME") &&
            !documentType.equals("SHARE_CODE")) {
            throw new RuntimeException("Invalid document type. Must be PASSPORT, VISA, CONTRACT, RESUME, or SHARE_CODE");
        }

        // For RESUME and SHARE_CODE documents, skip OCR extraction (just store the file)
        String extractedText = null;
        Map<String, Object> extractedInfo = new HashMap<>();

        if (!documentType.equals("RESUME") && !documentType.equals("SHARE_CODE")) {
            // Extract text from document (skip for RESUME and SHARE_CODE)
            log.info("🔍 Starting OCR text extraction for file: {}", file.getOriginalFilename());

            // For CONTRACT documents, only extract from first page (required data is there)
            boolean firstPageOnly = documentType.equals("CONTRACT");
            extractedText = ocrService.extractTextFromDocument(file, firstPageOnly);

            if (extractedText == null || extractedText.trim().isEmpty()) {
                log.error("❌ No text extracted from document!");
            } else {
                log.info("✅ Text extraction successful. Length: {} characters", extractedText.length());
                log.debug("📄 Extracted text preview (first 500 chars): {}",
                    extractedText.length() > 500 ? extractedText.substring(0, 500) : extractedText);
            }

            // Extract information based on document type
            if (documentType.equals("PASSPORT")) {
                log.info("🛂 Processing as PASSPORT document");
                extractedInfo = ocrService.extractPassportInformation(extractedText);
            } else if (documentType.equals("VISA")) {
                log.info("✈️ Processing as VISA document");
                extractedInfo = ocrService.extractVisaInformation(extractedText);
            } else if (documentType.equals("CONTRACT")) {
                log.info("📝 Processing as CONTRACT document");
                extractedInfo = ocrService.extractContractInformation(extractedText);
            }
        } else {
            log.info("📄 Processing as RESUME document - skipping OCR extraction");
        }

        // Debug: Log what was extracted
        log.info("📋 Extracted info from OCR: {}", extractedInfo);
        log.info("   - Full Name: {}", extractedInfo.get("fullName"));
        log.info("   - Document Number: {}", extractedInfo.get("documentNumber"));
        log.info("   - Issue Date: {}", extractedInfo.get("issueDate"));
        log.info("   - Expiry Date: {}", extractedInfo.get("expiryDate"));
        log.info("   - Issuing Country: {}", extractedInfo.get("issuingCountry"));
        log.info("   - Company Name: {}", extractedInfo.get("companyName"));
        log.info("   - Date of Check: {}", extractedInfo.get("dateOfCheck"));
        log.info("   - Reference Number: {}", extractedInfo.get("referenceNumber"));

        // Check for duplicate document
        String documentNumber = (String) extractedInfo.get("documentNumber");
        if (documentNumber != null && !documentNumber.isEmpty()) {
            List<Document> existingDocs = documentRepository.findByEmployeeId(employeeId);
            boolean isDuplicate = existingDocs.stream()
                .anyMatch(doc -> doc.getDocumentType().equals(documentType)
                              && documentNumber.equals(doc.getDocumentNumber()));

            if (isDuplicate) {
                log.warn("Warning: Duplicate document detected - {} with number {}", documentType, documentNumber);
                // Allow upload but log warning - frontend will show warning to user
            }
        }

        // Calculate MD5 hash for deduplication
        String fileHash = calculateMD5(file);
        log.info("📝 Calculated file hash: {}", fileHash);

        // Check if this exact file already exists (based on hash)
        Document existingFile = documentRepository.findByFileHash(fileHash);

        Path filePath;
        String fileName;

        if (existingFile != null) {
            // File already exists, reuse the existing file path
            log.info("♻️ File already exists in storage (hash: {}), reusing existing file", fileHash);
            log.info("   Existing file path: {}", existingFile.getFilePath());

            filePath = Paths.get(existingFile.getFilePath());
            fileName = existingFile.getFileName();
        } else {
            // New unique file, save it with MD5 hash + document type in filename
            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            // Format: {MD5_HASH}_{DOCUMENT_TYPE}{EXTENSION}
            // Example: 5d41402abc4b2a76b9719d911017c592_PASSPORT.jpg
            fileName = fileHash + "_" + documentType + extension;
            filePath = Paths.get(uploadDir + fileName);

            // Check if file with this exact name already exists (shouldn't happen with MD5, but safe check)
            if (Files.exists(filePath)) {
                log.warn("⚠️ File with this hash and type already exists: {}", fileName);
                log.info("   Reusing existing file instead of overwriting");
            } else {
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                log.info("💾 New file saved to disk: {}", fileName);
                log.info("   Format: {MD5_HASH}_{DOCUMENT_TYPE}{EXTENSION}");
            }
        }

        // Create document entity
        Document document = new Document();
        document.setEmployee(employee);
        document.setDocumentType(documentType);
        document.setFileName(file.getOriginalFilename());
        document.setFilePath(filePath.toString());
        document.setFileType(file.getContentType());
        document.setFileHash(fileHash); // Store MD5 hash for deduplication
        // Note: extractedText is used only temporarily for parsing structured fields, not stored
        document.setUploadedDate(LocalDateTime.now());

        // Set extracted information
        if (extractedInfo.containsKey("documentNumber")) {
            document.setDocumentNumber((String) extractedInfo.get("documentNumber"));
            log.info("✓ Set document number: {}", extractedInfo.get("documentNumber"));
        }
        if (extractedInfo.containsKey("issueDate")) {
            LocalDate issueDate = (LocalDate) extractedInfo.get("issueDate");
            document.setIssueDate(issueDate);
            log.info("✓ Set issue date: {}", issueDate);
        } else {
            log.warn("⚠ No issue date in extractedInfo to set");
        }
        if (extractedInfo.containsKey("expiryDate")) {
            LocalDate expiryDate = (LocalDate) extractedInfo.get("expiryDate");
            document.setExpiryDate(expiryDate);
            log.info("✓ Set expiry date: {}", expiryDate);
        } else {
            log.warn("⚠ No expiry date in extractedInfo to set");
        }
        if (extractedInfo.containsKey("issuingCountry")) {
            document.setIssuingCountry((String) extractedInfo.get("issuingCountry"));
            log.info("✓ Set issuing country: {}", extractedInfo.get("issuingCountry"));
        }
        if (extractedInfo.containsKey("fullName")) {
            document.setFullName((String) extractedInfo.get("fullName"));
            log.info("✓ Set full name: {}", extractedInfo.get("fullName"));
        }
        if (extractedInfo.containsKey("dateOfBirth")) {
            LocalDate dob = (LocalDate) extractedInfo.get("dateOfBirth");
            document.setDateOfBirth(dob);
            log.info("✓ Set date of birth: {}", dob);
        }
        if (extractedInfo.containsKey("nationality")) {
            document.setNationality((String) extractedInfo.get("nationality"));
            log.info("✓ Set nationality: {}", extractedInfo.get("nationality"));
        }

        // UK VISA specific fields
        if (extractedInfo.containsKey("companyName")) {
            document.setCompanyName((String) extractedInfo.get("companyName"));
            log.info("✓ Set company name: {}", extractedInfo.get("companyName"));
        }

        // For UK VISA: Date of Check → Issue Date (if not already set)
        if (extractedInfo.containsKey("dateOfCheck")) {
            LocalDate checkDate = (LocalDate) extractedInfo.get("dateOfCheck");
            document.setDateOfCheck(checkDate);
            // Also set as issue date if not already extracted
            if (document.getIssueDate() == null) {
                document.setIssueDate(checkDate);
                log.info("✓ Set issue date from date of check: {}", checkDate);
            }
            log.info("✓ Set date of check: {}", checkDate);
        }

        // For UK VISA: Reference Number → Document Number (if not already set)
        if (extractedInfo.containsKey("referenceNumber")) {
            String referenceNumber = (String) extractedInfo.get("referenceNumber");
            document.setReferenceNumber(referenceNumber);
            // Also set as document number if not already extracted
            if (document.getDocumentNumber() == null || document.getDocumentNumber().isEmpty()) {
                document.setDocumentNumber(referenceNumber);
                log.info("✓ Set document number from reference: {}", referenceNumber);
            }
            log.info("✓ Set reference number: {}", referenceNumber);
        }

        // CONTRACT specific fields
        if (extractedInfo.containsKey("contractDate")) {
            LocalDate contractDate = (LocalDate) extractedInfo.get("contractDate");
            document.setContractDate(contractDate);
            // Also set as issue date if not already set
            if (document.getIssueDate() == null) {
                document.setIssueDate(contractDate);
                log.info("✓ Set issue date from contract date: {}", contractDate);
            }
            log.info("✓ Set contract date: {}", contractDate);
        }
        if (extractedInfo.containsKey("placeOfWork")) {
            document.setPlaceOfWork((String) extractedInfo.get("placeOfWork"));
            log.info("✓ Set place of work: {}", extractedInfo.get("placeOfWork"));
        }
        if (extractedInfo.containsKey("contractBetween")) {
            document.setContractBetween((String) extractedInfo.get("contractBetween"));
            log.info("✓ Set contract between: {}", extractedInfo.get("contractBetween"));
        }
        if (extractedInfo.containsKey("jobTitleContract")) {
            document.setJobTitleContract((String) extractedInfo.get("jobTitleContract"));
            log.info("✓ Set job title (from contract): {}", extractedInfo.get("jobTitleContract"));
        }

        Document savedDocument = documentRepository.save(document);
        return convertToDTO(savedDocument);
    }

    public List<DocumentDTO> getAllDocuments() {
        User currentUser = securityUtils.getCurrentUser();

        // ROOT cannot access employee documents
        if (securityUtils.isRoot()) {
            log.warn("⚠️ ROOT user attempted to access employee documents - Access denied");
            throw new AccessDeniedException("ROOT user cannot access employee documents. ROOT can only manage organizations.");
        }

        // SUPER_ADMIN can see all documents in their organization
        if (securityUtils.isSuperAdmin()) {
            log.debug("SUPER_ADMIN retrieving all documents for organization: {}", currentUser.getOrganizationId());
            return documentRepository.findAll().stream()
                    .filter(doc -> currentUser.getOrganizationId().equals(doc.getEmployee().getOrganizationId()))
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }

        // ADMIN can only see documents of USER role employees in their department and organization
        if (securityUtils.isAdmin()) {
            Employee adminEmployee = employeeRepository.findByUserId(currentUser.getId())
                    .orElse(null);

            if (adminEmployee == null || adminEmployee.getDepartment() == null) {
                log.warn("Admin user {} has no employee profile or department", currentUser.getUsername());
                return List.of();
            }

            log.debug("ADMIN retrieving documents for department: {} in organization: {}",
                     adminEmployee.getDepartment().getName(), adminEmployee.getOrganizationId());

            // Get all documents and filter by access control and organization
            return documentRepository.findAll().stream()
                    .filter(doc -> canAccessEmployee(doc.getEmployee()))
                    .filter(doc -> adminEmployee.getOrganizationId().equals(doc.getEmployee().getOrganizationId()))
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }

        // Regular users can only see their own documents
        if (currentUser != null) {
            return employeeRepository.findByUserId(currentUser.getId())
                    .map(Employee::getId)
                    .map(documentRepository::findByEmployeeId)
                    .orElseGet(List::of)
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    public List<DocumentDTO> getDocumentsByEmployeeId(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

        if (!canAccessEmployee(employee)) {
            throw new RuntimeException("Access denied. You can only view your own documents.");
        }

        return documentRepository.findByEmployeeId(employeeId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public DocumentDTO getDocumentById(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));

        if (!canAccessEmployee(document.getEmployee())) {
            throw new RuntimeException("Access denied. You can only view your own documents.");
        }

        // Track document view for ADMIN and SUPER_ADMIN
        if (securityUtils.isAdminOrSuperAdmin()) {
            User currentUser = securityUtils.getCurrentUser();
            if (currentUser != null) {
                document.setLastViewedAt(LocalDateTime.now());
                document.setLastViewedBy(currentUser.getUsername());
                documentRepository.save(document);
                log.info("📊 Document {} viewed by {} at {}", id, currentUser.getUsername(), LocalDateTime.now());
            }
        }

        return convertToDTO(document);
    }

    public void deleteDocument(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));

        if (!canAccessEmployee(document.getEmployee())) {
            throw new RuntimeException("Access denied. You can only delete your own documents.");
        }

        String filePath = document.getFilePath();

        // Delete document from database first
        documentRepository.deleteById(id);
        log.info("🗑️ Document deleted from database: ID {}", id);

        // Check if any other documents still reference this file
        long referenceCount = documentRepository.countByFilePath(filePath);

        if (referenceCount == 0) {
            // No other documents reference this file, safe to delete
            try {
                Files.deleteIfExists(Paths.get(filePath));
                log.info("🗑️ Physical file deleted: {} (no other references)", filePath);
            } catch (IOException e) {
                log.error("❌ Could not delete file: {}", e.getMessage());
            }
        } else {
            log.info("♻️ Physical file kept: {} ({} other document(s) still reference it)", filePath, referenceCount);
        }
    }

    public List<DocumentDTO> getExpiringDocuments(int daysAhead) {
        LocalDate currentDate = LocalDate.now();
        LocalDate expiryDate = currentDate.plusDays(daysAhead);

        return documentRepository.findDocumentsExpiringBefore(expiryDate, currentDate).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public DocumentDTO updateDocument(Long id, Map<String, Object> updateData) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));

        Employee employee = employeeRepository.findById(document.getEmployee().getId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Check access permissions
        if (!canAccessEmployee(employee)) {
            log.warn("Unauthorized document update attempt for document ID: {}", id);
            throw new RuntimeException("Access denied. You can only update your own documents.");
        }

        // Update fields if provided
        if (updateData.containsKey("documentNumber")) {
            document.setDocumentNumber((String) updateData.get("documentNumber"));
            log.info("Updated document number for ID {}: {}", id, updateData.get("documentNumber"));
        }

        if (updateData.containsKey("issuingCountry")) {
            document.setIssuingCountry((String) updateData.get("issuingCountry"));
            log.info("Updated issuing country for ID {}: {}", id, updateData.get("issuingCountry"));
        }

        if (updateData.containsKey("issueDate")) {
            String issueDateStr = (String) updateData.get("issueDate");
            if (issueDateStr != null && !issueDateStr.isEmpty()) {
                document.setIssueDate(LocalDate.parse(issueDateStr));
                log.info("Updated issue date for ID {}: {}", id, issueDateStr);
            }
        }

        if (updateData.containsKey("expiryDate")) {
            String expiryDateStr = (String) updateData.get("expiryDate");
            if (expiryDateStr != null && !expiryDateStr.isEmpty()) {
                document.setExpiryDate(LocalDate.parse(expiryDateStr));
                log.info("Updated expiry date for ID {}: {}", id, expiryDateStr);
            }
        }

        if (updateData.containsKey("nationality")) {
            document.setNationality((String) updateData.get("nationality"));
        }

        if (updateData.containsKey("fullName")) {
            document.setFullName((String) updateData.get("fullName"));
        }

        if (updateData.containsKey("dateOfBirth")) {
            String dobStr = (String) updateData.get("dateOfBirth");
            if (dobStr != null && !dobStr.isEmpty()) {
                document.setDateOfBirth(LocalDate.parse(dobStr));
            }
        }

        // UK VISA specific fields
        if (updateData.containsKey("companyName")) {
            document.setCompanyName((String) updateData.get("companyName"));
            log.info("Updated company name for ID {}: {}", id, updateData.get("companyName"));
        }

        if (updateData.containsKey("dateOfCheck")) {
            String checkDateStr = (String) updateData.get("dateOfCheck");
            if (checkDateStr != null && !checkDateStr.isEmpty()) {
                document.setDateOfCheck(LocalDate.parse(checkDateStr));
                log.info("Updated date of check for ID {}: {}", id, checkDateStr);
            }
        }

        if (updateData.containsKey("referenceNumber")) {
            document.setReferenceNumber((String) updateData.get("referenceNumber"));
            log.info("Updated reference number for ID {}: {}", id, updateData.get("referenceNumber"));
        }

        Document updatedDocument = documentRepository.save(document);
        log.info("✓ Document updated successfully - ID: {}", id);

        return convertToDTO(updatedDocument);
    }

    public byte[] getDocumentImage(Long id) {
        log.info("🔍 Retrieving document image for ID: {}", id);

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));

        Employee employee = employeeRepository.findById(document.getEmployee().getId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Check access permissions
        if (!canAccessEmployee(employee)) {
            log.warn("❌ Unauthorized document access attempt for document ID: {}", id);
            throw new RuntimeException("Access denied. You can only access your own documents.");
        }

        // Get file path and normalize it
        String storedPath = document.getFilePath();
        log.info("📂 Stored file path: {}", storedPath);

        // Try to resolve the path (handles both absolute and relative paths)
        Path filePath = Paths.get(storedPath);
        if (!filePath.isAbsolute()) {
            // If relative, resolve from current directory
            filePath = Paths.get(System.getProperty("user.dir"), storedPath);
        }

        log.info("📍 Resolved file path: {}", filePath.toAbsolutePath());

        // Check if file exists
        if (!Files.exists(filePath)) {
            log.error("❌ File not found on disk: {}", filePath.toAbsolutePath());
            log.error("   Current directory: {}", System.getProperty("user.dir"));
            log.error("   File name: {}", document.getFileName());

            // Try alternative path without absolute resolution
            Path alternativePath = Paths.get(storedPath);
            if (Files.exists(alternativePath)) {
                log.info("✓ Found file at alternative path: {}", alternativePath.toAbsolutePath());
                filePath = alternativePath;
            } else {
                throw new RuntimeException("Document file not found on server. Path: " + filePath.toAbsolutePath());
            }
        }

        // Read file
        try {
            byte[] fileData = Files.readAllBytes(filePath);
            log.info("✓ Retrieved document from file system - ID: {}, Type: {}, Size: {} KB, Path: {}",
                    id, document.getDocumentType(), fileData.length / 1024, filePath.getFileName());
            return fileData;
        } catch (IOException e) {
            log.error("❌ Error reading file from disk: {}", e.getMessage(), e);
            throw new RuntimeException("Error reading document file: " + e.getMessage());
        }
    }

    /**
     * @deprecated Preview images are no longer stored in database.
     * Use getDocumentImage() instead which loads from file system.
     * This method redirects to getDocumentImage() for backward compatibility.
     */
    @Deprecated
    public byte[] getDocumentPreview(Long id) {
        log.warn("⚠️ getDocumentPreview() is deprecated. Use getDocumentImage() instead.");
        // Redirect to full image from file system
        return getDocumentImage(id);
    }

    private boolean canAccessEmployee(Employee employee) {
        User currentUser = securityUtils.getCurrentUser();

        if (currentUser == null) {
            return false;
        }

        // ROOT cannot access employee documents
        if (securityUtils.isRoot()) {
            log.warn("⚠️ ROOT user attempted to access employee {} documents - Access denied", employee.getId());
            return false;
        }

        // Check organization boundary - users can only access employees in their organization
        if (!currentUser.getOrganizationId().equals(employee.getOrganizationId())) {
            log.debug("Access denied: Employee {} not in user's organization", employee.getId());
            return false;
        }

        // SUPER_ADMIN can access all employees' documents in their organization
        if (securityUtils.isSuperAdmin()) {
            log.debug("SUPER_ADMIN access granted for employee ID: {} in same organization", employee.getId());
            return true;
        }

        // ADMIN can access USER role employees in their department AND their own documents
        if (securityUtils.isAdmin()) {
            // First check if it's the admin's own document
            if (employee.getUserId() != null && employee.getUserId().equals(currentUser.getId())) {
                log.debug("ADMIN access granted for own documents: employee ID {}", employee.getId());
                return true;
            }

            // Get the admin's employee profile
            Employee adminEmployee = employeeRepository.findByUserId(currentUser.getId())
                    .orElse(null);

            if (adminEmployee == null || adminEmployee.getDepartment() == null) {
                log.warn("Admin user {} has no employee profile or department", currentUser.getUsername());
                return false;
            }

            // Check if target employee is in the same department
            if (employee.getDepartment() == null ||
                !employee.getDepartment().getId().equals(adminEmployee.getDepartment().getId())) {
                log.debug("ADMIN access denied: Employee {} not in admin's department", employee.getId());
                return false;
            }

            // Check if target employee has USER role (not another ADMIN)
            if (employee.getUserId() != null) {
                Employee targetEmployee = employeeRepository.findById(employee.getId()).orElse(null);
                if (targetEmployee != null && targetEmployee.getUserId() != null) {
                    // Get the user associated with the employee to check their role
                    // ADMINs should not be able to view other ADMINs' documents (except their own, which we already checked)
                    User targetUser = securityUtils.getUserById(employee.getUserId());
                    if (targetUser != null && targetUser.getRoles().contains("ADMIN")) {
                        log.debug("ADMIN access denied: Target employee {} is another ADMIN", employee.getId());
                        return false;
                    }
                }
            }

            log.debug("ADMIN access granted for USER employee ID: {} in same department", employee.getId());
            return true;
        }

        // Regular users can only access their own documents
        boolean isOwnDocument = employee.getUserId() != null && employee.getUserId().equals(currentUser.getId());
        log.debug("USER access {} for employee ID: {}", isOwnDocument ? "granted" : "denied", employee.getId());
        return isOwnDocument;
    }


    private DocumentDTO convertToDTO(Document document) {
        DocumentDTO dto = new DocumentDTO();
        dto.setId(document.getId());
        dto.setEmployeeId(document.getEmployee().getId());
        dto.setEmployeeName(document.getEmployee().getFullName());
        dto.setDocumentType(document.getDocumentType());
        dto.setDocumentNumber(document.getDocumentNumber());
        dto.setFileName(document.getFileName());
        dto.setFileType(document.getFileType());
        dto.setIssueDate(document.getIssueDate());
        dto.setExpiryDate(document.getExpiryDate());
        dto.setIssuingCountry(document.getIssuingCountry());
        dto.setFullName(document.getFullName());
        dto.setDateOfBirth(document.getDateOfBirth());
        dto.setNationality(document.getNationality());

        // UK VISA specific fields
        dto.setCompanyName(document.getCompanyName());
        dto.setDateOfCheck(document.getDateOfCheck());
        dto.setReferenceNumber(document.getReferenceNumber());

        // CONTRACT specific fields
        dto.setContractDate(document.getContractDate());
        dto.setPlaceOfWork(document.getPlaceOfWork());
        dto.setContractBetween(document.getContractBetween());
        dto.setJobTitleContract(document.getJobTitleContract());

        dto.setUploadedDate(document.getUploadedDate());

        // Document view tracking
        dto.setLastViewedAt(document.getLastViewedAt());
        dto.setLastViewedBy(document.getLastViewedBy());

        if (document.getExpiryDate() != null) {
            long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), document.getExpiryDate());
            dto.setDaysUntilExpiry((int) daysUntilExpiry);
        }

        return dto;
    }

    /**
     * Calculate MD5 hash of file content for deduplication
     * Same file will always produce the same hash
     */
    private String calculateMD5(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] fileBytes = file.getBytes();
            byte[] hashBytes = md.digest(fileBytes);

            // Convert byte array to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("❌ Failed to calculate MD5 hash: {}", e.getMessage());
            // Return a random UUID as fallback to prevent errors
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}

