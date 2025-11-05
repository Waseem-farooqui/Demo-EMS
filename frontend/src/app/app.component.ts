import {Component, OnInit} from '@angular/core';
import {Router, RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {CommonModule} from '@angular/common';
import {AuthService} from './services/auth.service';
import {ToastComponent} from './components/toast/toast.component';
import {NotificationDropdownComponent} from './components/notification-dropdown/notification-dropdown.component';
import {HttpClient} from '@angular/common/http';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, CommonModule, RouterLink, RouterLinkActive, ToastComponent, NotificationDropdownComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  title = 'Employee Management System';
  isLoggedIn = false;
  isAdmin = false;
  isSuperAdmin = false;
  isRoot = false;
  userName = '';
  isMobileMenuOpen = false;
  organizationName = '';
  organizationLogoUrl = '';

  constructor(
    private authService: AuthService,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.checkAuthStatus();
    // Subscribe to auth state changes
    this.authService.isAuthenticated().subscribe((isAuth: boolean) => {
      this.checkAuthStatus();
    });
  }

  checkAuthStatus(): void {
    this.isLoggedIn = this.authService.isLoggedIn();
    if (this.isLoggedIn) {
      const user = this.authService.getCurrentUser();
      const roles = user?.roles || [];
      this.isRoot = roles.includes('ROOT');
      this.isSuperAdmin = roles.includes('SUPER_ADMIN');
      this.isAdmin = roles.includes('ADMIN') || this.isSuperAdmin;
      this.userName = user?.name || user?.username || user?.email || 'User';

      console.log('Auth Status - ROOT:', this.isRoot, 'SUPER_ADMIN:', this.isSuperAdmin, 'ADMIN:', this.isAdmin);

      // Load organization info for non-ROOT users
      if (!this.isRoot && user?.organizationId) {
        this.loadOrganizationInfo(user.organizationId);
      }
    }
  }

  loadOrganizationInfo(organizationId: number): void {
    this.http.get<any>(`http://localhost:8080/api/organizations/${organizationId}`).subscribe({
      next: (org) => {
        this.organizationName = org.name;
        if (org.logoUrl) {
          // If logoUrl is relative, prepend the API base URL
          this.organizationLogoUrl = org.logoUrl.startsWith('http')
            ? org.logoUrl
            : `http://localhost:8080${org.logoUrl}`;
          console.log('Organization logo URL:', this.organizationLogoUrl);
        } else {
          this.organizationLogoUrl = '';
          console.log('No organization logo available');
        }
      },
      error: (err) => {
        console.error('Failed to load organization info:', err);
        this.organizationName = 'EMS';
        this.organizationLogoUrl = '';
      }
    });
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen = false;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
    this.closeMobileMenu();
  }
}
