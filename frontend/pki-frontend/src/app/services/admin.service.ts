import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { HttpParams } from '@angular/common/http';
import { AuthService } from './auth.service';
import { Observable } from 'rxjs';
import { PagedResponse, CertificateListItem, RevokeRequest } from '../models/certificate';
import { CreateCaUserRequest, UserDto } from '../models/user';
import { map } from 'rxjs/operators';


@Injectable({ providedIn: 'root' })
export class AdminApiService {
  // private apiUrl = 'http://localhost:8080';
  private apiUrl = 'https://localhost:8443';

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
  listActiveCaIssuers() {
    // ask for ACTIVE CA certs, first page with a generous size
    return this.listCertificatesPaged({
      ca: true,
      status: 'ACTIVE',
      page: 0,
      size: 200
    }).pipe(
      map((res: PagedResponse<CertificateListItem>) => {
        const now = Date.now();
        // only issuers that are CA, ACTIVE, and currently valid
        return (res.content || []).filter(i =>
          i.ca === true &&
          i.status === 'ACTIVE' &&
          new Date(i.notBefore).getTime() <= now &&
          new Date(i.notAfter).getTime() >= now
        );
      })
    );
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
  listCaUsers(): Observable<UserDto[]> {
    const params = new HttpParams().set('role', 'CA_USER');
    return this.http.get<UserDto[]>(`${this.apiUrl}/api/admin/users`, {
      headers: this.authHeaders(),
      params
    });
  }

  /** Create (invite) a new CA user */
  createCaUser(body: CreateCaUserRequest): Observable<{ success: boolean; message: string; data?: any }> {
    return this.http.post<{ success: boolean; message: string; data?: any }>(
      `${this.apiUrl}/api/admin/users/ca`,
      body,
      { headers: this.authHeaders() }
    );
  }
}
