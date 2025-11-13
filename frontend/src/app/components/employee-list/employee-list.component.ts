import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {Router, RouterModule} from '@angular/router';
import {DomSanitizer, SafeResourceUrl} from '@angular/platform-browser';
import {EmployeeService} from '../../services/employee.service';
import {AttendanceService} from '../../services/attendance.service';
import {RotaService} from '../../services/rota.service';
import {DocumentService} from '../../services/document.service';
import {AuthService} from '../../services/auth.service';
import {ToastService} from '../../services/toast.service';
import {Employee} from '../../models/employee.model';
import {EmployeeWorkSummary} from '../../models/attendance.model';

@Component({
  selector: 'app-employee-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './employee-list.component.html',
  styleUrls: ['./employee-list.component.css']
})
export class EmployeeListComponent implements OnInit {
  employees: Employee[] = [];
  loading = false;
  error: string | null = null;
  currentUser: any;

  // Employee Details Modal
  showDetailsModal = false;
  loadingDetails = false;
  selectedEmployee: Employee | null = null;
  employeeWorkSummary: EmployeeWorkSummary | null = null;
  employeeRotaSchedule: any[] = [];
  employeeDocuments: any[] = [];

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

  constructor(
    private employeeService: EmployeeService,
    private attendanceService: AttendanceService,
    private rotaService: RotaService,
    private documentService: DocumentService,
    private authService: AuthService,
    private toastService: ToastService,
    private sanitizer: DomSanitizer,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.currentUser = this.authService.getUser();
    this.loadEmployees();
  }

  loadEmployees(): void {
    this.loading = true;
    this.error = null;
    this.employeeService.getAllEmployees().subscribe({
      next: (data) => {
        // Filter out the current logged-in user from the list
        const currentUserEmail = this.currentUser?.email;
        this.employees = data.filter(emp => emp.workEmail !== currentUserEmail);
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load employees. Please try again.';
        this.loading = false;
        console.error('Error loading employees:', err);
      }
    });
  }

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
  }

  closeDetailsModal(): void {
    this.showDetailsModal = false;
    this.selectedEmployee = null;
    this.employeeWorkSummary = null;
    this.employeeRotaSchedule = [];
    this.employeeDocuments = [];
    this.closeDocumentViewer();
  }

  getDutyClass(duty: string | undefined): string {
    if (!duty) return 'duty-off';
    if (duty.match(/(OFF|Leave|Holiday)/i)) return 'duty-off';
    if (duty.toLowerCase().includes('set')) return 'duty-setup';
    return 'duty-work';
  }

  downloadDocument(documentId: number): void {
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

  viewDocument(documentId: number, documentType: string): void {
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
}
