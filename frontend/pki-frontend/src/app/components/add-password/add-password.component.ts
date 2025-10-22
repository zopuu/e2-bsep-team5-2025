import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { PasswordService } from '../../services/password.service';
import { CsrService } from '../../services/csr.service';
import { CaIssuerDto } from '../../models/csr';

@Component({
  selector: 'app-add-password',
  templateUrl: './add-password.component.html',
  styleUrls: ['./add-password.component.scss']
})
export class AddPasswordComponent implements OnInit {
  passwordForm!: FormGroup;
  certificates: CaIssuerDto[] = [];
  loading = false;
  message = '';
  messageType: 'success' | 'error' = 'success';

  constructor(
    private fb: FormBuilder,
    private passwordService: PasswordService,
    private csrService: CsrService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initializeForm();
    this.loadCertificates();
  }

  private initializeForm(): void {
    this.passwordForm = this.fb.group({
      siteName: ['', [Validators.required, Validators.minLength(2)]],
      username: ['', [Validators.required, Validators.minLength(2)]],
      password: ['', [Validators.required, Validators.minLength(4)]],
      notes: [''],
      certificateId: ['', Validators.required]
    });
  }

  private loadCertificates(): void {
    this.csrService.getMyCertificates().subscribe({
      next: (certificates) => {
        this.certificates = certificates;
      },
      error: (error) => {
        this.message = 'Failed to load certificates';
        this.messageType = 'error';
        console.error('Error loading certificates:', error);
      }
    });
  }

  onSubmit(): void {
    if (this.passwordForm.valid) {
      this.loading = true;
      this.message = '';

      const formData = this.passwordForm.value;
      
      this.passwordService.createPassword(formData).subscribe({
        next: (response) => {
          this.loading = false;
          this.message = 'Password saved successfully!';
          this.messageType = 'success';
          this.passwordForm.reset();
          
          // Redirect to password manager after 2 seconds
          setTimeout(() => {
            this.router.navigate(['/password-manager']);
          }, 2000);
        },
        error: (error) => {
          this.loading = false;
          this.message = error.error?.message || 'Failed to save password';
          this.messageType = 'error';
        }
      });
    } else {
      this.markFormGroupTouched();
    }
  }

  private markFormGroupTouched(): void {
    Object.keys(this.passwordForm.controls).forEach(key => {
      const control = this.passwordForm.get(key);
      control?.markAsTouched();
    });
  }

  getFieldError(fieldName: string): string {
    const control = this.passwordForm.get(fieldName);
    if (control?.errors && control.touched) {
      if (control.errors['required']) {
        return `${this.getFieldLabel(fieldName)} is required`;
      }
      if (control.errors['minlength']) {
        return `${this.getFieldLabel(fieldName)} must be at least ${control.errors['minlength'].requiredLength} characters`;
      }
    }
    return '';
  }

  private getFieldLabel(fieldName: string): string {
    const labels: { [key: string]: string } = {
      siteName: 'Site Name',
      username: 'Username',
      password: 'Password',
      notes: 'Notes',
      certificateId: 'Certificate'
    };
    return labels[fieldName] || fieldName;
  }

  goBack(): void {
    this.router.navigate(['/password-manager']);
  }
}
