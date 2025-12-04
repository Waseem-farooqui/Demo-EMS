import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';

interface OrganizationOnboarding {
  organizationId: number;
  organizationUuid: string;
  organizationName: string;
  onboardingDate: string;
  superAdminUsername: string;
  superAdminEmail: string;
  isActive: boolean;
  daysActive: number;
}

interface RootDashboard {
  totalOrganizations: number;
  activeOrganizations: number;
  inactiveOrganizations: number;
  systemStartDate: string;
  totalSuperAdmins: number;
  recentOnboardings: OrganizationOnboarding[];
}

@Component({
  selector: 'app-root-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './root-dashboard.component.html',
  styleUrls: ['./root-dashboard.component.css']
})
export class RootDashboardComponent implements OnInit {
  dashboardData: RootDashboard | null = null;
  loading = true;
  error: string | null = null;

  private apiUrl = `${environment.apiUrl}/root/dashboard`;
  private organizationApiUrl = `${environment.apiUrl}/organizations`;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading = true;
    this.error = null;

    this.http.get<RootDashboard>(`${this.apiUrl}/stats`).subscribe({
      next: (data) => {
        this.dashboardData = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('❌ Error loading ROOT dashboard:', err);
        this.error = 'Failed to load dashboard data';
        this.loading = false;

        // If unauthorized, redirect to login
        if (err.status === 401 || err.status === 403) {
          this.router.navigate(['/login']);
        }
      }
    });
  }

  viewOrganizationDetails(orgId: number): void {
    this.router.navigate(['/root/organizations', orgId]);
  }

  createOrganization(): void {
    this.router.navigate(['/root/organizations/create']);
  }

  deactivateOrganization(orgId: number, orgName: string): void {
    if (!confirm(`⚠️ Are you sure you want to DEACTIVATE "${orgName}"?\n\nThis will block access for ALL users in this organization including Super Admins and Admins.`)) {
      return;
    }

    this.loading = true;
    this.error = null;

    this.http.post<any>(`${this.organizationApiUrl}/${orgId}/deactivate`, {}).subscribe({
      next: (response) => {
        alert(`✅ Organization "${orgName}" has been deactivated successfully.\n\nAll users are now blocked.`);
        this.loadDashboard(); // Reload to show updated status
      },
      error: (err) => {
        console.error('❌ Error deactivating organization:', err);
        this.error = err.error?.message || 'Failed to deactivate organization';
        this.loading = false;
        alert(`❌ Failed to deactivate organization: ${this.error}`);
      }
    });
  }

  activateOrganization(orgId: number, orgName: string): void {
    if (!confirm(`Are you sure you want to ACTIVATE "${orgName}"?\n\nThis will restore access for all users in this organization.`)) {
      return;
    }

    this.loading = true;
    this.error = null;

    this.http.post<any>(`${this.organizationApiUrl}/${orgId}/activate`, {}).subscribe({
      next: (response) => {
        alert(`✅ Organization "${orgName}" has been activated successfully.\n\nAll users can now access the system.`);
        this.loadDashboard(); // Reload to show updated status
      },
      error: (err) => {
        console.error('❌ Error activating organization:', err);
        this.error = err.error?.message || 'Failed to activate organization';
        this.loading = false;
        alert(`❌ Failed to activate organization: ${this.error}`);
      }
    });
  }

  deleteOrganization(orgId: number, orgName: string): void {
    const confirmMessage = `⚠️⚠️⚠️ WARNING: PERMANENT DELETION ⚠️⚠️⚠️\n\n` +
      `Are you absolutely sure you want to DELETE "${orgName}"?\n\n` +
      `This action will PERMANENTLY DELETE:\n` +
      `• The organization itself\n` +
      `• ALL employees and their data\n` +
      `• ALL users and accounts\n` +
      `• ALL departments\n` +
      `• ALL documents and uploaded files\n` +
      `• ALL leaves, attendance, rotas\n` +
      `• ALL notifications and configurations\n\n` +
      `THIS ACTION CANNOT BE UNDONE!\n\n` +
      `Type "${orgName}" to confirm deletion:`;

    const userInput = prompt(confirmMessage);
    
    if (userInput !== orgName) {
      if (userInput !== null) {
        alert('❌ Organization name does not match. Deletion cancelled.');
      }
      return;
    }

    // Final confirmation
    if (!confirm(`🚨 FINAL CONFIRMATION 🚨\n\nYou are about to PERMANENTLY DELETE "${orgName}" and ALL its data.\n\nThis cannot be undone!\n\nProceed with deletion?`)) {
      return;
    }

    this.loading = true;
    this.error = null;

    this.http.delete<any>(`${this.organizationApiUrl}/${orgId}`).subscribe({
      next: (response) => {
        alert(`✅ Organization "${orgName}" and all related data have been permanently deleted.`);
        this.loadDashboard(); // Reload to show updated list
      },
      error: (err) => {
        console.error('❌ Error deleting organization:', err);
        this.error = err.error?.message || 'Failed to delete organization';
        this.loading = false;
        alert(`❌ Failed to delete organization: ${this.error}`);
      }
    });
  }
}

