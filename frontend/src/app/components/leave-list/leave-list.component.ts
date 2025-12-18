import {Component, OnInit, OnDestroy} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {Router, RouterModule} from '@angular/router';
import {LeaveService} from '../../services/leave.service';
import {AuthService} from '../../services/auth.service';
import {EmployeeService} from '../../services/employee.service';
import {Leave, LeaveApprovalRequest, LeaveBalance} from '../../models/leave.model';
import {Employee} from '../../models/employee.model';
import {Subscription, forkJoin, of} from 'rxjs';
import {catchError} from 'rxjs/operators';

interface User {
  id?: number;
  email?: string;
  username?: string;
  roles?: string[];
  organizationId?: number;
}

interface EmployeeLeaveBalance {
  employeeId: number;
  employeeName: string;
  balances: LeaveBalance[];
}

@Component({
  selector: 'app-leave-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './leave-list.component.html',
  styleUrls: ['./leave-list.component.css']
})
export class LeaveListComponent implements OnInit, OnDestroy {
  leaves: Leave[] = [];
  loading = false;
  error: string | null = null;
  currentUser: User | null = null;
  isAdmin = false;
  isSuperAdmin = false;
  filterStatus = 'ALL';
  showCommentModal = false;
  selectedLeave: Leave | null = null;
  commentText = '';
  actionType: 'APPROVE' | 'REJECT' | 'COMMENT' | 'REQUEST_UPDATE' = 'COMMENT';
  
  // Leave balance properties
  employeeLeaveBalances: EmployeeLeaveBalance[] = [];
  userLeaveBalances: LeaveBalance[] = [];
  loadingLeaveBalances = false;

  // Employee list view
  employees: Employee[] = [];
  loadingEmployees = false;
  selectedEmployee: Employee | null = null;
  showEmployeeListView = true; // Start with employee list view

  // Pagination properties
  currentPage = 0;
  pageSize = 10;
  totalLeaves = 0;
  totalPages = 0;
  paginatedLeaves: Leave[] = [];

  private subscriptions: Subscription[] = [];

  constructor(
    private leaveService: LeaveService,
    private authService: AuthService,
    private employeeService: EmployeeService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.currentUser = this.authService.getUser();
    const roles = this.currentUser?.roles || [];
    this.isSuperAdmin = roles.includes('SUPER_ADMIN');
    this.isAdmin = roles.includes('ADMIN') || this.isSuperAdmin;
    
    // Load employees list first (for admin/super admin)
    if (this.isAdmin) {
      this.loadEmployees();
    } else {
      // For regular users, show their own leaves directly
      this.showEmployeeListView = false;
      this.loadLeaves();
    }
    this.loadLeaveBalances();
  }

  loadEmployees(): void {
    this.loadingEmployees = true;
    this.error = null;
    
    const employeesSub = this.employeeService.getAllEmployees().subscribe({
      next: (data) => {
        this.employees = data;
        this.loadingEmployees = false;
      },
      error: (err) => {
        this.error = 'Failed to load employees. Please try again.';
        this.loadingEmployees = false;
        console.error('Error loading employees:', err);
      }
    });
    this.subscriptions.push(employeesSub);
  }

  loadLeaves(employeeId?: number): void {
    this.loading = true;
    this.error = null;

    let observable;
    if (employeeId) {
      // Load leaves for specific employee
      observable = this.leaveService.getLeavesByEmployeeId(employeeId);
    } else if (this.isSuperAdmin) {
      // Super admin: load all leaves
      observable = this.filterStatus === 'ALL'
        ? this.leaveService.getAllLeaves()
        : this.leaveService.getLeavesByStatus(this.filterStatus);
    } else {
      // Regular user: load their own leaves
      observable = this.filterStatus === 'ALL'
        ? this.leaveService.getAllLeaves() // This will return only their own leaves from backend
        : this.leaveService.getLeavesByStatus(this.filterStatus);
    }

    const leavesSub = observable.subscribe({
      next: (data) => {
        // Filter by status if needed (for employee-specific leaves)
        if (employeeId && this.filterStatus !== 'ALL') {
          this.leaves = data.filter(leave => leave.status === this.filterStatus);
        } else {
          this.leaves = data;
        }
        
        // Apply pagination
        this.totalLeaves = this.leaves.length;
        this.totalPages = Math.ceil(this.totalLeaves / this.pageSize);
        this.updatePaginatedLeaves();
        
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load leaves. Please try again.';
        this.loading = false;
        console.error('Error loading leaves:', err);
      }
    });
    this.subscriptions.push(leavesSub);
  }

