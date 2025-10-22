import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from './auth.service';
import { Observable } from 'rxjs';

export type KeyUsage =
  | 'digitalSignature' | 'nonRepudiation' | 'keyEncipherment' | 'dataEncipherment'
  | 'keyAgreement' | 'keyCertSign' | 'cRLSign' | 'encipherOnly' | 'decipherOnly';

export type ExtendedKeyUsage =
  | 'serverAuth' | 'clientAuth' | 'codeSigning' | 'emailProtection'
  | 'timeStamping' | 'OCSPSigning';

export interface CreateTemplateRequest {
  name: string;
  issuerCertificateId: number;
  cnRegex: string;
  sanRegex: string;
  ttlDays: number;
  keyUsages: KeyUsage[];
  extendedKeyUsages: ExtendedKeyUsage[];
}

export interface TemplateResponse {
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

@Injectable({ providedIn: 'root' })
export class TemplatesApiService {
  private apiUrl = 'http://localhost:8080';

  constructor(private http: HttpClient, private auth: AuthService) {}

  private authHeaders(): HttpHeaders {
    return new HttpHeaders({
      Authorization: `Bearer ${this.auth.getToken() || ''}`,
    });
  }

  listTemplates(): Observable<TemplateResponse[]> {
    return this.http.get<TemplateResponse[]>(
      `${this.apiUrl}/api/ca-templates`,
      { headers: this.authHeaders() }
    );
  }

  createTemplate(body: CreateTemplateRequest): Observable<TemplateResponse> {
    return this.http.post<TemplateResponse>(
      `${this.apiUrl}/api/ca-templates`,
      body,
      { headers: this.authHeaders() }
    );
  }

  deleteTemplate(id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/api/ca-templates/${id}`,
      { headers: this.authHeaders() }
    );
  }
}
