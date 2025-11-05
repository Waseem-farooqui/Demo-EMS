import {Component, OnDestroy, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, Router, RouterModule} from '@angular/router';
import {DomSanitizer, SafeResourceUrl} from '@angular/platform-browser';
import {DocumentService} from '../../services/document.service';
import {Document} from '../../models/document.model';

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
  documentImageUrl: SafeResourceUrl | null = null;
  private rawDocumentUrl: string | null = null; // For cleanup
  imageLoading = false;
  imageError = false;

  // Manual entry fields
  manualDocumentNumber = '';
  manualIssuingCountry = '';
  manualIssueDate = '';
  manualExpiryDate = '';

  saving = false;
  saveSuccess = false;
  saveError: string | null = null;

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
        // Create object URL from blob
        const url = URL.createObjectURL(blob);
        this.rawDocumentUrl = url; // Store for cleanup
        // Sanitize URL for iframe (needed for PDF viewing)
        this.documentImageUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
        this.imageLoading = false;
      },
      error: (err) => {
        console.error('Error loading document image:', err);
        this.imageError = true;
        this.imageLoading = false;
      }
    });
  }

  hasMinimalData(): boolean {
    return !!(this.document?.documentNumber &&
              this.document.issuingCountry &&
              this.document.expiryDate);
  }

  isDataIncomplete(): boolean {
    if (!this.document) return false;

    // CONTRACT and RESUME documents don't need validation
    if (this.document.documentType === 'CONTRACT' || this.document.documentType === 'RESUME') {
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

    // CONTRACT and RESUME documents don't need validation
    if (this.document.documentType === 'CONTRACT' || this.document.documentType === 'RESUME') {
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

    const updateData = {
      documentNumber: this.manualDocumentNumber,
      issuingCountry: this.manualIssuingCountry,
      issueDate: this.manualIssueDate || null,
      expiryDate: this.manualExpiryDate
    };

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
    if (this.document?.id) {
      // Open download endpoint in new window
      window.open(`http://localhost:8080/api/documents/${this.document.id}/download`, '_blank');
    }
  }

  openPdfInNewTab(): void {
    if (this.rawDocumentUrl) {
      // Open the blob URL in a new tab
      window.open(this.rawDocumentUrl, '_blank');
    } else if (this.document?.id) {
      // Fallback: open the API endpoint directly
      window.open(`http://localhost:8080/api/documents/${this.document.id}/image`, '_blank');
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

