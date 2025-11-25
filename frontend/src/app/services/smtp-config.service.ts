import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface SmtpConfiguration {
  id?: number;
  organizationId?: number;
  provider: 'GMAIL' | 'OUTLOOK' | 'CUSTOM';
  host?: string;
  port?: number;
  username?: string;
  password?: string; // Only for create/update
  fromEmail?: string;
  fromName?: string;
  enabled?: boolean;
  useDefault?: boolean;
  isConfigured?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class SmtpConfigService {
  private apiUrl = `${environment.apiUrl}/smtp-configuration`;

  constructor(private http: HttpClient) {}

  getSmtpConfiguration(): Observable<SmtpConfiguration> {
    return this.http.get<SmtpConfiguration>(this.apiUrl);
  }

  saveSmtpConfiguration(config: SmtpConfiguration): Observable<SmtpConfiguration> {
    return this.http.post<SmtpConfiguration>(this.apiUrl, config);
  }

  isSmtpConfigured(): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/check`);
  }
}

