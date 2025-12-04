import {Component, OnDestroy, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, Router, RouterModule} from '@angular/router';
import {DomSanitizer, SafeResourceUrl} from '@angular/platform-browser';
import {DocumentService} from '../../services/document.service';
import {Document} from '../../models/document.model';
import {environment} from '../../../environments/environment';

@Component({
  selector: 'app-document-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './document-detail.component.html',
  styleUrls: ['./document-detail.component.css']
})
export class DocumentDetailComponent implements OnInit, OnDestroy {
  document: Document | null = null;
  loading = false;
  error: string | null = null;
  editMode = false;
  maxDate: string; // Maximum date (today) for issue date

  // Image/PDF display
  documentImageUrl: SafeResourceUrl | null = null; // For PDFs in iframe
  documentImageRawUrl: string | null = null; // For images in img tag
  private rawDocumentUrl: string | null = null; // For cleanup
  imageLoading = false;
  imageError = false;

  // Smart display detection
  isPdfDocument = false; // True if blob type is PDF
  isImageDocument = false; // True if blob type is image

  // Manual entry fields
  manualDocumentNumber = '';
  manualIssuingCountry = '';
  manualIssueDate = '';
  manualExpiryDate = '';
  manualVisaType = '';

  saving = false;
  saveSuccess = false;
  saveError: string | null = null;

  private readonly metadataOptionalTypes = new Set([
    'CONTRACT',
    'RESUME',
    'SHARE_CODE',
    'PROOF_OF_ADDRESS',
    'REGISTRATION_FORM',
    'CERTIFICATE',
    'PROFESSIONAL_CERTIFICATE',
    'TERM_LETTER',
    'NATIONAL_INSURANCE',
    'BANK_STATEMENT'
  ]);

  private readonly identityDocumentTypes = new Set(['PASSPORT', 'VISA']);
  private readonly supportingDocumentTypes = new Set([
    'RESUME',
    'SHARE_CODE',
    'PROOF_OF_ADDRESS',
    'REGISTRATION_FORM',
    'CERTIFICATE',
    'PROFESSIONAL_CERTIFICATE',
    'TERM_LETTER',
    'NATIONAL_INSURANCE',
    'BANK_STATEMENT',
    'OTHERS'
  ]);

