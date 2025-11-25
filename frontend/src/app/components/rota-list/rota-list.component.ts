import {Component, OnInit, OnDestroy} from '@angular/core';
import {CommonModule} from '@angular/common';
import {Router, RouterModule} from '@angular/router';
import {DayScheduleEntry, Rota, RotaSchedule, RotaService} from '../../services/rota.service';
import {AuthService} from '../../services/auth.service';
import {ToastService} from '../../services/toast.service';
import {Subscription} from 'rxjs';

interface User {
  id?: number;
  email?: string;
  username?: string;
  roles?: string[];
  organizationId?: number;
}

@Component({
  selector: 'app-rota-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './rota-list.component.html',
  styleUrls: ['./rota-list.component.css']
})
export class RotaListComponent implements OnInit, OnDestroy {
  rotas: Rota[] = [];
  selectedRota: Rota | null = null;
  schedules: RotaSchedule[] = [];
  loading = false;
  loadingSchedules = false;
  currentUser: User | null = null;
  isAdmin = false;
  scheduleDates: string[] = [];

  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  usePagination = true;

  private subscriptions: Subscription[] = [];

  constructor(
    private rotaService: RotaService,
    private authService: AuthService,
    private toastService: ToastService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getUser();
    const roles = this.currentUser?.roles || [];
    this.isAdmin = roles.includes('ADMIN') || roles.includes('SUPER_ADMIN');

    this.loadRotas();
  }

  loadRotas(): void {
    this.loading = true;
    
    if (this.usePagination) {
      this.loadRotasPaginated();
    } else {
      const rotasSub = this.rotaService.getAllRotas().subscribe({
        next: (data) => {
          this.rotas = data;
          this.loading = false;

          // Auto-select first ROTA if available
          if (this.rotas.length > 0 && !this.selectedRota) {
            this.viewRota(this.rotas[0]);
          }
        },
        error: (err) => {
          this.toastService.error('Failed to load ROTAs');
          this.loading = false;
        }
      });
      this.subscriptions.push(rotasSub);
    }
  }

