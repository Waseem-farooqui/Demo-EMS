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
    enabled: true,
    alertPriority: 'WARNING',
    notificationType: 'EMAIL',
    alertFrequency: 'ONCE',
    repeatUntilResolved: false
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

  alertPriorities = [
    { value: 'EXPIRED', label: '🔴 Expired', days: 0, color: '#dc3545' },
    { value: 'CRITICAL', label: '🟠 Critical', days: 7, color: '#fd7e14' },
    { value: 'WARNING', label: '🟡 Warning', days: 30, color: '#ffc107' },
    { value: 'ATTENTION', label: '🔵 Attention', days: 90, color: '#17a2b8' }
  ];

  notificationTypes = [
    { value: 'EMAIL', label: '📧 Email Only', icon: '📧' },
    { value: 'NOTIFICATION', label: '🔔 In-App Only', icon: '🔔' },
    { value: 'BOTH', label: '📧🔔 Both', icon: '📧🔔' }
  ];

  alertFrequencies = [
    { value: 'ONCE', label: '🔂 Once (then every 7 days)', description: 'Send alert once, then repeat every 7 days if still expiring' },
    { value: 'DAILY', label: '📅 Daily', description: 'Send alert once per day while document is expiring' },
    { value: 'HOURLY', label: '⏰ Every Hour', description: 'Send alert every hour (for critical documents)' }
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
        // Handle JSON error response
        let errorMsg = 'Failed to load alert configurations';
        if (err.error) {
          if (typeof err.error === 'string') {
            errorMsg = err.error;
          } else if (err.error.message) {
            errorMsg = err.error.message;
          } else if (err.error.error) {
            errorMsg = err.error.error;
          }
        }
        this.toastService.error(errorMsg);
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
      enabled: true,
      alertPriority: 'WARNING',
      notificationType: 'EMAIL'
    };
  }

  closeAddForm(): void {
    this.showAddForm = false;
    this.newConfig = {
      documentType: '',
      alertDaysBefore: 30,
      alertEmail: '',
      enabled: true,
      alertPriority: 'WARNING',
      notificationType: 'EMAIL'
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
    if (!this.newConfig.documentType || !this.newConfig.alertEmail || !this.newConfig.alertPriority || !this.newConfig.notificationType) {
      this.toastService.warning('Please fill all required fields');
      return;
    }

    if (this.newConfig.alertDaysBefore < 0 || this.newConfig.alertDaysBefore > 365) {
      this.toastService.warning('Alert days must be between 0 and 365');
      return;
    }

    this.loading = true;
    this.alertConfigService.createConfiguration(this.newConfig).subscribe({
      next: (created) => {
        this.toastService.success(`Alert configuration created: ${this.newConfig.alertPriority} priority for ${this.newConfig.documentType}`);
        this.loadConfigurations();
        this.closeAddForm();
      },
      error: (err) => {
        console.error('Error creating configuration:', err);
        // Handle JSON error response
        let errorMsg = 'Failed to create configuration';
        if (err.error) {
          if (typeof err.error === 'string') {
            errorMsg = err.error;
          } else if (err.error.message) {
            errorMsg = err.error.message;
          } else if (err.error.error) {
            errorMsg = err.error.error;
          }
        }
        this.toastService.error(errorMsg);
        this.loading = false;
      }
    });
  }

  updateConfiguration(): void {
    if (!this.editingConfig) return;

    if (!this.editingConfig.documentType || !this.editingConfig.alertEmail || !this.editingConfig.alertPriority || !this.editingConfig.notificationType) {
      this.toastService.warning('Please fill all required fields');
      return;
    }

    if (this.editingConfig.alertDaysBefore < 0 || this.editingConfig.alertDaysBefore > 365) {
      this.toastService.warning('Alert days must be between 0 and 365');
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
        // Handle JSON error response
        let errorMsg = 'Failed to update configuration';
        if (err.error) {
          if (typeof err.error === 'string') {
            errorMsg = err.error;
          } else if (err.error.message) {
            errorMsg = err.error.message;
          } else if (err.error.error) {
            errorMsg = err.error.error;
          }
        }
        this.toastService.error(errorMsg);
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
        next: (response: any) => {
          const message = response?.message || 'Alert check triggered successfully. Check email and logs.';
          this.toastService.success(message);
          this.testingAlerts = false;
        },
        error: (err) => {
          console.error('Error testing alerts:', err);
          // Handle JSON error response
          let errorMsg = 'Failed to trigger alert check';
          if (err.error) {
            if (typeof err.error === 'string') {
              errorMsg = err.error;
            } else if (err.error.message) {
              errorMsg = err.error.message;
            } else if (err.error.error) {
              errorMsg = err.error.error;
            }
          }
          this.toastService.error(errorMsg);
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

    const priorityLabels: any = {
      'EXPIRED': 'Expired',
      'CRITICAL': 'Critical',
      'WARNING': 'Warning',
      'ATTENTION': 'Attention'
    };

    return priorityLabels[config.alertPriority] || config.alertPriority;
  }

  getStatusClass(config: AlertConfiguration): string {
    if (!config.enabled) return 'status-disabled';

    const statusClasses: any = {
      'EXPIRED': 'status-expired',
      'CRITICAL': 'status-critical',
      'WARNING': 'status-warning',
      'ATTENTION': 'status-attention'
    };

    return statusClasses[config.alertPriority] || 'status-notice';
  }

  getPriorityLabel(priority: string): string {
    const priorityObj = this.alertPriorities.find(p => p.value === priority);
    return priorityObj ? priorityObj.label : priority;
  }

  getNotificationTypeLabel(type: string): string {
    const typeObj = this.notificationTypes.find(t => t.value === type);
    return typeObj ? typeObj.label : type;
  }

  getFrequencyDescription(frequency?: string): string {
    const freqObj = this.alertFrequencies.find(f => f.value === frequency);
    return freqObj ? freqObj.description : 'Select frequency to see description';
  }

  getFrequencyLabel(frequency?: string): string {
    const freqObj = this.alertFrequencies.find(f => f.value === frequency);
    return freqObj ? freqObj.label : 'Once';
  }
}

