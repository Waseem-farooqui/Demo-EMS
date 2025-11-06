import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface Organization {
  id: number;
  organizationUuid: string;
  name: string;
  description: string;
  contactEmail: string;
  contactPhone: string;
  address: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  logoUrl?: string;
}

@Component({
  selector: 'app-organization-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './organization-detail.component.html',
  styleUrls: ['./organization-detail.component.css']
})
export class OrganizationDetailComponent implements OnInit {
  organization: Organization | null = null;
  loading = true;
  error: string | null = null;
  organizationId: number | null = null;

  private apiUrl = `${environment.apiUrl}/organizations`;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    // Get organization ID from route parameters
    this.route.params.subscribe(params => {
      this.organizationId = +params['id'];
      if (this.organizationId) {
        this.loadOrganizationDetails();
      }
    });
  }

  loadOrganizationDetails(): void {
    if (!this.organizationId) return;

    this.loading = true;
    this.error = null;

    this.http.get<Organization>(`${this.apiUrl}/${this.organizationId}`).subscribe({
      next: (data) => {
        console.log('✅ Organization details loaded:', data);
        this.organization = data;

        // Fix logo URL - prepend API base URL if it's a relative path
        if (this.organization.logoUrl && !this.organization.logoUrl.startsWith('http')) {
          this.organization.logoUrl = `${environment.apiBaseUrl}${this.organization.logoUrl}`;
        }

        this.loading = false;
      },
      error: (err) => {
        console.error('❌ Error loading organization details:', err);
        this.error = err.error?.message || 'Failed to load organization details';
        this.loading = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/root/dashboard']);
  }

  deactivateOrganization(): void {
    if (!this.organization || !this.organizationId) return;

    if (!confirm(`⚠️ Are you sure you want to DEACTIVATE "${this.organization.name}"?\n\nThis will block access for ALL users in this organization.`)) {
      return;
    }

    this.loading = true;
    this.http.post<any>(`${this.apiUrl}/${this.organizationId}/deactivate`, {}).subscribe({
      next: (response) => {
        console.log('✅ Organization deactivated:', response);
        alert(`✅ Organization deactivated successfully`);
        this.loadOrganizationDetails(); // Reload to show updated status
      },
      error: (err) => {
        console.error('❌ Error deactivating organization:', err);
        alert(`❌ Failed to deactivate: ${err.error?.message || 'Unknown error'}`);
        this.loading = false;
      }
    });
  }

  activateOrganization(): void {
    if (!this.organization || !this.organizationId) return;

    if (!confirm(`Are you sure you want to ACTIVATE "${this.organization.name}"?`)) {
      return;
    }

    this.loading = true;
    this.http.post<any>(`${this.apiUrl}/${this.organizationId}/activate`, {}).subscribe({
      next: (response) => {
        console.log('✅ Organization activated:', response);
        alert(`✅ Organization activated successfully`);
        this.loadOrganizationDetails(); // Reload to show updated status
      },
      error: (err) => {
        console.error('❌ Error activating organization:', err);
        alert(`❌ Failed to activate: ${err.error?.message || 'Unknown error'}`);
        this.loading = false;
      }
    });
  }

  formatDate(dateString: string): string {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-GB', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}

