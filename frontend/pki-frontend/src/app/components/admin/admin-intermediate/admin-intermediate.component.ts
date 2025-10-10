// components/admin/admin-intermediate/admin-intermediate.component.ts
import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators, FormGroup } from '@angular/forms';
import { AdminApiService, CertificateListItem } from '../../../services/admin.service';
import { AuthService } from '../../../services/auth.service';
import { notBeyondIssuerValidator, pathLenWithinIssuerValidator } from '../../../validators/certificate-validators';

interface IssuerOption {
  id: number;
  subjectDn: string;
  isCa: boolean;
  status: string;
  notAfter: string;
}


@Component({
  selector: 'app-admin-intermediate',
  templateUrl: './admin-intermediate.component.html',
  styleUrls: ['./admin-intermediate.component.scss']
})
export class AdminIntermediateComponent implements OnInit {
  isBusy = false;
  serverError = '';
  result: any = null;  // backend CertificateResponse
  selectedIssuer: CertificateListItem | null = null;

  //issuers: IssuerOption[] = [];
  issuers: CertificateListItem[] = [];
  form: FormGroup = this.fb.group({
    issuerRecordId: [null, [Validators.required]],
    commonName: ['', [Validators.required, Validators.maxLength(128)]],
    organization: [''],
    organizationalUnit: [''],
    country: ['', [Validators.pattern(/^[A-Z]{2}$/)]],
    state: [''],
    locality: [''],
    yearsValid: [5, [Validators.required, Validators.min(1), Validators.max(10)]],
    pathLenConstraint: [1, [Validators.min(0)]],
    crlDistributionPoint: [''],
    ocspUrl: [''],
    ownerUserId: [null],
  }, {
    validators: [
      notBeyondIssuerValidator(() => this.selectedIssuer ? new Date(this.selectedIssuer.notAfter) : null),
      pathLenWithinIssuerValidator(() => this.selectedIssuer ? this.selectedIssuer.pathLenConstraint : null),
    ]
  });

  constructor(
    private fb: FormBuilder,
    private api: AdminApiService,
    private auth: AuthService
  ) { }

  ngOnInit(): void {
    this.loadIssuers();
    this.form.get('issuerRecordId')!.valueChanges.subscribe(id => {
      this.selectedIssuer = this.issuers.find(x => x.id === id) ?? null;
      this.form.updateValueAndValidity({ emitEvent: false });
    });
  }

  private loadIssuers() {
    this.isBusy = true;
    this.api.listActiveCaIssuers().subscribe({
      next: (items) => {
        // filtriraj lokalno na CA + ACTIVE
        this.issuers = (items || []).filter((x: any) => x.ca === true && x.status === 'ACTIVE');
        this.isBusy = false;
      },
      error: (err) => {
        this.serverError = err?.error?.message || 'Failed to load CA issuers';
        this.isBusy = false;
      }
    });
  }

  submit() {
    this.serverError = '';
    this.result = null;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.value;

    const body = {
      issuerRecordId: v.issuerRecordId,
      subject: {
        commonName: v.commonName,
        organization: v.organization || undefined,
        organizationalUnit: v.organizationalUnit || undefined,
        country: v.country || undefined,
        state: v.state || undefined,
        locality: v.locality || undefined,
      },
      yearsValid: v.yearsValid,
      pathLenConstraint: v.pathLenConstraint ?? null,
      crlDistributionPoint: v.crlDistributionPoint || null,
      ocspUrl: v.ocspUrl || null,
      ownerUserId: v.ownerUserId || null
    };

    this.isBusy = true;
    this.api.createIntermediateCA(body).subscribe({
      next: (res) => {
        this.isBusy = false;
        this.result = res; // { serialNumber, subjectDn, issuerDn, notBefore, notAfter, isCa, pathLenConstraint, chainSubjectDns }
      },
      error: (err) => {
        this.isBusy = false;
        const msg = err?.error?.message || err?.error || err?.message || 'Internal error';
        this.serverError = String(msg);
      }

    });
  }

  downloadIssuerPem() {
    if (!this.result?.issuerId) return; // samo ako backend vraća id — u tvom response-u nema; preskoči
  }
}
