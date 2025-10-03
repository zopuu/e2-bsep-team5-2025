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
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.message = '';
    const payload: LoginRequest = this.loginForm.value;

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
      }
    });
  }
}


