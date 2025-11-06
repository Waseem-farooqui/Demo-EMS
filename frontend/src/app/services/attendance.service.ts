import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Attendance, DashboardStats, EmployeeWorkSummary, WorkLocationOption} from '../models/attendance.model';
import {environment} from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AttendanceService {
  private apiUrl = `${environment.apiUrl}${environment.endpoints.attendance}`;

  constructor(private http: HttpClient) {}

  checkIn(employeeId: number, workLocation: string, notes?: string): Observable<Attendance> {
    return this.http.post<Attendance>(`${this.apiUrl}/check-in`, {
      employeeId,
      workLocation,
      notes
    });
  }

  checkOut(employeeId: number, notes?: string): Observable<Attendance> {
    return this.http.post<Attendance>(`${this.apiUrl}/check-out`, {
      employeeId,
      notes
    });
  }

  getCurrentStatus(employeeId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/status/${employeeId}`);
  }

  getAttendanceByDateRange(employeeId: number, startDate: string, endDate: string): Observable<Attendance[]> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    return this.http.get<Attendance[]>(`${this.apiUrl}/employee/${employeeId}`, { params });
  }

  getEmployeeWorkSummary(employeeId: number): Observable<EmployeeWorkSummary> {
    return this.http.get<EmployeeWorkSummary>(`${this.apiUrl}/summary/${employeeId}`);
  }

  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.apiUrl}/dashboard/stats`);
  }

  getTodayActiveCheckIns(): Observable<Attendance[]> {
    return this.http.get<Attendance[]>(`${this.apiUrl}/active-today`);
  }

  getWorkLocations(): Observable<WorkLocationOption[]> {
    return this.http.get<WorkLocationOption[]>(`${this.apiUrl}/work-locations`);
  }
}

