export interface LoginRequest {
  username: string;
  password: string;
}

export interface SignupRequest {
  username: string;
  email: string;
  password: string;
  roles?: string[];
}

export interface JwtResponse {
  token: string;
  type: string;
  id: number;
  username: string;
  email: string;
  roles: string[];
  organizationUuid?: string; // UUID for organization identification
  firstLogin: boolean;
  profileCompleted: boolean;
  temporaryPassword: boolean;
  smtpConfigured?: boolean; // Whether SMTP is configured for SUPER_ADMIN
}

