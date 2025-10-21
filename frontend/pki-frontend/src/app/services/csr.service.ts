import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CsrUploadResponse, CaIssuerDto, CsrUploadRequest } from '../models/csr';

@Injectable({
  providedIn: 'root'
})
export class CsrService {
  private apiUrl = 'http://localhost:8080/api/csr';

  constructor(private http: HttpClient) { }

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  private getAuthHeadersForFile(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
  }

  uploadCsr(file: File): Observable<CsrUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<CsrUploadResponse>(
      `${this.apiUrl}/upload`,
      formData,
      { headers: this.getAuthHeadersForFile() }
    );
  }

  parseCsr(csrContent: string): Observable<CsrUploadResponse> {
    const request: CsrUploadRequest = { csrContent };

    return this.http.post<CsrUploadResponse>(
      `${this.apiUrl}/parse`,
      request,
      { headers: this.getAuthHeaders() }
    );
  }

  getCaCertificates(): Observable<CaIssuerDto[]> {
    return this.http.get<CaIssuerDto[]>(
      `${this.apiUrl}/ca-certificates`,
      { headers: this.getAuthHeaders() }
    );
  }
}
