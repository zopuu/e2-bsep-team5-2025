// src/app/components/ca/ca-issue-intermediate/ca-issue-intermediate.component.ts
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CaApiService, CaIssuerDto, CertificateResponse, IntermediateCaRequest } from '../../../services/ca.service';

@Component({
  selector: 'app-ca-issue-intermediate',
  templateUrl: './ca-issue-intermediate.component.html',
  styleUrls: ['./ca-issue-intermediate.component.scss']
})
export class CaIssueIntermediateComponent implements OnInit {
  title = 'Issue Intermediate CA';
  issuers: CaIssuerDto[] = [];
  form!: FormGroup;
  loading = false;
  issuing = false;
  toast: { type: 'success'|'error'|'', text: string } = { type:'', text:'' };
  result?: CertificateResponse;

  constructor(private fb: FormBuilder, private api: CaApiService) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      issuerRecordId: [null, Validators.required],
      subject: this.fb.group({
        commonName: ['', Validators.required],
        organization: [''],
        organizationalUnit: [''],
        country: [''],
        state: [''],
        locality: ['']
      }),
      yearsValid: [5, [Validators.required, Validators.min(1), Validators.max(20)]],
      pathLenConstraint: [1],
      crlDistributionPoint: [''],
      ocspUrl: ['']
    });

    this.loading = true;
    this.api.listIssuers().subscribe({
      next: res => { this.issuers = res; this.loading = false; },
      error: _ => { this.toast = { type:'error', text:'Failed to load issuers' }; this.loading = false; }
    });
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const payload: IntermediateCaRequest = this.form.value;
    this.issuing = true;
    this.api.createIntermediateCAAsCa(payload).subscribe({
      next: res => {
        this.issuing = false;
        if (res.success) { this.result = res.data; this.toast = { type:'success', text: res.message }; }
        else { this.toast = { type:'error', text: res.message || 'Failed to issue' }; }
      },
      error: err => { this.issuing = false; this.toast = { type:'error', text: err?.error?.message || 'Failed to issue' }; }
    });
  }
}
