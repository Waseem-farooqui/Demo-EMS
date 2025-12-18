import {Component, OnInit, OnDestroy} from "@angular/core";
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, Router, RouterModule} from '@angular/router';
import {DocumentService} from '../../services/document.service';
import {AuthService} from '../../services/auth.service';
import {Document} from '../../models/document.model';
import {Subscription} from 'rxjs';

@Component({
  selector: 'app-document-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './document-list.component.html',
  styleUrls: ['./document-list.component.css']
})
export class DocumentListComponent implements OnInit, OnDestroy {
  documents: Document[] = [];
  filteredDocuments: Document[] = [];
  allDocuments: Document[] = []; // Store all documents for filtering
  loading = false;
  error: string | null = null;
  currentUser: any;
  isAdmin = false;
  filterType = 'ALL';
  expiryFilter = 'all'; // New expiry filter
  selectedDocument: Document | null = null;

  // Search and Filter
  globalSearchQuery = '';
  columnFilters = {
    employeeName: '',
    documentType: '',
    documentNumber: '',
    expiryDate: ''
  };

  // Sorting
  sortColumn: string = '';
  sortDirection: 'asc' | 'desc' = 'asc';

  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  usePagination = true;
  documentTypeFilters = [
    { value: 'ALL', label: 'All Documents' },
    { value: 'PASSPORT', label: 'Passports' },
    { value: 'VISA', label: 'Visas' },
    { value: 'BRP', label: 'BRP' },
    { value: 'CONTRACT', label: 'Contracts' },
    { value: 'RESUME', label: 'Resumes' },
    { value: 'SHARE_CODE', label: 'Share Codes' },
    { value: 'PROOF_OF_ADDRESS', label: 'Proof of Address' },
    { value: 'REGISTRATION_FORM', label: 'Registration Forms' },
    { value: 'CERTIFICATE', label: 'Certificates' },
    { value: 'PROFESSIONAL_CERTIFICATE', label: 'Professional Certificates' },
    { value: 'TERM_LETTER', label: 'Term Letters' },
    { value: 'NATIONAL_INSURANCE', label: 'National Insurance' },
    { value: 'BANK_STATEMENT', label: 'Bank Statements' },
    { value: 'OTHERS', label: 'Others' }
  ];

