import {Component, OnInit} from '@angular/core';
import {Router, RouterLink, RouterLinkActive, RouterOutlet, NavigationEnd} from '@angular/router';
import {CommonModule} from '@angular/common';
import {AuthService} from './services/auth.service';
import {ToastComponent} from './components/toast/toast.component';
import {NotificationDropdownComponent} from './components/notification-dropdown/notification-dropdown.component';
import {PwaInstallComponent} from './components/pwa-install/pwa-install.component';
import {GlobalSearchComponent} from './components/global-search/global-search.component';
import {HttpClient} from '@angular/common/http';
import {environment} from '../environments/environment';
import {filter} from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, CommonModule, RouterLink, RouterLinkActive, ToastComponent, NotificationDropdownComponent, PwaInstallComponent, GlobalSearchComponent],
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
  isSidebarCollapsed = false;
  organizationName = '';
  organizationLogoUrl = '';
  showNavigation = true;

  constructor(
    private authService: AuthService,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    // Check current route and hide navigation for password change
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      this.showNavigation = !event.url.includes('/change-password');
    });
    
    // Check initial route
    this.showNavigation = !this.router.url.includes('/change-password');
    
    // Load sidebar state from localStorage
    const savedState = localStorage.getItem('sidebarCollapsed');
    if (savedState !== null) {
      this.isSidebarCollapsed = savedState === 'true';
    }
    
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

      // Load organization info for non-ROOT users
      if (!this.isRoot && user?.organizationUuid) {
        this.loadOrganizationInfo();
      }
    }
  }

  loadOrganizationInfo(): void {
    // Use the /my-organization endpoint which doesn't require organization ID
    this.http.get<any>(`${environment.apiUrl}/organizations/my-organization`).subscribe({
      next: (org) => {
        this.organizationName = org.name || 'EMS';
        if (org.logoUrl) {
          // If logoUrl is relative, prepend the API base URL
          this.organizationLogoUrl = org.logoUrl.startsWith('http')
            ? org.logoUrl
            : `${environment.apiBaseUrl}${org.logoUrl}`;
        } else {
          this.organizationLogoUrl = '';
        }
      },
      error: (err) => {
        console.error('Failed to load organization info');
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

  toggleSidebar(): void {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
    // Save preference to localStorage
    localStorage.setItem('sidebarCollapsed', String(this.isSidebarCollapsed));
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
    this.closeMobileMenu();
  }
}
