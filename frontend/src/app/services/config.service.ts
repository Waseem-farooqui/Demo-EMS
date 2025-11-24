import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ConfigService {

  // API URLs
  get apiUrl(): string {
    return environment.apiUrl;
  }

  get apiBaseUrl(): string {
    return environment.apiBaseUrl;
  }

  get frontendUrl(): string {
    return environment.frontendUrl;
  }

  // Endpoint builder
  getEndpoint(endpoint: keyof typeof environment.endpoints): string {
    return `${environment.apiUrl}${environment.endpoints[endpoint]}`;
  }

  // Full URL builder
  getFullUrl(path: string): string {
    return `${environment.apiBaseUrl}${path}`;
  }

  // Feature flags
  get isProduction(): boolean {
    return environment.production;
  }

  get debugMode(): boolean {
    return environment.enableDebugMode;
  }

  get loggingEnabled(): boolean {
    return environment.enableLogging;
  }

  // File upload config
  get maxFileSize(): number {
    return environment.fileUpload.maxSizeMB * 1024 * 1024; // Convert to bytes
  }

  get allowedDocumentTypes(): string[] {
    return environment.fileUpload.allowedDocumentTypes;
  }

  get allowedImageTypes(): string[] {
    return environment.fileUpload.allowedImageTypes;
  }

  // Pagination
  get defaultPageSize(): number {
    return environment.pagination.defaultPageSize;
  }

  get pageSizeOptions(): number[] {
    return environment.pagination.pageSizeOptions;
  }

  // Session
  get sessionTimeout(): number {
    return environment.sessionTimeout;
  }

  // Date formats
  get dateFormat(): string {
    return environment.dateFormat;
  }

  get dateTimeFormat(): string {
    return environment.dateTimeFormat;
  }

  get displayDateFormat(): string {
    return environment.displayDateFormat;
  }

  // App metadata
  get appName(): string {
    return environment.appName;
  }

  get appVersion(): string {
    return environment.appVersion;
  }

  get company(): string {
    return environment.company;
  }

  // Helper method to log in dev mode only (disabled for security)
  log(...args: any[]): void {
    // Logging disabled to prevent PII exposure
  }
}

