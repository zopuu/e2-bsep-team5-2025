import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from './auth.service';
import { Observable } from 'rxjs';

export interface CaIssuerDto {
  id: number;
  subjectDn: string;
  issuerDn: string;
  serialNumber: string;
  ca: boolean;
  notBefore: string;
  notAfter: string;
}
export interface IntermediateCaRequest {
  issuerRecordId: number;
  subject: {
    commonName: string;
    organization?: string;
    organizationalUnit?: string;
    country?: string;
    state?: string;
    locality?: string;
  };
  yearsValid: number;
  pathLenConstraint?: number | null;
  crlDistributionPoint?: string | null;
  ocspUrl?: string | null;
  // NOTE: ownerUserId is *ignored* by backend for CA route; owner = current CA user
}
export interface CertificateResponse {
  serialNumber: string;
  subjectDn: string;
  issuerDn: string;
  notBefore: string;
  notAfter: string;
  isCa: boolean;
  pathLenConstraint?: number | null;
  chainSubjectDns: string[];
}

@Injectable({ providedIn: 'root' })
export class CaApiService {
  // private apiUrl = 'http://localhost:8080';
  private apiUrl = 'https://localhost:8443';

  constructor(private http: HttpClient, private auth: AuthService) {}

  private authHeaders(): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${this.auth.getToken() || ''}` });
  }

  listIssuers(): Observable<CaIssuerDto[]> {
    return this.http.get<CaIssuerDto[]>(`${this.apiUrl}/api/ca/issuers`, {
      headers: this.authHeaders()
    });
  }
  createIntermediateCAAsCa(body: IntermediateCaRequest) {
  return this.http.post<{ success:boolean; message:string; data: CertificateResponse }>(
    `${this.apiUrl}/api/ca/intermediate`,
    body,
    { headers: this.authHeaders() }
  );
}
}
