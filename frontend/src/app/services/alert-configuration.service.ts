import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AlertConfiguration } from '../models/alert-configuration.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AlertConfigurationService {
  private apiUrl = `${environment.apiUrl}/alert-config`;

  constructor(private http: HttpClient) { }

  getAllConfigurations(): Observable<AlertConfiguration[]> {
    return this.http.get<AlertConfiguration[]>(this.apiUrl);
  }

  getConfigurationByType(documentType: string): Observable<AlertConfiguration> {
    return this.http.get<AlertConfiguration>(`${this.apiUrl}/type/${documentType}`);
  }

  createConfiguration(config: AlertConfiguration): Observable<AlertConfiguration> {
    return this.http.post<AlertConfiguration>(this.apiUrl, config);
  }

  updateConfiguration(id: number, config: AlertConfiguration): Observable<AlertConfiguration> {
    return this.http.put<AlertConfiguration>(`${this.apiUrl}/${id}`, config);
  }

  testAlerts(): Observable<any> {
    return this.http.post(`${this.apiUrl}/test-alerts`, {});
  }
}

