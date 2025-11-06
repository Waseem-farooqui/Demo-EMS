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
  workingTiming?: string;
  holidayAllowance?: number;
  employmentStatus?: string;
  contractType?: string;
  userId?: number;
  username?: string;  // Add username for display
  departmentId?: number;
  departmentName?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelationship?: string;
}

