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

@Injectable({ providedIn: 'root' })
export class CaApiService {
  private apiUrl = 'http://localhost:8080';

  constructor(private http: HttpClient, private auth: AuthService) {}

  private authHeaders(): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${this.auth.getToken() || ''}` });
  }

  listIssuers(): Observable<CaIssuerDto[]> {
    return this.http.get<CaIssuerDto[]>(`${this.apiUrl}/api/ca/issuers`, {
      headers: this.authHeaders()
    });
  }
}
