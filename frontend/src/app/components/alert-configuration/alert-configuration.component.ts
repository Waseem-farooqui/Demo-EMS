import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AlertConfigurationService } from '../../services/alert-configuration.service';
import { AlertConfiguration } from '../../models/alert-configuration.model';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-alert-configuration',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './alert-configuration.component.html',
  styleUrls: ['./alert-configuration.component.css']
})
export class AlertConfigurationComponent implements OnInit {
  configurations: AlertConfiguration[] = [];
  loading = false;
  testingAlerts = false;
  editingConfig: AlertConfiguration | null = null;
  showAddForm = false;

  newConfig: AlertConfiguration = {
    documentType: '',
    alertDaysBefore: 30,
    alertEmail: '',
    enabled: true
  };

  documentTypes = [
    { value: 'PASSPORT', label: 'Passport' },
    { value: 'VISA', label: 'Visa' },
    { value: 'WORK_PERMIT', label: 'Work Permit' },
    { value: 'ID_CARD', label: 'ID Card' },
    { value: 'DRIVING_LICENSE', label: 'Driving License' },
    { value: 'CONTRACT', label: 'Contract' },
    { value: 'CERTIFICATE', label: 'Certificate' }
  ];

  constructor(
    private alertConfigService: AlertConfigurationService,
    private toastService: ToastService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Check if user is SUPER_ADMIN
    const user = this.authService.getUser();
    const roles = user?.roles || [];

    if (!roles.includes('SUPER_ADMIN')) {
      this.toastService.error('Access denied. Only SUPER_ADMIN can configure alerts.');
      this.router.navigate(['/dashboard']);
      return;
    }

    this.loadConfigurations();
  }

  loadConfigurations(): void {
    this.loading = true;
    this.alertConfigService.getAllConfigurations().subscribe({
      next: (configs) => {
        this.configurations = configs;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading configurations:', err);
        this.toastService.error('Failed to load alert configurations');
        this.loading = false;
      }
    });
  }

  openAddForm(): void {
    this.showAddForm = true;
    this.editingConfig = null;
    this.newConfig = {
      documentType: '',
      alertDaysBefore: 30,
      alertEmail: '',
      enabled: true
    };
  }

  closeAddForm(): void {
    this.showAddForm = false;
    this.newConfig = {
      documentType: '',
      alertDaysBefore: 30,
      alertEmail: '',
      enabled: true
    };
  }

  editConfiguration(config: AlertConfiguration): void {
    this.editingConfig = { ...config };
    this.showAddForm = false;
  }

  cancelEdit(): void {
    this.editingConfig = null;
  }

  saveConfiguration(): void {
    if (!this.newConfig.documentType || !this.newConfig.alertEmail) {
      this.toastService.warning('Please fill all required fields');
      return;
    }

    if (this.newConfig.alertDaysBefore < 1 || this.newConfig.alertDaysBefore > 365) {
      this.toastService.warning('Alert days must be between 1 and 365');
      return;
    }

    this.loading = true;
    this.alertConfigService.createConfiguration(this.newConfig).subscribe({
      next: (created) => {
        this.toastService.success('Alert configuration created successfully');
        this.loadConfigurations();
        this.closeAddForm();
      },
      error: (err) => {
        console.error('Error creating configuration:', err);
        this.toastService.error(err.error?.message || 'Failed to create configuration');
        this.loading = false;
      }
    });
  }

  updateConfiguration(): void {
    if (!this.editingConfig) return;

    if (!this.editingConfig.documentType || !this.editingConfig.alertEmail) {
      this.toastService.warning('Please fill all required fields');
      return;
    }

    if (this.editingConfig.alertDaysBefore < 1 || this.editingConfig.alertDaysBefore > 365) {
      this.toastService.warning('Alert days must be between 1 and 365');
      return;
    }

    this.loading = true;
    this.alertConfigService.updateConfiguration(this.editingConfig.id!, this.editingConfig).subscribe({
      next: (updated) => {
        this.toastService.success('Alert configuration updated successfully');
        this.loadConfigurations();
        this.cancelEdit();
      },
      error: (err) => {
        console.error('Error updating configuration:', err);
        this.toastService.error(err.error?.message || 'Failed to update configuration');
        this.loading = false;
      }
    });
  }

  toggleEnabled(config: AlertConfiguration): void {
    const updatedConfig = { ...config, enabled: !config.enabled };

    this.alertConfigService.updateConfiguration(config.id!, updatedConfig).subscribe({
      next: () => {
        this.toastService.success(`Alert ${updatedConfig.enabled ? 'enabled' : 'disabled'} for ${config.documentType}`);
        this.loadConfigurations();
      },
      error: (err) => {
        console.error('Error toggling configuration:', err);
        this.toastService.error('Failed to toggle alert status');
      }
    });
  }

  testAlerts(): void {
    if (confirm('This will check all documents and send expiry alerts. Continue?')) {
      this.testingAlerts = true;
      this.alertConfigService.testAlerts().subscribe({
        next: (response) => {
          this.toastService.success('Alert check triggered successfully. Check email and logs.');
          this.testingAlerts = false;
        },
        error: (err) => {
          console.error('Error testing alerts:', err);
          this.toastService.error('Failed to trigger alert check');
          this.testingAlerts = false;
        }
      });
    }
  }

  getDocumentTypeLabel(type: string): string {
    const doc = this.documentTypes.find(d => d.value === type);
    return doc ? doc.label : type;
  }

  getAlertStatus(config: AlertConfiguration): string {
    if (!config.enabled) return 'Disabled';

    if (config.alertDaysBefore <= 7) {
      return 'Critical (1 week)';
    } else if (config.alertDaysBefore <= 30) {
      return 'Warning (1 month)';
    } else if (config.alertDaysBefore <= 90) {
      return 'Notice (3 months)';
    } else {
      return 'Early Warning';
    }
  }

  getStatusClass(config: AlertConfiguration): string {
    if (!config.enabled) return 'status-disabled';

    if (config.alertDaysBefore <= 7) {
      return 'status-critical';
    } else if (config.alertDaysBefore <= 30) {
      return 'status-warning';
    } else {
      return 'status-notice';
    }
  }
}

