// components/admin-ca/admin-ca.component.tsmport { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { AdminApiService } from '../../../services/admin.service';
import { Component } from '@angular/core';

@Component({
  selector: 'app-admin-ca',
  templateUrl: './admin-ca.component.html',
  styleUrls: ['./admin-ca.component.scss']
})
export class AdminCaComponent {
  isBusy = false;
  serverError = '';
  result: any = null;   // { id, serial, subject, fingerprint, validFrom, validTo }

  form = this.fb.group({
    commonName: ['', [Validators.required, Validators.maxLength(128)]],
    organization: [''],
    organizationalUnit: [''],
    country: ['', [Validators.pattern(/^[A-Z]{2}$/)]], // e.g. RS, US
    state: [''],
    locality: [''],
    yearsValid: [5, [Validators.required, Validators.min(1), Validators.max(10)]],
  });

  constructor(private fb: FormBuilder, private api: AdminApiService) {}

  submit() {
    this.serverError = '';
    this.result = null;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isBusy = true;
    this.api.createRootCA(this.form.value).subscribe({
      next: (res) => {
        this.isBusy = false;
        this.result = res;
      },
      error: (err) => {
        this.isBusy = false;
        this.serverError = err?.error?.message || 'Failed to create Root CA';
      }
    });
  }

  downloadPem() {
    if (!this.result?.id) return;
    this.api.downloadPem(this.result.id).subscribe({
      next: (pem) => this.saveTextFile(pem, `root-ca-${this.result.id}.pem`),
      error: () => (this.serverError = 'Download failed')
    });
  }

  private saveTextFile(text: string, filename: string) {
    const blob = new Blob([text], { type: 'application/x-pem-file' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = filename; a.click();
    window.URL.revokeObjectURL(url);
  }
}
