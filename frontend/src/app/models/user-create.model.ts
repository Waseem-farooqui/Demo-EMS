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
  fullName: string;
  email: string;
  personalEmail?: string;
  phoneNumber?: string;
  dateOfBirth?: string;
  nationality?: string;
  address?: string;
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
  hasMedicalCondition: boolean;
  medicalConditionDetails?: string;
  nextOfKinName?: string;
  nextOfKinContact?: string;
  nextOfKinAddress?: string;
  employmentRecords?: any[];
  bloodGroup?: string;
  allottedOrganization?: string;
}

