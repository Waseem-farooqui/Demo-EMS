import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {Router, RouterModule} from '@angular/router';
import {DocumentService} from '../../services/document.service';
import {EmployeeService} from '../../services/employee.service';
import {AuthService} from '../../services/auth.service';
import {Document} from '../../models/document.model';
import {Employee} from '../../models/employee.model';

@Component({
  selector: 'app-document-upload',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './document-upload.component.html',
  styleUrls: ['./document-upload.component.css']
})
export class DocumentUploadComponent implements OnInit {
  employees: Employee[] = [];
  selectedEmployeeId: number = 0;
  documentType: string = '';
  selectedFile: File | null = null;
  fileName: string = '';

  loading = false;
  uploading = false;
  error: string | null = null;
  success: string | null = null;
  warning: string | null = null;

  uploadedDocument: Document | null = null;
  existingDocuments: Document[] = [];
  duplicateDocument: Document | null = null;

  currentUser: any;
  isAdmin = false;

  documentTypes = ['PASSPORT', 'VISA', 'CONTRACT', 'RESUME', 'SHARE_CODE'];

  constructor(
    private documentService: DocumentService,
    private employeeService: EmployeeService,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.currentUser = this.authService.getUser();
    const roles = this.currentUser?.roles || [];
    this.isAdmin = roles.includes('ADMIN') || roles.includes('SUPER_ADMIN');
    this.loadEmployees();
    this.loadExistingDocuments();
  }

  loadEmployees(): void {
    this.loading = true;
    this.employeeService.getAllEmployees().subscribe({
      next: (data) => {
        this.employees = data;
        // For non-admin, auto-select their employee
        if (!this.isAdmin && data.length > 0) {
          this.selectedEmployeeId = data[0].id!;
        }
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load employees.';
        this.loading = false;
        console.error('Error loading employees:', err);
      }
    });
  }

  loadExistingDocuments(): void {
    this.documentService.getAllDocuments().subscribe({
      next: (data) => {
        this.existingDocuments = data;
      },
      error: (err) => {
        console.error('Error loading existing documents:', err);
      }
    });
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      // Validate file type
      const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'application/pdf'];
      if (!allowedTypes.includes(file.type)) {
        this.error = 'Only JPG, PNG, and PDF files are allowed.';
        this.selectedFile = null;
        this.fileName = '';
        return;
      }

      // Validate file size (max 10MB)
      if (file.size > 10 * 1024 * 1024) {
        this.error = 'File size must be less than 10MB.';
        this.selectedFile = null;
        this.fileName = '';
        return;
      }

      this.selectedFile = file;
      this.fileName = file.name;
      this.error = null;
    }
  }

  onSubmit(): void {
    this.error = null;
    this.success = null;
    this.warning = null;
    this.duplicateDocument = null;

    // Validation
    if (!this.selectedEmployeeId) {
      this.error = 'Please select an employee.';
      return;
    }

    if (!this.documentType) {
      this.error = 'Please select a document type.';
      return;
    }

    if (!this.selectedFile) {
      this.error = 'Please select a file to upload.';
      return;
    }

    this.uploading = true;

    this.documentService.uploadDocument(this.selectedEmployeeId, this.documentType, this.selectedFile).subscribe({
      next: (response) => {
        this.uploadedDocument = response;
        this.uploading = false;
        this.success = 'Document uploaded successfully!';

        // Check for duplicate
        const duplicate = this.documentService.checkDuplicateDocument(
          this.existingDocuments,
          response
        );

        if (duplicate) {
          this.duplicateDocument = duplicate;
          this.warning = `Warning: A similar ${response.documentType} document already exists with number ${duplicate.documentNumber}. Uploaded on ${this.formatDate(duplicate.uploadedDate!)}.`;
        }

        // Reload documents list
        this.loadExistingDocuments();

        // Reset form
        this.resetForm();
      },
      error: (err) => {
        this.error = err.error?.message || err.error || 'Failed to upload document. Please try again.';
        this.uploading = false;
        console.error('Error uploading document:', err);
      }
    });
  }

  resetForm(): void {
    this.documentType = '';
    this.selectedFile = null;
    this.fileName = '';
    // Don't reset employee selection for non-admin users
    if (this.isAdmin) {
      this.selectedEmployeeId = 0;
    }

    // Reset file input
    const fileInput = document.getElementById('fileInput') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  }

  reuploadDocument(): void {
    this.uploadedDocument = null;
    this.success = null;
    this.warning = null;
    this.duplicateDocument = null;
  }

  viewAllDocuments(): void {
    this.router.navigate(['/documents']);
  }

  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-GB');
  }

  getExpiryStatus(daysUntilExpiry?: number): string {
    if (!daysUntilExpiry) return '';
    if (daysUntilExpiry < 0) return 'expired';
    if (daysUntilExpiry <= 30) return 'critical';
    if (daysUntilExpiry <= 90) return 'warning';
    return 'valid';
  }

  getExpiryStatusText(daysUntilExpiry?: number): string {
    if (!daysUntilExpiry) return '';
    if (daysUntilExpiry < 0) return 'EXPIRED';
    if (daysUntilExpiry <= 30) return 'EXPIRING SOON';
    if (daysUntilExpiry <= 90) return 'EXPIRES IN ' + daysUntilExpiry + ' DAYS';
    return 'VALID';
  }
}

