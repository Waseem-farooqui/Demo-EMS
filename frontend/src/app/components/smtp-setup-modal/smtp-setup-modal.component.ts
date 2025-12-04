import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { SmtpConfigService, SmtpConfiguration } from '../../services/smtp-config.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-smtp-setup-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './smtp-setup-modal.component.html',
  styleUrls: ['./smtp-setup-modal.component.css']
})
export class SmtpSetupModalComponent implements OnInit {
  @Output() closed = new EventEmitter<void>();
  @Output() configured = new EventEmitter<void>();

  smtpForm: FormGroup;
  saving = false;
  showInstructions = false;
  selectedProvider: 'GMAIL' | 'OUTLOOK' | 'CUSTOM' = 'GMAIL';
  showSkipOption = true;

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

  onSubmit(): void {
    if (this.smtpForm.invalid) {
      this.markFormGroupTouched(this.smtpForm);
      return;
    }

    this.saving = true;
    const formValue = this.smtpForm.value;

    const config: SmtpConfiguration = {
      provider: formValue.provider,
      host: formValue.host,
      port: formValue.port,
      username: formValue.username,
      password: formValue.password,
      fromEmail: formValue.fromEmail,
      fromName: formValue.fromName,
      enabled: formValue.enabled,
      useDefault: false
    };

    this.smtpService.saveSmtpConfiguration(config).subscribe({
      next: () => {
        this.saving = false;
        this.toastService.success('SMTP configuration saved successfully');
        sessionStorage.removeItem('showSmtpConfig');
        this.configured.emit();
        this.closed.emit();
      },
      error: (err) => {
        console.error('Error saving SMTP configuration:', err);
        this.saving = false;
        this.toastService.error('Failed to save SMTP configuration');
      }
    });
  }

  skipConfiguration(): void {
    // Set useDefault to true
    const config: SmtpConfiguration = {
      provider: 'GMAIL',
      useDefault: true
    };

    this.saving = true;
    this.smtpService.saveSmtpConfiguration(config).subscribe({
      next: () => {
        this.saving = false;
        this.toastService.info('Using default SMTP settings from environment variables');
        sessionStorage.removeItem('showSmtpConfig');
        this.configured.emit();
        this.closed.emit();
      },
      error: (err) => {
        console.error('Error saving SMTP configuration:', err);
        this.saving = false;
        this.toastService.error('Failed to save SMTP configuration');
      }
    });
  }

  closeModal(): void {
    this.closed.emit();
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

