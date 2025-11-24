import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import {Router} from '@angular/router';
import {HttpClient} from '@angular/common/http';
import {AuthService} from '../../services/auth.service';
import {ToastService} from '../../services/toast.service';
import {environment} from '../../../environments/environment';

@Component({
  selector: 'app-user-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './user-create.component.html',
  styleUrls: ['./user-create.component.css']
})
export class UserCreateComponent implements OnInit {
  userForm!: FormGroup;
  loading = false;
  error = '';
  success = '';
  currentUser: any;
  isSuperAdmin = false;
  maxDate: string; // Maximum date (today)

  departments: any[] = [];
  showCredentials = false;
  createdCredentials: any = null;
  showCustomDepartment = false;

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private toastService: ToastService,
    private authService: AuthService,
    public router: Router
  ) {
    // Set max date to today
    this.maxDate = this.formatDate(new Date());
  }

  ngOnInit(): void {
    this.currentUser = this.authService.getUser();
    // FIX: Check roles array instead of role property
    const roles = this.currentUser?.roles || [];
    this.isSuperAdmin = roles.includes('SUPER_ADMIN');


    this.initForm();
    this.loadDepartments();
  }

  initForm(): void {
    this.userForm = this.fb.group({
      fullName: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      jobTitle: ['', Validators.required],
      personType: ['Employee', Validators.required],
      role: ['USER', Validators.required],
      departmentId: [null, this.isSuperAdmin ? Validators.required : []],
      customDepartmentName: [''],
      reference: [''],
      dateOfJoining: [this.formatDate(new Date()), [Validators.required, this.dateNotInFutureValidator()]],
      employmentStatus: ['FULL_TIME', Validators.required],
      contractType: ['PERMANENT', Validators.required],
      workingTiming: ['9:00 AM - 5:00 PM'],
      holidayAllowance: [20, [Validators.required, Validators.min(0)]]
    });

    // Enable role selection for SUPER_ADMIN, disable for ADMIN
    if (this.isSuperAdmin) {
      this.userForm.get('role')?.enable();
      this.userForm.get('departmentId')?.enable();
    } else {
      this.userForm.get('role')?.disable();
      this.userForm.get('departmentId')?.disable();
    }
  }

  // Custom validator to ensure date is not in the future
  dateNotInFutureValidator() {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) {
        return null;
      }

      // Parse the selected date from the input (format: YYYY-MM-DD)
      const selectedDate = new Date(control.value + 'T00:00:00');

      // Get today's date at midnight for proper comparison
      const today = new Date();
      today.setHours(0, 0, 0, 0);

      // Check if selected date is after today
      if (selectedDate.getTime() > today.getTime()) {
        return { futureDate: true };
      }

      return null;
    };
  }

  loadDepartments(): void {
    // Use different endpoint for SUPER_ADMIN to get admin status
    const endpoint = this.isSuperAdmin
      ? `${environment.apiUrl}/departments/with-admin-status`
      : `${environment.apiUrl}/departments`;

    this.http.get<any[]>(endpoint).subscribe({
      next: (data) => {
        // Sort departments alphabetically by name
        this.departments = data.sort((a, b) => a.name.localeCompare(b.name));

        // Add "Create Custom" option for SUPER_ADMIN
        if (this.isSuperAdmin) {
          this.departments.push({
            id: 'custom',
            name: 'Create Custom Department',
            code: 'CUSTOM',
            hasAdmin: false
          });
        }

      },
      error: (err) => {
        console.error('Error loading departments:', err);
        this.error = 'Failed to load departments. Please refresh the page.';
      }
    });
  }

  onDepartmentChange(event: any): void {
    const selectedValue = event.target.value;
    if (selectedValue === 'custom') {
      this.showCustomDepartment = true;
      this.userForm.get('customDepartmentName')?.setValidators([Validators.required]);
      this.userForm.get('departmentId')?.clearValidators();
    } else {
      this.showCustomDepartment = false;
      this.userForm.get('customDepartmentName')?.clearValidators();
      this.userForm.get('customDepartmentName')?.setValue('');
      this.userForm.get('departmentId')?.setValidators([Validators.required]);
    }
    this.userForm.get('customDepartmentName')?.updateValueAndValidity();
    this.userForm.get('departmentId')?.updateValueAndValidity();
  }

  onSubmit(): void {
    if (this.userForm.invalid) {
      this.error = 'Please fill all required fields correctly';
      this.markFormGroupTouched(this.userForm);
      return;
    }

    this.loading = true;
    this.error = '';
    this.success = '';
    this.showCredentials = false;

    let formData = this.userForm.getRawValue(); // Get all values including disabled fields

    // Handle custom department creation
    if (this.showCustomDepartment && formData.customDepartmentName) {
      // First create the department
      this.createCustomDepartment(formData.customDepartmentName).subscribe({
        next: (newDept: any) => {
          formData.departmentId = newDept.id;
          delete formData.customDepartmentName;
          this.createUser(formData);
        },
        error: (err: any) => {
          this.loading = false;
          this.error = 'Failed to create custom department: ' + this.extractErrorMessage(err);
          window.scrollTo({ top: 0, behavior: 'smooth' });
        }
      });
    } else {
      delete formData.customDepartmentName;
      this.createUser(formData);
    }
  }

  createCustomDepartment(departmentName: string): any {
    const deptData = {
      name: departmentName,
      code: departmentName.substring(0, 3).toUpperCase(),
      description: 'Custom department created by SUPER_ADMIN',
      isActive: true
    };

    const token = this.authService.getToken();
    return this.http.post<any>(`${environment.apiUrl}/departments`, deptData, {
      headers: { Authorization: `Bearer ${token}` }
    });
  }

  createUser(formData: any): void {
    const token = this.authService.getToken();
    this.http.post<any>(`${environment.apiUrl}/users/create`, formData, {
      headers: { Authorization: `Bearer ${token}` }
    }).subscribe({
      next: (response) => {
        this.success = response.message;
        this.createdCredentials = response;
        this.showCredentials = true;
        this.loading = false;

        // Scroll to top to show credentials
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error: (err) => {
        this.loading = false;
        this.error = this.extractErrorMessage(err);
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    });
  }

  copyToClipboard(text: string, type: string): void {
    // Check if text is valid
    if (!text || text.trim() === '') {
      this.toastService.warning(`No ${type} to copy`);
      return;
    }

    // Try modern Clipboard API first (requires HTTPS or localhost)
    const isSecureContext = window.isSecureContext || 
                           window.location.protocol === 'https:' || 
                           window.location.hostname === 'localhost' || 
                           window.location.hostname === '127.0.0.1';

    if (navigator.clipboard && isSecureContext) {
      navigator.clipboard.writeText(text).then(() => {
        this.toastService.success(`${type} copied to clipboard!`);
      }).catch(err => {
        console.error('Clipboard API failed, trying fallback:', err);
        this.fallbackCopyToClipboard(text, type);
      });
    } else {
      // Fallback for non-HTTPS environments
      this.fallbackCopyToClipboard(text, type);
    }
  }

  private fallbackCopyToClipboard(text: string, type: string): void {
    // Create a temporary textarea element
    const textArea = document.createElement('textarea');
    textArea.value = text;
    // Position it off-screen but still in the viewport for better compatibility
    textArea.style.position = 'fixed';
    textArea.style.left = '0';
    textArea.style.top = '0';
    textArea.style.width = '2em';
    textArea.style.height = '2em';
    textArea.style.padding = '0';
    textArea.style.border = 'none';
    textArea.style.outline = 'none';
    textArea.style.boxShadow = 'none';
    textArea.style.background = 'transparent';
    textArea.style.opacity = '0';
    textArea.style.zIndex = '-9999';
    textArea.setAttribute('readonly', '');
    textArea.setAttribute('aria-hidden', 'true');
    
    document.body.appendChild(textArea);
    
    // Select the text
    textArea.focus();
    textArea.select();
    textArea.setSelectionRange(0, text.length); // For mobile devices

    try {
      // Use the older execCommand method as fallback
      const successful = document.execCommand('copy');
      if (successful) {
        this.toastService.success(`${type} copied to clipboard!`);
      } else {
        throw new Error('execCommand copy failed');
      }
    } catch (err) {
      console.error('Fallback copy failed:', err);
      // Last resort: show the text in a prompt or alert
      try {
        const copied = prompt(`Please copy the ${type}:\n\nPress Ctrl+C (Cmd+C on Mac) to copy:`, text);
        if (copied !== null) {
          this.toastService.info(`${type} displayed in prompt - please copy manually`);
        }
      } catch (promptErr) {
        // If prompt is blocked, show in alert
        alert(`Please copy this ${type}:\n\n${text}`);
        this.toastService.warning(`Please manually copy the ${type} from the alert`);
      }
    } finally {
      // Clean up
      if (document.body.contains(textArea)) {
        document.body.removeChild(textArea);
      }
    }
  }

  createAnother(): void {
    this.showCredentials = false;
    this.createdCredentials = null;
    this.success = '';
    this.userForm.reset({
      personType: 'Employee',
      role: 'USER',
      dateOfJoining: this.formatDate(new Date()),
      workingTiming: '9:00 AM - 5:00 PM',
      holidayAllowance: 20
    });
  }

  goToEmployees(): void {
    this.router.navigate(['/employees']);
  }

  /**
   * Check if a department should be disabled in the dropdown
   * Department is disabled if:
   * 1. User is SUPER_ADMIN
   * 2. Selected role is ADMIN
   * 3. Department already has an ADMIN assigned
   */
  isDepartmentDisabled(department: any): boolean {
    if (!this.isSuperAdmin) {
      return false; // Non-SUPER_ADMIN can't create ADMIN anyway
    }

    const selectedRole = this.userForm.get('role')?.value;

    // Only disable if creating ADMIN role and department already has admin
    return selectedRole === 'ADMIN' && department.hasAdmin;
  }

  /**
   * Get display name for department option
   * Shows (Already has Admin) suffix for departments that can't be selected
   */
  getDepartmentDisplayName(department: any): string {
    if (this.isDepartmentDisabled(department)) {
      return `${department.name} (Already has Admin)`;
    }
    return department.name;
  }

  /**
   * Handle role change - reload departments when switching between USER and ADMIN
   */
  onRoleChange(): void {
    // Clear department selection when role changes
    this.userForm.get('departmentId')?.setValue(null);
  }

  private extractErrorMessage(err: any): string {
    if (err.error?.message) {
      return err.error.message;
    }
    if (typeof err.error === 'string') {
      return err.error;
    }
    if (err.status === 409) {
      return 'This email already exists in the system.';
    }
    if (err.status === 403) {
      return 'You don\'t have permission to perform this action.';
    }
    return 'An error occurred while creating the user. Please try again.';
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}

