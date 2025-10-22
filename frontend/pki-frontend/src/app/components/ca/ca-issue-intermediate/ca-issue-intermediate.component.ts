import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { CaApiService, CaIssuerDto, CertificateResponse, IntermediateCaRequest } from '../../../services/ca.service';
import { AuthService } from 'src/app/services/auth.service';
import { notBeyondIssuerValidator, pathLenWithinIssuerValidator } from 'src/app/validators/certificate-validators';
import { HttpClient } from '@angular/common/http';

type KeyUsage =
  | 'digitalSignature' | 'nonRepudiation' | 'keyEncipherment' | 'dataEncipherment'
  | 'keyAgreement' | 'keyCertSign' | 'cRLSign' | 'encipherOnly' | 'decipherOnly';

type ExtendedKeyUsage = 'serverAuth' | 'clientAuth' | 'codeSigning' | 'emailProtection' | 'timeStamping' | 'OCSPSigning';

interface TemplateResponseDto {
  id: number;
  name: string;
  issuerCertificateId: number;   // may arrive as string in some backends; we coerce to number
  cnRegex: string;
  sanRegex: string;
  ttlDays: number;
  keyUsages: KeyUsage[];
  extendedKeyUsages: ExtendedKeyUsage[];
  createdAt: string;
}

@Component({
  selector: 'app-ca-issue-intermediate',
  templateUrl: './ca-issue-intermediate.component.html',
  styleUrls: ['./ca-issue-intermediate.component.scss']
})
export class CaIssueIntermediateComponent implements OnInit {
  title = 'Issue Intermediate CA';
  issuers: CaIssuerDto[] = [];
  selectedIssuer?: CaIssuerDto | null;

  // Templates
  templates: TemplateResponseDto[] = [];
  templatesLoading = false;
  selectedTemplate?: TemplateResponseDto | null;

  form!: FormGroup;
  loading = false;
  issuing = false;
  toast: { type: 'success' | 'error' | '', text: string } = { type: '', text: '' };
  result?: CertificateResponse;
  caOrg: string | null = null;
  orgLocked = false;
  serverError = '';

  // NEW: buffer desired issuer id if issuers aren’t loaded yet
  private pendingIssuerId: number | null = null;

  private templatesBaseUrl = 'http://localhost:8080/api/ca-templates';

  constructor(
    private fb: FormBuilder,
    private api: CaApiService,
    private auth: AuthService,
    private http: HttpClient
  ) {}

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
          const plc: any = (this.selectedIssuer as any)?.pathLenConstraint;
          return plc !== undefined ? plc : null;
        })
      ]
    });

    // Lock O to user org if present
    const orgCtrl = (this.form.get('subject') as FormGroup).get('organization')!;
    if (this.caOrg) {
      orgCtrl.setValue(this.caOrg, { emitEvent: false });
      orgCtrl.disable({ emitEvent: false });
      this.orgLocked = true;
    }

    // Keep selectedIssuer up-to-date for validators
    this.form.get('issuerRecordId')!.valueChanges.subscribe((id: number) => {
      this.selectedIssuer = this.issuers.find(x => x.id === id) ?? null;
      this.form.updateValueAndValidity({ emitEvent: false });
    });

    // Load issuers; apply pending issuer if set by template click
    this.api.listIssuers().subscribe({
      next: res => {
        this.issuers = res || [];
        if (this.pendingIssuerId != null) {
          this.form.get('issuerRecordId')!.setValue(this.pendingIssuerId);
          this.pendingIssuerId = null;
        }
      },
      error: err => this.serverError = err?.error?.message || 'Failed to load issuers'
    });

    this.fetchTemplates();
  }

  // --- Templates fetching + apply ---
  private fetchTemplates() {
    this.templatesLoading = true;
    this.http.get<TemplateResponseDto[]>(this.templatesBaseUrl).subscribe({
      next: (data) => {
        this.templates = data || [];
        this.templatesLoading = false;
      },
      error: () => {
        this.templates = [];
        this.templatesLoading = false;
      }
    });
  }

  applyTemplate(t: TemplateResponseDto) {
    this.selectedTemplate = t;

    // 1) Issuer from template (coerce to number)
    const issuerId = Number(t.issuerCertificateId);
    if (this.issuers?.length) {
      this.form.get('issuerRecordId')!.setValue(issuerId);
    } else {
      // issuers not loaded yet — remember it and apply after list arrives
      this.pendingIssuerId = issuerId;
    }

    // 2) Validity: template ttlDays -> years (clamped 1..20)
    const years = Math.max(1, Math.min(20, Math.floor(t.ttlDays / 365)));
    this.form.get('yearsValid')!.setValue(years);

    // 3) CN regex validator (keeps required+maxlength)
    const cnCtrl = this.form.get('subject.commonName')!;
    const base = [Validators.required, Validators.maxLength(128)];
    cnCtrl.setValidators([...base, this.makeRegexValidator(() => this.selectedTemplate?.cnRegex)]);
    cnCtrl.updateValueAndValidity({ emitEvent: false });

    this.form.markAsDirty();
  }

  clearTemplate() {
    this.selectedTemplate = null;
    const cnCtrl = this.form.get('subject.commonName')!;
    cnCtrl.setValidators([Validators.required, Validators.maxLength(128)]);
    cnCtrl.updateValueAndValidity({ emitEvent: false });
  }

  private makeRegexValidator(getRegex: () => string | undefined | null) {
    return (ctrl: AbstractControl): ValidationErrors | null => {
      const pattern = getRegex();
      if (!pattern) return null;
      try {
        const re = new RegExp(pattern);
        return re.test(String(ctrl.value ?? '')) ? null : { templateRegex: true };
      } catch {
        // If backend sends a malformed regex, ignore rather than block
        return null;
      }
    };
  }

  submit(): void {
    this.serverError = '';
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

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
