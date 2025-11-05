import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormArray, FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router} from '@angular/router';
import {RotaService, RotaUploadPreview} from '../../services/rota.service';
import {EmployeeService} from '../../services/employee.service';
import {ToastService} from '../../services/toast.service';

@Component({
  selector: 'app-rota-upload',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './rota-upload.component.html',
  styleUrls: ['./rota-upload.component.css']
})
export class RotaUploadComponent implements OnInit {
  // Upload mode selection
  uploadMode: 'select' | 'excel' | 'image' | 'manual' | 'preview' = 'select';

  // File upload
  selectedFile: File | null = null;
  fileName: string = '';
  uploading = false;
  previewUrl: string | null = null;

  // Excel upload preview
  excelPreview: RotaUploadPreview | null = null;
  showExcelPreview = false;

  // Image upload result
  imageUploadResult: any = null;
  showManualCorrectionPrompt = false;

  // Manual form
  manualForm!: FormGroup;
  employees: any[] = [];
  weekDates: Date[] = [];

  constructor(
    private rotaService: RotaService,
    private employeeService: EmployeeService,
    private toastService: ToastService,
    private router: Router,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.loadEmployees();
    this.initializeManualForm();
  }

  // ==================== Mode Selection ====================

  selectMode(mode: 'excel' | 'image' | 'manual'): void {
    this.uploadMode = mode;
    this.clearSelection();
  }

  backToSelection(): void {
    this.uploadMode = 'select';
    this.clearSelection();
  }

  // ==================== File Selection ====================

  onFileSelected(event: any, type: 'excel' | 'image'): void {
    const file = event.target.files[0];
    if (!file) return;

    if (type === 'excel') {
      this.handleExcelFileSelection(file);
    } else {
      this.handleImageFileSelection(file);
    }
  }

  handleExcelFileSelection(file: File): void {
    // Validate file type
    if (!file.name.toLowerCase().endsWith('.xlsx')) {
      this.toastService.error('Please select an Excel file (.xlsx)');
      return;
    }

    // Validate file size (max 10MB)
    if (file.size > 10 * 1024 * 1024) {
      this.toastService.error('File size must be less than 10MB');
      return;
    }

    this.selectedFile = file;
    this.fileName = file.name;
  }

  handleImageFileSelection(file: File): void {
    // Validate file type
    if (!file.type.startsWith('image/')) {
      this.toastService.error('Please select an image file (PNG, JPG, JPEG)');
      return;
    }

    // Validate file size (max 10MB)
    if (file.size > 10 * 1024 * 1024) {
      this.toastService.error('File size must be less than 10MB');
      return;
    }

    this.selectedFile = file;
    this.fileName = file.name;

    // Generate preview
    const reader = new FileReader();
    reader.onload = (e: any) => {
      this.previewUrl = e.target.result;
    };
    reader.readAsDataURL(file);
  }

  clearSelection(): void {
    this.selectedFile = null;
    this.fileName = '';
    this.previewUrl = null;
    this.imageUploadResult = null;
    this.showManualCorrectionPrompt = false;
    this.excelPreview = null;
    this.showExcelPreview = false;
  }

  // ==================== Excel Upload ====================

  uploadExcel(): void {
    if (!this.selectedFile) {
      this.toastService.warning('Please select an Excel file to upload');
      return;
    }

    this.uploading = true;
    this.toastService.info('Uploading Excel ROTA... This will be 100% accurate!');

    this.rotaService.uploadExcelRota(this.selectedFile).subscribe({
      next: (preview) => {
        this.excelPreview = preview;
        this.uploading = false;
        this.uploadMode = 'preview';
        this.showExcelPreview = true;
        this.toastService.success(`Excel ROTA uploaded! Found ${preview.totalEmployees} employees with ${preview.totalSchedules} schedules. Please review and confirm.`);
      },
      error: (err) => {
        const errorMsg = err.error?.message || 'Failed to upload Excel ROTA. Please try again.';
        this.toastService.error(errorMsg);
        this.uploading = false;
      }
    });
  }

  // Accept uploaded ROTA and navigate to list
  acceptExcelUpload(): void {
    this.toastService.success('ROTA accepted and saved successfully!');
    setTimeout(() => {
      this.router.navigate(['/rota']);
    }, 1000);
  }

