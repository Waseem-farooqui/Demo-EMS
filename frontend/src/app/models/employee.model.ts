export interface EmploymentRecord {
  id?: number;
  jobTitle?: string;
  employmentPeriod?: string;
  employerName?: string;
  employerAddress?: string;
}

export interface Employee {
  id?: number;
  fullName: string;
  personType: string;
  workEmail: string;
  personalEmail?: string;
  jobTitle: string;
  reference?: string;
  dateOfJoining: string;
  dateOfBirth?: string;
  phoneNumber?: string;
  nationality?: string;
  address?: string;
  presentAddress?: string;
  previousAddress?: string;
  hasMedicalCondition?: boolean;
  medicalConditionDetails?: string;
  nextOfKinName?: string;
  nextOfKinContact?: string;
  nextOfKinAddress?: string;
  bloodGroup?: string;
  workingTiming?: string;
  holidayAllowance?: number;
  employmentStatus?: string;
  contractType?: string;
  allottedOrganization?: string;
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

