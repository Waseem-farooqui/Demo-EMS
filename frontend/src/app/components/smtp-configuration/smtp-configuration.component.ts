import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { SmtpConfigService, SmtpConfiguration } from '../../services/smtp-config.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-smtp-configuration',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './smtp-configuration.component.html',
  styleUrls: ['./smtp-configuration.component.css']
})
export class SmtpConfigurationComponent implements OnInit {
  smtpForm: FormGroup;
  loading = false;
  saving = false;
  smtpConfig: SmtpConfiguration | null = null;
  showInstructions = false;
  selectedProvider: 'GMAIL' | 'OUTLOOK' | 'CUSTOM' = 'GMAIL';

  constructor(
    private fb: FormBuilder,
    private smtpService: SmtpConfigService,
    private toastService: ToastService
  ) {
    this.smtpForm = this.fb.group({
      provider: ['GMAIL', Validators.required],
      host: [''],
      port: [587],
      username: ['', Validators.required],
      password: ['', Validators.required],
      fromEmail: ['', [Validators.required, Validators.email]],
      fromName: ['Employee Management System'],
      enabled: [true],
      useDefault: [false]
    });
  }

  ngOnInit(): void {
    this.loadSmtpConfiguration();
    this.setupProviderWatcher();
  }

  setupProviderWatcher(): void {
    this.smtpForm.get('provider')?.valueChanges.subscribe(provider => {
      this.selectedProvider = provider;
      if (provider === 'GMAIL') {
        this.smtpForm.patchValue({
          host: 'smtp.gmail.com',
          port: 587
        });
      } else if (provider === 'OUTLOOK') {
        this.smtpForm.patchValue({
          host: 'smtp-mail.outlook.com',
          port: 587
        });
      }
    });
  }

  loadSmtpConfiguration(): void {
    this.loading = true;
    this.smtpService.getSmtpConfiguration().subscribe({
      next: (config) => {
        this.smtpConfig = config;
        if (config.isConfigured && !config.useDefault) {
          this.smtpForm.patchValue({
            provider: config.provider || 'GMAIL',
            host: config.host || '',
            port: config.port || 587,
            username: config.username || '',
            fromEmail: config.fromEmail || '',
            fromName: config.fromName || 'Employee Management System',
            enabled: config.enabled !== false,
            useDefault: false
          });
          this.selectedProvider = (config.provider as 'GMAIL' | 'OUTLOOK' | 'CUSTOM') || 'GMAIL';
        } else {
          // Default to Gmail
          this.smtpForm.patchValue({
            provider: 'GMAIL',
            host: 'smtp.gmail.com',
            port: 587,
            useDefault: true
          });
        }
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading SMTP configuration:', err);
        this.loading = false;
        this.toastService.error('Failed to load SMTP configuration');
      }
    });
  }

  onSubmit(): void {
    if (this.smtpForm.invalid) {
      this.markFormGroupTouched(this.smtpForm);
      return;
    }

    this.saving = true;
    const formValue = this.smtpForm.value;

    // If useDefault is true, don't send password
    const config: SmtpConfiguration = {
      provider: formValue.provider,
      host: formValue.host,
      port: formValue.port,
      username: formValue.username,
      password: formValue.useDefault ? undefined : formValue.password,
      fromEmail: formValue.fromEmail,
      fromName: formValue.fromName,
      enabled: formValue.enabled,
      useDefault: formValue.useDefault
    };

    this.smtpService.saveSmtpConfiguration(config).subscribe({
      next: (saved) => {
        this.smtpConfig = saved;
        this.saving = false;
        if (saved.useDefault) {
          this.toastService.success('SMTP configuration set to use default environment settings');
        } else {
          this.toastService.success('SMTP configuration saved successfully');
        }
      },
      error: (err) => {
        console.error('Error saving SMTP configuration:', err);
        this.saving = false;
        this.toastService.error('Failed to save SMTP configuration');
      }
    });
  }

  toggleUseDefault(): void {
    const useDefault = this.smtpForm.get('useDefault')?.value;
    if (useDefault) {
      this.smtpForm.get('password')?.clearValidators();
      this.smtpForm.get('password')?.updateValueAndValidity();
    } else {
      this.smtpForm.get('password')?.setValidators([Validators.required]);
      this.smtpForm.get('password')?.updateValueAndValidity();
    }
  }

  toggleInstructions(): void {
    this.showInstructions = !this.showInstructions;
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }
}

