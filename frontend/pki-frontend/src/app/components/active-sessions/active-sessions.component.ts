import { Component, OnInit } from '@angular/core';
import { TokenService, ActiveSession } from '../../services/token.service';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-active-sessions',
  templateUrl: './active-sessions.component.html',
  styleUrls: ['./active-sessions.component.scss']
})
export class ActiveSessionsComponent implements OnInit {
  sessions: ActiveSession[] = [];
  loading = false;
  error = '';
  revokingJti: string | null = null;

  constructor(
    private tokenService: TokenService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadSessions();
  }

  loadSessions(): void {
    this.loading = true;
    this.error = '';
    
    this.tokenService.getActiveSessions().subscribe({
      next: (sessions) => {
        this.sessions = sessions;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load active sessions';
        this.loading = false;
        console.error('Error loading sessions:', err);
      }
    });
  }

  revokeSession(jti: string): void {
    if (!confirm('Are you sure you want to log out from this device?')) {
      return;
    }

    this.revokingJti = jti;
    
    this.tokenService.revokeToken(jti).subscribe({
      next: () => {
        // If revoking current session, log out
        const currentSession = this.sessions.find(s => s.jti === jti && s.current);
        if (currentSession) {
          this.authService.logout();
          this.router.navigate(['/login']);
        } else {
          // Reload sessions to update the list
          this.loadSessions();
        }
        this.revokingJti = null;
      },
      error: (err) => {
        this.error = 'Failed to revoke session';
        this.revokingJti = null;
        console.error('Error revoking session:', err);
      }
    });
  }

  revokeAllOtherSessions(): void {
    if (!confirm('Are you sure you want to log out from all other devices?')) {
      return;
    }

    this.tokenService.revokeAllOtherTokens().subscribe({
      next: () => {
        // Reload sessions to update the list
        this.loadSessions();
      },
      error: (err) => {
        this.error = 'Failed to revoke other sessions';
        console.error('Error revoking other sessions:', err);
      }
    });
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getCurrentSession(): ActiveSession | undefined {
    return this.sessions.find(s => s.current);
  }

  getOtherSessions(): ActiveSession[] {
    return this.sessions.filter(s => !s.current);
  }
}

