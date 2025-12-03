import {Component, OnInit, OnDestroy} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {Router, RouterModule} from '@angular/router';
import {DocumentService} from '../../services/document.service';
import {EmployeeService} from '../../services/employee.service';
import {AuthService} from '../../services/auth.service';
import {Document} from '../../models/document.model';
import {Employee} from '../../models/employee.model';
import {Subscription} from 'rxjs';

interface DocumentTypeOption {
  value: string;
  label: string;
  description?: string;
}

interface User {
  id?: number;
  email?: string;
  username?: string;
  roles?: string[];
  organizationId?: number;
}

@Component({
  selector: 'app-document-upload',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './document-upload.component.html',
  styleUrls: ['./document-upload.component.css']
})
export class DocumentUploadComponent implements OnInit, OnDestroy {
  employees: Employee[] = [];
  selectedEmployeeId: number = 0;
  documentType: string = '';
  visaType: string = '';
  selectedFile: File | null = null;
  fileName: string = '';

  loading = false;
  uploading = false;
  error: string | null = null;
  success: string | null = null;
  warning: string | null = null;
  submitted = false;
  attemptedSubmit = false;

  // UK Visa Types
  visaTypeOptions: string[] = [
    'Skilled Worker Visa',
    'Student Visa',
    'Family Visa',
    'Youth Mobility Scheme Visa',
    'Health and Care Worker Visa',
    'Global Talent Visa',
    'Innovator Founder Visa',
    'Start-up Visa',
    'Seasonal Worker Visa',
    'Creative Worker Visa',
    'Charity Worker Visa',
    'Religious Worker Visa',
    'International Agreement Visa',
    'UK Ancestry Visa',
    'High Potential Individual (HPI) Visa',
    'Graduate Visa',
    'Other'
  ];

  uploadedDocument: Document | null = null;
  existingDocuments: Document[] = [];
  duplicateDocument: Document | null = null;

  currentUser: User | null = null;
  isAdmin = false;

  private subscriptions: Subscription[] = [];

  documentTypeOptions: DocumentTypeOption[] = [
    { value: 'PASSPORT', label: 'Passport (ID Document)' },
    { value: 'VISA', label: 'Visa / Work Permission' },
    { value: 'CONTRACT', label: 'Employment Contract' },
    { value: 'RESUME', label: 'CV / Resume' },
    { value: 'SHARE_CODE', label: 'Share Code Proof' },
    { value: 'PROOF_OF_ADDRESS', label: 'Proof of Address' },
    { value: 'REGISTRATION_FORM', label: 'Registration Form' },
    { value: 'CERTIFICATE', label: 'Certificate' },
    { value: 'PROFESSIONAL_CERTIFICATE', label: 'Professional Certificate' },
    { value: 'TERM_LETTER', label: 'Term Letter' },
    { value: 'NATIONAL_INSURANCE', label: 'National Insurance Document' },
    { value: 'BANK_STATEMENT', label: 'Bank Statement' },
    { value: 'OTHERS', label: 'Others' }
  ];

  private readonly ocrDocumentTypes = new Set(['PASSPORT', 'VISA', 'CONTRACT']);
  private readonly identityDocumentTypes = new Set(['PASSPORT', 'VISA']);

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
    const employeesSub = this.employeeService.getAllEmployees().subscribe({
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
    this.subscriptions.push(employeesSub);
  }

  loadExistingDocuments(): void {
    const documentsSub = this.documentService.getAllDocuments().subscribe({
      next: (data) => {
        this.existingDocuments = data;
      },
      error: (err) => {
        console.error('Error loading existing documents:', err);
      }
    });
    this.subscriptions.push(documentsSub);
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

      // Validate file size (max 20MB - supports high-resolution scans, multi-page PDFs)
      if (file.size > 20 * 1024 * 1024) {
        const fileSizeMB = (file.size / (1024 * 1024)).toFixed(2);
        this.error = `File size must be less than 20MB. Current size: ${fileSizeMB} MB`;
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

    // Validate visa type for VISA documents
    if (this.documentType === 'VISA' && !this.visaType) {
      this.error = 'Please select a visa type before uploading.';
      return;
    }

    this.uploading = true;

    const uploadSub = this.documentService.uploadDocument(
      this.selectedEmployeeId, 
      this.documentType, 
      this.selectedFile,
      this.visaType || undefined
    ).subscribe({
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
          this.warning = `Warning: A similar ${this.getDocumentTypeLabel(response.documentType)} document already exists with number ${duplicate.documentNumber}. Uploaded on ${this.formatDate(duplicate.uploadedDate!)}.`;
        }

        // Reload documents list
        this.loadExistingDocuments();

        // Reset form
        this.resetForm();
      },
      error: (err) => {
        console.error('Document upload failed:', err);
        this.error = err.error?.message || err.error || 'Failed to upload document. Please try again.';
        this.uploading = false;
      }
    });
    this.subscriptions.push(uploadSub);
  }

  onDocumentTypeChange(): void {
    // Reset visaType if documentType is not VISA
    if (this.documentType !== 'VISA') {
      this.visaType = '';
    }
    // Reset form submission flags when document type changes
    this.submitted = false;
    this.attemptedSubmit = false;
  }

  resetForm(): void {
    this.documentType = '';
    this.visaType = '';
    this.selectedFile = null;
    this.fileName = '';
    this.submitted = false;
    this.attemptedSubmit = false;
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

  isCurrentSelectionOcr(): boolean {
    return this.isOcrDocumentType(this.documentType);
  }

  isOcrDocumentType(type?: string | null): boolean {
    if (!type) { return false; }
    return this.ocrDocumentTypes.has(type.toUpperCase());
  }

  requiresMetadata(type?: string | null): boolean {
    if (!type) { return false; }
    return this.identityDocumentTypes.has(type.toUpperCase());
  }

  shouldShowMetadataCard(document?: Document | null): boolean {
    return !!document && this.requiresMetadata(document.documentType);
  }

  getDocumentTypeLabel(type?: string | null): string {
    if (!type) { return 'Document'; }
    const upperType = type.toUpperCase();
    const option = this.documentTypeOptions.find(opt => opt.value === upperType);
    if (option) {
      return option.label;
    }
    return upperType.replace(/_/g, ' ');
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.subscriptions = [];
  }
}

