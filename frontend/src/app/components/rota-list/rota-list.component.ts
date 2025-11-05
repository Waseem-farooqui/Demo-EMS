import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {Router, RouterModule} from '@angular/router';
import {Rota, RotaSchedule, RotaService} from '../../services/rota.service';
import {AuthService} from '../../services/auth.service';
import {ToastService} from '../../services/toast.service';

@Component({
  selector: 'app-rota-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './rota-list.component.html',
  styleUrls: ['./rota-list.component.css']
})
export class RotaListComponent implements OnInit {
  rotas: Rota[] = [];
  selectedRota: Rota | null = null;
  schedules: RotaSchedule[] = [];
  loading = false;
  loadingSchedules = false;
  currentUser: any;
  isAdmin = false;
  scheduleDates: string[] = [];

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
    this.rotaService.getAllRotas().subscribe({
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
  }

  viewRota(rota: Rota): void {
    this.selectedRota = rota;
    this.loadSchedules(rota.id);
  }

  loadSchedules(rotaId: number): void {
    this.loadingSchedules = true;
    this.rotaService.getRotaSchedules(rotaId).subscribe({
      next: (data) => {
        this.schedules = data;

        // Extract dates from schedules
        if (data.length > 0) {
          const firstEmployee = data[0];
          this.scheduleDates = Object.keys(firstEmployee.schedules).sort();
        }

        this.loadingSchedules = false;
      },
      error: (err) => {
        this.toastService.error('Failed to load schedules');
        this.loadingSchedules = false;
      }
    });
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

    this.rotaService.deleteRota(rota.id).subscribe({
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
  }
}

