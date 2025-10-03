import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private apiUrl = 'http://localhost:8080';

  constructor(private http: HttpClient, private auth: AuthService) {}

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
}
