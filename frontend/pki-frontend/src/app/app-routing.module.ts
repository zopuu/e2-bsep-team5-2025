import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RegistrationComponent } from './components/registration/registration.component';
import { LoginComponent } from './components/login/login.component';
import { ActivationComponent } from './components/activation/activation.component';
import { HomeComponent } from './components/home/home.component';
import { AdminComponent } from './components/admin/admin.component';
import { AdminGuard } from './guards/admin.guard';
import { AdminCaComponent } from './components/admin/admin-ca/admin-ca.component';
import { AdminIntermediateComponent } from './components/admin/admin-intermediate/admin-intermediate.component';
import { AdminDashboardComponent } from './components/admin/admin-dashboard/admin-dashboard.component';
import { AdminShellComponent } from './components/admin/admin-shell/admin-shell.component';
import { AdminCertificatesComponent } from './components/admin/admin-certificates/admin-certificates.component';
import { ForgotPasswordComponent } from './components/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './components/reset-password/reset-password.component';
import { AdminUsersComponent } from './components/admin/admin-users/admin-users.component';
import { CaIssuersComponent } from './components/ca/ca-issuers/ca-issuers.component';
import { CaGuard } from './guards/ca.guard';
import { CaShellComponent } from './components/ca/ca-shell/ca-shell.component';
import { CaIssueIntermediateComponent } from './components/ca/ca-issue-intermediate/ca-issue-intermediate.component';
import { ActiveSessionsComponent } from './components/active-sessions/active-sessions.component';
import { CsrUploadComponent } from './components/csr-upload/csr-upload.component';
import { MyCertificatesComponent } from './components/my-certificates/my-certificates.component';

const routes: Routes = [

  { path: '', redirectTo: '/register', pathMatch: 'full' },
  {
    path: 'admin',
    component: AdminShellComponent,           // NEW
    canActivate: [AdminGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: AdminDashboardComponent },     // NEW
      { path: 'ca', component: AdminCaComponent },                   // existing
      { path: 'ca/intermediate', component: AdminIntermediateComponent }, // existing
      { path: 'certificates', component: AdminCertificatesComponent },    // placeholder
      // { path: 'csr', component: AdminCsrQueueComponent },                 // placeholder
      // { path: 'templates', component: AdminTemplatesComponent },          // placeholder
      { path: 'users', component: AdminUsersComponent },
      // { path: 'revocations', component: AdminRevocationsComponent },      // placeholder
      // { path: 'logs', component: AdminLogsComponent },                    // placeholder
      // { path: 'security', component: AdminSecurityComponent }             // placeholder
    ]
  },
  { path: 'register', component: RegistrationComponent },
  { path: 'login', component: LoginComponent },
  { path: 'activate', component: ActivationComponent },
  { path: 'home', component: HomeComponent },
  { path: 'active-sessions', component: ActiveSessionsComponent },
  { path: 'csr-upload', component: CsrUploadComponent },
  { path: 'my-certificates', component: MyCertificatesComponent },
  { path: 'admin/ca', component: AdminCaComponent, canActivate: [AdminGuard] },
  { path: 'admin/ca/intermediate', component: AdminIntermediateComponent, canActivate: [AdminGuard] },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  {
    path: 'ca',
    component: CaShellComponent,   // reuse admin shell styling
    canActivate: [CaGuard],
    children: [
      { path: '', redirectTo: 'issuers', pathMatch: 'full' },
      { path: 'issuers', component: CaIssuersComponent },
      { path: 'issue-intermediate', component: CaIssueIntermediateComponent}
    ]
  },
  { path: '**', redirectTo: '/register' },

];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