  // Delete uploaded ROTA if user is not satisfied
  deleteExcelUpload(): void {
    if (!this.excelPreview?.rotaId) return;

    if (!confirm('Are you sure you want to delete this ROTA? This action cannot be undone.')) {
      return;
    }

    this.uploading = true;
    this.toastService.info('Deleting ROTA...');

    this.rotaService.deleteRota(this.excelPreview.rotaId).subscribe({
      next: () => {
        this.toastService.success('ROTA deleted successfully. You can upload again.');
        this.uploading = false;
        this.uploadMode = 'excel';
        this.clearSelection();
      },
      error: (err) => {
        const errorMsg = err.error?.error || 'Failed to delete ROTA';
        this.toastService.error(errorMsg);
        this.uploading = false;
      }
    });
  }

  // ==================== Image Upload ====================

  uploadImage(): void {
    if (!this.selectedFile) {
      this.toastService.warning('Please select an image to upload');
      return;
    }

    this.uploading = true;
    this.toastService.info('Uploading and processing ROTA image... Using OCR...');

    this.rotaService.uploadRota(this.selectedFile).subscribe({
      next: (result) => {
        this.imageUploadResult = result;
        this.uploading = false;

        // Show prompt for manual correction
        this.showManualCorrectionPrompt = true;
        this.toastService.warning('Image processed! Please review - OCR may have errors. You can manually correct or accept as-is.');
      },
      error: (err) => {
        const errorMsg = err.error?.error || 'Failed to process image. OCR quality may be poor.';
        this.toastService.error(errorMsg);
        this.uploading = false;

        // Offer manual entry option
        this.showManualCorrectionPrompt = true;
      }
    });
  }

  acceptImageResult(): void {
    this.toastService.success('ROTA accepted and saved!');
    setTimeout(() => {
      this.router.navigate(['/rota']);
    }, 1500);
  }

  switchToManualEntry(): void {
    this.toastService.info('Switching to manual entry mode...');
    this.uploadMode = 'manual';
    this.showManualCorrectionPrompt = false;

    // Pre-populate form with image result if available
    if (this.imageUploadResult) {
      this.prefillFormFromImageResult();
    }
  }

  // ==================== Manual Form Entry ====================

  loadEmployees(): void {
    this.employeeService.getAllEmployees().subscribe({
      next: (employees) => {
        this.employees = employees;
      },
      error: (err) => {
        this.toastService.error('Failed to load employees');
      }
    });
  }

  initializeManualForm(): void {
    // Generate 7 days starting from today
    const today = new Date();
    this.weekDates = [];
    for (let i = 0; i < 7; i++) {
      const date = new Date(today);
      date.setDate(today.getDate() + i);
      this.weekDates.push(date);
    }

    this.manualForm = this.fb.group({
      hotelName: ['', Validators.required],
      department: ['', Validators.required],
      startDate: [this.formatDate(this.weekDates[0]), Validators.required],
      endDate: [this.formatDate(this.weekDates[6]), Validators.required],
      schedules: this.fb.array([])
    });
  }

  get schedules(): FormArray {
    return this.manualForm.get('schedules') as FormArray;
  }

  addEmployeeSchedule(): void {
    const scheduleGroup = this.fb.group({
      employeeId: ['', Validators.required],
      shifts: this.fb.array(
        this.weekDates.map(() => this.fb.control(''))
      )
    });
    this.schedules.push(scheduleGroup);
  }

  removeEmployeeSchedule(index: number): void {
    this.schedules.removeAt(index);
  }

  getShifts(scheduleIndex: number): FormArray {
    return this.schedules.at(scheduleIndex).get('shifts') as FormArray;
  }

  prefillFormFromImageResult(): void {
    if (!this.imageUploadResult) return;

    // Fill basic info
    this.manualForm.patchValue({
      hotelName: this.imageUploadResult.hotelName || '',
      department: this.imageUploadResult.department || '',
      startDate: this.imageUploadResult.startDate || '',
      endDate: this.imageUploadResult.endDate || ''
    });

    // TODO: Add logic to prefill employee schedules from OCR result
  }

  submitManualForm(): void {
    if (this.manualForm.invalid) {
      this.toastService.warning('Please fill all required fields');
      return;
    }

    this.uploading = true;
    this.toastService.info('Saving manual ROTA entry...');

    // TODO: Implement manual ROTA submission API
    this.rotaService.createManualRota(this.manualForm.value).subscribe({
      next: (result) => {
        this.toastService.success('ROTA created successfully!');
        this.uploading = false;

        setTimeout(() => {
          this.router.navigate(['/rota']);
        }, 1500);
      },
      error: (err) => {
        const errorMsg = err.error?.message || 'Failed to create ROTA';
        this.toastService.error(errorMsg);
        this.uploading = false;
      }
    });
  }

  // ==================== Utilities ====================

  formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  getDayName(date: Date): string {
    return date.toLocaleDateString('en-US', { weekday: 'short' });
  }

  cancel(): void {
    this.router.navigate(['/rota']);
  }
}

