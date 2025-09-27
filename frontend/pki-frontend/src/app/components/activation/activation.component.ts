import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-activation',
  templateUrl: './activation.component.html',
  styleUrls: ['./activation.component.scss']
})
export class ActivationComponent implements OnInit {
  
  isLoading = true;
  message = '';
  messageType: 'success' | 'error' = 'error';
  token: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token');
    
    if (this.token) {
      this.activateAccount();
    } else {
      this.isLoading = false;
      this.message = 'Invalid activation link. No token provided.';
      this.messageType = 'error';
    }
  }

  private activateAccount(): void {
    this.authService.activateAccount(this.token!).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.message = response.message;
        this.messageType = response.success ? 'success' : 'error';
        
        if (response.success) {
          // Redirect to login after 3 seconds
          setTimeout(() => {
            this.router.navigate(['/login']);
          }, 3000);
        }
      },
      error: (error) => {
        this.isLoading = false;
        this.message = error.error?.message || 'Activation failed. Please try again.';
        this.messageType = 'error';
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  goToRegistration(): void {
    this.router.navigate(['/register']);
  }
}