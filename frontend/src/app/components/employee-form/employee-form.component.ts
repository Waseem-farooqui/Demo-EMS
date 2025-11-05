import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {EmployeeService} from '../../services/employee.service';
import {Employee} from '../../models/employee.model';

@Component({
  selector: 'app-employee-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './employee-form.component.html',
  styleUrls: ['./employee-form.component.css']
})
export class EmployeeFormComponent implements OnInit {
  employeeForm: FormGroup;
  isEditMode = false;
  employeeId: number | null = null;
  loading = false;
  error: string | null = null;
  maxDate: string; // Maximum date (today) for date inputs

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
      jobTitle: ['', Validators.required],
      reference: [''],
      dateOfJoining: ['', Validators.required],
      employmentStatus: [''],
      contractType: [''],
      workingTiming: [''],
      holidayAllowance: ['']
    });
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

  loadEmployee(id: number): void {
    this.loading = true;
    this.employeeService.getEmployeeById(id).subscribe({
      next: (employee) => {
        this.employeeForm.patchValue(employee);
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
    const employee: Employee = this.employeeForm.value;

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

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.employeeForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }
}

