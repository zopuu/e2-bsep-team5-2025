// src/app/components/ca/ca-issue-intermediate/ca-issue-intermediate.component.ts
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CaApiService, CaIssuerDto, CertificateResponse, IntermediateCaRequest } from '../../../services/ca.service';
import { AuthService } from 'src/app/services/auth.service';
import { notBeyondIssuerValidator, pathLenWithinIssuerValidator } from 'src/app/validators/certificate-validators';

@Component({
  selector: 'app-ca-issue-intermediate',
  templateUrl: './ca-issue-intermediate.component.html',
  styleUrls: ['./ca-issue-intermediate.component.scss']
})
export class CaIssueIntermediateComponent implements OnInit {
  title = 'Issue Intermediate CA';
  issuers: CaIssuerDto[] = [];
  selectedIssuer?: CaIssuerDto | null;
  form!: FormGroup;
  loading = false;
  issuing = false;
  toast: { type: 'success' | 'error' | '', text: string } = { type: '', text: '' };
  result?: CertificateResponse;
  caOrg: string | null = null;
  orgLocked = false;
  serverError = '';

  constructor(private fb: FormBuilder, private api: CaApiService, private auth: AuthService) { }

  ngOnInit(): void {
    this.caOrg = this.auth.getOrganization();
    this.form = this.fb.group({
      issuerRecordId: [null, Validators.required],
      subject: this.fb.group({
        commonName: ['', [Validators.required, Validators.maxLength(128)]],
        organization: [''],
        organizationalUnit: [''],
        country: ['', Validators.pattern(/^[A-Z]{2}$/)],
        state: [''],
        locality: ['']
      }),
      yearsValid: [5, [Validators.required, Validators.min(1), Validators.max(20)]],
      pathLenConstraint: [1],
      crlDistributionPoint: [''],
      ocspUrl: ['']
    }, {
      validators: [
        notBeyondIssuerValidator(() => this.selectedIssuer ? new Date(this.selectedIssuer.notAfter) : null),
        pathLenWithinIssuerValidator(() => {
          // If issuer has no explicit pathLen, we won't set a max
          const plc: any = (this.selectedIssuer as any)?.pathLenConstraint;
          return plc !== undefined ? plc : null;
        })
      ]
    });

    // Prefill & lock Organization to the CA user's org
    const orgCtrl = (this.form.get('subject') as FormGroup).get('organization')!;
    if (this.caOrg) {
      orgCtrl.setValue(this.caOrg, { emitEvent: false });
      orgCtrl.disable({ emitEvent: false });
      this.orgLocked = true;
    }

    // Keep selectedIssuer in sync for validators
    this.form.get('issuerRecordId')!.valueChanges.subscribe((id: number) => {
      this.selectedIssuer = this.issuers.find(x => x.id === id) ?? null;
      this.form.updateValueAndValidity({ emitEvent: false });
    });

    this.api.listIssuers().subscribe({
      next: res => this.issuers = res || [],
      error: err => this.serverError = err?.error?.message || 'Failed to load issuers'
    });
  }

  submit(): void {
    this.serverError = '';
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    // include disabled 'organization' value:
    const payload: IntermediateCaRequest = this.form.getRawValue();
    this.issuing = true;

    this.api.createIntermediateCAAsCa(payload).subscribe({
      next: res => {
        this.issuing = false;
        if (res.success) this.result = res.data;
        else this.serverError = res.message || 'Failed to issue';
      },
      error: err => {
        this.issuing = false;
        this.serverError = err?.error?.message || 'Failed to issue';
      }
    });
  }
}
