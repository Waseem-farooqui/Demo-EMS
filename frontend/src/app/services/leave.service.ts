import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Leave, LeaveApprovalRequest} from '../models/leave.model';

@Injectable({
  providedIn: 'root'
})
export class LeaveService {
  private apiUrl = 'http://localhost:8080/api/leaves';

  constructor(private http: HttpClient) { }

  applyLeave(leave: Leave): Observable<Leave> {
    return this.http.post<Leave>(this.apiUrl, leave);
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

