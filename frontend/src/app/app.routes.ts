import {Routes} from '@angular/router';
import {EmployeeListComponent} from './components/employee-list/employee-list.component';
import {EmployeeFormComponent} from './components/employee-form/employee-form.component';
import {LeaveListComponent} from './components/leave-list/leave-list.component';
import {LeaveFormComponent} from './components/leave-form/leave-form.component';
import {DocumentListComponent} from './components/document-list/document-list.component';
import {DocumentUploadComponent} from './components/document-upload/document-upload.component';
import {DocumentDetailComponent} from './components/document-detail/document-detail.component';
import {LoginComponent} from './components/login/login.component';
import {AttendanceComponent} from './components/attendance/attendance.component';
import {DashboardComponent} from './components/dashboard/dashboard.component';
import {RootDashboardComponent} from './components/root-dashboard/root-dashboard.component';
import {OrganizationCreateComponent} from './components/organization-create/organization-create.component';
import {OrganizationDetailComponent} from './components/organization-detail/organization-detail.component';
import {ProfileCreateComponent} from './components/profile-create/profile-create.component';
import {UserCreateComponent} from './components/user-create/user-create.component';
import {PasswordChangeComponent} from './components/password-change/password-change.component';
import {ForgotPasswordComponent} from './components/forgot-password/forgot-password.component';
import {ForgotUsernameComponent} from './components/forgot-username/forgot-username.component';
import {ResetPasswordComponent} from './components/reset-password/reset-password.component';
import {RotaListComponent} from './components/rota-list/rota-list.component';
import {RotaUploadComponent} from './components/rota-upload/rota-upload.component';
import {AlertConfigurationComponent} from './components/alert-configuration/alert-configuration.component';
import {AuthGuard} from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'forgot-username', component: ForgotUsernameComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'change-password', component: PasswordChangeComponent, canActivate: [AuthGuard] },
  // ROOT Dashboard - Organization management only
  { path: 'root/dashboard', component: RootDashboardComponent, canActivate: [AuthGuard] },
  { path: 'root/organizations/create', component: OrganizationCreateComponent, canActivate: [AuthGuard] },
  { path: 'root/organizations/:id', component: OrganizationDetailComponent, canActivate: [AuthGuard] },
  // Employee Dashboard - SUPER_ADMIN and ADMIN
  { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard] },
  // Signup removed - users are created by admins
  { path: 'profile/create', component: ProfileCreateComponent, canActivate: [AuthGuard] },
  { path: 'users/create', component: UserCreateComponent, canActivate: [AuthGuard] },
  { path: 'employees/add', component: EmployeeFormComponent, canActivate: [AuthGuard] },
  { path: 'employees/edit/:id', component: EmployeeFormComponent, canActivate: [AuthGuard] },
  { path: 'employees', component: EmployeeListComponent, canActivate: [AuthGuard] },
  { path: 'attendance', component: AttendanceComponent, canActivate: [AuthGuard] },
  { path: 'leaves/apply', component: LeaveFormComponent, canActivate: [AuthGuard] },
  { path: 'leaves/edit/:id', component: LeaveFormComponent, canActivate: [AuthGuard] },
  { path: 'leaves', component: LeaveListComponent, canActivate: [AuthGuard] },
  { path: 'documents/upload', component: DocumentUploadComponent, canActivate: [AuthGuard] },
  { path: 'documents/:id', component: DocumentDetailComponent, canActivate: [AuthGuard] },
  { path: 'documents', component: DocumentListComponent, canActivate: [AuthGuard] },
  { path: 'rota/upload', component: RotaUploadComponent, canActivate: [AuthGuard] },
  { path: 'rota', component: RotaListComponent, canActivate: [AuthGuard] },
  { path: 'alert-configuration', component: AlertConfigurationComponent, canActivate: [AuthGuard] }
];

