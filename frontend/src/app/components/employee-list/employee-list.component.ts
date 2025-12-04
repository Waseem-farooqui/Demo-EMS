import {Component, OnInit, OnDestroy} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {Router, RouterModule, ActivatedRoute} from '@angular/router';
import {DomSanitizer, SafeResourceUrl} from '@angular/platform-browser';
import {EmployeeService} from '../../services/employee.service';
import {AttendanceService} from '../../services/attendance.service';
import {RotaService} from '../../services/rota.service';
import {DocumentService} from '../../services/document.service';
import {AuthService} from '../../services/auth.service';
import {ToastService} from '../../services/toast.service';
import {LeaveService} from '../../services/leave.service';
import {Employee} from '../../models/employee.model';
import {EmployeeWorkSummary} from '../../models/attendance.model';
import {Document} from '../../models/document.model';
import {LeaveBalance} from '../../models/leave.model';
import {Subscription} from 'rxjs';

interface User {
  id?: number;
  email?: string;
  username?: string;
  roles?: string[];
  organizationId?: number;
}

@Component({
  selector: 'app-employee-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './employee-list.component.html',
  styleUrls: ['./employee-list.component.css']
})
export class EmployeeListComponent implements OnInit, OnDestroy {
  employees: Employee[] = [];
  filteredEmployees: Employee[] = [];
  allEmployees: Employee[] = []; // Store all employees for filtering
  loading = false;
  error: string | null = null;
  currentUser: User | null = null;

  // Search and Filter
  globalSearchQuery = ''; // Global search filter
  searchQuery = '';
  selectedDepartment: string = '';
  selectedJobTitle: string = '';
  selectedAllottedOrg: string = '';
  departments: string[] = [];
  jobTitles: string[] = [];
  allottedOrganizations: string[] = [];

  // Column-specific filters
  columnFilters = {
    fullName: '',
    username: '',
    position: '',
    department: '',
    allottedOrg: ''
  };

  // Sorting
  sortColumn: string = '';
  sortDirection: 'asc' | 'desc' = 'asc';

  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  usePagination = true; // Toggle between paginated and non-paginated

  // Employee Details Modal
  showDetailsModal = false;
  loadingDetails = false;
  selectedEmployee: Employee | null = null;
  employeeWorkSummary: EmployeeWorkSummary | null = null;
  employeeRotaSchedule: Array<{date: string; dayOfWeek?: string; scheduleDate?: string; duty?: string; startTime?: string; endTime?: string}> = [];
  employeeDocuments: Document[] = [];
  employeeLeaveBalances: LeaveBalance[] = [];

  // Document Viewer
  selectedDocumentId: number | null = null;
  selectedDocumentUrl: SafeResourceUrl | null = null;
  selectedDocumentRawUrl: string | null = null; // For image tags
  private rawDocumentUrl: string | null = null; // For cleanup
  selectedDocumentType: string | null = null;
  documentLoading = false;

  // Smart viewer detection
  isSelectedDocPdf = false;
  isSelectedDocImage = false;

  // Subscriptions
  private subscriptions: Subscription[] = [];

  constructor(
    private employeeService: EmployeeService,
    private attendanceService: AttendanceService,
    private rotaService: RotaService,
    private documentService: DocumentService,
    private leaveService: LeaveService,
    private authService: AuthService,
    private toastService: ToastService,
    private sanitizer: DomSanitizer,
    private router: Router,
    private route: ActivatedRoute
  ) { }

  ngOnInit(): void {
    this.currentUser = this.authService.getUser();
    
    // Check for employeeId query parameter to open detail modal
    const queryParamsSub = this.route.queryParams.subscribe(params => {
      if (params['employeeId']) {
        const employeeId = +params['employeeId'];
        if (employeeId && !isNaN(employeeId)) {
          // Open detail modal directly (doesn't need employees list to be loaded)
          this.viewEmployeeDetails(employeeId);
          // Clear the query parameter from URL
          this.router.navigate([], {
            relativeTo: this.route,
            queryParams: {},
            replaceUrl: true
          });
        }
      }
    });
    this.subscriptions.push(queryParamsSub);
    
    this.loadEmployees();
  }

  ngOnDestroy(): void {
    // Unsubscribe from all subscriptions
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.subscriptions = [];
    
    // Clean up document viewer URLs
    this.closeDocumentViewer();
  }

