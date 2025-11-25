package com.was.employeemanagementsystem.service;

import com.was.employeemanagementsystem.dto.EmployeeDTO;
import com.was.employeemanagementsystem.dto.DocumentDTO;
import com.was.employeemanagementsystem.dto.LeaveDTO;
import com.was.employeemanagementsystem.entity.*;
import com.was.employeemanagementsystem.repository.*;
import com.was.employeemanagementsystem.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SearchService {

    private final EmployeeRepository employeeRepository;
    private final DocumentRepository documentRepository;
    private final LeaveRepository leaveRepository;
    private final RotaRepository rotaRepository;
    private final SecurityUtils securityUtils;
    private final EmployeeService employeeService;
    private final DocumentService documentService;
    private final LeaveService leaveService;

    public SearchService(
            EmployeeRepository employeeRepository,
            DocumentRepository documentRepository,
            LeaveRepository leaveRepository,
            RotaRepository rotaRepository,
            SecurityUtils securityUtils,
            EmployeeService employeeService,
            DocumentService documentService,
            LeaveService leaveService) {
        this.employeeRepository = employeeRepository;
        this.documentRepository = documentRepository;
        this.leaveRepository = leaveRepository;
        this.rotaRepository = rotaRepository;
        this.securityUtils = securityUtils;
        this.employeeService = employeeService;
        this.documentService = documentService;
        this.leaveService = leaveService;
    }

    public SearchResults search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new SearchResults();
        }

        String searchTerm = query.trim().toLowerCase();
        log.info("🔍 Performing global search for: '{}'", searchTerm);

        SearchResults results = new SearchResults();

        // Only ADMIN and SUPER_ADMIN can use global search
        if (!securityUtils.isAdminOrSuperAdmin()) {
            log.warn("⚠️ Non-admin user attempted to use global search");
            return results;
        }

        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            return results;
        }

        // Search Employees
        results.setEmployees(searchEmployees(searchTerm, currentUser));

        // Search Documents
        results.setDocuments(searchDocuments(searchTerm, currentUser));

        // Search Leaves
        results.setLeaves(searchLeaves(searchTerm, currentUser));

        // Search Rota
        results.setRotas(searchRotas(searchTerm, currentUser));

        log.info("✅ Search completed - Employees: {}, Documents: {}, Leaves: {}, Rotas: {}",
                results.getEmployees().size(),
                results.getDocuments().size(),
                results.getLeaves().size(),
                results.getRotas().size());

        return results;
    }

    private List<EmployeeDTO> searchEmployees(String searchTerm, User currentUser) {
        try {
            List<Employee> allEmployees = employeeRepository.findAll();
            String userOrgUuid = currentUser.getOrganizationUuid();

            List<Employee> filteredEmployees = allEmployees.stream()
                    .filter(emp -> {
                        // Filter by organization
                        if (userOrgUuid != null && !userOrgUuid.equals(emp.getOrganizationUuid())) {
                            return false;
                        }

                        // Exclude SUPER_ADMIN employees for non-SUPER_ADMIN users
                        if (!securityUtils.isSuperAdmin() && emp.getUserId() != null) {
                            User empUser = securityUtils.getUserById(emp.getUserId());
                            if (empUser != null && empUser.getRoles().contains("SUPER_ADMIN")) {
                                return false;
                            }
                        }

                        // Filter by department for ADMIN
                        if (securityUtils.isAdmin() && !securityUtils.isSuperAdmin()) {
                            Employee adminEmployee = employeeRepository.findByUserId(currentUser.getId())
                                    .orElse(null);
                            if (adminEmployee != null && adminEmployee.getDepartment() != null) {
                                if (emp.getDepartment() == null ||
                                    !emp.getDepartment().getId().equals(adminEmployee.getDepartment().getId())) {
                                    return false;
                                }
                            }
                        }

                        // Search in name, email, job title, phone, allotted organization
                        String fullName = (emp.getFullName() != null ? emp.getFullName() : "").toLowerCase();
                        String workEmail = (emp.getWorkEmail() != null ? emp.getWorkEmail() : "").toLowerCase();
                        String jobTitle = (emp.getJobTitle() != null ? emp.getJobTitle() : "").toLowerCase();
                        String phone = (emp.getPhoneNumber() != null ? emp.getPhoneNumber() : "").toLowerCase();
                        String allottedOrg = (emp.getAllottedOrganization() != null ? emp.getAllottedOrganization() : "").toLowerCase();

                        return fullName.contains(searchTerm) ||
                               workEmail.contains(searchTerm) ||
                               jobTitle.contains(searchTerm) ||
                               phone.contains(searchTerm) ||
                               allottedOrg.contains(searchTerm);
                    })
                    .collect(Collectors.toList());

            return filteredEmployees.stream()
                    .map(employeeService::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching employees: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<DocumentDTO> searchDocuments(String searchTerm, User currentUser) {
        try {
            List<Document> allDocuments = documentRepository.findAll();

            return allDocuments.stream()
                    .filter(doc -> {
                        Employee emp = doc.getEmployee();
                        
                        // Check organization boundary
                        if (!currentUser.getOrganizationId().equals(emp.getOrganizationId())) {
                            return false;
                        }

                        // SUPER_ADMIN can access all documents in their organization
                        if (securityUtils.isSuperAdmin()) {
                            // Continue to search filter
                        }
                        // ADMIN can access USER role employees in their department AND their own documents
                        else if (securityUtils.isAdmin()) {
                            // Check if it's the admin's own document
                            if (emp.getUserId() == null || !emp.getUserId().equals(currentUser.getId())) {
                                // Get the admin's employee profile
                                Employee adminEmployee = employeeRepository.findByUserId(currentUser.getId())
                                        .orElse(null);
                                
                                if (adminEmployee == null || adminEmployee.getDepartment() == null) {
                                    return false;
                                }
                                
                                // Check if target employee is in the same department
                                if (emp.getDepartment() == null ||
                                    !emp.getDepartment().getId().equals(adminEmployee.getDepartment().getId())) {
                                    return false;
                                }
                                
                                // Check if target employee has USER role (not another ADMIN)
                                if (emp.getUserId() != null) {
                                    User empUser = securityUtils.getUserById(emp.getUserId());
                                    if (empUser != null && (empUser.getRoles().contains("ADMIN") || empUser.getRoles().contains("SUPER_ADMIN"))) {
                                        return false;
                                    }
                                }
                            }
                        }

                        // Search in document type, file name, employee name
                        String docType = (doc.getDocumentType() != null ? doc.getDocumentType() : "").toLowerCase();
                        String fileName = (doc.getFileName() != null ? doc.getFileName() : "").toLowerCase();
                        String empName = (emp.getFullName() != null ? emp.getFullName() : "").toLowerCase();

                        return docType.contains(searchTerm) ||
                               fileName.contains(searchTerm) ||
                               empName.contains(searchTerm);
                    })
                    .map(documentService::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching documents: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<LeaveDTO> searchLeaves(String searchTerm, User currentUser) {
        try {
            List<Leave> allLeaves = leaveRepository.findAll();
            String userOrgUuid = currentUser.getOrganizationUuid();

            return allLeaves.stream()
                    .filter(leave -> {
                        Employee emp = leave.getEmployee();
                        
                        // Filter by organization
                        if (userOrgUuid != null && emp.getOrganizationUuid() != null &&
                            !userOrgUuid.equals(emp.getOrganizationUuid())) {
                            return false;
                        }

                        // For ADMIN (not SUPER_ADMIN), filter by department
                        if (securityUtils.isAdmin() && !securityUtils.isSuperAdmin()) {
                            Employee adminEmployee = employeeRepository.findByUserId(currentUser.getId())
                                    .orElse(null);
                            if (adminEmployee != null && adminEmployee.getDepartment() != null) {
                                if (emp.getDepartment() == null ||
                                    !emp.getDepartment().getId().equals(adminEmployee.getDepartment().getId())) {
                                    return false;
                                }
                            }
                        }

                        // Search in employee name, leave type, reason, status
                        String empName = (emp.getFullName() != null ? emp.getFullName() : "").toLowerCase();
                        String leaveType = (leave.getLeaveType() != null ? leave.getLeaveType() : "").toLowerCase();
                        String reason = (leave.getReason() != null ? leave.getReason() : "").toLowerCase();
                        String status = (leave.getStatus() != null ? leave.getStatus() : "").toLowerCase();

                        return empName.contains(searchTerm) ||
                               leaveType.contains(searchTerm) ||
                               reason.contains(searchTerm) ||
                               status.contains(searchTerm);
                    })
                    .map(leaveService::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching leaves: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<RotaSearchResult> searchRotas(String searchTerm, User currentUser) {
        try {
            List<Rota> allRotas = rotaRepository.findAll();
            Long userOrgId = currentUser.getOrganizationId();

            return allRotas.stream()
                    .filter(rota -> {
                        // Filter by organization if applicable
                        if (userOrgId != null && rota.getOrganizationId() != null &&
                            !userOrgId.equals(rota.getOrganizationId())) {
                            return false;
                        }

                        // Search in department, file name, period
                        String department = (rota.getDepartment() != null ? rota.getDepartment() : "").toLowerCase();
                        String fileName = (rota.getFileName() != null ? rota.getFileName() : "").toLowerCase();
                        String period = (rota.getStartDate() != null && rota.getEndDate() != null ?
                                rota.getStartDate().toString() + " " + rota.getEndDate().toString() : "").toLowerCase();

                        return department.contains(searchTerm) ||
                               fileName.contains(searchTerm) ||
                               period.contains(searchTerm);
                    })
                    .map(rota -> {
                        RotaSearchResult result = new RotaSearchResult();
                        result.setId(rota.getId());
                        result.setDepartment(rota.getDepartment());
                        result.setFileName(rota.getFileName());
                        result.setStartDate(rota.getStartDate());
                        result.setEndDate(rota.getEndDate());
                        result.setUploadedDate(rota.getUploadedDate());
                        return result;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching rotas: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // Inner classes for search results
    public static class SearchResults {
        private List<EmployeeDTO> employees = new ArrayList<>();
        private List<DocumentDTO> documents = new ArrayList<>();
        private List<LeaveDTO> leaves = new ArrayList<>();
        private List<RotaSearchResult> rotas = new ArrayList<>();

        public List<EmployeeDTO> getEmployees() {
            return employees;
        }

        public void setEmployees(List<EmployeeDTO> employees) {
            this.employees = employees;
        }

        public List<DocumentDTO> getDocuments() {
            return documents;
        }

        public void setDocuments(List<DocumentDTO> documents) {
            this.documents = documents;
        }

        public List<LeaveDTO> getLeaves() {
            return leaves;
        }

        public void setLeaves(List<LeaveDTO> leaves) {
            this.leaves = leaves;
        }

        public List<RotaSearchResult> getRotas() {
            return rotas;
        }

        public void setRotas(List<RotaSearchResult> rotas) {
            this.rotas = rotas;
        }
    }

    public static class RotaSearchResult {
        private Long id;
        private String department;
        private String fileName;
        private java.time.LocalDate startDate;
        private java.time.LocalDate endDate;
        private java.time.LocalDateTime uploadedDate;

        // Getters and setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public java.time.LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(java.time.LocalDate startDate) {
            this.startDate = startDate;
        }

        public java.time.LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(java.time.LocalDate endDate) {
            this.endDate = endDate;
        }

        public java.time.LocalDateTime getUploadedDate() {
            return uploadedDate;
        }

        public void setUploadedDate(java.time.LocalDateTime uploadedDate) {
            this.uploadedDate = uploadedDate;
        }
    }
}

