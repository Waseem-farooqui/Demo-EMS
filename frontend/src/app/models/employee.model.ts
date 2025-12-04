export interface EmploymentRecord {
  id?: number;
  jobTitle?: string;
  employmentPeriod?: string;
  employerName?: string;
  employerAddress?: string;
  contactPersonTitle?: string;
  contactPersonName?: string;
  contactPersonEmail?: string;
}

export interface NextOfKin {
  id?: number;
  title?: string; // Mr, Mrs, Miss, Ms, Dr, Prof, etc.
  name?: string;
  contact?: string;
  address?: string;
  relationship?: string;
}

export interface Employee {
  id?: number;
  title?: string;
  fullName: string;
  personType: string;
  workEmail: string;
  jobTitle: string;
  reference?: string;
  dateOfJoining: string;
  dateOfBirth?: string;
  phoneNumber?: string;
  nationality?: string;
  presentAddress?: string;
  previousAddress?: string;
  hasMedicalCondition?: boolean;
  medicalConditionDetails?: string;
  // Legacy next of kin fields (kept for backward compatibility)
  nextOfKinName?: string;
  nextOfKinContact?: string;
  nextOfKinAddress?: string;
  nextOfKinList?: NextOfKin[];
  bloodGroup?: string;
  workingTiming?: string;
  holidayAllowance?: number;
  employmentStatus?: string;
  contractType?: string;
  allottedOrganization?: string;
  nationalInsuranceNumber?: string;
  shareCode?: string;
  bankAccountNumber?: string;
  bankSortCode?: string;
  bankAccountHolderName?: string;
  bankName?: string;
  wageRate?: string;
  contractHours?: string;
  userId?: number;
  username?: string;  // Add username for display
  role?: string;  // User role (USER, ADMIN, SUPER_ADMIN)
  departmentId?: number;
  departmentName?: string;
  employmentRecords?: EmploymentRecord[];
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelationship?: string;
}

