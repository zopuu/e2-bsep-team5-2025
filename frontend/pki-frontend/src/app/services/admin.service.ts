import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { HttpParams } from '@angular/common/http';
import { AuthService } from './auth.service';
import { Observable } from 'rxjs';
import { PagedResponse, CertificateListItem, RevokeRequest } from '../models/certificate';


@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private apiUrl = 'http://localhost:8080';

  constructor(private http: HttpClient, private auth: AuthService) { }

  private authHeaders(): HttpHeaders {
    return new HttpHeaders({
      Authorization: `Bearer ${this.auth.getToken() || ''}`
    });
  }

  createRootCA(body: any) {
    return this.http.post<any>(`${this.apiUrl}/api/admin/ca/root`, body, {
      headers: this.authHeaders()
    });
  }

  downloadPem(id: number) {
    return this.http.get(`${this.apiUrl}/api/admin/cert/${id}/pem`, {
      headers: this.authHeaders(),
      responseType: 'text'
    });
  }

  listCertificates() {
    return this.http.get<any[]>(`${this.apiUrl}/api/admin/certificates`, {
      headers: this.authHeaders()
    });
  }
  listActiveCaIssuers(): Observable<CertificateListItem[]> {
    return this.listCertificates();
  }
  createIntermediateCA(body: {
    issuerRecordId: number;
    subject: {
      commonName: string; organization?: string; organizationalUnit?: string;
      country?: string; state?: string; locality?: string;
    };
    yearsValid: number;
    pathLenConstraint?: number | null;
    crlDistributionPoint?: string | null;
    ocspUrl?: string | null;
    ownerUserId?: number | null;
  }) {
    return this.http.post<any>(`${this.apiUrl}/api/admin/intermediate`, body, {
      headers: this.authHeaders()
    });
  }
  listCertificatesPaged(params: {
    q?: string; type?: string; status?: string; ca?: boolean; page?: number; size?: number;
  }) {
    let httpParams = new HttpParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') httpParams = httpParams.set(k, String(v));
    });

    return this.http.get<PagedResponse<CertificateListItem>>(
      `${this.apiUrl}/api/admin/certificates`,
      { headers: this.authHeaders(), params: httpParams }
    );
  }

  revokeCertificate(id: number, body: RevokeRequest) {
    return this.http.post<void>(`${this.apiUrl}/api/admin/cert/${id}/revoke`, body, {
      headers: this.authHeaders()
    });
  }


  getChain(id: number) {
    return this.http.get<{ id: number; subject: string; issuer: string; chain: string[] }>(
      `${this.apiUrl}/api/admin/cert/${id}/chain`,
      { headers: this.authHeaders() }
    );
  }
}
