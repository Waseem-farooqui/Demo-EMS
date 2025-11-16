import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-forgot-username',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-username.component.html',
  styleUrls: ['./forgot-username.component.css']
})
export class ForgotUsernameComponent {
  forgotUsernameForm: FormGroup;
  loading = false;
  error: string | null = null;
  success = false;
  email = '';

  private apiUrl = `${environment.apiUrl}/auth/forgot-username`;

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private router: Router
  ) {
    this.forgotUsernameForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  onSubmit(): void {
    if (this.forgotUsernameForm.invalid) {
      return;
    }

    this.loading = true;
    this.error = null;
    this.email = this.forgotUsernameForm.value.email;

    this.http.post<any>(this.apiUrl, { email: this.email }).subscribe({
      next: (response) => {
        this.success = true;
        this.loading = false;
      },
      error: (err) => {
        console.error('❌ Error sending username:', err);
        this.error = err.error?.message || err.error?.error || 'Failed to send username. Please try again.';
        this.loading = false;
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
