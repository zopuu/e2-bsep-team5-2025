import { Component, computed, inject, signal } from '@angular/core';
import {
  FormBuilder,
  Validators,
  ReactiveFormsModule,
  FormArray,
  FormControl,
  FormGroup,
  AbstractControl,
  ValidationErrors,
  ValidatorFn,
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

// reuse your service & dto
import { CaApiService, CaIssuerDto as ApiIssuer } from 'src/app/services/ca.service';
type KeyUsage =
  | 'digitalSignature'
  | 'nonRepudiation'
  | 'keyEncipherment'
  | 'dataEncipherment'
  | 'keyAgreement'
  | 'keyCertSign'
  | 'cRLSign'
  | 'encipherOnly'
  | 'decipherOnly';

type ExtendedKeyUsage =
  | 'serverAuth'
  | 'clientAuth'
  | 'codeSigning'
  | 'emailProtection'
  | 'timeStamping'
  | 'OCSPSigning';

// what the dropdown displays
interface IssuerOption {
  id: number;
  commonName: string; // label shown in <option>
}

interface TemplateResponseDto {
  id: number;
  name: string;
  issuerCertificateId: number;
  cnRegex: string;
  sanRegex: string;
  ttlDays: number;
  keyUsages: KeyUsage[];
  extendedKeyUsages: ExtendedKeyUsage[];
  createdAt: string;
}

@Component({
  selector: 'app-ca-templates',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './ca-templates.component.html',
  styleUrls: ['./ca-templates.component.scss'],
})
export class CaTemplatesComponent {
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private caApi = inject(CaApiService);

private baseUrl = 'http://localhost:8080/api/ca-templates';

  loading = signal(false);
  errorMsg = signal<string | null>(null);
  successMsg = signal<string | null>(null);

  issuers = signal<IssuerOption[]>([]);
  templates = signal<TemplateResponseDto[]>([]);

  keyUsageOptions: KeyUsage[] = [
    'digitalSignature',
    'nonRepudiation',
    'keyEncipherment',
    'dataEncipherment',
    'keyAgreement',
    'keyCertSign',
    'cRLSign',
    'encipherOnly',
    'decipherOnly',
  ];
  ekuOptions: ExtendedKeyUsage[] = [
    'serverAuth',
    'clientAuth',
    'codeSigning',
    'emailProtection',
    'timeStamping',
    'OCSPSigning',
  ];

  // keep issuerCertificateId nullable so the "Select issuer..." option works
  form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    issuerCertificateId: new FormControl<number | null>(null, {
      nonNullable: false,
      validators: [Validators.required],
    }),
    cnRegex: ['^.*\\.ftn\\.com$', [Validators.required, this.regexValidator()]],
    sanRegex: ['^.*\\.ftn\\.com$', [Validators.required, this.regexValidator()]],
    ttlDays: [365, [Validators.required, Validators.min(1), Validators.max(3650)]],
    // arrays of non-nullable boolean controls
    keyUsages: new FormArray<FormControl<boolean>>(
      this.keyUsageOptions.map(() => new FormControl<boolean>(false, { nonNullable: true }))
    ),
    extendedKeyUsages: new FormArray<FormControl<boolean>>(
      this.ekuOptions.map(() => new FormControl<boolean>(false, { nonNullable: true }))
    ),
  });

  get keyUsagesFA(): FormArray<FormControl<boolean>> {
    return this.form.get('keyUsages') as FormArray<FormControl<boolean>>;
  }
  get ekusFA(): FormArray<FormControl<boolean>> {
    return this.form.get('extendedKeyUsages') as FormArray<FormControl<boolean>>;
  }

  selectedKeyUsages = computed<KeyUsage[]>(() =>
    this.keyUsageOptions.filter((_, i) => this.keyUsagesFA.value[i])
  );
  selectedEKUs = computed<ExtendedKeyUsage[]>(() =>
    this.ekuOptions.filter((_, i) => this.ekusFA.value[i])
  );

  ngOnInit() {
    this.fetchIssuers();
    this.fetchTemplates();
  }

  // === issuers for dropdown, from your CaApiService ===
  private fetchIssuers() {
    this.caApi.listIssuers().subscribe({
      next: (raw: ApiIssuer[]) => {
        // keep only CA certs; map to {id, commonName}
        const mapped: IssuerOption[] = raw
          .filter(i => i.ca) // only real issuers
          .map(i => ({
            id: i.id,
            commonName: this.cnFromDn(i.subjectDn) ?? i.subjectDn ?? `Issuer #${i.id}`,
          }));
        this.issuers.set(mapped);
      },
      error: () => this.issuers.set([]),
    });
  }

  // extract CN from DN string like 'CN=foo, O=bar'
  private cnFromDn(dn?: string): string | null {
    if (!dn) return null;
    const m = dn.match(/(^|[,/])\s*CN\s*=\s*("?)([^",/]+)\2/i);
    return m ? m[3].trim() : null;
    // if your DNs include quoted commas, this simple parser is still fine for most cases
  }

  private fetchTemplates() {
    this.loading.set(true);
    this.http.get<TemplateResponseDto[]>(this.baseUrl).subscribe({
      next: data => {
        this.templates.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.errorMsg.set('Failed to load templates');
        this.loading.set(false);
      },
    });
  }

  createTemplate() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = {
      name: this.form.value.name as string,
      issuerCertificateId: this.form.value.issuerCertificateId as number,
      cnRegex: this.form.value.cnRegex as string,
      sanRegex: this.form.value.sanRegex as string,
      ttlDays: this.form.value.ttlDays as number,
      keyUsages: this.selectedKeyUsages(),
      extendedKeyUsages: this.selectedEKUs(),
    };

    this.loading.set(true);
    this.errorMsg.set(null);
    this.successMsg.set(null);

    this.http.post<TemplateResponseDto>(this.baseUrl, payload).subscribe({
      next: () => {
        this.successMsg.set('Template created.');
        this.form.reset({
          name: '',
          issuerCertificateId: null,
          cnRegex: '^.*\\.ftn\\.com$',
          sanRegex: '^.*\\.ftn\\.com$',
          ttlDays: 365,
          keyUsages: this.keyUsageOptions.map(() => false),
          extendedKeyUsages: this.ekuOptions.map(() => false),
        });
        this.fetchTemplates();
        this.loading.set(false);
      },
      error: err => {
        this.errorMsg.set(err?.error?.message ?? 'Failed to create template');
        this.loading.set(false);
      },
    });
  }

  deleteTemplate(id: number) {
    if (!confirm('Delete template?')) return;
    this.http.delete(`${this.baseUrl}/${id}`).subscribe({
      next: () => {
        this.templates.set(this.templates().filter(t => t.id !== id));
      },
      error: () => this.errorMsg.set('Delete failed.'),
    });
  }

  // ValidatorFn factory (fixes "used before initialization")
  private regexValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const v = control.value as string;
      if (!v) return { regex: true };
      try {
        new RegExp(v);
        return null;
      } catch {
        return { regex: true };
      }
    };
  }
}
