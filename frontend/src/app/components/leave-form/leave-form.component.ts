import {Component, OnInit, OnDestroy} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, Router, RouterModule} from '@angular/router';
import {LeaveService} from '../../services/leave.service';
import {EmployeeService} from '../../services/employee.service';
import {AuthService} from '../../services/auth.service';
import {ToastService} from '../../services/toast.service';
import {Leave, LeaveBalance, BlockedDate} from '../../models/leave.model';
import {Employee} from '../../models/employee.model';
import {Subscription} from 'rxjs';

interface User {
  id?: number;
  userId?: number;
  email?: string;
  username?: string;
  roles?: string[];
  organizationId?: number;
  user?: { id?: number; userId?: number };
}

@Component({
  selector: 'app-leave-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './leave-form.component.html',
  styleUrls: ['./leave-form.component.css']
})
export class LeaveFormComponent implements OnInit, OnDestroy {
  leave: Leave = {
    employeeId: 0,
    leaveType: '',
    startDate: '',
    endDate: '',
    reason: ''
  };

  employees: Employee[] = [];
  leaveBalances: LeaveBalance[] = [];
  blockedDates: BlockedDate[] = [];
  selectedFile: File | null = null;
  selectedHolidayFormFile: File | null = null;

  leaveTypes = [
    {value: 'ANNUAL', label: 'Annual Leave'},
    {value: 'SICK', label: 'Sick Leave'},
    {value: 'CASUAL', label: 'Casual Leave'},
    {value: 'OTHER', label: 'Other Leave'}
  ];

  isEditMode = false;
  leaveId: number | null = null;
  error: string | null = null;
  success: string | null = null;
  loading = false;
  currentUser: User | null = null;
  isAdmin = false;
  today: string;

  private subscriptions: Subscription[] = [];

  constructor(
    private leaveService: LeaveService,
    private employeeService: EmployeeService,
    private authService: AuthService,
    private toastService: ToastService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    // Set today's date (kept for reference, but not used as min date restriction)
    const today = new Date();
    this.today = today.toISOString().split('T')[0];
  }

  ngOnInit(): void {
    this.currentUser = this.authService.getUser();
    const roles = this.currentUser?.roles || [];
    const isSuperAdmin = roles.includes('SUPER_ADMIN');
    this.isAdmin = roles.includes('ADMIN') || isSuperAdmin;

    // SUPER_ADMIN (CEO) cannot apply for leaves - redirect to leave list
    if (isSuperAdmin && !this.route.snapshot.params['id']) {
      this.toastService.info('As SUPER_ADMIN, you can review and manage leaves but cannot apply for leaves.');
      this.router.navigate(['/leaves']);
      return;
    }

    this.leaveId = this.route.snapshot.params['id'];
    this.isEditMode = !!this.leaveId;

    // If editing, load the leave first to get the employeeId, then load employees filtered to that employee
    if (this.isEditMode && this.leaveId) {
      this.loadLeave(this.leaveId);
      // Load employees after leave is loaded (will be filtered in loadEmployees)
      setTimeout(() => {
        this.loadEmployees();
      }, 100);
    } else {
      // For new leave, load all employees
      this.loadEmployees();
    }
  }

  loadEmployees(): void {
    const employeesSub = this.employeeService.getAllEmployees().subscribe({
      next: (data) => {
        // If in edit mode, filter employees to only show the employee who owns this leave
        if (this.isEditMode && this.leave.employeeId) {
          // Only show the employee who owns this leave
          const leaveOwner = data.find(emp => emp.id === this.leave.employeeId);
          this.employees = leaveOwner ? [leaveOwner] : [];
          console.log('Edit mode: Filtered employees to leave owner only:', {
            employeeId: this.leave.employeeId,
            employeeName: leaveOwner?.fullName
          });
        } else {
          this.employees = data;
          // For non-admin users, auto-select their employee
          if (!this.isAdmin && data.length > 0) {
            this.leave.employeeId = data[0].id!;
            this.loadLeaveBalances(this.leave.employeeId);
            this.loadBlockedDates(this.leave.employeeId);
          }
        }
      },
      error: (err) => {
        console.error('Error loading employees:', err);
        this.toastService.error('Failed to load employees');
      }
    });
    this.subscriptions.push(employeesSub);
  }

