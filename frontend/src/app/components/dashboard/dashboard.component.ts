import {Component, Inject, OnInit, PLATFORM_ID} from '@angular/core';
import {CommonModule, isPlatformBrowser} from '@angular/common';
import {Router, RouterModule} from '@angular/router';
import {HttpClient} from '@angular/common/http';
import {AuthService} from '../../services/auth.service';
import {EmployeeService} from '../../services/employee.service';
import {DocumentService} from '../../services/document.service';
import {AttendanceService} from '../../services/attendance.service';

// Import Chart.js dynamically only in browser
import {Chart, registerables} from 'chart.js';

if (typeof window !== 'undefined') {
  Chart.register(...registerables);
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  currentUser: any;
  isAdmin = false;
  isSuperAdmin = false;
  loading = true;
  isBrowser = false;

  // SUPER_ADMIN dashboard stats
  dashboardStats: any = null;
  charts: any = {};

  // User profile status
  hasProfile = false;
  employeeProfile: any = null;

  // Document status
  hasDocuments = false;
  documentCount = 0;
  recentDocuments: any[] = [];

  // Attendance status
  isCheckedIn = false;
  attendanceStatus: any = null;

  constructor(
    private authService: AuthService,
    private employeeService: EmployeeService,
    private documentService: DocumentService,
    private attendanceService: AttendanceService,
    private http: HttpClient,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit(): void {
    this.currentUser = this.authService.getUser();
    const roles = this.currentUser?.roles || [];

    // Check if ROOT user - redirect to ROOT dashboard
    const isRoot = roles.includes('ROOT');
    if (isRoot) {
      this.router.navigate(['/root/dashboard']);
      return;
    }

    this.isSuperAdmin = roles.includes('SUPER_ADMIN');
    const isAdmin = roles.includes('ADMIN');
    this.isAdmin = this.isSuperAdmin || isAdmin;

    if (this.isSuperAdmin) {
      // SUPER_ADMIN sees dashboard with statistics
      this.loadSuperAdminDashboard();
    } else if (isAdmin) {
      // ADMIN (department manager) goes to employees
      this.router.navigate(['/employees']);
    } else {
      // Regular USER - check profile and documents
      this.checkUserStatus();
    }
  }

  loadSuperAdminDashboard(): void {
    this.loading = true;
    const token = this.authService.getToken();

    this.http.get<any>('http://localhost:8080/api/dashboard/stats', {
      headers: { Authorization: `Bearer ${token}` }
    }).subscribe({
      next: (stats) => {
        this.dashboardStats = stats;
        this.loading = false;
        // Create charts after view init
        setTimeout(() => this.createCharts(), 100);
      },
      error: (err) => {
        console.error('Error loading dashboard stats:', err);
        this.loading = false;
      }
    });
  }

  createCharts(): void {
    if (!this.isBrowser) return; // Only run in browser
    this.createDepartmentChart();
    this.createWorkLocationChart();
    this.createLeaveStatusChart();
    this.createDocumentExpiryChart();
  }

  createDepartmentChart(): void {
    const canvas = document.getElementById('departmentChart') as HTMLCanvasElement;
    if (!canvas || !this.dashboardStats?.employeesByDepartment) return;

    const data = this.dashboardStats.employeesByDepartment;
    const labels = Object.keys(data);
    const values = Object.values(data) as number[];

    if (this.charts.department) {
      this.charts.department.destroy();
    }

    this.charts.department = new Chart(canvas, {
      type: 'pie',
      data: {
        labels: labels,
        datasets: [{
          data: values,
          backgroundColor: [
            '#3B82F6', '#10B981', '#F59E0B', '#EF4444',
            '#8B5CF6', '#EC4899', '#06B6D4', '#84CC16'
          ],
          borderWidth: 2,
          borderColor: '#fff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: { padding: 15, font: { size: 12 } }
          },
          title: {
            display: true,
            text: 'Employees by Department',
            font: { size: 16, weight: 'bold' }
          }
        },
        onClick: (event, activeElements) => {
          if (activeElements.length > 0) {
            const index = activeElements[0].index;
            const departmentName = labels[index];
            this.onDepartmentChartClick(departmentName);
          }
        }
      }
    });
  }

  createWorkLocationChart(): void {
    const canvas = document.getElementById('workLocationChart') as HTMLCanvasElement;
    if (!canvas || !this.dashboardStats?.employeesByWorkLocation) return;

    const data = this.dashboardStats.employeesByWorkLocation;
    const labels = Object.keys(data);
    const values = Object.values(data) as number[];

    if (this.charts.workLocation) {
      this.charts.workLocation.destroy();
    }

    this.charts.workLocation = new Chart(canvas, {
      type: 'pie',
      data: {
        labels: labels,
        datasets: [{
          data: values,
          backgroundColor: [
            '#3B82F6', '#10B981', '#F59E0B', '#8B5CF6',
            '#EC4899', '#06B6D4', '#EF4444'
          ],
          borderWidth: 2,
          borderColor: '#fff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: { padding: 15, font: { size: 12 } }
          },
          title: {
            display: true,
            text: 'Employees by Work Location',
            font: { size: 16, weight: 'bold' }
          }
        }
      }
    });
  }

  createLeaveStatusChart(): void {
    const canvas = document.getElementById('leaveStatusChart') as HTMLCanvasElement;
    if (!canvas || !this.dashboardStats) return;

    const onLeave = this.dashboardStats.employeesOnLeave || 0;
    const working = this.dashboardStats.employeesWorking || 0;

    if (this.charts.leaveStatus) {
      this.charts.leaveStatus.destroy();
    }

    this.charts.leaveStatus = new Chart(canvas, {
      type: 'pie',
      data: {
        labels: ['On Leave', 'Working'],
        datasets: [{
          data: [onLeave, working],
          backgroundColor: ['#F59E0B', '#10B981'],
          borderWidth: 2,
          borderColor: '#fff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: { padding: 15, font: { size: 12 } }
          },
          title: {
            display: true,
            text: 'Employee Leave Status',
            font: { size: 16, weight: 'bold' }
          }
        }
      }
    });
  }

  createDocumentExpiryChart(): void {
    const canvas = document.getElementById('documentExpiryChart') as HTMLCanvasElement;
    if (!canvas || !this.dashboardStats) return;

    const expired = this.dashboardStats.documentsExpired || 0;
    const expiring30 = this.dashboardStats.documentsExpiringIn30Days || 0;
    const expiring60 = this.dashboardStats.documentsExpiringIn60Days || 0;

    if (this.charts.documentExpiry) {
      this.charts.documentExpiry.destroy();
    }

    this.charts.documentExpiry = new Chart(canvas, {
      type: 'pie',
      data: {
        labels: ['Expired', 'Expiring in 30 Days', 'Expiring in 31-60 Days'],
        datasets: [{
          data: [expired, expiring30, expiring60],
          backgroundColor: ['#EF4444', '#F59E0B', '#FCD34D'],
          borderWidth: 2,
          borderColor: '#fff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: { padding: 15, font: { size: 12 } }
          },
          title: {
            display: true,
            text: 'Documents Near Expiry',
            font: { size: 16, weight: 'bold' }
          }
        },
        onClick: (event, activeElements) => {
          if (activeElements.length > 0) {
            const index = activeElements[0].index;
            this.onDocumentExpiryChartClick(index);
          }
        }
      }
    });
  }

  checkUserStatus(): void {
    this.loading = true;

    // Check if employee profile exists by work email
    // This handles cases where admin created the profile
    const userEmail = this.currentUser?.email;

    if (!userEmail) {
      console.error('No email found for current user');
      this.loading = false;
      this.hasProfile = false;
      return;
    }

    // Fetch all employees and check if one matches this user's email
    this.employeeService.getAllEmployees().subscribe({
      next: (employees) => {
        const employeeProfile = employees.find(emp => emp.workEmail === userEmail);

        if (employeeProfile) {
          // Profile exists! Load it
          this.hasProfile = true;
          this.employeeProfile = employeeProfile;
          console.log('✅ Employee profile found for user:', userEmail);
          this.loadDocuments();
          this.loadAttendanceStatus();
        } else {
          // No profile exists - user needs to create one
          this.hasProfile = false;
          this.loading = false;
          console.log('⚠️ No employee profile found for user:', userEmail);
        }
      },
      error: (err) => {
        console.error('Error checking employee profile:', err);
        this.hasProfile = false;
        this.loading = false;
      }
    });
  }

  loadDocuments(): void {
    if (!this.employeeProfile?.id) return;

    this.documentService.getDocumentsByEmployeeId(this.employeeProfile.id).subscribe({
      next: (documents: any[]) => {
        this.hasDocuments = documents.length > 0;
        this.documentCount = documents.length;
        this.recentDocuments = documents.slice(0, 3); // Get 3 most recent
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading documents:', err);
        this.hasDocuments = false;
        this.loading = false;
      }
    });
  }

  loadAttendanceStatus(): void {
    if (!this.employeeProfile?.id) return;

    this.attendanceService.getCurrentStatus(this.employeeProfile.id).subscribe({
      next: (status) => {
        if (status.status === 'CHECKED_OUT') {
          this.isCheckedIn = false;
        } else {
          this.isCheckedIn = true;
          this.attendanceStatus = status;
        }
      },
      error: (err) => {
        console.error('Error loading attendance:', err);
      }
    });
  }

  navigateToCreateProfile(): void {
    this.router.navigate(['/profile/create']);
  }

  navigateToUploadDocument(): void {
    this.router.navigate(['/documents/upload']);
  }

  navigateToDocuments(): void {
    this.router.navigate(['/documents']);
  }

  navigateToAttendance(): void {
    this.router.navigate(['/attendance']);
  }

  /**
   * Handle click on department chart segment
   */
  onDepartmentChartClick(departmentName: string): void {
    console.log('Department chart clicked:', departmentName);
    // Navigate to employees page with department filter
    this.router.navigate(['/employees'], {
      queryParams: { department: departmentName }
    });
  }

  /**
   * Handle click on document expiry chart segment
   */
  onDocumentExpiryChartClick(index: number): void {
    console.log('Document expiry chart clicked, index:', index);
    let filter: string;

    switch(index) {
      case 0: // Expired
        filter = 'expired';
        break;
      case 1: // Expiring in 30 days
        filter = 'expiring30';
        break;
      case 2: // Expiring in 31-60 days
        filter = 'expiring60';
        break;
      default:
        filter = 'all';
    }

    // Navigate to documents page with expiry filter
    this.router.navigate(['/documents'], {
      queryParams: { expiryFilter: filter }
    });
  }
}

