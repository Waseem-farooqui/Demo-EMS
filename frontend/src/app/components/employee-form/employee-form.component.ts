import {Component, OnInit, OnDestroy} from '@angular/core';
import {Subscription} from 'rxjs';
import {CommonModule} from '@angular/common';
import {
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {EmployeeService} from '../../services/employee.service';
import {DocumentService} from '../../services/document.service';
import {Employee, EmploymentRecord} from '../../models/employee.model';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../../environments/environment';

@Component({
  selector: 'app-employee-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './employee-form.component.html',
  styleUrls: ['./employee-form.component.css']
})
export class EmployeeFormComponent implements OnInit, OnDestroy {
  employeeForm: FormGroup;
  isEditMode = false;
  employeeId: number | null = null;
  loading = false;
  error: string | null = null;
  maxDate: string; // Maximum date (today) for date inputs
  private medicalSubscription?: Subscription;
  private subscriptions: Subscription[] = [];

  // Wizard state
  currentStep = 1;
  totalSteps = 7;
  steps = [
    { number: 1, title: 'Personal Information', icon: '👤' },
    { number: 2, title: 'Work Information', icon: '💼' },
    { number: 3, title: 'Role & Department', icon: '🏢' },
    { number: 4, title: 'Next of Kin', icon: '👨‍👩‍👧‍👦' },
    { number: 5, title: 'Previous Employment', icon: '📋' },
    { number: 6, title: 'Medical Information', icon: '🩺' },
    { number: 7, title: 'Documents', icon: '📄' }
  ];

  // Document upload
  selectedDocuments: File[] = [];
  documentTypes: string[] = ['PASSPORT', 'VISA', 'CONTRACT', 'RESUME', 'SHARE_CODE', 
                             'PROOF_OF_ADDRESS', 'REGISTRATION_FORM', 'CERTIFICATE',
                             'NATIONAL_INSURANCE', 'BANK_STATEMENT'];
  documentTypeMap: { [key: string]: string } = {};

  // Departments
  departments: any[] = [];

  constructor(
    private fb: FormBuilder,
    private employeeService: EmployeeService,
    private documentService: DocumentService,
    private http: HttpClient,
    private router: Router,
    private route: ActivatedRoute
  ) {
    // Set max date to today in YYYY-MM-DD format
    const today = new Date();
    this.maxDate = today.toISOString().split('T')[0];

    this.employeeForm = this.fb.group({
      // Step 1: Personal Information
      fullName: ['', Validators.required],
      personType: ['', Validators.required],
      workEmail: ['', [Validators.required, Validators.email]],
      personalEmail: ['', [Validators.email]],
      phoneNumber: [''],
      dateOfBirth: [''],
      nationality: [''],
      address: [''],
      presentAddress: [''],
      previousAddress: [''],
      // Step 2: Work Information
      jobTitle: [''],
      reference: [''],
      dateOfJoining: ['', Validators.required],
      employmentStatus: [''],
      contractType: [''],
      // Step 3: Role & Department
      departmentId: [null],
      allottedOrganization: [''],
      // Step 4: Next of Kin
      nextOfKinName: [''],
      nextOfKinContact: [''],
      nextOfKinAddress: [''],
      // Step 5: Previous Employment (handled by FormArray)
      employmentRecords: this.fb.array([]),
      // Step 6: Medical Information
      hasMedicalCondition: [false],
      medicalConditionDetails: [''],
      bloodGroup: ['']
    });

    this.addEmploymentRecord();
    this.setupMedicalConditionWatcher();
  }

  ngOnInit(): void {
    this.loadDepartments();
    const paramsSub = this.route.params.subscribe(params => {
      if (params['id']) {
        this.isEditMode = true;
        this.employeeId = +params['id'];
        this.loadEmployee(this.employeeId);
      }
    });
    this.subscriptions.push(paramsSub);
  }

  loadDepartments(): void {
    this.http.get<any[]>(`${environment.apiUrl}${environment.endpoints.departments}`).subscribe({
      next: (depts) => {
        this.departments = depts.sort((a, b) => a.name.localeCompare(b.name));
      },
      error: (err) => {
        console.error('Error loading departments:', err);
        this.departments = [];
      }
    });
  }

  ngOnDestroy(): void {
    this.medicalSubscription?.unsubscribe();
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.subscriptions = [];
  }

  loadEmployee(id: number): void {
    this.loading = true;
    this.employeeService.getEmployeeById(id).subscribe({
      next: (employee) => {
        const {employmentRecords = [], ...rest} = employee;
        this.employeeForm.patchValue({
          ...rest,
          hasMedicalCondition: employee.hasMedicalCondition ?? false
        });
        this.resetEmploymentRecords(employmentRecords);
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load employee data.';
        this.loading = false;
        console.error('Error loading employee:', err);
      }
    });
  }

  // Wizard navigation methods
  nextStep(): void {
    if (this.validateCurrentStep()) {
      if (this.currentStep < this.totalSteps) {
        this.currentStep++;
      }
    }
  }

  previousStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  goToStep(step: number): void {
    if (step >= 1 && step <= this.totalSteps) {
      this.currentStep = step;
    }
  }

  skipStep(): void {
    if (this.currentStep < this.totalSteps) {
      this.currentStep++;
    }
  }

  canSkipStep(): boolean {
    // Steps that can be skipped (all optional fields)
    const skippableSteps = [4, 5, 6, 7]; // Next of Kin, Previous Employment, Medical, Documents
    return skippableSteps.includes(this.currentStep);
  }

  validateCurrentStep(): boolean {
    const stepFields: { [key: number]: string[] } = {
      1: ['fullName', 'personType', 'workEmail'], // Personal Information
      2: ['dateOfJoining'], // Work Information
      3: [], // Role & Department (all optional)
      4: [], // Next of Kin (all optional)
      5: [], // Previous Employment (all optional)
      6: [], // Medical Information (all optional)
      7: []  // Documents (all optional)
    };

    const fieldsToValidate = stepFields[this.currentStep] || [];
    let isValid = true;

    fieldsToValidate.forEach(field => {
      const control = this.employeeForm.get(field);
      if (control && control.invalid) {
        control.markAsTouched();
        isValid = false;
      }
    });

    // Special validation for medical condition
    if (this.currentStep === 6) {
      const hasCondition = this.employeeForm.get('hasMedicalCondition')?.value;
      const medicalDetails = this.employeeForm.get('medicalConditionDetails');
      if (hasCondition && medicalDetails && medicalDetails.invalid) {
        medicalDetails.markAsTouched();
        isValid = false;
      }
    }

    return isValid;
  }

  isStepComplete(step: number): boolean {
    // Check if step has required fields filled
    const stepFields: { [key: number]: string[] } = {
      1: ['fullName', 'personType', 'workEmail'],
      2: ['dateOfJoining'],
      3: [],
      4: [],
      5: [],
      6: [],
      7: []
    };

    const requiredFields = stepFields[step] || [];
    return requiredFields.every(field => {
      const control = this.employeeForm.get(field);
      return control && control.valid;
    });
  }

  onSubmit(): void {
    if (this.currentStep < this.totalSteps) {
      this.nextStep();
      return;
    }

    // Final step - validate and submit
    if (this.employeeForm.invalid) {
      this.markFormGroupTouched(this.employeeForm);
      return;
    }

    this.loading = true;
    this.error = null;
    const sanitizedRecords = this.getSanitizedEmploymentRecords();
    const formValue = this.employeeForm.value;

    const employee: Employee = {
      ...formValue,
      hasMedicalCondition: !!formValue.hasMedicalCondition,
      medicalConditionDetails: formValue.hasMedicalCondition ? formValue.medicalConditionDetails : '',
      employmentRecords: sanitizedRecords
    };

    const request = this.isEditMode && this.employeeId
      ? this.employeeService.updateEmployee(this.employeeId, employee)
      : this.employeeService.createEmployee(employee);

    request.subscribe({
      next: (savedEmployee) => {
        // Upload documents if any
        if (this.selectedDocuments.length > 0 && savedEmployee.id) {
          this.uploadDocuments(savedEmployee.id);
        } else {
          this.router.navigate(['/employees']);
        }
      },
      error: (err) => {
        this.error = this.isEditMode
          ? 'Failed to update employee. Please try again.'
          : 'Failed to create employee. Please try again.';
        this.loading = false;
        console.error('Error saving employee:', err);
      }
    });
  }

  uploadDocuments(employeeId: number): void {
    let uploadCount = 0;
    const totalDocs = this.selectedDocuments.length;

    if (totalDocs === 0) {
      this.router.navigate(['/employees']);
      return;
    }

    this.selectedDocuments.forEach((file, index) => {
      const documentType = this.documentTypeMap[file.name] || 'OTHERS';
      this.documentService.uploadDocument(employeeId, documentType, file).subscribe({
        next: () => {
          uploadCount++;
          if (uploadCount === totalDocs) {
            this.router.navigate(['/employees']);
          }
        },
        error: (err) => {
          console.error('Error uploading document:', err);
          uploadCount++;
          if (uploadCount === totalDocs) {
            this.router.navigate(['/employees']);
          }
        }
      });
    });
  }

  onDocumentSelected(event: any, documentType: string): void {
    const files = Array.from(event.target.files) as File[];
    files.forEach(file => {
      this.selectedDocuments.push(file);
      this.documentTypeMap[file.name] = documentType;
    });
  }

  removeDocument(index: number): void {
    const file = this.selectedDocuments[index];
    delete this.documentTypeMap[file.name];
    this.selectedDocuments.splice(index, 1);
  }

  onCancel(): void {
    this.router.navigate(['/employees']);
  }

  get employmentRecords(): FormArray {
    return this.employeeForm.get('employmentRecords') as FormArray;
  }

  addEmploymentRecord(record?: EmploymentRecord): void {
    this.employmentRecords.push(this.createEmploymentRecordGroup(record));
  }

  removeEmploymentRecord(index: number): void {
    if (this.employmentRecords.length === 1) {
      this.employmentRecords.at(0).reset();
      return;
    }
    this.employmentRecords.removeAt(index);
  }

  shouldShowMedicalDetails(): boolean {
    return !!this.employeeForm.get('hasMedicalCondition')?.value;
  }

  private createEmploymentRecordGroup(record?: EmploymentRecord): FormGroup {
    return this.fb.group({
      jobTitle: [record?.jobTitle || ''],
      employmentPeriod: [record?.employmentPeriod || ''],
      employerName: [record?.employerName || ''],
      employerAddress: [record?.employerAddress || '']
    });
  }

  private resetEmploymentRecords(records: EmploymentRecord[]): void {
    this.employmentRecords.clear();
    if (!records || records.length === 0) {
      this.addEmploymentRecord();
      return;
    }
    records.forEach(record => this.addEmploymentRecord(record));
  }

  private getSanitizedEmploymentRecords(): EmploymentRecord[] {
    const records = this.employmentRecords.getRawValue() as EmploymentRecord[];
    return records.filter(record => this.isEmploymentRecordFilled(record));
  }

  private isEmploymentRecordFilled(record: EmploymentRecord): boolean {
    if (!record) {
      return false;
    }
    return !!(
      (record.jobTitle && record.jobTitle.trim()) ||
      (record.employmentPeriod && record.employmentPeriod.trim()) ||
      (record.employerName && record.employerName.trim()) ||
      (record.employerAddress && record.employerAddress.trim())
    );
  }

  private setupMedicalConditionWatcher(): void {
    const hasConditionControl = this.employeeForm.get('hasMedicalCondition');
    const medicalControl = this.employeeForm.get('medicalConditionDetails');

    if (!hasConditionControl || !medicalControl) {
      return;
    }

    const applyValidation = (hasCondition: boolean) => {
      if (hasCondition) {
        medicalControl.setValidators([Validators.required, Validators.minLength(5)]);
      } else {
        medicalControl.clearValidators();
        medicalControl.setValue('');
      }
      medicalControl.updateValueAndValidity();
    };

    applyValidation(!!hasConditionControl.value);

    this.medicalSubscription = hasConditionControl.valueChanges.subscribe((hasCondition: boolean) => {
      applyValidation(hasCondition);
    });
  }

  private markFormGroupTouched(formGroup: FormGroup | FormArray): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      if (!control) {
        return;
      }
      if (control instanceof FormGroup || control instanceof FormArray) {
        this.markFormGroupTouched(control);
      } else {
        control.markAsTouched();
      }
    });
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.employeeForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }
}

