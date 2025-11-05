export interface Attendance {
  id?: number;
  employeeId: number;
  employeeName?: string;
  checkInTime: string;
  checkOutTime?: string;
  workDate: string;
  workLocation: WorkLocation;
  workLocationDisplay?: string;
  hoursWorked?: number;
  notes?: string;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export enum WorkLocation {
  OFFICE = 'OFFICE',
  HOME = 'HOME',
  CLIENT_SITE = 'CLIENT_SITE',
  FIELD_WORK = 'FIELD_WORK',
  HYBRID = 'HYBRID'
}

export interface WorkLocationOption {
  value: string;
  label: string;
  icon?: string;
}

export interface DashboardStats {
  totalEmployees: number;
  employeesOnLeave: number;
  employeesCheckedIn: number;
  employeesByLocation: { [key: string]: number };
  averageHoursToday: number;
}

export interface EmployeeWorkSummary {
  employeeId: number;
  employeeName: string;
  email: string;
  jobTitle: string;
  totalHoursThisWeek: number;
  totalHoursThisMonth: number;
  weeklyAttendance: Attendance[];
  daysWorkedThisWeek: number;
  daysWorkedThisMonth: number;
  currentStatus: 'CHECKED_IN' | 'CHECKED_OUT' | 'ON_LEAVE';
}

