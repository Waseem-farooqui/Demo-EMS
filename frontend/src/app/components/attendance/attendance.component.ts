import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {Router} from '@angular/router';
import {HttpClient} from '@angular/common/http';
import {AttendanceService} from '../../services/attendance.service';
import {AuthService} from '../../services/auth.service';
import {ToastService} from '../../services/toast.service';
import {Attendance, WorkLocationOption} from '../../models/attendance.model';
import {environment} from '../../../environments/environment';

@Component({
  selector: 'app-attendance',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './attendance.component.html',
  styleUrls: ['./attendance.component.css']
})
export class AttendanceComponent implements OnInit {
  currentUser: any;
  employeeId: number | null = null;
  currentStatus: Attendance | null = null;
  isCheckedIn = false;
  hasCheckedInToday = false; // Track if user has already checked in today (even after checkout)
  workLocations: WorkLocationOption[] = [];
  maxDate: string; // Maximum date (today) for date filters

  // Form data
  selectedLocation = '';
  checkInNotes = '';
  checkOutNotes = '';

  // Date range for history
  startDate = '';
  endDate = '';
  attendanceHistory: Attendance[] = [];

  loading = false;
  loadingEmployee = true;

  constructor(
    private attendanceService: AttendanceService,
    private authService: AuthService,
    private toastService: ToastService,
    private http: HttpClient,
    private router: Router
  ) {
    // Set max date to today in YYYY-MM-DD format
    const today = new Date();
    this.maxDate = today.toISOString().split('T')[0];
  }

  ngOnInit(): void {
    this.currentUser = this.authService.getUser();

    // SUPER_ADMIN (CEO) doesn't need to check in/out - redirect to dashboard
    const roles = this.currentUser?.roles || [];
    if (roles.includes('SUPER_ADMIN')) {
      this.toastService.info('As CEO, you do not need to check in/out');
      this.router.navigate(['/dashboard']);
      return;
    }

    this.loadEmployeeId();
  }

  loadEmployeeId(): void {
    const userEmail = this.currentUser?.email;
    if (!userEmail) {
      this.toastService.error('User email not found. Please login again.');
      this.loadingEmployee = false;
      return;
    }

    // Fetch employee by work email
    this.http.get<any[]>(`${environment.apiUrl}/employees`).subscribe({
      next: (employees) => {
        const employee = employees.find(emp => emp.workEmail === userEmail);
        if (employee) {
          this.employeeId = employee.id;
          this.loadWorkLocations();
          this.loadCurrentStatus(); // This will also check today's attendance
          this.initializeDateRange();
        } else {
          this.toastService.error('Employee profile not found. Please contact admin.');
        }
        this.loadingEmployee = false;
      },
      error: (err) => {
        console.error('Error loading employee:', err);
        this.toastService.error('Failed to load employee profile');
        this.loadingEmployee = false;
      }
    });
  }

  loadWorkLocations(): void {
    this.attendanceService.getWorkLocations().subscribe({
      next: (locations) => {
        this.workLocations = locations;
        if (locations.length > 0) {
          this.selectedLocation = locations[0].value;
        }
      },
      error: (err) => {
        console.error('Error loading locations:', err);
        this.toastService.error('Failed to load work locations');
      }
    });
  }

  loadCurrentStatus(): void {
    if (!this.employeeId) return;

    this.attendanceService.getCurrentStatus(this.employeeId).subscribe({
      next: (response) => {
        if (response.status === 'CHECKED_OUT') {
          this.isCheckedIn = false;
          this.currentStatus = null;
          // Check if user has already checked in today (even after checkout)
          this.checkTodayAttendance();
        } else {
          this.isCheckedIn = true;
          this.hasCheckedInToday = true; // If currently checked in, they've checked in today
          this.currentStatus = response;
        }
      },
      error: (err) => {
        console.error('Error loading status:', err);
        // Still check for today's attendance even if status check fails
        this.checkTodayAttendance();
      }
    });
  }