  loadRotasPaginated(): void {
    const rotasPaginatedSub = this.rotaService.getAllRotasPaginated(this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        this.rotas = response.content;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.loading = false;

        // Auto-select first ROTA if available
        if (this.rotas.length > 0 && !this.selectedRota) {
          this.viewRota(this.rotas[0]);
        }
      },
      error: (err) => {
        this.toastService.error('Failed to load ROTAs');
        this.loading = false;
      }
    });
    this.subscriptions.push(rotasPaginatedSub);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadRotasPaginated();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadRotasPaginated();
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

  viewRota(rota: Rota): void {
    this.selectedRota = rota;
    this.loadSchedules(rota.id);
  }

  loadSchedules(rotaId: number): void {
    // Prevent multiple simultaneous loads
    if (this.loadingSchedules) {
      console.log('Already loading schedules, skipping...');
      return;
    }
    
    this.loadingSchedules = true;
    this.schedules = [];
    this.scheduleDates = [];
    
    console.log('Loading schedules for ROTA ID:', rotaId);
    
    // Add timeout to prevent infinite loading
    let timeout: any = setTimeout(() => {
      if (this.loadingSchedules) {
        console.error('Timeout loading schedules for ROTA:', rotaId);
        this.toastService.error('Timeout loading schedules. Please try again.');
        this.loadingSchedules = false;
      }
    }, 30000); // 30 second timeout
    
    const schedulesSub = this.rotaService.getRotaSchedules(rotaId).subscribe({
      next: (data) => {
        console.log('Received schedules data:', data);
        console.log('Data type:', typeof data, 'Is array:', Array.isArray(data));
        
        // Backend returns flat list of RotaScheduleEntryDTO
        // Transform to grouped format expected by template
        if (!data) {
          console.warn('Received null or undefined data');
          clearTimeout(timeout);
          this.schedules = [];
          this.scheduleDates = [];
          this.loadingSchedules = false;
          return;
        }

        if (!Array.isArray(data)) {
          console.error('Expected array but received:', typeof data, data);
          clearTimeout(timeout);
          this.toastService.error('Invalid schedule data format');
          this.schedules = [];
          this.scheduleDates = [];
          this.loadingSchedules = false;
          return;
        }

        if (data.length === 0) {
          console.log('No schedules found for ROTA:', rotaId);
          clearTimeout(timeout);
          this.schedules = [];
          this.scheduleDates = [];
          this.loadingSchedules = false;
          return;
        }

        try {
          // Group schedules by employee
          const employeeMap = new Map<number, any>();
          const dateSet = new Set<string>();

          for (const entry of data) {
            // Validate entry structure
            if (!entry || typeof entry !== 'object') {
              console.warn('Invalid schedule entry:', entry);
              continue;
            }

            // Extract date - LocalDate is serialized as "YYYY-MM-DD" (no time component)
            let dateStr: string | null = null;
            if (entry.scheduleDate) {
              // Handle both "YYYY-MM-DD" and "YYYY-MM-DDTHH:mm:ss" formats
              dateStr = String(entry.scheduleDate).split('T')[0];
              // Validate date format (should be YYYY-MM-DD)
              if (!/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) {
                console.warn('Invalid date format:', entry.scheduleDate, 'extracted:', dateStr);
                continue;
              }
            }
            
            if (!dateStr) {
              console.warn('Missing or invalid scheduleDate in entry:', entry);
              continue;
            }

            dateSet.add(dateStr);

            const employeeId = entry.employeeId;
            if (!employeeId && employeeId !== 0) {
              console.warn('Missing employeeId in entry:', entry);
              continue;
            }

            if (!employeeMap.has(employeeId)) {
              employeeMap.set(employeeId, {
                employeeId: employeeId,
                employeeName: entry.employeeName || 'Unknown',
                schedules: {} as Record<string, DayScheduleEntry | undefined>
              });
            }

            const employee = employeeMap.get(employeeId);
            if (!employee) {
              console.warn('Failed to get employee from map:', employeeId);
              continue;
            }
            
            // Format time - LocalTime is serialized as "HH:mm:ss" or "HH:mm:ss.SSS"
            let startTimeStr: string | null = null;
            let endTimeStr: string | null = null;
            
            if (entry.startTime) {
              const timeStr = String(entry.startTime);
              // Extract just HH:mm from "HH:mm:ss" or "HH:mm:ss.SSS"
              startTimeStr = timeStr.substring(0, 5);
            }
            
            if (entry.endTime) {
              const timeStr = String(entry.endTime);
              // Extract just HH:mm from "HH:mm:ss" or "HH:mm:ss.SSS"
              endTimeStr = timeStr.substring(0, 5);
            }
            
            employee.schedules[dateStr] = {
              dayOfWeek: entry.dayOfWeek || '',
              duty: entry.duty || 'OFF',
              startTime: startTimeStr,
              endTime: endTimeStr,
              isOffDay: entry.isOffDay || false
            };
          }

          // Convert map to array and sort dates
          this.schedules = Array.from(employeeMap.values());
          this.scheduleDates = Array.from(dateSet).sort();
          
          console.log('Successfully transformed schedules:', {
            employeeCount: this.schedules.length,
            dateCount: this.scheduleDates.length,
            employees: this.schedules.map(e => ({ id: e.employeeId, name: e.employeeName, scheduleCount: e.schedules ? Object.keys(e.schedules).length : 0 }))
          });
        } catch (error) {
          console.error('Error transforming schedules:', error);
          console.error('Error stack:', error instanceof Error ? error.stack : 'No stack trace');
          this.toastService.error('Error processing schedule data: ' + (error instanceof Error ? error.message : String(error)));
          this.schedules = [];
          this.scheduleDates = [];
        }

        clearTimeout(timeout);
        this.loadingSchedules = false;
      },
      error: (err) => {
        clearTimeout(timeout);
        console.error('Error loading schedules:', err);
        
        // Check for specific error types
        if (err.status === 403) {
          this.toastService.error('You do not have permission to view schedules');
        } else if (err.status === 404) {
          this.toastService.error('ROTA not found');
        } else if (err.status === 0) {
          this.toastService.error('Network error. Please check your connection.');
        } else {
          const errorMsg = err.error?.error || err.error?.message || err.message || 'Failed to load schedules';
          this.toastService.error(errorMsg);
        }
        
        this.loadingSchedules = false;
        this.schedules = [];
        this.scheduleDates = [];
      }
    });
    this.subscriptions.push(schedulesSub);
  }

  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }

  formatDateWithDay(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
  }

  getDutyClass(duty: string | undefined): string {
    if (!duty) return 'duty-off';
    if (duty.match(/(OFF|Leave|Holiday)/i)) return 'duty-off';
    if (duty.toLowerCase().includes('set')) return 'duty-setup';
    return 'duty-work';
  }

  uploadNewRota(): void {
    this.router.navigate(['/rota/upload']);
  }

  deleteRota(rota: Rota, event: Event): void {
    event.stopPropagation(); // Prevent triggering viewRota

    if (!confirm(`Are you sure you want to delete the ROTA for ${rota.hotelName} - ${rota.department}?\n\nThis will delete all ${rota.totalEmployees} employee schedules. This action cannot be undone.`)) {
      return;
    }

    this.loading = true;
    this.toastService.info('Deleting ROTA...');

    const deleteSub = this.rotaService.deleteRota(rota.id).subscribe({
      next: () => {
        this.toastService.success('ROTA deleted successfully');

        // If deleted rota was selected, clear selection
        if (this.selectedRota?.id === rota.id) {
          this.selectedRota = null;
          this.schedules = [];
        }

        // Reload rotas list
        this.loadRotas();
      },
      error: (err) => {
        const errorMsg = err.error?.error || 'Failed to delete ROTA';
        this.toastService.error(errorMsg);
        this.loading = false;
      }
    });
    this.subscriptions.push(deleteSub);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.subscriptions = [];
  }
}

