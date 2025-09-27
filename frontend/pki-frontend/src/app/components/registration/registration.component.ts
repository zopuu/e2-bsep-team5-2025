import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { PasswordStrengthService } from '../../services/password-strength.service';

@Component({
  selector: 'app-registration',
  templateUrl: './registration.component.html',
  styleUrls: ['./registration.component.scss']
})
export class RegistrationComponent implements OnInit {
  
  registrationForm!: FormGroup;
  passwordStrength: any = null;
  isLoading = false;
  message = '';
  messageType: 'success' | 'error' = 'error';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private passwordStrengthService: PasswordStrengthService
  ) {}

  ngOnInit(): void {
    this.initializeForm();
  }

  private initializeForm(): void {
    this.registrationForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]],
      confirmPassword: ['', [Validators.required]],
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      organization: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });
  }

  private passwordMatchValidator(form: FormGroup) {
    const password = form.get('password');
    const confirmPassword = form.get('confirmPassword');
    
    if (password && confirmPassword && password.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }
    
    return null;
  }

  onPasswordChange(): void {
    const password = this.registrationForm.get('password')?.value;
    if (password) {
      this.passwordStrengthService.checkPasswordStrength(password).subscribe({
        next: (result) => {
          this.passwordStrength = result;
        },
        error: (error) => {
          console.error('Password strength check failed:', error);
        }
      });
    } else {
      this.passwordStrength = null;
    }
  }

  onSubmit(): void {
    if (this.registrationForm.valid) {
      this.isLoading = true;
      this.message = '';

      const formData = this.registrationForm.value;
      
      this.authService.register(formData).subscribe({
        next: (response) => {
          this.isLoading = false;
          this.message = response.message;
          this.messageType = 'success';
          this.registrationForm.reset();
          this.passwordStrength = null;
        },
        error: (error) => {
          this.isLoading = false;
          this.message = error.error?.message || 'Registration failed. Please try again.';
          this.messageType = 'error';
        }
      });
    } else {
      this.markFormGroupTouched();
    }
  }

  private markFormGroupTouched(): void {
    Object.keys(this.registrationForm.controls).forEach(key => {
      const control = this.registrationForm.get(key);
      control?.markAsTouched();
    });
  }

  getFieldError(fieldName: string): string {
    const field = this.registrationForm.get(fieldName);
    if (field?.errors && field.touched) {
      if (field.errors['required']) {
        return `${this.getFieldLabel(fieldName)} is required`;
      }
      if (field.errors['email']) {
        return 'Please enter a valid email address';
      }
      if (field.errors['minlength']) {
        return `${this.getFieldLabel(fieldName)} must be at least ${field.errors['minlength'].requiredLength} characters`;
      }
      if (field.errors['maxlength']) {
        return `${this.getFieldLabel(fieldName)} cannot exceed ${field.errors['maxlength'].requiredLength} characters`;
      }
      if (field.errors['passwordMismatch']) {
        return 'Passwords do not match';
      }
    }
    return '';
  }

  private getFieldLabel(fieldName: string): string {
    const labels: { [key: string]: string } = {
      email: 'Email',
      password: 'Password',
      confirmPassword: 'Confirm Password',
      firstName: 'First Name',
      lastName: 'Last Name',
      organization: 'Organization'
    };
    return labels[fieldName] || fieldName;
  }

  getPasswordStrengthClass(): string {
    if (!this.passwordStrength) return '';
    
    switch (this.passwordStrength.score) {
      case 0:
      case 1:
        return 'very-weak';
      case 2:
        return 'weak';
      case 3:
        return 'medium';
      case 4:
        return 'strong';
      case 5:
        return 'very-strong';
      default:
        return '';
    }
  }
}