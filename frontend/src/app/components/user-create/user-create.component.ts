import {Component, OnDestroy, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import {Router} from '@angular/router';
import {HttpClient} from '@angular/common/http';
import {AuthService} from '../../services/auth.service';
import {ToastService} from '../../services/toast.service';
import {DocumentService} from '../../services/document.service';
import {PositionService} from '../../services/position.service';
import {environment} from '../../../environments/environment';
import {EmploymentRecord} from '../../models/employee.model';
import {Department, CreateUserResponse, UserFormData} from '../../models/user-create.model';
import {Position} from '../../models/position.model';
import {Subscription, of} from 'rxjs';
import {debounceTime, distinctUntilChanged, switchMap} from 'rxjs/operators';
import {Subject} from 'rxjs';

@Component({
  selector: 'app-user-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './user-create.component.html',
  styleUrls: ['./user-create.component.css']
})
export class UserCreateComponent implements OnInit, OnDestroy {
  userForm!: FormGroup;
  loading = false;
  error = '';
  success = '';
  currentUser: { roles?: string[] } | null = null;
  isSuperAdmin = false;
  maxDate: string; // Maximum date (today)

  departments: Department[] = [];
  positions: Position[] = [];
  filteredPositions: Position[] = [];
  showPositionDropdown = false;
  showCredentials = false;
  createdCredentials: CreateUserResponse | null = null;
  showCustomDepartment = false;
  private medicalSubscription?: Subscription;
  private positionSearchSubject = new Subject<string>();

  // Document upload
  selectedDocuments: File[] = [];
  documentTypes: string[] = ['PASSPORT', 'VISA', 'CONTRACT', 'RESUME', 'SHARE_CODE', 
                             'PROOF_OF_ADDRESS', 'REGISTRATION_FORM', 'CERTIFICATE',
                             'PROFESSIONAL_CERTIFICATE', 'TERM_LETTER',
                             'NATIONAL_INSURANCE', 'BANK_STATEMENT'];
  documentTypeMap: { [key: string]: string } = {};

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

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private toastService: ToastService,
    private authService: AuthService,
    private documentService: DocumentService,
    private positionService: PositionService,
    public router: Router
  ) {
    // Set max date to today
    this.maxDate = this.formatDate(new Date());
  }

  ngOnInit(): void {
    this.currentUser = this.authService.getUser();
    // FIX: Check roles array instead of role property
    const roles = this.currentUser?.roles || [];
    this.isSuperAdmin = roles.includes('SUPER_ADMIN');


    this.initForm();
    this.loadDepartments();
    this.loadPositions();
    this.setupPositionSearch();
  }

  ngOnDestroy(): void {
    this.medicalSubscription?.unsubscribe();
    this.positionSearchSubject.complete();
  }

  initForm(): void {
    this.userForm = this.fb.group({
      title: ['', Validators.required],
      fullName: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      phoneNumber: [''],
      dateOfBirth: [''],
      nationality: [''],
      presentAddress: [''],
      previousAddress: [''],
      jobTitle: ['', Validators.required],
      personType: ['Employee', Validators.required],
      role: ['USER', Validators.required],
      departmentId: [null, Validators.required],
      customDepartmentName: [''],
      allottedOrganization: [''],
      reference: [''],
      dateOfJoining: [this.formatDate(new Date()), [Validators.required, this.dateNotInFutureValidator()]],
      employmentStatus: ['FULL_TIME', Validators.required],
      contractType: ['PERMANENT', Validators.required],
      nationalInsuranceNumber: [''],
      shareCode: [''],
      bankAccountNumber: [''],
      bankSortCode: [''],
      bankAccountHolderName: [''],
      bankName: [''],
      wageRate: [''],
      contractHours: [''],
      hasMedicalCondition: [false],
      medicalConditionDetails: [''],
      // Legacy next of kin fields (kept for backward compatibility)
      nextOfKinName: [''],
      nextOfKinContact: [''],
      nextOfKinAddress: [''],
      nextOfKinList: this.fb.array([this.createNextOfKinGroup()]),
      bloodGroup: [''],
      emergencyContactName: [''],
      emergencyContactPhone: [''],
      emergencyContactRelationship: [''],
      employmentRecords: this.fb.array([this.createEmploymentRecordGroup()])
    });

    // Enable role selection for SUPER_ADMIN, disable for ADMIN
    if (this.isSuperAdmin) {
      this.userForm.get('role')?.enable();
      this.userForm.get('departmentId')?.enable();
    } else {
      this.userForm.get('role')?.disable();
      this.userForm.get('departmentId')?.disable();
    }

    this.setupMedicalConditionWatcher();
  }

  // Custom validator to ensure date is not in the future
  dateNotInFutureValidator() {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) {
        return null;
      }

      // Parse the selected date from the input (format: YYYY-MM-DD)
      const selectedDate = new Date(control.value + 'T00:00:00');

      // Get today's date at midnight for proper comparison
      const today = new Date();
      today.setHours(0, 0, 0, 0);

      // Check if selected date is after today
      if (selectedDate.getTime() > today.getTime()) {
        return { futureDate: true };
      }

      return null;
    };
  }

  loadDepartments(): void {
    // Use different endpoint for SUPER_ADMIN to get admin status
    const endpoint = this.isSuperAdmin
      ? `${environment.apiUrl}/departments/with-admin-status`
      : `${environment.apiUrl}/departments`;

    this.http.get<Department[]>(endpoint).subscribe({
      next: (data) => {
        // Sort departments alphabetically by name
        this.departments = data.sort((a, b) => a.name.localeCompare(b.name));

        // Add "Create Custom" option for SUPER_ADMIN
        if (this.isSuperAdmin) {
          this.departments.push({
            id: 'custom',
            name: 'Create Custom Department',
            code: 'CUSTOM',
            hasAdmin: false
          });
        }

      },
      error: (err) => {
        // Error logged via error message to user
        this.error = 'Failed to load departments. Please refresh the page.';
        this.toastService.error('Failed to load departments');
      }
    });
  }

  loadPositions(): void {
    this.positionService.getAllPositions().subscribe({
      next: (data) => {
        this.positions = data.sort((a, b) => a.name.localeCompare(b.name));
        this.filteredPositions = [...this.positions];
      },
      error: (err) => {
        console.error('Error loading positions:', err);
        this.toastService.error('Failed to load positions');
      }
    });
  }

  setupPositionSearch(): void {
    this.positionSearchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((query: string) => {
        if (!query || query.trim().length === 0) {
          this.filteredPositions = [...this.positions];
          return of(this.positions);
        }
        return this.positionService.searchPositions(query);
      })
    ).subscribe({
      next: (results) => {
        if (results && results.length > 0) {
          this.filteredPositions = results;
        } else if (results && results.length === 0) {
          // Keep current filtered positions if search returns empty
          // User can still type custom position
        }
      },
      error: (err) => {
        console.error('Error searching positions:', err);
      }
    });
  }

  onJobTitleInput(event: Event): void {
    const target = event.target as HTMLInputElement;
    const value = target.value;
    this.showPositionDropdown = value.length > 0;
    this.positionSearchSubject.next(value);
    
    // Filter positions locally for immediate feedback
    if (value.length === 0) {
      this.filteredPositions = [...this.positions];
    } else {
      const lowerValue = value.toLowerCase();
      this.filteredPositions = this.positions.filter(pos =>
        pos.name.toLowerCase().includes(lowerValue) ||
        pos.description.toLowerCase().includes(lowerValue)
      );
    }
  }

  selectPosition(position: Position): void {
    this.userForm.get('jobTitle')?.setValue(position.name);
    this.showPositionDropdown = false;
  }

  onJobTitleBlur(): void {
    // Delay hiding dropdown to allow click on option
    setTimeout(() => {
      this.showPositionDropdown = false;
    }, 200);
  }

  onDepartmentChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    const selectedValue = target.value;
    if (selectedValue === 'custom') {
      this.showCustomDepartment = true;
      this.userForm.get('customDepartmentName')?.setValidators([Validators.required]);
      this.userForm.get('departmentId')?.clearValidators();
    } else {
      this.showCustomDepartment = false;
      this.userForm.get('customDepartmentName')?.clearValidators();
      this.userForm.get('customDepartmentName')?.setValue('');
      this.userForm.get('departmentId')?.setValidators([Validators.required]);
    }
    this.userForm.get('customDepartmentName')?.updateValueAndValidity();
    this.userForm.get('departmentId')?.updateValueAndValidity();
  }

  onSubmit(): void {
    // If not on last step, go to next step
    if (this.currentStep < this.totalSteps) {
      this.nextStep();
      return;
    }

    // Final step - validate and submit
    if (this.userForm.invalid) {
      this.error = 'Please fill all required fields correctly';
      this.markFormGroupTouched(this.userForm);
      return;
    }

    this.loading = true;
    this.error = '';
    this.success = '';
    this.showCredentials = false;

    const sanitizedRecords = this.getSanitizedEmploymentRecords();
    const sanitizedNextOfKin = this.getSanitizedNextOfKin();
    let formData = this.userForm.getRawValue(); // Get all values including disabled fields
    formData.hasMedicalCondition = !!formData.hasMedicalCondition;
    formData.medicalConditionDetails = formData.hasMedicalCondition ? formData.medicalConditionDetails : '';
    formData.employmentRecords = sanitizedRecords;
    formData.nextOfKinList = sanitizedNextOfKin;

    // Handle custom department creation
    if (this.showCustomDepartment && formData.customDepartmentName) {
      // First create the department
      this.createCustomDepartment(formData.customDepartmentName).subscribe({
        next: (newDept: Department) => {
          formData.departmentId = typeof newDept.id === 'number' ? newDept.id : null;
          delete formData.customDepartmentName;
          this.createUser(formData);
        },
        error: (err: unknown) => {
          this.loading = false;
          this.error = 'Failed to create custom department: ' + this.extractErrorMessage(err);
          window.scrollTo({ top: 0, behavior: 'smooth' });
        }
      });
    } else {
      delete formData.customDepartmentName;
      this.createUser(formData);
    }
  }

  createCustomDepartment(departmentName: string) {
    const deptData: Partial<Department> = {
      name: departmentName,
      code: departmentName.substring(0, 3).toUpperCase(),
      description: 'Custom department created by SUPER_ADMIN',
      isActive: true
    };

    const token = this.authService.getToken();
    return this.http.post<Department>(`${environment.apiUrl}/departments`, deptData, {
      headers: { Authorization: `Bearer ${token}` }
    });
  }

  createUser(formData: UserFormData): void {
    const token = this.authService.getToken();
    this.http.post<CreateUserResponse>(`${environment.apiUrl}/users/create`, formData, {
      headers: { Authorization: `Bearer ${token}` }
    }).subscribe({
      next: (response) => {
        this.success = response.message;
        this.createdCredentials = response;
        this.showCredentials = true;
        this.loading = false;

        // Upload documents if any were selected
        if (this.selectedDocuments.length > 0 && response.employeeId) {
          this.uploadDocuments(response.employeeId);
        }

        // Scroll to top to show credentials
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error: (err) => {
        this.loading = false;
        this.error = this.extractErrorMessage(err);
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    });
  }

  uploadDocuments(employeeId: number): void {
    if (this.selectedDocuments.length === 0) {
      return;
    }

    const totalDocs = this.selectedDocuments.length;
    let uploadedCount = 0;
    let failedCount = 0;

    this.selectedDocuments.forEach((file, index) => {
      const documentType = this.documentTypeMap[file.name];

      this.documentService.uploadDocument(employeeId, documentType, file).subscribe({
        next: () => {
          uploadedCount++;
          if (uploadedCount + failedCount === totalDocs) {
            if (uploadedCount > 0) {
              this.toastService.success(`Successfully uploaded ${uploadedCount} document(s)`);
            }
            if (failedCount > 0) {
              this.toastService.warning(`Failed to upload ${failedCount} document(s)`);
            }
          }
        },
        error: (err: unknown) => {
          failedCount++;
          // Error handling - log to service if needed
          if (uploadedCount + failedCount === totalDocs) {
            if (uploadedCount > 0) {
              this.toastService.success(`Successfully uploaded ${uploadedCount} document(s)`);
            }
            if (failedCount > 0) {
              this.toastService.warning(`Failed to upload ${failedCount} document(s)`);
            }
          }
        }
      });
    });
  }

  onDocumentSelected(event: Event, documentType: string): void {
    const target = event.target as HTMLInputElement;
    if (!target.files || target.files.length === 0) {
      return;
    }
    const files = Array.from(target.files) as File[];
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

  get employmentRecords(): FormArray {
    return this.userForm.get('employmentRecords') as FormArray;
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

  private createEmploymentRecordGroup(record?: EmploymentRecord): FormGroup {
    return this.fb.group({
      jobTitle: [record?.jobTitle || ''],
      employmentPeriod: [record?.employmentPeriod || ''],
      employerName: [record?.employerName || ''],
      employerAddress: [record?.employerAddress || ''],
      contactPersonTitle: [record?.contactPersonTitle || ''],
      contactPersonName: [record?.contactPersonName || ''],
      contactPersonEmail: [record?.contactPersonEmail || '']
    });
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
      (record.employerAddress && record.employerAddress.trim()) ||
      (record.contactPersonTitle && record.contactPersonTitle.trim()) ||
      (record.contactPersonName && record.contactPersonName.trim()) ||
      (record.contactPersonEmail && record.contactPersonEmail.trim())
    );
  }

  get nextOfKinList(): FormArray {
    return this.userForm.get('nextOfKinList') as FormArray;
  }

  addNextOfKin(kin?: any): void {
    this.nextOfKinList.push(this.createNextOfKinGroup(kin));
  }

  removeNextOfKin(index: number): void {
    if (this.nextOfKinList.length === 1) {
      this.nextOfKinList.at(0).reset();
      return;
    }
    this.nextOfKinList.removeAt(index);
  }

  private createNextOfKinGroup(kin?: any): FormGroup {
    return this.fb.group({
      title: [kin?.title || ''],
      name: [kin?.name || ''],
      contact: [kin?.contact || ''],
      address: [kin?.address || ''],
      relationship: [kin?.relationship || '']
    });
  }

  private getSanitizedNextOfKin(): any[] {
    const nextOfKin = this.nextOfKinList.getRawValue() as any[];
    return nextOfKin.filter(kin => this.isNextOfKinFilled(kin));
  }

  private isNextOfKinFilled(kin: any): boolean {
    if (!kin) {
      return false;
    }
    return !!(
      (kin.name && kin.name.trim()) ||
      (kin.contact && kin.contact.trim()) ||
      (kin.address && kin.address.trim()) ||
      (kin.relationship && kin.relationship.trim())
    );
  }

  copyToClipboard(text: string, type: string): void {
    // Check if text is valid
    if (!text || text.trim() === '') {
      this.toastService.warning(`No ${type} to copy`);
      return;
    }

    // Try modern Clipboard API first (requires HTTPS or localhost)
    const isSecureContext = window.isSecureContext || 
                           window.location.protocol === 'https:' || 
                           window.location.hostname === 'localhost' || 
                           window.location.hostname === '127.0.0.1';

    if (navigator.clipboard && isSecureContext) {
      navigator.clipboard.writeText(text).then(() => {
        this.toastService.success(`${type} copied to clipboard!`);
      }).catch(() => {
        // Clipboard API failed, use fallback
        this.fallbackCopyToClipboard(text, type);
      });
    } else {
      // Fallback for non-HTTPS environments
      this.fallbackCopyToClipboard(text, type);
    }
  }

  private fallbackCopyToClipboard(text: string, type: string): void {
    // Create a temporary textarea element
    const textArea = document.createElement('textarea');
    textArea.value = text;
    // Position it off-screen but still in the viewport for better compatibility
    textArea.style.position = 'fixed';
    textArea.style.left = '0';
    textArea.style.top = '0';
    textArea.style.width = '2em';
    textArea.style.height = '2em';
    textArea.style.padding = '0';
    textArea.style.border = 'none';
    textArea.style.outline = 'none';
    textArea.style.boxShadow = 'none';
    textArea.style.background = 'transparent';
    textArea.style.opacity = '0';
    textArea.style.zIndex = '-9999';
    textArea.setAttribute('readonly', '');
    textArea.setAttribute('aria-hidden', 'true');
    
    document.body.appendChild(textArea);
    
    // Select the text
    textArea.focus();
    textArea.select();
    textArea.setSelectionRange(0, text.length); // For mobile devices

    try {
      // Use the older execCommand method as fallback
      const successful = document.execCommand('copy');
      if (successful) {
        this.toastService.success(`${type} copied to clipboard!`);
      } else {
        throw new Error('execCommand copy failed');
      }
    } catch {
      // Fallback copy failed - last resort: show the text in a prompt or alert
      try {
        const copied = prompt(`Please copy the ${type}:\n\nPress Ctrl+C (Cmd+C on Mac) to copy:`, text);
        if (copied !== null) {
          this.toastService.info(`${type} displayed in prompt - please copy manually`);
        }
      } catch (promptErr) {
        // If prompt is blocked, show in alert
        alert(`Please copy this ${type}:\n\n${text}`);
        this.toastService.warning(`Please manually copy the ${type} from the alert`);
      }
    } finally {
      // Clean up
      if (document.body.contains(textArea)) {
        document.body.removeChild(textArea);
      }
    }
  }

  createAnother(): void {
    this.showCredentials = false;
    this.createdCredentials = null;
    this.success = '';
    this.userForm.reset({
      personType: 'Employee',
      role: 'USER',
      dateOfJoining: this.formatDate(new Date()),
      hasMedicalCondition: false
    });
    this.employmentRecords.clear();
    this.addEmploymentRecord();
    this.nextOfKinList.clear();
    this.addNextOfKin();
    // Clear selected documents
    this.selectedDocuments = [];
    this.documentTypeMap = {};
  }

  goToEmployees(): void {
    this.router.navigate(['/employees']);
  }

  // Wizard navigation methods
  nextStep(): void {
    if (this.validateCurrentStep()) {
      if (this.currentStep < this.totalSteps) {
        this.currentStep++;
        this.error = ''; // Clear any previous errors
      }
    } else {
      // Show error message for invalid step
      const stepNames: { [key: number]: string } = {
        1: 'Personal Information',
        2: 'Work Information',
        3: 'Role & Department',
        4: 'Next of Kin',
        5: 'Previous Employment',
        6: 'Medical Information',
        7: 'Documents'
      };
      this.error = `Please fill all required fields in ${stepNames[this.currentStep]} step before proceeding.`;
      this.toastService.error(this.error);
      window.scrollTo({ top: 0, behavior: 'smooth' });
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
      1: ['title', 'fullName', 'email'], // Personal Information
      2: ['dateOfJoining'], // Work Information
      3: ['jobTitle', 'departmentId'], // Role & Department (jobTitle and departmentId are required)
      4: [], // Next of Kin (all optional)
      5: [], // Previous Employment (all optional)
      6: [], // Medical Information (all optional)
      7: []  // Documents (all optional)
    };

    const fieldsToValidate = stepFields[this.currentStep] || [];
    let isValid = true;

    fieldsToValidate.forEach(field => {
      const control = this.userForm.get(field);
      if (control && control.invalid) {
        control.markAsTouched();
        isValid = false;
      }
    });

    // Special validation for medical condition
    if (this.currentStep === 6) {
      const hasCondition = this.userForm.get('hasMedicalCondition')?.value;
      const medicalDetails = this.userForm.get('medicalConditionDetails');
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
      1: ['fullName', 'email'],
      2: ['dateOfJoining'],
      3: [],
      4: [],
      5: [],
      6: [],
      7: []
    };

    const requiredFields = stepFields[step] || [];
    return requiredFields.every(field => {
      const control = this.userForm.get(field);
      return control && control.valid;
    });
  }

  private setupMedicalConditionWatcher(): void {
    const hasConditionControl = this.userForm.get('hasMedicalCondition');
    const medicalControl = this.userForm.get('medicalConditionDetails');

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

  /**
   * Check if a department should be disabled in the dropdown
   * Department is disabled if:
   * 1. User is SUPER_ADMIN
   * 2. Selected role is ADMIN
   * 3. Department already has an ADMIN assigned
   */
  isDepartmentDisabled(department: Department): boolean {
    if (!this.isSuperAdmin) {
      return false; // Non-SUPER_ADMIN can't create ADMIN anyway
    }

    const selectedRole = this.userForm.get('role')?.value;

    // Only disable if creating ADMIN role and department already has admin
    return selectedRole === 'ADMIN' && (department.hasAdmin === true);
  }

  /**
   * Get display name for department option
   * Shows (Already has Admin) suffix for departments that can't be selected
   */
  getDepartmentDisplayName(department: Department): string {
    if (this.isDepartmentDisabled(department)) {
      return `${department.name} (Already has Admin)`;
    }
    return department.name;
  }

  /**
   * Handle role change - reload departments when switching between USER and ADMIN
   */
  onRoleChange(): void {
    // Clear department selection when role changes
    this.userForm.get('departmentId')?.setValue(null);
  }

  private extractErrorMessage(err: unknown): string {
    if (!err || typeof err !== 'object') {
      return 'An error occurred while creating the user. Please try again.';
    }
    
    const error = err as { error?: { message?: string } | string; status?: number };
    
    if (error.error) {
      if (typeof error.error === 'object' && 'message' in error.error) {
        return (error.error as { message: string }).message;
      }
      if (typeof error.error === 'string') {
        return error.error;
      }
    }
    
    if (error.status === 409) {
      return 'This email already exists in the system.';
    }
    if (error.status === 403) {
      return 'You don\'t have permission to perform this action.';
    }
    
    return 'An error occurred while creating the user. Please try again.';
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}

