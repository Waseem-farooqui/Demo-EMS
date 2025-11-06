export interface AlertConfiguration {
  id?: number;
  documentType: string;
  alertDaysBefore: number;
  alertEmail: string;
  enabled: boolean;
  alertPriority: string; // EXPIRED, CRITICAL, WARNING, ATTENTION
  notificationType: string; // EMAIL, NOTIFICATION, BOTH
  organizationId?: number;
}

