export interface AlertConfiguration {
  id?: number;
  documentType: string;
  alertDaysBefore: number;
  alertEmail: string;
  enabled: boolean;
}

