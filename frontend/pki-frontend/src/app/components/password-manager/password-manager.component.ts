import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { PasswordService } from '../../services/password.service';
import { PasswordEntry } from '../../models/password';

@Component({
  selector: 'app-password-manager',
  templateUrl: './password-manager.component.html',
  styleUrls: ['./password-manager.component.scss']
})
export class PasswordManagerComponent implements OnInit {
  passwords: PasswordEntry[] = [];
  filteredPasswords: PasswordEntry[] = [];
  loading = false;
  error = '';
  searchTerm = '';
  privateKeyFile: File | null = null;
  decryptedPasswords: Map<number, string> = new Map();

  constructor(
    private passwordService: PasswordService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadPasswords();
  }

  loadPasswords(): void {
    this.loading = true;
    this.error = '';

    this.passwordService.getPasswords().subscribe({
      next: (passwords) => {
        this.passwords = passwords;
        this.filteredPasswords = passwords;
        this.loading = false;
      },
      error: (error) => {
        this.error = 'Failed to load passwords';
        this.loading = false;
        console.error('Error loading passwords:', error);
      }
    });
  }

  onSearchChange(): void {
    if (!this.searchTerm.trim()) {
      this.filteredPasswords = this.passwords;
      return;
    }

    this.passwordService.searchPasswords(this.searchTerm).subscribe({
      next: (passwords) => {
        this.filteredPasswords = passwords;
      },
      error: (error) => {
        this.error = 'Search failed';
        console.error('Search error:', error);
      }
    });
  }

  onPrivateKeyFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.privateKeyFile = file;
    }
  }

  async decryptPassword(passwordEntry: PasswordEntry): Promise<void> {
    if (!this.privateKeyFile) {
      this.error = 'Please select a private key file first';
      return;
    }

    try {
      const privateKeyPem = await this.passwordService.readPrivateKeyFromFile(this.privateKeyFile);
      const decryptedPassword = await this.passwordService.decryptPasswordLocally(
        passwordEntry.encryptedPassword,
        privateKeyPem
      );
      
      this.decryptedPasswords.set(passwordEntry.id, decryptedPassword);
      
    } catch (error) {
      this.error = 'Failed to decrypt password';
      console.error('Decryption error:', error);
    }
  }

  getDecryptedPassword(passwordEntry: PasswordEntry): string {
    return this.decryptedPasswords.get(passwordEntry.id) || '••••••••';
  }

  isPasswordDecrypted(passwordEntry: PasswordEntry): boolean {
    return this.decryptedPasswords.has(passwordEntry.id);
  }

  copyToClipboard(text: string): void {
    navigator.clipboard.writeText(text).then(() => {
      // Could show a toast notification here
      console.log('Password copied to clipboard');
    });
  }

  deletePassword(passwordEntry: PasswordEntry): void {
    if (!confirm(`Are you sure you want to delete the password for ${passwordEntry.siteName}?`)) {
      return;
    }

    this.passwordService.deletePassword(passwordEntry.id).subscribe({
      next: () => {
        this.loadPasswords(); // Reload the list
      },
      error: (error) => {
        this.error = 'Failed to delete password';
        console.error('Delete error:', error);
      }
    });
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString();
  }
}
