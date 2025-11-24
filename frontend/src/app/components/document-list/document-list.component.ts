import {Component, OnInit} from "@angular/core";
import {CommonModule} from '@angular/common';
import {ActivatedRoute, Router, RouterModule} from '@angular/router';
import {DocumentService} from '../../services/document.service';
import {AuthService} from '../../services/auth.service';
import {Document} from '../../models/document.model';

@Component({
  selector: 'app-document-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './document-list.component.html',
  styleUrls: ['./document-list.component.css']
})
export class DocumentListComponent implements OnInit {
  documents: Document[] = [];
  loading = false;
  error: string | null = null;
  currentUser: any;
  isAdmin = false;
  filterType = 'ALL';
  expiryFilter = 'all'; // New expiry filter
  selectedDocument: Document | null = null;

  constructor(
    private documentService: DocumentService,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute // Add ActivatedRoute to read query params
  ) { }

  ngOnInit(): void {
    this.currentUser = this.authService.getUser();
    const roles = this.currentUser?.roles || [];
    this.isAdmin = roles.includes('ADMIN') || roles.includes('SUPER_ADMIN');

    // Check for query parameters (from dashboard navigation)
    this.route.queryParams.subscribe(params => {
      if (params['expiryFilter']) {
        this.expiryFilter = params['expiryFilter'];
      }
    });

    this.loadDocuments();
  }

  loadDocuments(): void {
    this.loading = true;
    this.error = null;

    this.documentService.getAllDocuments().subscribe({
      next: (data) => {
        this.documents = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load documents. Please try again.';
        this.loading = false;
        console.error('Error loading documents:', err);
      }
    });
  }

  get filteredDocuments(): Document[] {
    let filtered = this.documents;

    // Apply document type filter
    if (this.filterType !== 'ALL') {
      filtered = filtered.filter(doc => doc.documentType === this.filterType);
    }

    // Apply expiry filter (from dashboard click)
    if (this.expiryFilter !== 'all') {
      filtered = filtered.filter(doc => {
        const days = doc.daysUntilExpiry;
        if (days === undefined || days === null) return false;

        switch(this.expiryFilter) {
          case 'expired':
            return days < 0;
          case 'expiring30':
            return days >= 0 && days <= 30;
          case 'expiring60':
            return days > 30 && days <= 60;
          default:
            return true;
        }
      });
    }

    return filtered;
  }

  filterByType(type: string): void {
    this.filterType = type;
    // Clear expiry filter when changing document type filter
    this.expiryFilter = 'all';
    // Clear query params
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {},
      queryParamsHandling: 'merge'
    });
  }

  clearExpiryFilter(): void {
    this.expiryFilter = 'all';
    // Clear query params
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { expiryFilter: null },
      queryParamsHandling: 'merge'
    });
  }

  getExpiryFilterLabel(): string {
    switch(this.expiryFilter) {
      case 'expired':
        return 'Showing Expired Documents';
      case 'expiring30':
        return 'Showing Documents Expiring in 30 Days';
      case 'expiring60':
        return 'Showing Documents Expiring in 31-60 Days';
      default:
        return '';
    }
  }

  viewDetails(document: Document): void {
    this.router.navigate(['/documents', document.id]);
  }

  closeDetails(): void {
    this.selectedDocument = null;
  }

  deleteDocument(document: Document): void {
    if (confirm(`Delete ${document.documentType} document for ${document.employeeName}?`)) {
      this.documentService.deleteDocument(document.id!).subscribe({
        next: () => {
          this.loadDocuments();
          if (this.selectedDocument?.id === document.id) {
            this.selectedDocument = null;
          }
        },
        error: (err) => {
          this.error = 'Failed to delete document. Please try again.';
          console.error('Error deleting document:', err);
        }
      });
    }
  }

  uploadNewDocument(): void {
    this.router.navigate(['/documents/upload']);
  }

  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-GB');
  }

  formatDateTime(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleString('en-GB', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getExpiryStatus(daysUntilExpiry?: number): string {
    if (!daysUntilExpiry) return '';
    if (daysUntilExpiry < 0) return 'expired';
    if (daysUntilExpiry <= 30) return 'critical';
    if (daysUntilExpiry <= 90) return 'warning';
    return 'valid';
  }

  getExpiryStatusText(daysUntilExpiry?: number): string {
    if (!daysUntilExpiry) return 'No expiry date';
    if (daysUntilExpiry < 0) return 'EXPIRED';
    if (daysUntilExpiry <= 30) return `${daysUntilExpiry} days left`;
    if (daysUntilExpiry <= 90) return `${daysUntilExpiry} days`;
    return `${daysUntilExpiry} days`;
  }
}

