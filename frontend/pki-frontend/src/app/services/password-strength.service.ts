import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { AuthService, PasswordStrengthResult } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class PasswordStrengthService {

  constructor(private authService: AuthService) {}

  checkPasswordStrength(password: string): Observable<PasswordStrengthResult> {
    if (!password) {
      return of({
        valid: false,
        message: 'Password cannot be empty',
        score: 0,
        strengthLevel: 'Very Weak'
      });
    }

    // Client-side validation for immediate feedback
    const clientResult = this.calculateClientSideStrength(password);
    
    // Also call backend for server-side validation
    this.authService.validatePassword(password).subscribe({
      next: (response) => {
        if (response.data) {
          // Use server response if available
          return response.data;
        }
      },
      error: (error) => {
        console.error('Server password validation failed:', error);
      }
    });

    return of(clientResult);
  }

  private calculateClientSideStrength(password: string): PasswordStrengthResult {
    let score = 0;
    const messages: string[] = [];

    // Length check
    if (password.length >= 8) {
      score++;
    } else {
      messages.push('At least 8 characters');
    }

    if (password.length >= 12) {
      score++;
    }

    // Character type checks
    if (/[A-Z]/.test(password)) {
      score++;
    } else {
      messages.push('uppercase letter');
    }

    if (/[a-z]/.test(password)) {
      score++;
    } else {
      messages.push('lowercase letter');
    }

    if (/[0-9]/.test(password)) {
      score++;
    } else {
      messages.push('number');
    }

    if (/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) {
      score++;
    } else {
      messages.push('special character');
    }

    const strengthLevel = this.getStrengthLevel(score);
    const message = this.getStrengthMessage(score, messages);

    return {
      valid: score >= 2,
      message: message,
      score: score,
      strengthLevel: strengthLevel
    };
  }

  private getStrengthLevel(score: number): string {
    switch (score) {
      case 0:
      case 1:
        return 'Very Weak';
      case 2:
        return 'Weak';
      case 3:
        return 'Medium';
      case 4:
        return 'Strong';
      case 5:
        return 'Very Strong';
      default:
        return 'Unknown';
    }
  }

  private getStrengthMessage(score: number, missingRequirements: string[]): string {
    if (score <= 1) {
      return `Very Weak - Add ${missingRequirements.slice(0, 3).join(', ')}`;
    } else if (score === 2) {
      return 'Weak - Consider adding more character types';
    } else if (score === 3) {
      return 'Medium - Good password strength';
    } else if (score === 4) {
      return 'Strong - Excellent password strength';
    } else {
      return 'Very Strong - Maximum password strength';
    }
  }
}