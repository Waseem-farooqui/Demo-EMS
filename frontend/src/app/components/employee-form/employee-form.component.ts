import {Component, OnInit, OnDestroy} from '@angular/core';
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
import {Employee, EmploymentRecord} from '../../models/employee.model';
import {Subscription} from 'rxjs';

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

  constructor(
    private fb: FormBuilder,
    private employeeService: EmployeeService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    // Set max date to today in YYYY-MM-DD format
    const today = new Date();
    this.maxDate = today.toISOString().split('T')[0];

    this.employeeForm = this.fb.group({
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
      hasMedicalCondition: [false],
      medicalConditionDetails: [''],
      nextOfKinName: [''],
      nextOfKinContact: [''],
      nextOfKinAddress: [''],
      jobTitle: ['', Validators.required],
      reference: [''],
      dateOfJoining: ['', Validators.required],
      employmentStatus: [''],
      contractType: [''],
      workingTiming: [''],
      holidayAllowance: [''],
      employmentRecords: this.fb.array([])
    });

    this.addEmploymentRecord();
    this.setupMedicalConditionWatcher();
  }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.isEditMode = true;
        this.employeeId = +params['id'];
        this.loadEmployee(this.employeeId);
      }
    });
  }

  ngOnDestroy(): void {
    this.medicalSubscription?.unsubscribe();
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

  onSubmit(): void {
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
      next: () => {
        this.router.navigate(['/employees']);
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

