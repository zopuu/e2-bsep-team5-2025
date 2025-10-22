import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PasswordEntry, PasswordEntryRequest, DecryptRequest, DecryptResponse } from '../models/password';

@Injectable({
  providedIn: 'root'
})
export class PasswordService {
  private apiUrl = 'http://localhost:8080/api/passwords';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  /**
   * Get all passwords for the current user
   */
  getPasswords(): Observable<PasswordEntry[]> {
    return this.http.get<PasswordEntry[]>(
      this.apiUrl,
      { headers: this.getAuthHeaders() }
    );
  }

  /**
   * Create a new password entry
   */
  createPassword(request: PasswordEntryRequest): Observable<PasswordEntry> {
    return this.http.post<PasswordEntry>(
      this.apiUrl,
      request,
      { headers: this.getAuthHeaders() }
    );
  }

  /**
   * Delete a password entry
   */
  deletePassword(id: number): Observable<any> {
    return this.http.delete(
      `${this.apiUrl}/${id}`,
      { headers: this.getAuthHeaders() }
    );
  }

  /**
   * Search passwords
   */
  searchPasswords(query: string): Observable<PasswordEntry[]> {
    return this.http.get<PasswordEntry[]>(
      `${this.apiUrl}/search?q=${encodeURIComponent(query)}`,
      { headers: this.getAuthHeaders() }
    );
  }

  /**
   * Decrypt password using Web Crypto API
   */
  decryptPassword(encryptedPassword: string, privateKeyPem: string): Observable<DecryptResponse> {
    const request: DecryptRequest = {
      encryptedPassword,
      privateKeyPem
    };

    return this.http.post<DecryptResponse>(
      `${this.apiUrl}/decrypt`,
      request,
      { headers: this.getAuthHeaders() }
    );
  }

  /**
   * Decrypt password locally using Web Crypto API
   */
  async decryptPasswordLocally(encryptedPassword: string, privateKeyPem: string): Promise<string> {
    try {
      // Import private key
      const privateKey = await this.importPrivateKey(privateKeyPem);
      
      // Decode base64 encrypted password
      const encryptedData = this.base64ToArrayBuffer(encryptedPassword);
      
      // Decrypt using Web Crypto API
      const decryptedData = await window.crypto.subtle.decrypt(
        {
          name: 'RSA-OAEP'
        },
        privateKey,
        encryptedData
      );
      
      // Convert to string
      return new TextDecoder().decode(decryptedData);
      
    } catch (error) {
      console.error('Decryption failed:', error);
      throw new Error('Failed to decrypt password');
    }
  }

  /**
   * Import private key from PEM format
   */
  private async importPrivateKey(privateKeyPem: string): Promise<CryptoKey> {
    try {
      // Remove PEM headers and decode base64
      const pemHeader = '-----BEGIN PRIVATE KEY-----';
      const pemFooter = '-----END PRIVATE KEY-----';
      const pemContents = privateKeyPem
        .replace(pemHeader, '')
        .replace(pemFooter, '')
        .replace(/\s/g, '');
      
      const binaryDer = this.base64ToArrayBuffer(pemContents);
      
      // Import the key
      return await window.crypto.subtle.importKey(
        'pkcs8',
        binaryDer,
        {
          name: 'RSA-OAEP',
          hash: 'SHA-1'
        },
        false,
        ['decrypt']
      );
      
    } catch (error) {
      console.error('Failed to import private key:', error);
      throw new Error('Invalid private key format');
    }
  }

  /**
   * Convert base64 string to ArrayBuffer
   */
  private base64ToArrayBuffer(base64: string): ArrayBuffer {
    const binaryString = window.atob(base64);
    const bytes = new Uint8Array(binaryString.length);
    for (let i = 0; i < binaryString.length; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes.buffer;
  }

  /**
   * Read private key from file
   */
  async readPrivateKeyFromFile(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const content = e.target?.result as string;
        resolve(content);
      };
      reader.onerror = () => reject(new Error('Failed to read file'));
      reader.readAsText(file);
    });
  }
}
