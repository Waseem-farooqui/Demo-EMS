import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router, RouterModule} from '@angular/router';
import {AuthService} from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  loginForm: FormGroup;
  loading = false;
  error: string | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      return;
    }

    this.loading = true;
    this.error = null;

    this.authService.login(this.loginForm.value).subscribe({
      next: (response) => {
        console.log('✅ Login successful');
        console.log('Response:', response);
        console.log('User roles:', response.roles);

        // Check temporaryPassword flag from response FIRST, then from storage as fallback
        const hasTemporaryPassword = response.temporaryPassword === true;

        if (hasTemporaryPassword) {
          console.log('🔒 Temporary password detected - Redirecting to password change');
          this.router.navigate(['/change-password']);
          this.loading = false;
          return;
        }

        // CRITICAL: Check user role and redirect accordingly
        const roles = response.roles || [];

        if (roles.includes('ROOT')) {
          console.log('👑 ROOT user detected - Redirecting to ROOT dashboard');
          this.router.navigate(['/root/dashboard']);
        } else if (roles.includes('SUPER_ADMIN')) {
          console.log('⭐ SUPER_ADMIN detected - Redirecting to employee dashboard');
          this.router.navigate(['/dashboard']);
        } else if (roles.includes('ADMIN')) {
          console.log('🔧 ADMIN detected - Redirecting to dashboard');
          this.router.navigate(['/dashboard']);
        } else {
          console.log('👤 Regular user - Redirecting to employee list');
          this.router.navigate(['/employees']);
        }

        this.loading = false;
      },
      error: (err) => {
        // Display detailed error message from backend
        if (err.error?.message) {
          this.error = err.error.message;
        } else if (err.status === 403) {
          this.error = 'Access denied. Your account or organization may be disabled.';
        } else if (err.status === 401) {
          this.error = 'Invalid username or password';
        } else {
          this.error = 'Login failed. Please try again.';
        }

        this.loading = false;
        console.error('Login error:', err);
      }
    });
  }
}

