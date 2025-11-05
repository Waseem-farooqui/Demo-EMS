import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router} from '@angular/router';
import {EmployeeService} from '../../services/employee.service';
import {AuthService} from '../../services/auth.service';

@Component({
  selector: 'app-profile-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile-create.component.html',
  styleUrls: ['./profile-create.component.css']
})
export class ProfileCreateComponent implements OnInit {
  profileForm!: FormGroup;
  loading = false;
  error = '';
  success = '';
  currentUser: any;
  maxDate: string; // Maximum date (today) for date inputs

  constructor(
    private fb: FormBuilder,
    private employeeService: EmployeeService,
    public authService: AuthService,
    private router: Router
  ) {
    // Set max date to today in YYYY-MM-DD format
    const today = new Date();
    this.maxDate = today.toISOString().split('T')[0];
  }

  ngOnInit(): void {
    this.currentUser = this.authService.getUser();
    this.initForm();
  }

  initForm(): void {
    this.profileForm = this.fb.group({
      fullName: ['', [Validators.required, Validators.minLength(3)]],
      personType: ['Employee', Validators.required],
      workEmail: [this.currentUser?.email || '', [Validators.required, Validators.email]],
      jobTitle: ['', Validators.required],
      reference: [''],
      dateOfJoining: ['', Validators.required],
      workingTiming: ['9:00 AM - 5:00 PM'],
      holidayAllowance: [20, [Validators.required, Validators.min(0)]]
    });
  }

  onSubmit(): void {
    if (this.profileForm.invalid) {
      this.error = 'Please fill all required fields correctly';
      this.markFormGroupTouched(this.profileForm);
      return;
    }

    this.loading = true;
    this.error = '';
    this.success = '';

    const profileData = {
      ...this.profileForm.value,
      userId: this.currentUser.id
    };

    this.employeeService.createSelfProfile(profileData).subscribe({
      next: (employee: any) => {
        this.success = 'Profile created successfully! Redirecting to dashboard...';

        // Update user data with employee ID
        const updatedUser = { ...this.currentUser, employeeId: employee.id };
        this.authService.saveUser(updatedUser);

        setTimeout(() => {
          this.router.navigate(['/dashboard']);
        }, 1500);
      },
      error: (err: any) => {
        this.loading = false;
        this.error = this.extractErrorMessage(err);
        console.error('Profile creation error:', err);

        // Scroll to error message
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    });
  }

  private extractErrorMessage(err: any): string {
    // Try different error response formats
    if (err.error) {
      // Standard error response from GlobalExceptionHandler
      if (err.error.message) {
        return err.error.message;
      }
      // String error
      if (typeof err.error === 'string') {
        return err.error;
      }
      // Validation errors with fields
      if (err.error.fields) {
        const fieldErrors = Object.entries(err.error.fields)
          .map(([field, msg]) => `${field}: ${msg}`)
          .join(', ');
        return `Validation failed: ${fieldErrors}`;
      }
    }

    // HTTP status based messages
    if (err.status === 409) {
      return 'This information already exists in the system. Please check your email address.';
    }
    if (err.status === 403) {
      return 'You don\'t have permission to perform this action.';
    }
    if (err.status === 401) {
      return 'Your session has expired. Please log in again.';
    }

    // Generic fallback
    return err.message || 'An error occurred while creating your profile. Please try again.';
  }

  private markFormGroupTouched(formGroup: any): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }
}