  updatePaginatedLeaves(): void {
    const start = this.currentPage * this.pageSize;
    const end = start + this.pageSize;
    this.paginatedLeaves = this.leaves.slice(start, end);
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.updatePaginatedLeaves();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.updatePaginatedLeaves();
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.updatePaginatedLeaves();
    }
  }

  changePageSize(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.totalPages = Math.ceil(this.totalLeaves / this.pageSize);
    this.updatePaginatedLeaves();
  }

  getPageNumbers(): (number | string)[] {
    const pages: (number | string)[] = [];
    const maxVisible = 5;
    
    if (this.totalPages <= maxVisible) {
      // Show all pages if total pages is less than max visible
      for (let i = 1; i <= this.totalPages; i++) {
        pages.push(i);
      }
    } else {
      // Show first page
      pages.push(1);
      
      if (this.currentPage + 1 > 3) {
        pages.push('...');
      }
      
      // Show pages around current page
      const start = Math.max(2, this.currentPage + 1 - 1);
      const end = Math.min(this.totalPages - 1, this.currentPage + 1 + 1);
      
      for (let i = start; i <= end; i++) {
        if (i !== 1 && i !== this.totalPages) {
          pages.push(i);
        }
      }
      
      if (this.currentPage + 1 < this.totalPages - 2) {
        pages.push('...');
      }
      
      // Show last page
      pages.push(this.totalPages);
    }
    
    return pages;
  }

  // Expose Math to template
  Math = Math;

  selectEmployee(employee: Employee): void {
    this.selectedEmployee = employee;
    this.showEmployeeListView = false;
    this.currentPage = 0; // Reset to first page when selecting employee
    this.loadLeaves(employee.id!);
  }

  backToEmployeeList(): void {
    this.selectedEmployee = null;
    this.showEmployeeListView = true;
    this.leaves = [];
    this.filterStatus = 'ALL';
  }

  filterByStatus(status: string): void {
    this.filterStatus = status;
    this.currentPage = 0; // Reset to first page when filtering
    if (this.selectedEmployee) {
      this.loadLeaves(this.selectedEmployee.id!);
    } else {
      this.loadLeaves();
    }
  }

  approveLeave(leave: Leave): void {
    if (!this.isAdmin) {
      this.error = 'Only admins can approve leaves.';
      return;
    }

    // SUPER_ADMIN uses modal for detailed approval
    if (this.isSuperAdmin) {
      this.openCommentModal(leave, 'APPROVE');
      return;
    }

    if (confirm(`Approve leave for ${leave.employeeName}?`)) {
      const request: LeaveApprovalRequest = {
        remarks: 'Approved'
      };

      const approveSub = this.leaveService.approveLeave(leave.id!, request).subscribe({
        next: () => {
          if (this.selectedEmployee) {
            this.loadLeaves(this.selectedEmployee.id!);
          } else {
            this.loadLeaves();
          }
        },
        error: (err) => {
          this.error = err?.error?.error || 'Failed to approve leave. Please try again.';
          console.error('Error approving leave:', err);
        }
      });
      this.subscriptions.push(approveSub);
    }
  }

  rejectLeave(leave: Leave): void {
    if (!this.isAdmin) {
      this.error = 'Only admins can reject leaves.';
      return;
    }

    // SUPER_ADMIN uses modal for detailed rejection
    if (this.isSuperAdmin) {
      this.openCommentModal(leave, 'REJECT');
      return;
    }

    const remarks = prompt('Enter rejection reason:');
    if (remarks) {
      const request: LeaveApprovalRequest = {
        approvedBy: undefined,
        rejectedBy: this.currentUser?.username || '',
        remarks: remarks
      };

      const rejectSub = this.leaveService.rejectLeave(leave.id!, request).subscribe({
        next: () => {
          if (this.selectedEmployee) {
            this.loadLeaves(this.selectedEmployee.id!);
          } else {
            this.loadLeaves();
          }
        },
        error: (err) => {
          this.error = err?.error?.error || 'Failed to reject leave. Please try again.';
          console.error('Error rejecting leave:', err);
        }
      });
      this.subscriptions.push(rejectSub);
    }
  }

  openCommentModal(leave: Leave, action: 'APPROVE' | 'REJECT' | 'COMMENT' | 'REQUEST_UPDATE'): void {
    this.selectedLeave = leave;
    this.actionType = action;
    this.commentText = '';
    this.showCommentModal = true;
  }

  closeCommentModal(): void {
    this.showCommentModal = false;
    this.selectedLeave = null;
    this.commentText = '';
  }

  submitComment(): void {
    if (!this.selectedLeave || !this.commentText.trim()) {
      return;
    }

    const request: LeaveApprovalRequest = {
      remarks: this.commentText,
      adminComments: this.commentText
    };

    switch (this.actionType) {
      case 'APPROVE':
        // approvedBy is now automatically set by backend from current authenticated user
        const approveModalSub = this.leaveService.approveLeave(this.selectedLeave.id!, request).subscribe({
          next: () => {
            this.closeCommentModal();
            if (this.selectedEmployee) {
              this.loadLeaves(this.selectedEmployee.id!);
            } else {
              this.loadLeaves();
            }
          },
          error: (err) => {
            this.error = err?.error?.error || 'Failed to approve leave. Please try again.';
            console.error('Error approving leave:', err);
          }
        });
        this.subscriptions.push(approveModalSub);
        break;

      case 'REJECT':
        // rejectedBy is now automatically set by backend from current authenticated user
        const rejectModalSub = this.leaveService.rejectLeave(this.selectedLeave.id!, request).subscribe({
          next: () => {
            this.closeCommentModal();
            if (this.selectedEmployee) {
              this.loadLeaves(this.selectedEmployee.id!);
            } else {
              this.loadLeaves();
            }
          },
          error: (err) => {
            this.error = err?.error?.error || 'Failed to reject leave. Please try again.';
            console.error('Error rejecting leave:', err);
          }
        });
        this.subscriptions.push(rejectModalSub);
        break;

      case 'COMMENT':
      case 'REQUEST_UPDATE':
        // For now, we'll use the remarks field to store comments
        // In a full implementation, this would hit a separate comments endpoint
        alert('Comment added: ' + this.commentText);
        this.closeCommentModal();
        break;
    }
  }

  deleteLeave(leave: Leave): void {
    if (confirm(`Delete leave request for ${leave.employeeName}?`)) {
      const deleteSub = this.leaveService.deleteLeave(leave.id!).subscribe({
        next: () => {
          if (this.selectedEmployee) {
            this.loadLeaves(this.selectedEmployee.id!);
          } else {
            this.loadLeaves();
          }
        },
        error: (err) => {
          this.error = 'Failed to delete leave. Please try again.';
          console.error('Error deleting leave:', err);
        }
      });
      this.subscriptions.push(deleteSub);
    }
  }

  editLeave(leave: Leave): void {
    if (leave.status !== 'PENDING') {
      this.error = 'Can only edit pending leaves.';
      return;
    }
    this.router.navigate(['/leaves/edit', leave.id]);
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'PENDING': return 'status-pending';
      case 'APPROVED': return 'status-approved';
      case 'REJECTED': return 'status-rejected';
      default: return '';
    }
  }

  canEdit(leave: Leave): boolean {
    return leave.status === 'PENDING';
  }

  canDelete(leave: Leave): boolean {
    return leave.status !== 'APPROVED';
  }

  viewHolidayForm(leave: Leave): void {
    if (!leave.hasHolidayForm) {
      alert('No holiday form attached to this leave');
      return;
    }
    const holidayFormSub = this.leaveService.getHolidayForm(leave.id!).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = leave.holidayFormFileName || `holiday-form-${leave.id}.pdf`;
        link.target = '_blank';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Error loading holiday form:', err);
        alert('Failed to load holiday form');
      }
    });
    this.subscriptions.push(holidayFormSub);
  }

  loadLeaveBalances(): void {
    this.loadingLeaveBalances = true;
    
    if (this.isAdmin) {
      // For admins/super admins: load balances for all employees
      const employeesSub = this.employeeService.getAllEmployees().subscribe({
        next: (employees) => {
          if (employees.length === 0) {
            this.loadingLeaveBalances = false;
            return;
          }
          
          // Load leave balances for all employees
          const balanceRequests = employees.map(emp => 
            this.leaveService.getLeaveBalances(emp.id!).pipe(
              catchError(err => {
                console.error(`Error loading leave balance for employee ${emp.id}:`, err);
                return of([]); // Return empty array on error
              })
            )
          );
          
          const balancesSub = forkJoin(balanceRequests).subscribe({
            next: (allBalances) => {
              this.employeeLeaveBalances = employees
                .map((emp, index) => ({
                  employeeId: emp.id!,
                  employeeName: emp.fullName || emp.username || 'Unknown',
                  balances: allBalances[index] || []
                }))
                .filter(item => item.balances.length > 0); // Only include employees with balances
              this.loadingLeaveBalances = false;
            },
            error: (err) => {
              console.error('Error loading leave balances:', err);
              this.loadingLeaveBalances = false;
            }
          });
          this.subscriptions.push(balancesSub);
        },
        error: (err) => {
          console.error('Error loading employees:', err);
          this.loadingLeaveBalances = false;
        }
      });
      this.subscriptions.push(employeesSub);
    } else {
      // For regular users: load their own leave balance
      // Regular users can only see themselves in the employee list
      const employeesSub = this.employeeService.getAllEmployees().subscribe({
        next: (employees) => {
          if (employees.length > 0 && employees[0].id) {
            const balanceSub = this.leaveService.getLeaveBalances(employees[0].id).subscribe({
              next: (balances) => {
                this.userLeaveBalances = balances;
                this.loadingLeaveBalances = false;
              },
              error: (err) => {
                console.error('Error loading leave balance:', err);
                this.loadingLeaveBalances = false;
              }
            });
            this.subscriptions.push(balanceSub);
          } else {
            this.loadingLeaveBalances = false;
          }
        },
        error: (err) => {
          console.error('Error loading employee:', err);
          this.loadingLeaveBalances = false;
        }
      });
      this.subscriptions.push(employeesSub);
    }
  }

  getLeaveBalanceStatusClass(remainingLeaves: number): string {
    if (remainingLeaves <= 2) {
      return 'balance-low';
    } else if (remainingLeaves <= 5) {
      return 'balance-warning';
    }
    return 'balance-good';
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.subscriptions = [];
  }
}