  onEmployeeChange(): void {
    if (this.leave.employeeId) {
      this.loadLeaveBalances(this.leave.employeeId);
      this.loadBlockedDates(this.leave.employeeId);
    }
  }

  loadLeaveBalances(employeeId: number): void {
    const balancesSub = this.leaveService.getLeaveBalances(employeeId).subscribe({
      next: (balances) => {
        this.leaveBalances = balances;
      },
      error: (err) => {
        console.error('Error loading leave balances:', err);
      }
    });
    this.subscriptions.push(balancesSub);
  }

  loadBlockedDates(employeeId: number): void {
    const datesSub = this.leaveService.getBlockedDates(employeeId).subscribe({
      next: (dates) => {
        this.blockedDates = dates;
      },
      error: (err) => {
        console.error('Error loading blocked dates:', err);
      }
    });
    this.subscriptions.push(datesSub);
  }

  getBalance(leaveType: string): number {
    const balance = this.leaveBalances.find(b => b.leaveType === leaveType);
    return balance ? balance.remainingLeaves : 0;
  }

  isDateBlocked(dateString: string): boolean {
    const date = new Date(dateString);
    return this.blockedDates.some(blocked => {
      const start = new Date(blocked.startDate);
      const end = new Date(blocked.endDate);
      return date >= start && date <= end;
    });
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      // Validate file type
      const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'application/pdf'];
      if (!allowedTypes.includes(file.type)) {
        this.toastService.error('Only JPG, PNG, and PDF files are allowed');
        event.target.value = '';
        return;
      }
      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.toastService.error('File size must be less than 5MB');
        event.target.value = '';
        return;
      }
      this.selectedFile = file;
    }
  }

  onHolidayFormSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      // Validate file type
      const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'application/pdf'];
      if (!allowedTypes.includes(file.type)) {
        this.toastService.error('Only JPG, PNG, and PDF files are allowed');
        event.target.value = '';
        return;
      }
      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.toastService.error('File size must be less than 5MB');
        event.target.value = '';
        return;
      }
      this.selectedHolidayFormFile = file;
    }
  }

  isHolidayFormRequired(): boolean {
    // Don't show in edit mode
    if (this.isEditMode) {
      return false;
    }

    // Need an employee selected
    if (!this.leave.employeeId || this.leave.employeeId === 0) {
      return false;
    }

    // Find the selected employee
    const selectedEmployee = this.employees.find(emp => emp.id === this.leave.employeeId);
    if (!selectedEmployee) {
      return false;
    }

    // Employee must have a userId (user account) for holiday form to be relevant
    if (!selectedEmployee.userId) {
      return false;
    }

    // If current user is ADMIN/SUPER_ADMIN
    if (this.isAdmin && this.currentUser) {
      // Try different possible properties for user ID
      const currentUserId = this.currentUser.id || 
                           this.currentUser.userId || 
                           (this.currentUser as any)?.user?.id ||
                           (this.currentUser as any)?.user?.userId;
      
      // Check if admin is applying for their own leave
      if (currentUserId != null && selectedEmployee.userId != null) {
        const currentId = Number(currentUserId);
        const selectedId = Number(selectedEmployee.userId);
        if (currentId === selectedId && !isNaN(currentId) && !isNaN(selectedId)) {
          // ADMIN applying for own leave - holiday form NOT required
          return false;
        }
      }
      
      // ADMIN selecting any other employee (staff member) - holiday form IS required
      // This covers the case where admin is applying leave for staff members
      return true;
    }

    // Regular USER applying for their own leave - holiday form IS required
    return true;
  }

  loadLeave(id: number): void {
    this.loading = true;
    const leaveSub = this.leaveService.getLeaveById(id).subscribe({
      next: (data) => {
        console.log('Loaded leave for editing:', { 
          leaveId: data.id, 
          employeeId: data.employeeId, 
          employeeName: data.employeeName 
        });
        
        this.leave = {
          ...data,
          startDate: this.formatDateForInput(data.startDate),
          endDate: this.formatDateForInput(data.endDate)
        };
        
        // Ensure employeeId is set correctly
        if (data.employeeId) {
          this.leave.employeeId = data.employeeId;
          // Load leave balances and blocked dates for this specific employee
          this.loadLeaveBalances(data.employeeId);
          this.loadBlockedDates(data.employeeId);
        }
        
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading leave:', err);
        this.toastService.error('Failed to load leave details');
        this.loading = false;
      }
    });
    this.subscriptions.push(leaveSub);
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

    // Check if dates are blocked
    if (this.isDateBlocked(this.leave.startDate) || this.isDateBlocked(this.leave.endDate)) {
      this.toastService.error('Selected dates are already taken or pending approval');
      return;
    }

    if (!this.leave.reason || this.leave.reason.trim() === '') {
      this.toastService.warning('Please provide a reason for leave');
      return;
    }

    const days = this.calculateDays();

    // Check balance
    const balance = this.leaveBalances.find(b => b.leaveType === this.leave.leaveType);
    if (balance && balance.remainingLeaves < days) {
      this.toastService.error(`Insufficient ${this.leave.leaveType} leave balance. Available: ${balance.remainingLeaves} days`);
      return;
    }

    // Check if SICK > 2 days requires certificate
    if (this.leave.leaveType === 'SICK' && days > 2 && !this.selectedFile) {
      this.toastService.error('Medical certificate is required for sick leave more than 2 days');
      return;
    }

    // Check if CASUAL > 1 day
    if (this.leave.leaveType === 'CASUAL' && days > 1) {
      this.toastService.error('Casual leave cannot be more than 1 day');
      return;
    }

    // Check if holiday form is required
    if (this.isHolidayFormRequired() && !this.selectedHolidayFormFile) {
      this.toastService.error('Holiday form is required for this leave application');
      return;
    }

    this.loading = true;

    if (this.isEditMode && this.leaveId) {
      // Update existing leave
      const updateSub = this.leaveService.updateLeave(this.leaveId, this.leave).subscribe({
        next: () => {
          this.toastService.success('Leave updated successfully!');
          this.loading = false;
          setTimeout(() => this.router.navigate(['/leaves']), 2000);
        },
        error: (err) => {
          const errorMsg = err.error?.error || err.error?.message || 'Failed to update leave. Please try again.';
          this.toastService.error(errorMsg);
          this.loading = false;
          console.error('Error updating leave:', err);
        }
      });
      this.subscriptions.push(updateSub);
    } else {
      // Create new leave with FormData
      const formData = new FormData();
      formData.append('employeeId', this.leave.employeeId.toString());
      formData.append('leaveType', this.leave.leaveType);
      formData.append('startDate', this.leave.startDate);
      formData.append('endDate', this.leave.endDate);
      formData.append('reason', this.leave.reason);

      if (this.selectedFile) {
        formData.append('medicalCertificate', this.selectedFile);
      }

      if (this.selectedHolidayFormFile) {
        formData.append('holidayForm', this.selectedHolidayFormFile);
      }

      const applySub = this.leaveService.applyLeave(formData).subscribe({
        next: () => {
          this.toastService.success('Leave applied successfully!');
          this.loading = false;
          setTimeout(() => this.router.navigate(['/leaves']), 2000);
        },
        error: (err) => {
          const errorMsg = err.error?.error || err.error?.message || 'Failed to apply leave. Please try again.';
          this.toastService.error(errorMsg);
          this.loading = false;
          console.error('Error applying leave:', err);
        }
      });
      this.subscriptions.push(applySub);
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

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.subscriptions = [];
  }
}