  private readonly documentTypeLabelMap: Record<string, string> = {
    PASSPORT: 'Passport',
    VISA: 'Visa',
    CONTRACT: 'Employment Contract',
    RESUME: 'Resume',
    SHARE_CODE: 'Share Code',
    PROOF_OF_ADDRESS: 'Proof of Address',
    REGISTRATION_FORM: 'Registration Form',
    CERTIFICATE: 'Certificate',
    PROFESSIONAL_CERTIFICATE: 'Professional Certificate',
    TERM_LETTER: 'Term Letter',
    NATIONAL_INSURANCE: 'National Insurance',
    BANK_STATEMENT: 'Bank Statement',
    OTHERS: 'Others'
  };

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

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private documentService: DocumentService,
    private sanitizer: DomSanitizer
  ) {
    // Set max date to today in YYYY-MM-DD format
    const today = new Date();
    this.maxDate = today.toISOString().split('T')[0];
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadDocument(parseInt(id));
    }
  }

  loadDocument(id: number): void {
    this.loading = true;
    this.error = null;

    this.documentService.getDocumentById(id).subscribe({
      next: (data) => {
        this.document = data;
        this.loading = false;

        // Pre-fill manual entry fields with existing data
        this.manualDocumentNumber = data.documentNumber || '';
        this.manualIssuingCountry = data.issuingCountry || '';
        this.manualIssueDate = data.issueDate || '';
        this.manualExpiryDate = data.expiryDate || '';
        this.manualVisaType = data.visaType || '';

        // Load document image
        this.loadDocumentImage(id);
      },
      error: (err) => {
        this.error = 'Failed to load document details. Please try again.';
        this.loading = false;
        console.error('Error loading document:', err);
      }
    });
  }

  loadDocumentImage(id: number): void {
    this.imageLoading = true;
    this.imageError = false;

    this.documentService.getDocumentImage(id).subscribe({
      next: (blob) => {
        if (blob.size === 0) {
          console.error('Received empty blob for document:', id);
          this.error = 'Document file is empty or corrupted';
          this.imageError = true;
          this.imageLoading = false;
          return;
        }

        // Revoke previous URL if exists
        if (this.rawDocumentUrl) {
          URL.revokeObjectURL(this.rawDocumentUrl);
        }

        // Create object URL from blob
        const url = URL.createObjectURL(blob);
        this.rawDocumentUrl = url;

        // Set both URLs for different viewer types
        this.documentImageRawUrl = url;
        this.documentImageUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);

        // Smart detection: Choose viewer based on actual blob type
        this.isPdfDocument = blob.type === 'application/pdf' || blob.type.includes('pdf');
        this.isImageDocument = blob.type.startsWith('image/');

        this.imageLoading = false;
      },
      error: (err) => {
        console.error('Failed to load document:', err.status, err.message);
        this.imageError = true;
        this.imageLoading = false;

        if (err?.status === 403) {
          this.error = 'Access denied. You do not have permission to view this document.';
        } else if (err?.status === 404) {
          this.error = 'Document file not found on server.';
        } else if (err?.status === 0) {
          this.error = 'Cannot connect to server. Please check your connection.';
        } else {
          this.error = 'Failed to load document. Please try again.';
        }
      }
    });
  }

  onImageError(): void {
    console.error('Image failed to load');
    this.imageError = true;
    this.error = 'Failed to display document. Please try downloading it instead.';
  }

  hasMinimalData(): boolean {
    if (!this.document) {
      return false;
    }
    if (this.isMetadataOptional(this.document.documentType)) {
      return true;
    }
    return !!(this.document.documentNumber &&
              this.document.issuingCountry &&
              this.document.expiryDate);
  }

  isDataIncomplete(): boolean {
    if (!this.document) return false;

    if (this.isMetadataOptional(this.document.documentType)) {
      return false;
    }

    const missingFields = [];
    if (!this.document.documentNumber) missingFields.push('Document Number');
    if (!this.document.issuingCountry) missingFields.push('Issuing Country');
    if (!this.document.issueDate) missingFields.push('Issue Date');
    if (!this.document.expiryDate) missingFields.push('Expiry Date');

    return missingFields.length > 0;
  }

  getMissingFields(): string[] {
    if (!this.document) return [];

    if (this.isMetadataOptional(this.document.documentType)) {
      return [];
    }

    const missingFields = [];
    if (!this.document.documentNumber) missingFields.push('Document Number');
    if (!this.document.issuingCountry) missingFields.push('Issuing Country');
    if (!this.document.issueDate) missingFields.push('Issue Date');
    if (!this.document.expiryDate) missingFields.push('Expiry Date');

    return missingFields;
  }

  enableEditMode(): void {
    this.editMode = true;
    this.saveSuccess = false;
    this.saveError = null;
    
    // Initialize manual fields with current document values
    if (this.document) {
      this.manualDocumentNumber = this.document.documentNumber || '';
      this.manualIssuingCountry = this.document.issuingCountry || '';
      this.manualIssueDate = this.document.issueDate || '';
      this.manualExpiryDate = this.document.expiryDate || '';
      this.manualVisaType = this.document.visaType || '';
    }
  }

  cancelEdit(): void {
    this.editMode = false;
    this.saveError = null;

    // Reset to original values
    if (this.document) {
      this.manualDocumentNumber = this.document.documentNumber || '';
      this.manualIssuingCountry = this.document.issuingCountry || '';
      this.manualIssueDate = this.document.issueDate || '';
      this.manualExpiryDate = this.document.expiryDate || '';
      this.manualVisaType = this.document.visaType || '';
    }
  }

  saveManualEntry(): void {
    if (!this.document) return;

    // Validate required fields
    if (!this.manualDocumentNumber || !this.manualIssuingCountry || !this.manualExpiryDate) {
      this.saveError = 'Please fill in all required fields (Document Number, Issuing Country, and Expiry Date)';
      return;
    }

    this.saving = true;
    this.saveError = null;
    this.saveSuccess = false;

    const updateData: any = {
      documentNumber: this.manualDocumentNumber,
      issuingCountry: this.manualIssuingCountry,
      issueDate: this.manualIssueDate || null,
      expiryDate: this.manualExpiryDate
    };

    // Include visa type for VISA documents
    if (this.document.documentType === 'VISA') {
      updateData.visaType = this.manualVisaType || null;
    }

    this.documentService.updateDocument(this.document.id!, updateData).subscribe({
      next: (updated) => {
        this.document = updated;
        this.saving = false;
        this.saveSuccess = true;
        this.editMode = false;

        setTimeout(() => {
          this.saveSuccess = false;
        }, 3000);
      },
      error: (err) => {
        this.saveError = 'Failed to update document. Please try again.';
        this.saving = false;
        console.error('Error updating document:', err);
      }
    });
  }

  deleteDocument(): void {
    if (!this.document) return;

    if (confirm(`Are you sure you want to delete this ${this.document.documentType} document?`)) {
      this.documentService.deleteDocument(this.document.id!).subscribe({
        next: () => {
          this.router.navigate(['/documents']);
        },
        error: (err) => {
          this.error = 'Failed to delete document. Please try again.';
          console.error('Error deleting document:', err);
        }
      });
    }
  }

  goBack(): void {
    this.router.navigate(['/documents']);
  }

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return 'Not available';
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-GB', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    });
  }

  shouldShowExpiryBadge(doc?: Document | null): boolean {
    if (!doc) {
      return false;
    }
    return this.tracksExpiry(doc.documentType);
  }

  shouldShowImportantDates(doc?: Document | null): boolean {
    if (!doc) {
      return false;
    }
    return this.tracksExpiry(doc.documentType) && (!!doc.issueDate || !!doc.expiryDate);
  }

  shouldShowIdentityFields(doc?: Document | null): boolean {
    if (!doc) {
      return false;
    }
    return this.identityDocumentTypes.has((doc.documentType || '').toUpperCase());
  }

  isSupportingDocument(type?: string | null): boolean {
    if (!type) {
      return false;
    }
    return this.supportingDocumentTypes.has(type.toUpperCase());
  }

  getDocumentTypeLabel(type?: string | null): string {
    if (!type) {
      return 'Document';
    }
    const upper = type.toUpperCase();
    return this.documentTypeLabelMap[upper] ?? upper.replace(/_/g, ' ');
  }

  private isMetadataOptional(type?: string | null): boolean {
    if (!type) {
      return false;
    }
    return this.metadataOptionalTypes.has(type.toUpperCase());
  }

  private tracksExpiry(type?: string | null): boolean {
    if (!type) {
      return false;
    }
    return !this.metadataOptionalTypes.has(type.toUpperCase());
  }

  getExpiryStatus(daysUntilExpiry?: number): string {
    if (!daysUntilExpiry) return 'unknown';
    if (daysUntilExpiry < 0) return 'expired';
    if (daysUntilExpiry <= 30) return 'critical';
    if (daysUntilExpiry <= 90) return 'warning';
    return 'valid';
  }

  getExpiryStatusText(daysUntilExpiry?: number): string {
    if (!daysUntilExpiry) return 'Unknown';
    if (daysUntilExpiry < 0) return 'EXPIRED';
    if (daysUntilExpiry <= 30) return `Critical - ${daysUntilExpiry} days left`;
    if (daysUntilExpiry <= 90) return `Expires soon - ${daysUntilExpiry} days`;
    return `Valid - ${daysUntilExpiry} days remaining`;
  }

  downloadDocument(): void {
    if (!this.document?.id) return;

    // Use the existing blob if already loaded
    if (this.rawDocumentUrl) {
      const link = document.createElement('a');
      link.href = this.rawDocumentUrl;
      link.download = this.document.fileName || 'document';
      link.click();
    } else {
      // Fetch with authentication and download
      this.documentService.getDocumentImage(this.document.id).subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = this.document?.fileName || 'document';
          link.click();
          window.URL.revokeObjectURL(url);
        },
        error: (err) => {
          console.error('Failed to download document:', err);
          this.error = 'Failed to download document. Please try again.';
        }
      });
    }
  }


  openPdfInNewTab(): void {
    if (this.rawDocumentUrl) {
      window.open(this.rawDocumentUrl, '_blank');
    } else if (this.document?.id) {
      // Fetch with authentication and open in new tab
      this.documentService.getDocumentImage(this.document.id).subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          window.open(url, '_blank');
        },
        error: (err) => {
          console.error('Failed to open document:', err);
          this.error = 'Failed to open document. Please try again.';
        }
      });
    }
  }

  isFilePdf(): boolean {
    if (!this.document?.fileType) return false;
    return this.document.fileType === 'application/pdf';
  }

  ngOnDestroy(): void {
    // Clean up object URL to prevent memory leaks
    if (this.rawDocumentUrl) {
      URL.revokeObjectURL(this.rawDocumentUrl);
    }
  }
}

