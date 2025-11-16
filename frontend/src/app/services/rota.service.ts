import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../environments/environment';

export interface Rota {
  id: number;
  hotelName: string;
  department: string;
  fileName: string;
  startDate: string;
  endDate: string;
  uploadedDate: string;
  uploadedByName: string;
  totalEmployees: number;
}

// Backend returns flat list of RotaScheduleEntryDTO, not grouped format
export interface RotaSchedule {
  id?: number;
  rotaId?: number;
  employeeId: number;
  employeeName: string;
  scheduleDate?: string; // For flat format
  dayOfWeek?: string;
  startTime?: string | null;
  endTime?: string | null;
  duty?: string;
  isOffDay?: boolean;
  // Legacy grouped format (may not be present)
  schedules?: {
    [date: string]: {
      dayOfWeek: string;
      duty: string;
      startTime: string | null;
      endTime: string | null;
      isOffDay: boolean;
    } | undefined;
  };
}

export interface RotaUploadPreview {
  rotaId: number;
  hotelName: string;
  department: string;
  fileName: string;
  startDate: string;
  endDate: string;
  uploadedDate: string;
  uploadedByName: string;
  totalSchedules: number;
  totalEmployees: number;
  employeeSchedules: EmployeeSchedulePreview[];
}

export interface EmployeeSchedulePreview {
  employeeId: number;
  employeeName: string;
  totalDays: number;
  workDays: number;
  offDays: number;
  schedules: DaySchedulePreview[];
}

export interface DaySchedulePreview {
  date: string;
  dayOfWeek: string;
  duty: string;
  startTime: string | null;
  endTime: string | null;
  isOffDay: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class RotaService {
  private apiUrl = `${environment.apiUrl}${environment.endpoints.rotas}`;

  constructor(private http: HttpClient) {}

  uploadRota(file: File): Observable<Rota> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Rota>(`${this.apiUrl}/upload`, formData);
  }

  uploadExcelRota(file: File): Observable<RotaUploadPreview> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<RotaUploadPreview>(`${this.apiUrl}/upload-excel`, formData);
  }

  createManualRota(rotaData: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/manual`, rotaData);
  }

  getAllRotas(): Observable<Rota[]> {
    return this.http.get<Rota[]>(this.apiUrl);
  }

  getRotaSchedules(rotaId: number): Observable<RotaSchedule[]> {
    return this.http.get<RotaSchedule[]>(`${this.apiUrl}/${rotaId}/schedules`);
  }

  getEmployeeCurrentWeekSchedule(employeeId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/employee/${employeeId}/current-week`);
  }

  getRotaPreview(rotaId: number): Observable<RotaUploadPreview> {
    return this.http.get<RotaUploadPreview>(`${this.apiUrl}/${rotaId}/preview`);
  }

  deleteRota(rotaId: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${rotaId}`);
  }
}

