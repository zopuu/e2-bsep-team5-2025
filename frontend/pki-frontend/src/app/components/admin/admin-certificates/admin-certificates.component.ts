import { Component, OnInit, ViewChild } from '@angular/core';
import { AdminApiService } from './../../../services/admin.service';
import { CertificateListItem, CertificateStatus, CertificateType, PagedResponse, RevokeRequest } from '../../../models/certificate';
import { FormBuilder } from '@angular/forms';
import { MatPaginator } from '@angular/material/paginator';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-admin-certificates',
  templateUrl: './admin-certificates.component.html',
  styleUrls: ['./admin-certificates.component.scss']
})
export class AdminCertificatesComponent implements OnInit {
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  loading = false;
  data: CertificateListItem[] = [];
  total = 0;

  filters = this.fb.group({
    q: [''],
    type: [''],
    status: [''],
    ca: ['']
  });

  displayed: (keyof CertificateListItem | 'actions')[] = [
    'type', 'subjectDn', 'issuerDn', 'serialNumber', 'status', 'notAfter', 'ca', 'actions'
  ];

  page = 0; size = 20;

  constructor(
    private api: AdminApiService,
    private fb: FormBuilder,
    private dlg: MatDialog,
    private toast: MatSnackBar
  ) { }

  ngOnInit(): void {
    this.load();
  }

  load() {
    this.loading = true;
    const f = this.filters.value;
    this.api.listCertificatesPaged({
      q: f.q || undefined,
      type: f.type || undefined,
      status: f.status || undefined,
      ca: f.ca === '' ? undefined : f.ca === 'true',
      page: this.page,
      size: this.size
    }).subscribe({
      next: (res) => {
        this.data = res.content;
        this.total = res.totalElements;
        this.loading = false;
      },
      error: () => { this.loading = false; this.toast.open('Failed to load certificates', 'Dismiss', { duration: 3000 }); }
    });
  }

  resetFilters() {
    this.filters.reset({ q: '', type: '', status: '', ca: '' });
    this.page = 0;
    this.load();
  }

  onPage(ev: any) {
    this.page = ev.pageIndex;
    this.size = ev.pageSize;
    this.load();
  }

  downloadPem(row: CertificateListItem) {
    this.api.downloadPem(row.id).subscribe(pem => {
      const blob = new Blob([pem as any], { type: 'application/x-pem-file' });
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = `certificate-${row.serialNumber}.pem`;
      a.click();
      URL.revokeObjectURL(a.href);
    });
  }

  viewChain(row: CertificateListItem) {
    this.api.getChain(row.id).subscribe(info => {
      this.toast.open(`Subject: ${info.subject}\nIssuer: ${info.issuer}`, 'OK', { duration: 5000 });
    });
  }

  revoke(row: CertificateListItem) {
    const reason: RevokeRequest['reason'] = 'UNSPECIFIED';
    // if you want a dialog, wire it here; for now quick action:
    this.api.revokeCertificate(row.id, { reason }).subscribe({
      next: () => { this.toast.open('Certificate revoked', 'OK', { duration: 2000 }); this.load(); },
      error: () => this.toast.open('Revocation failed', 'Dismiss', { duration: 3000 })
    });
  }

  isExpired(row: CertificateListItem) {
    return new Date(row.notAfter).getTime() < Date.now();
  }

  statusChipColor(row: CertificateListItem) {
    if (row.status === 'REVOKED') return 'warn';
    if (this.isExpired(row)) return 'accent';
    return 'primary';
  }
  copy(text?: string | null) {
    if (!text) return;
    navigator.clipboard?.writeText(text);
  }

}
