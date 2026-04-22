import {Component, OnInit, OnDestroy} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, Router, RouterModule} from '@angular/router';
import {LeaveService} from '../../services/leave.service';
import {EmployeeService} from '../../services/employee.service';
import {AuthService} from '../../services/auth.service';
import {ToastService} from '../../services/toast.service';
import {AttendanceService} from '../../services/attendance.service';
import {Leave, LeaveBalance, BlockedDate, OnBehalfCumulativeLeaveSummary} from '../../models/leave.model';
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
  onBehalfCumulativeSummary: OnBehalfCumulativeLeaveSummary | null = null;
  blockedDates: BlockedDate[] = [];
  selectedFile: File | null = null;
  selectedHolidayFormFile: File | null = null;
  currentlyWorkingOnly = false;
  currentlyWorkingEmployeeIds = new Set<number>();

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
    private attendanceService: AttendanceService,
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
      this.prefillEmployeeFromQueryParams();
    }
  }

  prefillEmployeeFromQueryParams(): void {
    const queryParamsSub = this.route.queryParams.subscribe(params => {
      const employeeIdParam = params['employeeId'];
      if (!employeeIdParam || this.isEditMode || !this.isAdmin) {
        return;
      }

      const employeeId = Number(employeeIdParam);
      if (!isNaN(employeeId) && employeeId > 0) {
        this.leave.employeeId = employeeId;
        this.onEmployeeChange();
      }
    });
    this.subscriptions.push(queryParamsSub);
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
          if (this.isAdmin && this.leave.employeeId) {
            this.loadOnBehalfCumulativeSummary(this.leave.employeeId);
          }
        } else {
          this.employees = data;
          if (this.isAdmin) {
            this.loadCurrentlyWorkingEmployees();
            if (this.leave.employeeId && this.leave.employeeId > 0) {
              this.onEmployeeChange();
            }
          }
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
      const selectedEmployee = this.employees.find(emp => emp.id === this.leave.employeeId);
      const currentUserId = this.currentUser?.id ?? this.currentUser?.userId ?? this.currentUser?.user?.id;
      const applyingOnBehalf = !!(this.isAdmin && selectedEmployee?.userId && currentUserId && selectedEmployee.userId !== currentUserId);

      // Admin/Super Admin applying on behalf: force annual leave rule.
      if (applyingOnBehalf) {
        this.leave.leaveType = 'ANNUAL';
      }

      if (this.isAdmin && this.currentlyWorkingOnly && !this.isEmployeeCurrentlyWorking(this.leave.employeeId)) {
        this.toastService.error('Selected employee is not currently working');
        this.leave.employeeId = 0;
        this.onBehalfCumulativeSummary = null;
        return;
      }
      this.loadLeaveBalances(this.leave.employeeId);
      this.loadBlockedDates(this.leave.employeeId);
      this.loadOnBehalfCumulativeSummary(this.leave.employeeId);
    } else {
      this.onBehalfCumulativeSummary = null;
    }
  }

  loadCurrentlyWorkingEmployees(): void {
    const activeTodaySub = this.attendanceService.getTodayActiveCheckIns().subscribe({
      next: (activeCheckIns) => {
        this.currentlyWorkingEmployeeIds = new Set(
          activeCheckIns
            .map(att => att.employeeId)
            .filter((id): id is number => id !== undefined && id !== null)
        );
      },
      error: (err) => {
        console.error('Error loading currently working employees:', err);
        this.currentlyWorkingEmployeeIds = new Set<number>();
      }
    });
    this.subscriptions.push(activeTodaySub);
  }

  get visibleEmployees(): Employee[] {
    if (!this.isAdmin || !this.currentlyWorkingOnly) {
      return this.employees;
    }
    return this.employees.filter(emp => this.isEmployeeCurrentlyWorking(emp.id));
  }

  /** Options for the leave-type control (per-type balances, or single cumulative row when applying on behalf). */
  get leaveTypeSelectOptions(): { value: string; label: string }[] {
    if (this.isApplyingOnBehalf()) {
      const rem = this.onBehalfCumulativeSummary?.remainingDaysUnderCap;
      const remStr = rem != null ? String(rem) : '—';
      return [{
        value: 'ANNUAL',
        label: `Annual Leave (${remStr} days remaining under cumulative cap)`
      }];
    }
    return this.leaveTypes.map(t => ({
      value: t.value,
      label: `${t.label} (${this.getBalance(t.value)} days remaining)`
    }));
  }

  isEmployeeCurrentlyWorking(employeeId: number | undefined): boolean {
    if (!employeeId) {
      return false;
    }
    return this.currentlyWorkingEmployeeIds.has(employeeId);
  }

  onWorkingFilterToggle(): void {
    if (!this.currentlyWorkingOnly) {
      return;
    }
    // Refresh active list when enabling this filter.
    this.loadCurrentlyWorkingEmployees();
    if (this.leave.employeeId && !this.isEmployeeCurrentlyWorking(this.leave.employeeId)) {
      this.leave.employeeId = 0;
      this.onBehalfCumulativeSummary = null;
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

  loadOnBehalfCumulativeSummary(employeeId: number): void {
    if (!this.isApplyingOnBehalf()) {
      this.onBehalfCumulativeSummary = null;
      return;
    }
    const cumulativeSub = this.leaveService.getOnBehalfCumulativeLeaveSummary(employeeId).subscribe({
      next: (summary) => {
        this.onBehalfCumulativeSummary = summary;
      },
      error: (err) => {
        console.error('Error loading on-behalf cumulative leave summary:', err);
        this.onBehalfCumulativeSummary = null;
      }
    });
    this.subscriptions.push(cumulativeSub);
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
    if (days > 28) {
      this.toastService.error('Leave days (excluding holidays/weekends) cannot exceed 28 days');
      return;
    }

    // Check balance (on-behalf uses cumulative approved days cap, not per-type buckets)
    if (this.isApplyingOnBehalf()) {
      const summary = this.onBehalfCumulativeSummary;
      if (summary && summary.approvedDaysThisFinancialYear + days > summary.cumulativeCap) {
        this.toastService.error(
          `Cannot apply: cumulative approved leave for this financial year would exceed ${summary.cumulativeCap} days (already approved: ${summary.approvedDaysThisFinancialYear}, requested: ${days}).`
        );
        return;
      }
    } else {
      const balance = this.leaveBalances.find(b => b.leaveType === this.leave.leaveType);
      if (balance && balance.remainingLeaves < days) {
        this.toastService.error(`Insufficient ${this.leave.leaveType} leave balance. Available: ${balance.remainingLeaves} days`);
        return;
      }
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
      let workingDays = 0;
      const current = new Date(start);
      while (current <= end) {
        const day = current.getDay(); // 0 Sunday, 6 Saturday
        if (day !== 0 && day !== 6) {
          workingDays++;
        }
        current.setDate(current.getDate() + 1);
      }
      return workingDays;
    }
    return 0;
  }

  isApplyingOnBehalf(): boolean {
    if (!this.isAdmin || !this.leave.employeeId) {
      return false;
    }
    const selectedEmployee = this.employees.find(emp => emp.id === this.leave.employeeId);
    const currentUserId = this.currentUser?.id ?? this.currentUser?.userId ?? this.currentUser?.user?.id;
    return !!(selectedEmployee?.userId && currentUserId && selectedEmployee.userId !== currentUserId);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.subscriptions = [];
  }
}