  loadEmployees(): void {
    this.loading = true;
    this.error = null;
    
    if (this.usePagination) {
      this.loadEmployeesPaginated();
    } else {
      this.employeeService.getAllEmployees().subscribe({
        next: (data) => {
          // Filter out the current logged-in user from the list
          const currentUserEmail = this.currentUser?.email;
          this.allEmployees = data.filter(emp => emp.workEmail !== currentUserEmail);
          this.employees = [...this.allEmployees];
          this.filteredEmployees = [...this.allEmployees];
          
          // Extract unique departments and job titles for filters
          this.extractFilterOptions();
          
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Failed to load employees. Please try again.';
          this.loading = false;
          console.error('Error loading employees:', err);
        }
      });
    }
  }

  loadEmployeesPaginated(): void {
    this.employeeService.getAllEmployeesPaginated(this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        // Filter out the current logged-in user from the list
        const currentUserEmail = this.currentUser?.email;
        const filtered = response.content.filter(emp => emp.workEmail !== currentUserEmail);
        
        this.employees = filtered;
        this.filteredEmployees = filtered;
        // Set allEmployees for template visibility check (use current page data)
        // In a real scenario, you might want to load all employees separately for filters
        this.allEmployees = filtered;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        
        // Extract unique departments and job titles for filters (from current page)
        // Note: For better filter options, consider loading all employees separately
        this.extractFilterOptions();
        
        // Apply search and filters to current page
        this.applyFilters();
        
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load employees. Please try again.';
        this.loading = false;
        console.error('Error loading employees:', err);
      }
    });
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadEmployeesPaginated();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadEmployeesPaginated();
  }

  extractFilterOptions(): void {
    const departmentsSet = new Set<string>();
    const jobTitlesSet = new Set<string>();
    const allottedOrgsSet = new Set<string>();

    // Use employees array (current page) for filter options when using pagination
    const sourceArray = this.usePagination ? this.employees : this.allEmployees;
    
    sourceArray.forEach(emp => {
      if (emp.departmentName) {
        departmentsSet.add(emp.departmentName);
      }
      if (emp.jobTitle) {
        jobTitlesSet.add(emp.jobTitle);
      }
      if (emp.allottedOrganization) {
        allottedOrgsSet.add(emp.allottedOrganization);
      }
    });

    this.departments = Array.from(departmentsSet).sort();
    this.jobTitles = Array.from(jobTitlesSet).sort();
    this.allottedOrganizations = Array.from(allottedOrgsSet).sort();
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  onFilterChange(): void {
    this.applyFilters();
  }

  clearFilters(): void {
    this.globalSearchQuery = '';
    this.searchQuery = '';
    this.selectedDepartment = '';
    this.selectedJobTitle = '';
    this.selectedAllottedOrg = '';
    this.columnFilters = {
      fullName: '',
      username: '',
      position: '',
      department: '',
      allottedOrg: ''
    };
    this.sortColumn = '';
    this.sortDirection = 'asc';
    this.applyFilters();
  }

  onColumnFilterChange(): void {
    this.applyFilters();
  }

  applyFilters(): void {
    if (this.usePagination) {
      // For pagination, filters are applied on the current page
      // Re-fetch with current filters (search/filter would ideally be server-side)
      let filtered = [...this.employees];

      // Apply global search filter
      if (this.globalSearchQuery.trim()) {
        const query = this.globalSearchQuery.toLowerCase().trim();
        filtered = filtered.filter(emp => {
          const fullName = (emp.fullName || '').toLowerCase();
          const workEmail = (emp.workEmail || '').toLowerCase();
          const jobTitle = (emp.jobTitle || '').toLowerCase();
          const username = (emp.username || '').toLowerCase();
          const phone = (emp.phoneNumber || '').toLowerCase();
          const department = (emp.departmentName || '').toLowerCase();
          const allottedOrg = (emp.allottedOrganization || '').toLowerCase();
          
          return fullName.includes(query) ||
                 workEmail.includes(query) ||
                 jobTitle.includes(query) ||
                 username.includes(query) ||
                 phone.includes(query) ||
                 department.includes(query) ||
                 allottedOrg.includes(query);
        });
      }

      // Apply search filter (legacy - keeping for backward compatibility)
      if (this.searchQuery.trim()) {
        const query = this.searchQuery.toLowerCase().trim();
        filtered = filtered.filter(emp => {
          const fullName = (emp.fullName || '').toLowerCase();
          const workEmail = (emp.workEmail || '').toLowerCase();
          const jobTitle = (emp.jobTitle || '').toLowerCase();
          const username = (emp.username || '').toLowerCase();
          const phone = (emp.phoneNumber || '').toLowerCase();
          const department = (emp.departmentName || '').toLowerCase();
          const allottedOrg = (emp.allottedOrganization || '').toLowerCase();
          
          return fullName.includes(query) ||
                 workEmail.includes(query) ||
                 jobTitle.includes(query) ||
                 username.includes(query) ||
                 phone.includes(query) ||
                 department.includes(query) ||
                 allottedOrg.includes(query);
        });
      }

      // Apply department filter
      if (this.selectedDepartment) {
        filtered = filtered.filter(emp => emp.departmentName === this.selectedDepartment);
      }

      // Apply job title filter
      if (this.selectedJobTitle) {
        filtered = filtered.filter(emp => emp.jobTitle === this.selectedJobTitle);
      }

      // Apply allotted organization filter
      if (this.selectedAllottedOrg) {
        filtered = filtered.filter(emp => emp.allottedOrganization === this.selectedAllottedOrg);
      }

      // Apply sorting
      filtered = this.sortEmployees(filtered);

      this.filteredEmployees = filtered;
    } else {
      // For non-paginated, filter all employees
      let filtered = [...this.allEmployees];

      // Apply search filter
      if (this.searchQuery.trim()) {
        const query = this.searchQuery.toLowerCase().trim();
        filtered = filtered.filter(emp => {
          const fullName = (emp.fullName || '').toLowerCase();
          const workEmail = (emp.workEmail || '').toLowerCase();
          const jobTitle = (emp.jobTitle || '').toLowerCase();
          const username = (emp.username || '').toLowerCase();
          const phone = (emp.phoneNumber || '').toLowerCase();
          const department = (emp.departmentName || '').toLowerCase();
          const allottedOrg = (emp.allottedOrganization || '').toLowerCase();
          
          return fullName.includes(query) ||
                 workEmail.includes(query) ||
                 jobTitle.includes(query) ||
                 username.includes(query) ||
                 phone.includes(query) ||
                 department.includes(query) ||
                 allottedOrg.includes(query);
        });
      }

      // Apply department filter
      if (this.selectedDepartment) {
        filtered = filtered.filter(emp => emp.departmentName === this.selectedDepartment);
      }

      // Apply job title filter
      if (this.selectedJobTitle) {
        filtered = filtered.filter(emp => emp.jobTitle === this.selectedJobTitle);
      }

      // Apply allotted organization filter
      if (this.selectedAllottedOrg) {
        filtered = filtered.filter(emp => emp.allottedOrganization === this.selectedAllottedOrg);
      }

      // Apply column-specific filters
      if (this.columnFilters.fullName) {
        const query = this.columnFilters.fullName.toLowerCase().trim();
        filtered = filtered.filter(emp => 
          (emp.fullName || '').toLowerCase().includes(query)
        );
      }

      if (this.columnFilters.username) {
        const query = this.columnFilters.username.toLowerCase().trim();
        filtered = filtered.filter(emp => 
          (emp.username || '').toLowerCase().includes(query)
        );
      }

      if (this.columnFilters.position) {
        const query = this.columnFilters.position.toLowerCase().trim();
        filtered = filtered.filter(emp => 
          (emp.jobTitle || '').toLowerCase().includes(query)
        );
      }

      if (this.columnFilters.department) {
        filtered = filtered.filter(emp => 
          emp.departmentName === this.columnFilters.department
        );
      }

      if (this.columnFilters.allottedOrg) {
        filtered = filtered.filter(emp => 
          emp.allottedOrganization === this.columnFilters.allottedOrg
        );
      }

      // Apply sorting
      filtered = this.sortEmployees(filtered);

      this.filteredEmployees = filtered;
      this.employees = filtered;
    }
  }

  sortEmployees(employees: Employee[]): Employee[] {
    if (!this.sortColumn) {
      return employees;
    }

    return [...employees].sort((a, b) => {
      let aValue: any;
      let bValue: any;

      switch (this.sortColumn) {
        case 'fullName':
          aValue = (a.fullName || '').toLowerCase();
          bValue = (b.fullName || '').toLowerCase();
          break;
        case 'username':
          aValue = (a.username || '').toLowerCase();
          bValue = (b.username || '').toLowerCase();
          break;
        case 'position':
          aValue = (a.jobTitle || '').toLowerCase();
          bValue = (b.jobTitle || '').toLowerCase();
          break;
        case 'department':
          aValue = (a.departmentName || '').toLowerCase();
          bValue = (b.departmentName || '').toLowerCase();
          break;
        case 'allottedOrganization':
          aValue = (a.allottedOrganization || '').toLowerCase();
          bValue = (b.allottedOrganization || '').toLowerCase();
          break;
        default:
          return 0;
      }

      if (aValue < bValue) {
        return this.sortDirection === 'asc' ? -1 : 1;
      }
      if (aValue > bValue) {
        return this.sortDirection === 'asc' ? 1 : -1;
      }
      return 0;
    });
  }

  onSort(column: string): void {
    if (this.sortColumn === column) {
      // Toggle direction if same column
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      // New column, default to ascending
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.applyFilters();
  }

  getSortIcon(column: string): string {
    if (this.sortColumn !== column) {
      return '⇅'; // Neutral sort icon
    }
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  onGlobalSearchChange(): void {
    this.applyFilters();
  }

  get hasActiveFilters(): boolean {
    return !!(this.globalSearchQuery.trim() ||
              this.searchQuery.trim() || 
              this.selectedDepartment || 
              this.selectedJobTitle || 
              this.selectedAllottedOrg ||
              this.columnFilters.fullName ||
              this.columnFilters.username ||
              this.columnFilters.position ||
              this.columnFilters.department ||
              this.columnFilters.allottedOrg ||
              this.sortColumn);
  }

  get resultCount(): number {
    if (this.usePagination) {
      return this.totalElements;
    }
    return this.filteredEmployees.length;
  }

  get pageNumbers(): number[] {
    const pages: number[] = [];
    const maxPages = Math.min(this.totalPages, 10); // Show max 10 page numbers
    const startPage = Math.max(0, Math.min(this.currentPage - 4, this.totalPages - maxPages));
    
    for (let i = startPage; i < Math.min(startPage + maxPages, this.totalPages); i++) {
      pages.push(i);
    }
    return pages;
  }

  // Expose Math to template
  Math = Math;

  viewEmployeeDetails(employeeId: number): void {
    this.showDetailsModal = true;
    this.loadingDetails = true;
    this.selectedEmployee = null;
    this.employeeWorkSummary = null;
    this.employeeRotaSchedule = [];
    this.employeeDocuments = [];

    // Load employee basic info
    this.employeeService.getEmployeeById(employeeId).subscribe({
      next: (employee) => {
        this.selectedEmployee = employee;
        this.loadingDetails = false;
      },
      error: (err) => {
        console.error('Error loading employee details:', err);
        this.loadingDetails = false;
        this.toastService.error('Failed to load employee details');
      }
    });

    // Load work summary
    this.attendanceService.getEmployeeWorkSummary(employeeId).subscribe({
      next: (summary) => {
        this.employeeWorkSummary = summary;
      },
      error: (err) => {
        console.error('Error loading work summary:', err);
        // Don't show error - employee might not have attendance records
      }
    });

    // Load ROTA schedule
    this.rotaService.getEmployeeCurrentWeekSchedule(employeeId).subscribe({
      next: (schedule) => {
        this.employeeRotaSchedule = schedule;
      },
      error: (err) => {
        console.error('Error loading ROTA schedule:', err);
        // Don't show error - ROTA might not exist
      }
    });

    // Load documents
    this.documentService.getEmployeeDocuments(employeeId).subscribe({
      next: (documents) => {
        this.employeeDocuments = documents;
      },
      error: (err) => {
        console.error('Error loading documents:', err);
        // Don't show error - employee might not have documents
      }
    });

    // Load leave balances (for admins/super admins)
    if (this.currentUser?.roles?.includes('ADMIN') || this.currentUser?.roles?.includes('SUPER_ADMIN')) {
      this.leaveService.getLeaveBalances(employeeId).subscribe({
        next: (balances) => {
          this.employeeLeaveBalances = balances;
        },
        error: (err) => {
          console.error('Error loading leave balances:', err);
          // Don't show error - leave balances might not be initialized yet
        }
      });
    }
  }

  closeDetailsModal(): void {
    this.showDetailsModal = false;
    this.selectedEmployee = null;
    this.employeeWorkSummary = null;
    this.employeeRotaSchedule = [];
    this.employeeDocuments = [];
    this.employeeLeaveBalances = [];
    this.closeDocumentViewer();
  }

  getDutyClass(duty: string | undefined): string {
    if (!duty) return 'duty-off';
    if (duty.match(/(OFF|Leave|Holiday)/i)) return 'duty-off';
    if (duty.toLowerCase().includes('set')) return 'duty-setup';
    return 'duty-work';
  }

  downloadDocument(documentId: number | undefined): void {
    if (!documentId) {
      this.toastService.error('Document ID is missing');
      return;
    }
    this.documentService.downloadDocument(documentId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `document_${documentId}`;
        link.click();
        window.URL.revokeObjectURL(url);
        this.toastService.success('Document downloaded successfully');
      },
      error: (err) => {
        console.error('Error downloading document:', err);
        this.toastService.error('Failed to download document');
      }
    });
  }

  viewDocument(documentId: number | undefined, documentType: string): void {
    if (!documentId) {
      this.toastService.error('Document ID is missing');
      return;
    }
    // Clear previous selection
    if (this.rawDocumentUrl) {
      window.URL.revokeObjectURL(this.rawDocumentUrl);
    }

    this.selectedDocumentId = documentId;
    this.selectedDocumentType = documentType;
    this.selectedDocumentUrl = null;
    this.selectedDocumentRawUrl = null;
    this.rawDocumentUrl = null;
    this.documentLoading = true;
    this.isSelectedDocPdf = false;
    this.isSelectedDocImage = false;

    this.documentService.getDocumentImage(documentId).subscribe({
      next: (blob) => {
        if (blob.size === 0) {
          console.error('Received empty blob for document:', documentId);
          this.toastService.error('Document file is empty');
          this.selectedDocumentId = null;
          this.selectedDocumentType = null;
          this.documentLoading = false;
          return;
        }

        const url = window.URL.createObjectURL(blob);
        this.rawDocumentUrl = url;

        // Set both URL types
        this.selectedDocumentRawUrl = url;
        this.selectedDocumentUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);

        // Smart detection based on blob type
        this.isSelectedDocPdf = blob.type === 'application/pdf' || blob.type.includes('pdf');
        this.isSelectedDocImage = blob.type.startsWith('image/');

        this.documentLoading = false;
      },
      error: (err) => {
        console.error('Failed to load document:', err.status, err.message);

        let errorMessage = 'Failed to load document';
        if (err?.status === 403) {
          errorMessage = 'Access denied to this document';
        } else if (err?.status === 404) {
          errorMessage = 'Document file not found';
        } else if (err?.status === 0) {
          errorMessage = 'Cannot connect to server';
        }

        this.toastService.error(errorMessage);
        this.selectedDocumentId = null;
        this.selectedDocumentType = null;
        this.documentLoading = false;
      }
    });
  }

  closeDocumentViewer(): void {
    if (this.rawDocumentUrl) {
      window.URL.revokeObjectURL(this.rawDocumentUrl);
    }
    this.selectedDocumentId = null;
    this.selectedDocumentUrl = null;
    this.selectedDocumentRawUrl = null;
    this.selectedDocumentType = null;
    this.rawDocumentUrl = null;
    this.documentLoading = false;
    this.isSelectedDocPdf = false;
    this.isSelectedDocImage = false;
  }

  uploadDocument(employeeId: number): void {
    this.closeDetailsModal();
    this.router.navigate(['/documents/upload'], { queryParams: { employeeId } });
  }

  deleteEmployee(id: number | undefined): void {
    if (!id) return;

    const confirmed = window.confirm('Are you sure you want to delete this employee? This action cannot be undone.');

    if (confirmed) {
      this.employeeService.deleteEmployee(id).subscribe({
        next: () => {
          this.toastService.success('Employee deleted successfully');
          this.loadEmployees();
        },
        error: (err) => {
          this.toastService.error('Failed to delete employee');
          console.error('Error deleting employee:', err);
        }
      });
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  getPassportDocument(): Document | null {
    if (!this.employeeDocuments || this.employeeDocuments.length === 0) {
      return null;
    }
    return this.employeeDocuments.find(doc => doc.documentType === 'PASSPORT') || null;
  }

  getVisaDocument(): Document | null {
    if (!this.employeeDocuments || this.employeeDocuments.length === 0) {
      return null;
    }
    return this.employeeDocuments.find(doc => doc.documentType === 'VISA') || null;
  }

  isDocumentExpired(expiryDate: string | undefined): boolean {
    if (!expiryDate) return false;
    const expiry = new Date(expiryDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return expiry < today;
  }

  getLeaveBalanceStatusClass(remainingLeaves: number): string {
    if (remainingLeaves <= 2) {
      return 'balance-low';
    } else if (remainingLeaves <= 5) {
      return 'balance-warning';
    }
    return 'balance-good';
  }
}
