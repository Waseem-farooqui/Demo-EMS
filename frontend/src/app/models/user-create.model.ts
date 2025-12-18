export interface Department {
  id: number | string;
  name: string;
  code?: string;
  hasAdmin?: boolean;
  description?: string;
  isActive?: boolean;
}

export interface CreateUserResponse {
  employeeId: number;
  userId: number;
  fullName: string;
  email: string;
  username: string;
  temporaryPassword: string;
  role: string;
  departmentName: string;
  message: string;
  emailSent: boolean;
}

export interface UserFormData {
  title?: string;
  fullName: string;
  email: string;
  phoneNumber?: string;
  dateOfBirth?: string;
  nationality?: string;
  presentAddress?: string;
  previousAddress?: string;
  jobTitle: string;
  personType: string;
  role: string;
  departmentId?: number | null;
  customDepartmentName?: string;
  reference?: string;
  dateOfJoining: string;
  employmentStatus: string;
  contractType: string;
  comments?: string; // General comments/notes about the employee
  hasMedicalCondition: boolean;
  medicalConditionDetails?: string;
  // Legacy next of kin fields (kept for backward compatibility)
  nextOfKinName?: string;
  nextOfKinContact?: string;
  nextOfKinAddress?: string;
  nextOfKinList?: any[];
  employmentRecords?: any[];
  allottedOrganization?: string;
}

