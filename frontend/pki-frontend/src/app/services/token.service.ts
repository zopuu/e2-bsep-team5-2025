import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

export interface ActiveSession {
  jti: string;
  device: string;
  ip: string;
  issuedAt: string;
  lastActivity: string;
  current: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class TokenService {
  private apiUrl = 'http://localhost:8080';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  private getAuthHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    return new HttpHeaders({
      Authorization: `Bearer ${token}`
    });
  }

  /**
   * Get all active sessions for the current user
   */
  getActiveSessions(): Observable<ActiveSession[]> {
    return this.http.get<ActiveSession[]>(
      `${this.apiUrl}/api/auth/tokens`,
      { headers: this.getAuthHeaders() }
    );
  }

  /**
   * Revoke a specific token (logout from a specific device)
   */
  revokeToken(jti: string): Observable<any> {
    return this.http.delete(
      `${this.apiUrl}/api/auth/tokens/${jti}`,
      { headers: this.getAuthHeaders() }
    );
  }

  /**
   * Revoke all other tokens (logout from all other devices)
   */
  revokeAllOtherTokens(): Observable<any> {
    return this.http.delete(
      `${this.apiUrl}/api/auth/tokens/others`,
      { headers: this.getAuthHeaders() }
    );
  }
}

