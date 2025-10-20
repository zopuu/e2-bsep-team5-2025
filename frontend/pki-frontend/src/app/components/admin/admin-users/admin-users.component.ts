import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AdminApiService } from '../../../services/admin.service';
import { UserDto, CreateCaUserRequest } from '../../../models/user';

@Component({
  selector: 'app-admin-users',
  templateUrl: './admin-users.component.html',
  styleUrls: ['./admin-users.component.scss']
})
export class AdminUsersComponent implements OnInit {
  loadingList = false;
  creating = false;
  users: UserDto[] = [];
  filtered: UserDto[] = [];
  q = '';

  form!: FormGroup;
  toast: { type: 'success'|'error'|'', text: string } = { type: '', text: '' };

  constructor(private fb: FormBuilder, private api: AdminApiService) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      firstName: ['', [Validators.required, Validators.maxLength(64)]],
      lastName: ['', [Validators.required, Validators.maxLength(64)]],
      organization: ['', [Validators.required, Validators.maxLength(128)]],
    });

    this.loadUsers();
  }

  loadUsers(): void {
    this.loadingList = true;
    this.api.listCaUsers().subscribe({
      next: (res) => {
        this.users = res;
        this.applyFilter();
        this.loadingList = false;
      },
      error: () => {
        this.toast = { type: 'error', text: 'Failed to load CA users.' };
        this.loadingList = false;
      }
    });
  }

  applyFilter(): void {
    const q = (this.q || '').toLowerCase().trim();
    if (!q) {
      this.filtered = this.users.slice();
      return;
    }
    this.filtered = this.users.filter(u =>
      [u.email, u.firstName, u.lastName, u.organization]
        .filter(Boolean)
        .some(v => v.toLowerCase().includes(q))
    );
  }

  clearToastSoon() {
    setTimeout(() => (this.toast = { type: '', text: '' }), 2500);
  }

  createCaUser(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const payload: CreateCaUserRequest = this.form.value;
    this.creating = true;
    this.api.createCaUser(payload).subscribe({
      next: (res) => {
        this.toast = { type: 'success', text: res.message || 'CA user invited.' };
        this.creating = false;
        this.form.reset();
        this.loadUsers();
        this.clearToastSoon();
      },
      error: (err) => {
        this.toast = { type: 'error', text: err?.error?.message || 'Failed to create CA user.' };
        this.creating = false;
        this.clearToastSoon();
      }
    });
  }
}
