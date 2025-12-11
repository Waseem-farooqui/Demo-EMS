export interface Document {
  id?: number;
  employeeId: number;
  employeeName?: string;
  documentType: string;
  documentNumber?: string;
  fileName: string;
  filePath?: string;
  fileType?: string;
  issueDate?: string;
  expiryDate?: string;
  issuingCountry?: string;
  fullName?: string;
  dateOfBirth?: string;
  nationality?: string;

  // UK VISA specific fields
  visaType?: string;
  companyName?: string;
  dateOfCheck?: string;
  referenceNumber?: string;

  // CONTRACT specific fields
  contractDate?: string; // Date of employment/contract start date
  placeOfWork?: string; // Work location
  contractBetween?: string; // Parties involved
  jobTitleContract?: string; // Job title from contract

  uploadedDate?: string;
  daysUntilExpiry?: number;
  alertSentCount?: number;
  lastAlertSent?: string;

  // Document view tracking
  lastViewedAt?: string;
  lastViewedBy?: string;

  // OCR processing status
  ocrFailed?: boolean; // true if OCR failed, undefined if OCR not attempted, false if OCR succeeded
  ocrFailureMessage?: string; // Message explaining why OCR failed
}

export interface DocumentUploadRequest {
  employeeId: number;
  documentType: string;
  file: File;
}

