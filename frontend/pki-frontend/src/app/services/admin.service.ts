import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from './auth.service';
import { Observable } from 'rxjs';

export interface CertificateListItem {
  id: number;
  subjectDn: string;
  issuerDn: string;
  serialNumber: string;
  notBefore: string;   // ISO string
  notAfter: string;    // ISO string
  ca: boolean;
  status: 'ACTIVE' | 'REVOKED' | 'EXPIRED';
  type: 'ROOT' | 'INTERMEDIATE' | 'EE' | 'CA';
}

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
    // ako dodaš serverski filter, promijeni na .../certificates?type=CA&status=ACTIVE
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
}