  private readonly nonExpiryDocumentTypes = new Set([
    'CONTRACT',
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
    BRP: 'BRP',
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

  private subscriptions: Subscription[] = [];

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
    const queryParamsSub = this.route.queryParams.subscribe(params => {
      if (params['expiryFilter']) {
        this.expiryFilter = params['expiryFilter'];
      }
    });
    this.subscriptions.push(queryParamsSub);

    this.loadDocuments();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.subscriptions = [];
  }

  loadDocuments(): void {
    this.loading = true;
    this.error = null;

    if (this.usePagination) {
      this.loadDocumentsPaginated();
    } else {
      this.documentService.getAllDocuments().subscribe({
        next: (data) => {
          this.allDocuments = data;
          this.documents = data;
          this.applyFilters();
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Failed to load documents. Please try again.';
          this.loading = false;
          console.error('Error loading documents:', err);
        }
      });
    }
  }

  loadDocumentsPaginated(): void {
    this.documentService.getAllDocumentsPaginated(this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        this.documents = response.content;
        this.allDocuments = response.content;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        // Apply filters to current page
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load documents. Please try again.';
        this.loading = false;
        console.error('Error loading documents:', err);
      }
    });
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadDocumentsPaginated();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadDocumentsPaginated();
  }

  get pageNumbers(): number[] {
    const pages: number[] = [];
    const maxPages = Math.min(this.totalPages, 10);
    const startPage = Math.max(0, Math.min(this.currentPage - 4, this.totalPages - maxPages));
    
    for (let i = startPage; i < Math.min(startPage + maxPages, this.totalPages); i++) {
      pages.push(i);
    }
    return pages;
  }

  Math = Math;

  onGlobalSearchChange(): void {
    this.applyFilters();
  }

  onColumnFilterChange(): void {
    this.applyFilters();
  }

  onFilterChange(): void {
    this.applyFilters();
  }

  clearFilters(): void {
    this.globalSearchQuery = '';
    this.filterType = 'ALL';
    this.expiryFilter = 'all';
    this.columnFilters = {
      employeeName: '',
      documentType: '',
      documentNumber: '',
      expiryDate: ''
    };
    this.sortColumn = '';
    this.sortDirection = 'asc';
    this.applyFilters();
  }

  applyFilters(): void {
    let filtered = this.usePagination ? [...this.documents] : [...this.allDocuments];

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

    // Apply global search filter
    if (this.globalSearchQuery.trim()) {
      const query = this.globalSearchQuery.toLowerCase().trim();
      filtered = filtered.filter(doc => {
        const employeeName = (doc.employeeName || '').toLowerCase();
        const documentType = (doc.documentType || '').toLowerCase();
        const documentNumber = (doc.documentNumber || '').toLowerCase();
        const fileName = (doc.fileName || '').toLowerCase();
        const lastViewedBy = (doc.lastViewedBy || '').toLowerCase();
        
        return employeeName.includes(query) ||
               documentType.includes(query) ||
               documentNumber.includes(query) ||
               fileName.includes(query) ||
               lastViewedBy.includes(query);
      });
    }

    // Apply column-specific filters
    if (this.columnFilters.employeeName) {
      const query = this.columnFilters.employeeName.toLowerCase().trim();
      filtered = filtered.filter(doc => 
        (doc.employeeName || '').toLowerCase().includes(query)
      );
    }

    if (this.columnFilters.documentType) {
      filtered = filtered.filter(doc => 
        doc.documentType === this.columnFilters.documentType
      );
    }

    if (this.columnFilters.documentNumber) {
      const query = this.columnFilters.documentNumber.toLowerCase().trim();
      filtered = filtered.filter(doc => 
        (doc.documentNumber || '').toLowerCase().includes(query)
      );
    }

    // Apply sorting
    filtered = this.sortDocuments(filtered);

    this.filteredDocuments = filtered;
  }

  sortDocuments(documents: Document[]): Document[] {
    if (!this.sortColumn) {
      return documents;
    }

    return [...documents].sort((a, b) => {
      let aValue: any;
      let bValue: any;

      switch (this.sortColumn) {
        case 'employeeName':
          aValue = (a.employeeName || '').toLowerCase();
          bValue = (b.employeeName || '').toLowerCase();
          break;
        case 'documentType':
          aValue = (a.documentType || '').toLowerCase();
          bValue = (b.documentType || '').toLowerCase();
          break;
        case 'documentNumber':
          aValue = (a.documentNumber || '').toLowerCase();
          bValue = (b.documentNumber || '').toLowerCase();
          break;
        case 'expiryDate':
          aValue = a.expiryDate ? new Date(a.expiryDate).getTime() : 0;
          bValue = b.expiryDate ? new Date(b.expiryDate).getTime() : 0;
          break;
        case 'uploadedDate':
          aValue = a.uploadedDate ? new Date(a.uploadedDate).getTime() : 0;
          bValue = b.uploadedDate ? new Date(b.uploadedDate).getTime() : 0;
          break;
        case 'daysUntilExpiry':
          aValue = a.daysUntilExpiry ?? 999999;
          bValue = b.daysUntilExpiry ?? 999999;
          break;
        case 'lastViewedAt':
          aValue = a.lastViewedAt ? new Date(a.lastViewedAt).getTime() : 0;
          bValue = b.lastViewedAt ? new Date(b.lastViewedAt).getTime() : 0;
          break;
        case 'lastViewedBy':
          aValue = (a.lastViewedBy || '').toLowerCase();
          bValue = (b.lastViewedBy || '').toLowerCase();
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
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.applyFilters();
  }

  getSortIcon(column: string): string {
    if (this.sortColumn !== column) {
      return '⇅';
    }
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  filterByType(type: string): void {
    this.currentPage = 0; // Reset to first page when filter changes
    this.filterType = type;
    // Clear expiry filter when changing document type filter
    this.expiryFilter = 'all';
    // Clear query params
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {},
      queryParamsHandling: 'merge'
    });
    // Reload if using pagination
    if (this.usePagination) {
      this.loadDocumentsPaginated();
    } else {
      this.applyFilters();
    }
  }

  clearExpiryFilter(): void {
    this.currentPage = 0; // Reset to first page when filter changes
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
    if (confirm(`Delete ${this.getDocumentTypeLabel(document.documentType)} document for ${document.employeeName}?`)) {
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

  shouldShowExpiryBadge(doc: Document): boolean {
    if (!doc) {
      return false;
    }
    if (!doc.documentType) {
      return false;
    }
    return !this.nonExpiryDocumentTypes.has(doc.documentType.toUpperCase());
  }

  getDocumentTypeLabel(type?: string): string {
    if (!type) {
      return 'Document';
    }
    const upper = type.toUpperCase();
    return this.documentTypeLabelMap[upper] ?? upper.replace(/_/g, ' ');
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

