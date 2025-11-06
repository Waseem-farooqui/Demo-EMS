import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Leave, LeaveApprovalRequest, LeaveBalance, BlockedDate} from '../models/leave.model';
import {environment} from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class LeaveService {
  private apiUrl = `${environment.apiUrl}${environment.endpoints.leaves}`;

  constructor(private http: HttpClient) { }

  /**
   * Apply leave with validation (NEW - supports medical certificate)
   * Uses multipart/form-data for file upload
   */
  applyLeave(formData: FormData): Observable<Leave> {
    return this.http.post<Leave>(this.apiUrl, formData);
  }

  /**
   * Get leave balances for an employee
   */
  getLeaveBalances(employeeId: number): Observable<LeaveBalance[]> {
    return this.http.get<LeaveBalance[]>(`${this.apiUrl}/balances/employee/${employeeId}`);
  }

  /**
   * Get blocked dates (approved or pending leaves) for an employee
   * Use this to disable dates in calendar
   */
  getBlockedDates(employeeId: number): Observable<BlockedDate[]> {
    return this.http.get<BlockedDate[]>(`${this.apiUrl}/blocked-dates/employee/${employeeId}`);
  }

  /**
   * Get medical certificate for a leave (ADMIN/SUPER_ADMIN only)
   */
  getMedicalCertificate(leaveId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${leaveId}/certificate`, {
      responseType: 'blob'
    });
  }

  getAllLeaves(): Observable<Leave[]> {
    return this.http.get<Leave[]>(this.apiUrl);
  }

  getLeaveById(id: number): Observable<Leave> {
    return this.http.get<Leave>(`${this.apiUrl}/${id}`);
  }

  getLeavesByEmployeeId(employeeId: number): Observable<Leave[]> {
    return this.http.get<Leave[]>(`${this.apiUrl}/employee/${employeeId}`);
  }

  getLeavesByStatus(status: string): Observable<Leave[]> {
    return this.http.get<Leave[]>(`${this.apiUrl}/status/${status}`);
  }

  updateLeave(id: number, leave: Leave): Observable<Leave> {
    return this.http.put<Leave>(`${this.apiUrl}/${id}`, leave);
  }

  approveLeave(id: number, request: LeaveApprovalRequest): Observable<Leave> {
    return this.http.put<Leave>(`${this.apiUrl}/${id}/approve`, request);
  }

  rejectLeave(id: number, request: LeaveApprovalRequest): Observable<Leave> {
    return this.http.put<Leave>(`${this.apiUrl}/${id}/reject`, request);
  }

  deleteLeave(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

