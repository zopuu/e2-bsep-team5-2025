import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CsrService } from '../../services/csr.service';
import { CsrData, CaIssuerDto, CsrUploadResponse, CsrCertificateRequest, CertificateIssueResponse } from '../../models/csr';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-csr-upload',
  templateUrl: './csr-upload.component.html',
  styleUrls: ['./csr-upload.component.scss']
})
export class CsrUploadComponent implements OnInit {
  
  csrForm!: FormGroup;
  selectedFile: File | null = null;
  csrData: CsrData | null = null;
  caCertificates: CaIssuerDto[] = [];
  isLoading = false;
  showCsrData = false;

  constructor(
    private fb: FormBuilder,
    private csrService: CsrService,
    private snackBar: MatSnackBar
  ) { }

  ngOnInit(): void {
    this.initForm();
    this.loadCaCertificates();
  }

  private initForm(): void {
    this.csrForm = this.fb.group({
      file: [null, Validators.required],
      caCertificateId: [null, Validators.required],
      validityDays: [365, [Validators.required, Validators.min(1), Validators.max(3650)]]
    });
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      this.csrForm.patchValue({ file: file });
      this.uploadAndParseCsr();
    }
  }

  private uploadAndParseCsr(): void {
    if (!this.selectedFile) {
      return;
    }

    this.isLoading = true;
    this.showCsrData = false;

    this.csrService.uploadCsr(this.selectedFile).subscribe({
      next: (response: CsrUploadResponse) => {
        this.isLoading = false;
        if (response.success && response.csrData) {
          this.csrData = response.csrData;
          this.showCsrData = true;
          this.snackBar.open('CSR parsed successfully!', 'Close', { duration: 3000 });
        } else {
          this.snackBar.open(response.message || 'Failed to parse CSR', 'Close', { duration: 5000 });
        }
      },
      error: (error) => {
        this.isLoading = false;
        console.error('Error uploading CSR:', error);
        this.snackBar.open('Error uploading CSR: ' + (error.error?.message || error.message), 'Close', { duration: 5000 });
      }
    });
  }

  private loadCaCertificates(): void {
    this.csrService.getCaCertificates().subscribe({
      next: (certificates: CaIssuerDto[]) => {
        this.caCertificates = certificates;
      },
      error: (error) => {
        console.error('Error loading CA certificates:', error);
        this.snackBar.open('Error loading CA certificates', 'Close', { duration: 3000 });
      }
    });
  }

  onSubmit(): void {
    if (this.csrForm.valid && this.csrData) {
      const request: CsrCertificateRequest = {
        csrData: this.csrData,
        caCertificateId: this.csrForm.value.caCertificateId,
        validityDays: this.csrForm.value.validityDays
      };

      this.isLoading = true;
      
      this.csrService.issueCertificate(request).subscribe({
        next: (response: CertificateIssueResponse) => {
          this.isLoading = false;
          if (response.success) {
            this.snackBar.open(
              `Certificate issued successfully! ID: ${response.certificateId}`, 
              'Close', 
              { duration: 5000 }
            );
            // Reset form
            this.resetForm();
          } else {
            this.snackBar.open(response.message || 'Failed to issue certificate', 'Close', { duration: 5000 });
          }
        },
        error: (error) => {
          this.isLoading = false;
          console.error('Error issuing certificate:', error);
          this.snackBar.open('Error issuing certificate: ' + (error.error?.message || error.message), 'Close', { duration: 5000 });
        }
      });
    } else {
      this.snackBar.open('Please fill all required fields', 'Close', { duration: 3000 });
    }
  }

  private resetForm(): void {
    this.csrForm.reset();
    this.selectedFile = null;
    this.csrData = null;
    this.showCsrData = false;
    this.initForm();
  }

  getFileName(): string {
    return this.selectedFile ? this.selectedFile.name : 'No file selected';
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString();
  }
}
