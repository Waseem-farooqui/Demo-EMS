import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-organization-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './organization-create.component.html',
  styleUrls: ['./organization-create.component.css']
})
export class OrganizationCreateComponent {
  organizationForm: FormGroup;
  loading = false;
  error: string | null = null;
  success = false;

  private apiUrl = 'http://localhost:8080/api/organizations';

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private router: Router
  ) {
    this.organizationForm = this.fb.group({
      organizationName: ['', [Validators.required, Validators.minLength(2)]],
      superAdminUsername: ['', [Validators.required, Validators.minLength(3)]],
      superAdminEmail: ['', [Validators.required, Validators.email]],
      superAdminFullName: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(form: FormGroup) {
    const password = form.get('password');
    const confirmPassword = form.get('confirmPassword');

    if (password && confirmPassword && password.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }
    return null;
  }

  onSubmit(): void {
    if (this.organizationForm.invalid) {
      this.markFormGroupTouched(this.organizationForm);
      return;
    }

    this.loading = true;
    this.error = null;

    const formData = { ...this.organizationForm.value };
    delete formData.confirmPassword; // Don't send confirm password to backend

    this.http.post<any>(this.apiUrl, formData).subscribe({
      next: (response) => {
        console.log('✅ Organization created successfully:', response);
        this.success = true;
        this.loading = false;

        // Show success message for 2 seconds then redirect
        setTimeout(() => {
          this.router.navigate(['/root/dashboard']);
        }, 2000);
      },
      error: (err) => {
        console.error('❌ Error creating organization:', err);
        this.error = err.error?.message || err.error?.error || 'Failed to create organization';
        this.loading = false;
      }
    });
  }

  private markFormGroupTouched(formGroup: FormGroup) {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  cancel(): void {
    this.router.navigate(['/root/dashboard']);
  }

  getFieldError(fieldName: string): string {
    const field = this.organizationForm.get(fieldName);
    if (field?.touched && field?.errors) {
      if (field.errors['required']) return `${this.getFieldLabel(fieldName)} is required`;
      if (field.errors['email']) return 'Invalid email format';
      if (field.errors['minlength']) return `Minimum ${field.errors['minlength'].requiredLength} characters required`;
      if (field.errors['passwordMismatch']) return 'Passwords do not match';
    }
    return '';
  }

  private getFieldLabel(fieldName: string): string {
    const labels: { [key: string]: string } = {
      organizationName: 'Organization Name',
      superAdminUsername: 'Username',
      superAdminEmail: 'Email',
      superAdminFullName: 'Full Name',
      password: 'Password',
      confirmPassword: 'Confirm Password'
    };
    return labels[fieldName] || fieldName;
  }
}

