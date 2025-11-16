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
  showLogo = true; // Start with logo visible, will hide if it fails to load
  logoSrc = 'assets/logo.png'; // Try PNG first, fallback to SVG if needed

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

  onLogoError(event: Event): void {
    const img = event.target as HTMLImageElement;
    
    // If PNG fails, try SVG
    if (this.logoSrc.endsWith('.png')) {
      this.logoSrc = 'assets/logo.svg';
      img.src = this.logoSrc;
      return;
    }
    
    // If both fail, hide logo and show fallback icon
    this.showLogo = false;
    console.log('Logo image not found, showing fallback icon');
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      return;
    }

    this.loading = true;
    this.error = null;

    this.authService.login(this.loginForm.value).subscribe({
      next: (response) => {
        // Check temporaryPassword flag from response FIRST, then from storage as fallback
        const hasTemporaryPassword = response.temporaryPassword === true;

        if (hasTemporaryPassword) {
          this.router.navigate(['/change-password']);
          this.loading = false;
          return;
        }

        // CRITICAL: Check user role and redirect accordingly
        const roles = response.roles || [];

        if (roles.includes('ROOT')) {
          this.router.navigate(['/root/dashboard']);
        } else if (roles.includes('SUPER_ADMIN')) {
          this.router.navigate(['/dashboard']);
        } else if (roles.includes('ADMIN')) {
          this.router.navigate(['/dashboard']);
        } else {
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

