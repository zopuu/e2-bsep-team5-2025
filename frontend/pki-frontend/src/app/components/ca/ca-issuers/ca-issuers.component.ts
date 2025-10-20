import { Component, OnInit } from '@angular/core';
import { CaApiService, CaIssuerDto } from '../../../services/ca.service';

@Component({
  selector: 'app-ca-issuers',
  templateUrl: './ca-issuers.component.html',
  styleUrls: ['./ca-issuers.component.scss']
})
export class CaIssuersComponent implements OnInit {
  loading = false;
  issuers: CaIssuerDto[] = [];
  q = '';
  filtered: CaIssuerDto[] = [];
  toast: { type: 'success'|'error'|'', text: string } = { type: '', text: '' };

  constructor(private api: CaApiService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.api.listIssuers().subscribe({
      next: res => { this.issuers = res; this.applyFilter(); this.loading = false; },
      error: _ => { this.toast = { type:'error', text:'Failed to load issuers' }; this.loading = false; }
    });
  }

  applyFilter(): void {
    const q = (this.q || '').toLowerCase().trim();
    if (!q) { this.filtered = this.issuers.slice(); return; }
    this.filtered = this.issuers.filter(i =>
      [i.subjectDn, i.issuerDn, i.serialNumber].some(v => v?.toLowerCase().includes(q))
    );
  }
}
