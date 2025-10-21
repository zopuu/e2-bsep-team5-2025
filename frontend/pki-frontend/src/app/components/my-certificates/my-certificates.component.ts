import { Component, OnInit } from '@angular/core';
import { CsrService } from '../../services/csr.service';
import { CaIssuerDto } from '../../models/csr';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-my-certificates',
  templateUrl: './my-certificates.component.html',
  styleUrls: ['./my-certificates.component.scss']
})
export class MyCertificatesComponent implements OnInit {
  
  certificates: CaIssuerDto[] = [];
  isLoading = false;

  constructor(
    private csrService: CsrService,
    private snackBar: MatSnackBar
  ) { }

  ngOnInit(): void {
    this.loadMyCertificates();
  }

  loadMyCertificates(): void {
    this.isLoading = true;
    
    this.csrService.getMyCertificates().subscribe({
      next: (certificates) => {
        this.certificates = certificates;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading certificates:', error);
        this.snackBar.open('Error loading certificates: ' + (error.error?.message || error.message), 'Close', { duration: 5000 });
        this.isLoading = false;
      }
    });
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('sr-RS');
  }

  downloadCertificate(certificate: CaIssuerDto): void {
    // TODO: Implement certificate download
    this.snackBar.open(`Download certificate ${certificate.serialNumber}`, 'Close', { duration: 3000 });
  }

  copy(text: string): void {
    navigator.clipboard.writeText(text).then(() => {
      this.snackBar.open('Copied to clipboard', 'Close', { duration: 2000 });
    }).catch(() => {
      this.snackBar.open('Failed to copy', 'Close', { duration: 2000 });
    });
  }

  refreshCertificates(): void {
    this.loadMyCertificates();
  }
}
