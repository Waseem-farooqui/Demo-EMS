import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, Router, RouterModule} from '@angular/router';
import {LeaveService} from '../../services/leave.service';
import {EmployeeService} from '../../services/employee.service';
import {AuthService} from '../../services/auth.service';
import {ToastService} from '../../services/toast.service';
import {Leave} from '../../models/leave.model';
import {Employee} from '../../models/employee.model';

@Component({
  selector: 'app-leave-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './leave-form.component.html',
  styleUrls: ['./leave-form.component.css']
})
export class LeaveFormComponent implements OnInit {
  leave: Leave = {
    employeeId: 0,
    leaveType: '',
    startDate: '',
    endDate: '',
    reason: ''
  };

  employees: Employee[] = [];
  leaveTypes = [
    'Annual Leave',
    'Sick Leave',
    'Casual Leave',
    'Unpaid Leave',
    'Maternity/Paternity Leave',
    'Bereavement Leave'
  ];

  isEditMode = false;
  leaveId: number | null = null;
  error: string | null = null;
  success: string | null = null;
  loading = false;
  currentUser: any;
  isAdmin = false;

  constructor(
    private leaveService: LeaveService,
    private employeeService: EmployeeService,
    private authService: AuthService,
    private toastService: ToastService,
    private router: Router,
    private route: ActivatedRoute
  ) { }

  ngOnInit(): void {
    this.currentUser = this.authService.getUser();
    const roles = this.currentUser?.roles || [];
    const isSuperAdmin = roles.includes('SUPER_ADMIN');
    this.isAdmin = roles.includes('ADMIN') || isSuperAdmin;

    // SUPER_ADMIN (CEO) cannot apply for leaves - redirect to leave list
    if (isSuperAdmin && !this.route.snapshot.params['id']) {
      console.log('SUPER_ADMIN redirected from leave application - CEOs do not apply for leaves');
      this.toastService.info('As SUPER_ADMIN, you can review and manage leaves but cannot apply for leaves.');
      this.router.navigate(['/leaves']);
      return;
    }

    this.leaveId = this.route.snapshot.params['id'];
    this.isEditMode = !!this.leaveId;

    this.loadEmployees();

    if (this.isEditMode && this.leaveId) {
      this.loadLeave(this.leaveId);
    }
  }

  loadEmployees(): void {
    this.employeeService.getAllEmployees().subscribe({
      next: (data) => {
        this.employees = data;
        // For non-admin users, auto-select their employee
        if (!this.isAdmin && data.length > 0) {
          this.leave.employeeId = data[0].id!;
        }
      },
      error: (err) => {
        console.error('Error loading employees:', err);
        this.toastService.error('Failed to load employees');
      }
    });
  }

  loadLeave(id: number): void {
    this.loading = true;
    this.leaveService.getLeaveById(id).subscribe({
      next: (data) => {
        this.leave = {
          ...data,
          startDate: this.formatDateForInput(data.startDate),
          endDate: this.formatDateForInput(data.endDate)
        };
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading leave:', err);
        this.toastService.error('Failed to load leave details');
        this.loading = false;
      }
    });
  }

  formatDateForInput(dateString: string): string {
    const date = new Date(dateString);
    return date.toISOString().split('T')[0];
  }

  onSubmit(): void {
    this.error = null;
    this.success = null;

    // Validation
    if (!this.leave.employeeId) {
      this.toastService.warning('Please select an employee');
      return;
    }

    if (!this.leave.leaveType) {
      this.toastService.warning('Please select a leave type');
      return;
    }

    if (!this.leave.startDate || !this.leave.endDate) {
      this.toastService.warning('Please select start and end dates');
      return;
    }

    if (new Date(this.leave.startDate) > new Date(this.leave.endDate)) {
      this.toastService.warning('End date must be after start date');
      return;
    }

    if (!this.leave.reason || this.leave.reason.trim() === '') {
      this.toastService.warning('Please provide a reason for leave');
      return;
    }

    this.loading = true;

    if (this.isEditMode && this.leaveId) {
      // Update existing leave
      this.leaveService.updateLeave(this.leaveId, this.leave).subscribe({
        next: () => {
          this.toastService.success('Leave updated successfully!');
          this.loading = false;
          setTimeout(() => this.router.navigate(['/leaves']), 2000);
        },
        error: (err) => {
          const errorMsg = err.error?.message || 'Failed to update leave. Please try again.';
          this.toastService.error(errorMsg);
          this.loading = false;
          console.error('Error updating leave:', err);
        }
      });
    } else {
      // Create new leave
      this.leaveService.applyLeave(this.leave).subscribe({
        next: () => {
          this.toastService.success('Leave applied successfully!');
          this.loading = false;
          setTimeout(() => this.router.navigate(['/leaves']), 2000);
        },
        error: (err) => {
          const errorMsg = err.error?.message || 'Failed to apply leave. Please try again.';
          this.toastService.error(errorMsg);
          this.loading = false;
          console.error('Error applying leave:', err);
        }
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/leaves']);
  }

  calculateDays(): number {
    if (this.leave.startDate && this.leave.endDate) {
      const start = new Date(this.leave.startDate);
      const end = new Date(this.leave.endDate);
      const diffTime = Math.abs(end.getTime() - start.getTime());
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
      return diffDays;
    }
    return 0;
  }
}

