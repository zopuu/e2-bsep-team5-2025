import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, LoginRequest, LoginResponse } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  isLoading = false;
  message = '';
  messageType: 'success' | 'error' | '' = '';
  showPassword = false;
  
  // Custom CAPTCHA
  captchaQuestion: string = '';
  captchaAnswer: string = '';
  captchaSolved: boolean = false;
  captchaError: string = '';
  private captchaCorrectAnswer: number = 0;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    });

    // Generate initial CAPTCHA
    this.generateCaptcha();
  }

  generateCaptcha(): void {
    const operations = ['+', '-', '*'];
    const operation = operations[Math.floor(Math.random() * operations.length)];
    let num1: number, num2: number, answer: number;

    switch (operation) {
      case '+':
        num1 = Math.floor(Math.random() * 20) + 1;
        num2 = Math.floor(Math.random() * 20) + 1;
        answer = num1 + num2;
        break;
      case '-':
        num1 = Math.floor(Math.random() * 20) + 10;
        num2 = Math.floor(Math.random() * num1) + 1;
        answer = num1 - num2;
        break;
      case '*':
        num1 = Math.floor(Math.random() * 10) + 1;
        num2 = Math.floor(Math.random() * 10) + 1;
        answer = num1 * num2;
        break;
      default:
        num1 = 5;
        num2 = 3;
        answer = 8;
    }

    this.captchaQuestion = `${num1} ${operation} ${num2}`;
    this.captchaCorrectAnswer = answer;
    this.captchaAnswer = '';
    this.captchaSolved = false;
    this.captchaError = '';
    console.log(`Generated CAPTCHA: ${this.captchaQuestion} = ${answer}`);
  }

  checkCaptchaAnswer(): void {
    if (!this.captchaAnswer || this.captchaAnswer === '') {
      this.captchaSolved = false;
      this.captchaError = '';
      return;
    }

    const userAnswer = parseInt(String(this.captchaAnswer));
    if (!isNaN(userAnswer) && userAnswer === this.captchaCorrectAnswer) {
      this.captchaSolved = true;
      this.captchaError = '';
      console.log('CAPTCHA solved correctly!');
    } else {
      this.captchaSolved = false;
      this.captchaError = 'Wrong answer! Try again.';
    }
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    if (!this.captchaSolved) {
      this.messageType = 'error';
      this.message = 'Please solve the CAPTCHA correctly';
      return;
    }

    this.isLoading = true;
    this.message = '';
    const payload: LoginRequest = { ...this.loginForm.value, captchaToken: 'custom-arithmetic-solved' };

    this.authService.login(payload).subscribe({
      next: (res: LoginResponse) => {
        this.isLoading = false;
        localStorage.setItem('token', res.token);
        this.messageType = 'success';
        this.message = 'Login successful';

        // Redirect by role
        const role = this.authService.getRole();
        if (role === 'ROLE_ADMIN' || role === 'ADMIN') {
          this.router.navigate(['/admin']);
        } else {
          this.router.navigate(['/home']);
        }
      },

      error: (err) => {
        this.isLoading = false;
        this.messageType = 'error';
        this.message = err.error?.message || 'Invalid credentials';
        // Reset CAPTCHA on error
        this.generateCaptcha();
      }
    });
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }
}
