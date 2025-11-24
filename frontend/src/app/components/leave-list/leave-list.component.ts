import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {Router, RouterModule} from '@angular/router';
import {LeaveService} from '../../services/leave.service';
import {AuthService} from '../../services/auth.service';
import {Leave, LeaveApprovalRequest} from '../../models/leave.model';

@Component({
  selector: 'app-leave-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './leave-list.component.html',
  styleUrls: ['./leave-list.component.css']
})
export class LeaveListComponent implements OnInit {
  leaves: Leave[] = [];
  loading = false;
  error: string | null = null;
  currentUser: any;
  isAdmin = false;
  isSuperAdmin = false;
  filterStatus = 'ALL';
  showCommentModal = false;
  selectedLeave: Leave | null = null;
  commentText = '';
  actionType: 'APPROVE' | 'REJECT' | 'COMMENT' | 'REQUEST_UPDATE' = 'COMMENT';

  constructor(
    private leaveService: LeaveService,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.currentUser = this.authService.getUser();
    const roles = this.currentUser?.roles || [];
    this.isSuperAdmin = roles.includes('SUPER_ADMIN');
    this.isAdmin = roles.includes('ADMIN') || this.isSuperAdmin;
    this.loadLeaves();
  }

  loadLeaves(): void {
    this.loading = true;
    this.error = null;

    const observable = this.filterStatus === 'ALL'
      ? this.leaveService.getAllLeaves()
      : this.leaveService.getLeavesByStatus(this.filterStatus);

    observable.subscribe({
      next: (data) => {
        this.leaves = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load leaves. Please try again.';
        this.loading = false;
        console.error('Error loading leaves:', err);
      }
    });
  }

  filterByStatus(status: string): void {
    this.filterStatus = status;
    this.loadLeaves();
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

      this.leaveService.approveLeave(leave.id!, request).subscribe({
        next: () => {
          this.loadLeaves();
        },
        error: (err) => {
          this.error = err?.error?.error || 'Failed to approve leave. Please try again.';
          console.error('Error approving leave:', err);
        }
      });
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
        rejectedBy: this.currentUser.username,
        remarks: remarks
      };

      this.leaveService.rejectLeave(leave.id!, request).subscribe({
        next: () => {
          this.loadLeaves();
        },
        error: (err) => {
          this.error = err?.error?.error || 'Failed to reject leave. Please try again.';
          console.error('Error rejecting leave:', err);
        }
      });
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
        this.leaveService.approveLeave(this.selectedLeave.id!, request).subscribe({
          next: () => {
            this.closeCommentModal();
            this.loadLeaves();
          },
          error: (err) => {
            this.error = err?.error?.error || 'Failed to approve leave. Please try again.';
            console.error('Error approving leave:', err);
          }
        });
        break;

      case 'REJECT':
        // rejectedBy is now automatically set by backend from current authenticated user
        this.leaveService.rejectLeave(this.selectedLeave.id!, request).subscribe({
          next: () => {
            this.closeCommentModal();
            this.loadLeaves();
          },
          error: (err) => {
            this.error = err?.error?.error || 'Failed to reject leave. Please try again.';
            console.error('Error rejecting leave:', err);
          }
        });
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
      this.leaveService.deleteLeave(leave.id!).subscribe({
        next: () => {
          this.loadLeaves();
        },
        error: (err) => {
          this.error = 'Failed to delete leave. Please try again.';
          console.error('Error deleting leave:', err);
        }
      });
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
}