  checkTodayAttendance(): void {
    if (!this.employeeId) return;
    
    const today = new Date();
    const todayStr = this.formatDate(today);
    
    // Load today's attendance to check if user has already checked in
    this.attendanceService.getAttendanceByDateRange(
      this.employeeId,
      todayStr,
      todayStr
    ).subscribe({
      next: (history) => {
        // If there's any record for today, user has already checked in
        this.hasCheckedInToday = history.length > 0;
      },
      error: (err) => {
        console.error('Error checking today attendance:', err);
        // Don't show error to user, just log it
      }
    });
  }

  initializeDateRange(): void {
    const today = new Date();
    const firstDayOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);

    this.endDate = this.formatDate(today);
    this.startDate = this.formatDate(firstDayOfMonth);

    this.loadAttendanceHistory();
  }

  checkIn(): void {
    if (!this.selectedLocation) {
      this.toastService.warning('Please select a work location');
      return;
    }

    if (!this.employeeId) {
      this.toastService.error('Employee ID not found. Please refresh the page.');
      return;
    }

    this.loading = true;

    this.attendanceService.checkIn(
      this.employeeId,
      this.selectedLocation,
      this.checkInNotes
    ).subscribe({
      next: (attendance) => {
        this.isCheckedIn = true;
        this.hasCheckedInToday = true; // Mark as checked in today
        this.currentStatus = attendance;
        this.toastService.success('Checked in successfully!');
        this.checkInNotes = '';
        this.loading = false;
        this.loadAttendanceHistory();
      },
      error: (err) => {
        const errorMsg = err.error?.error || 'Failed to check in';
        this.toastService.error(errorMsg);
        this.loading = false;
      }
    });
  }

  checkOut(): void {
    if (!this.employeeId) {
      this.toastService.error('Employee ID not found. Please refresh the page.');
      return;
    }

    this.loading = true;

    this.attendanceService.checkOut(
      this.employeeId,
      this.checkOutNotes
    ).subscribe({
      next: (attendance) => {
        this.isCheckedIn = false;
        this.currentStatus = null;
        // Keep hasCheckedInToday = true even after checkout (user can't check in again today)
        const hours = attendance.hoursWorked?.toFixed(2) || 0;
        this.toastService.success(`Checked out successfully! Total hours: ${hours}`);
        this.checkOutNotes = '';
        this.loading = false;
        this.loadAttendanceHistory();
      },
      error: (err) => {
        const errorMsg = err.error?.error || 'Failed to check out';
        this.toastService.error(errorMsg);
        this.loading = false;
      }
    });
  }

  loadAttendanceHistory(): void {
    if (!this.startDate || !this.endDate || !this.employeeId) return;

    this.attendanceService.getAttendanceByDateRange(
      this.employeeId,
      this.startDate,
      this.endDate
    ).subscribe({
      next: (history) => {
        this.attendanceHistory = history;
        // Check if user has already checked in today (even after checkout)
        this.checkIfCheckedInToday(history);
      },
      error: (err) => {
        console.error('Error loading history:', err);
        this.toastService.error('Failed to load attendance history');
      }
    });
  }

  checkIfCheckedInToday(history: Attendance[]): void {
    const today = new Date();
    const todayStr = this.formatDate(today);
    
    // Check if there's any attendance record for today
    const todayRecord = history.find(record => {
      const recordDate = new Date(record.workDate);
      const recordDateStr = this.formatDate(recordDate);
      return recordDateStr === todayStr;
    });
    
    // If there's a record for today, user has already checked in (regardless of checkout status)
    this.hasCheckedInToday = !!todayRecord;
  }

  getElapsedTime(): string {
    if (!this.currentStatus?.checkInTime) return '0h 0m';

    const checkIn = new Date(this.currentStatus.checkInTime);
    const now = new Date();
    const diff = now.getTime() - checkIn.getTime();

    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));

    return `${hours}h ${minutes}m`;
  }

  formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  formatDateTime(dateTime: string): string {
    return new Date(dateTime).toLocaleString();
  }

  formatTime(dateTime: string): string {
    return new Date(dateTime).toLocaleTimeString();
  }
}

