import {Component, OnInit, OnDestroy, HostListener, ElementRef, ViewChild} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {Router, RouterModule} from '@angular/router';
import {Subject, debounceTime, distinctUntilChanged, switchMap, takeUntil} from 'rxjs';
import {SearchService, SearchResults} from '../../services/search.service';

@Component({
  selector: 'app-global-search',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './global-search.component.html',
  styleUrls: ['./global-search.component.css']
})
export class GlobalSearchComponent implements OnInit, OnDestroy {
  @ViewChild('searchInput', {static: false}) searchInput!: ElementRef<HTMLInputElement>;
  @ViewChild('resultsContainer', {static: false}) resultsContainer!: ElementRef<HTMLDivElement>;

  searchQuery = '';
  isOpen = false;
  isLoading = false;
  searchResults: SearchResults = {
    employees: [],
    documents: [],
    leaves: [],
    rotas: []
  };
  hasResults = false;
  activeTab: 'all' | 'employees' | 'documents' | 'leaves' | 'rotas' = 'all';

  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  constructor(
    private searchService: SearchService,
    private router: Router,
    private elementRef: ElementRef
  ) {}

  ngOnInit(): void {
    // Debounce search input
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(query => {
        this.isLoading = true;
        if (!query || query.trim().length < 2) {
          this.searchResults = {employees: [], documents: [], leaves: [], rotas: []};
          this.isLoading = false;
          this.hasResults = false;
          return [];
        }
        return this.searchService.search(query.trim());
      }),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (results) => {
        this.searchResults = results || {employees: [], documents: [], leaves: [], rotas: []};
        this.hasResults = this.getTotalResults() > 0;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Search error:', err);
        this.isLoading = false;
        this.hasResults = false;
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  @HostListener('document:click', ['$event'])
  onClickOutside(event: Event): void {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      this.closeSearch();
    }
  }

  @HostListener('keydown.escape')
  onEscapeKey(): void {
    this.closeSearch();
  }

  @HostListener('document:keydown', ['$event'])
  onKeyDown(event: KeyboardEvent): void {
    // Ctrl+K or Cmd+K to open search
    if ((event.ctrlKey || event.metaKey) && event.key === 'k') {
      event.preventDefault();
      if (!this.isOpen) {
        this.openSearch();
      }
    }
  }

  openSearch(): void {
    if (!this.isOpen) {
      this.isOpen = true;
    }
    setTimeout(() => {
      if (this.searchInput?.nativeElement) {
        this.searchInput.nativeElement.focus();
      }
    }, 100);
  }

  closeSearch(): void {
    this.isOpen = false;
    this.searchQuery = '';
    this.searchResults = {employees: [], documents: [], leaves: [], rotas: []};
    this.hasResults = false;
    this.activeTab = 'all';
  }

  onSearchInput(event: Event): void {
    const query = (event.target as HTMLInputElement).value;
    this.searchQuery = query;
    if (!this.isOpen && query.trim().length > 0) {
      this.isOpen = true;
    }
    if (query.trim().length >= 2) {
      this.searchSubject.next(query);
    } else {
      this.searchResults = {employees: [], documents: [], leaves: [], rotas: []};
      this.hasResults = false;
      this.isLoading = false;
    }
  }

  getTotalResults(): number {
    return this.searchResults.employees.length +
           this.searchResults.documents.length +
           this.searchResults.leaves.length +
           this.searchResults.rotas.length;
  }

  navigateToEmployee(employeeId: number): void {
    // Navigate to employees list - the list component can show details in modal
    this.router.navigate(['/employees'], { queryParams: { employeeId: employeeId } });
    this.closeSearch();
  }

  navigateToDocument(documentId: number): void {
    // Navigate to document detail page
    this.router.navigate(['/documents', documentId]);
    this.closeSearch();
  }

  navigateToLeave(leaveId: number): void {
    // Navigate to leaves list - can add query param to highlight specific leave
    this.router.navigate(['/leaves'], { queryParams: { leaveId: leaveId } });
    this.closeSearch();
  }

  navigateToRota(rotaId: number): void {
    // Navigate to rota list - can add query param to highlight specific rota
    this.router.navigate(['/rota'], { queryParams: { rotaId: rotaId } });
    this.closeSearch();
  }

  setActiveTab(tab: 'all' | 'employees' | 'documents' | 'leaves' | 'rotas'): void {
    this.activeTab = tab;
  }

  getFilteredResults() {
    if (this.activeTab === 'all') {
      return this.searchResults;
    }
    return {
      employees: this.activeTab === 'employees' ? this.searchResults.employees : [],
      documents: this.activeTab === 'documents' ? this.searchResults.documents : [],
      leaves: this.activeTab === 'leaves' ? this.searchResults.leaves : [],
      rotas: this.activeTab === 'rotas' ? this.searchResults.rotas : []
    };
  }
}

