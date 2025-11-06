export interface Leave {
  id?: number;
  employeeId: number;
  employeeName?: string;
  leaveType: string;
  startDate: string;
  endDate: string;
  numberOfDays?: number;
  reason: string;
  status?: string;
  appliedDate?: string;
  approvedBy?: string;
  rejectedBy?: string;
  approvalDate?: string;
  rejectionDate?: string;
  remarks?: string;
  adminComments?: string;
  requiresSuperAdminApproval?: boolean;
  hasMedicalCertificate?: boolean;
  certificateFileName?: string;
  financialYear?: string;
}

export interface LeaveBalance {
  id: number;
  employeeId: number;
  employeeName: string;
  financialYear: string;
  leaveType: string;
  totalAllocated: number;
  usedLeaves: number;
  remainingLeaves: number;
}

export interface BlockedDate {
  startDate: string;
  endDate: string;
  status: string;
  leaveType: string;
}

export interface LeaveApprovalRequest {
  approvedBy?: string;
  rejectedBy?: string;
  remarks: string;
  adminComments?: string;
}

export interface LeaveComment {
  id?: number;
  leaveId: number;
  commentBy: string;
  commentDate: string;
  comment: string;
  actionType?: 'COMMENT' | 'APPROVE' | 'REJECT' | 'REQUEST_UPDATE';
}

