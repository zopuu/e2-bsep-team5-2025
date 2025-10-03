import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface RegistrationRequest {
  email: string;
  password: string;
  confirmPassword: string;
  firstName: string;
  lastName: string;
  organization: string;
}

export interface ApiResponse {
  success: boolean;
  message: string;
  data?: any;
}

export interface PasswordStrengthResult {
  valid: boolean;
  message: string;
  score: number;
  strengthLevel: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) { }

  register(registrationData: RegistrationRequest): Observable<ApiResponse> {
    return this.http.post<ApiResponse>(`${this.apiUrl}/auth/register`, registrationData);
  }

  activateAccount(token: string): Observable<ApiResponse> {
    return this.http.post<ApiResponse>(`${this.apiUrl}/auth/activate`, null, {
      params: { token }
    });
  }

  validatePassword(password: string): Observable<ApiResponse> {
    return this.http.post<ApiResponse>(`${this.apiUrl}/auth/validate-password`, password);
  }

  login(payload: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/auth/login`, payload);
  }
  getToken(): string | null {
    return localStorage.getItem('token');
  }
  isAuthenticated(): boolean {
    return !!this.getToken();
  }
  // === JWT helpers ===
  private decodeJwt<T = any>(token: string): T | null {
    try {
      const payload = token.split('.')[1];
      const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(decodeURIComponent(escape(json)));
    } catch {
      return null;
    }
  }

  /** Vrati rolu iz tokena. Backend najčešće stavlja "roles": ["ROLE_ADMIN"] ili "role": "ROLE_ADMIN" */
  getRole(): string | null {
    const token = this.getToken();
    if (!token) return null;
    const payload = this.decodeJwt<any>(token);
    if (!payload) return null;

    // POKRIJ OBA FORMATa:
    if (Array.isArray(payload.roles) && payload.roles.length) {
      return payload.roles[0];
    }
    if (typeof payload.role === 'string') {
      return payload.role;
    }
    // ili ako backend šalje bez prefiksa:
    if (typeof payload.authority === 'string') return payload.authority;
    return null;
  }

  isAdmin(): boolean {
    const r = this.getRole();
    return r === 'ROLE_ADMIN' || r === 'ADMIN';
  }
}